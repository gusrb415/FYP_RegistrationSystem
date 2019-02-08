package main

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import viewmodel.Config

import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.*
import java.math.BigInteger
import java.net.InetAddress
import java.nio.file.Files
import java.security.*
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import java.util.Date
import java.util.Random


object SecurityHelper {
    internal val random: Random = SecureRandom()


    init {
        Security.addProvider(BouncyCastleProvider())
    }


    //reference https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
    @Throws(NoSuchAlgorithmException::class)
    fun hash(data: ByteArray, algo: String): ByteArray {
        val digest = MessageDigest.getInstance(algo)
        return digest.digest(data)

    }

    // SHA256withECDSA for EC & SHA256withRSA for RSA
    //Check whether the encrypt(signature) equals to the hash of content
    //reference: http://www.java2s.com/Tutorial/Java/0490__Security/SimpleDigitalSignatureExample.htm
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    fun checkDigitalSignature(
        signerPublicKey: PublicKey,
        signature: ByteArray,
        content: ByteArray,
        algo: String
    ): Boolean {
        var valid = false
        val sig = Signature.getInstance(algo)
        sig.initVerify(signerPublicKey)
        sig.update(content)
        valid = sig.verify(signature)

        return valid
    }

    // SHA256withECDSA for EC & SHA256withRSA for RSA
    //For now, just convert the byte values of the content into string (May be changed later)
    @Throws(SignatureException::class, InvalidKeyException::class, NoSuchAlgorithmException::class)
    fun createDigitalSignature(signerPrivateKey: PrivateKey, content: ByteArray, algo: String): ByteArray {

        val sig = Signature.getInstance(algo)
        sig.initSign(signerPrivateKey)
        sig.update(content)

        return sig.sign()

    }


    // SHA256withECDSA implementation
    // input content unlike createECDSASignatureWithHash - automatically hashes
    @Throws(NoSuchAlgorithmException::class)
    fun verifyECDSASignatureWithContent(
        signerPublicKey: ECPublicKey,
        content: ByteArray,
        signature: ByteArray,
        hashAlgo: String
    ): Boolean {

        val hash = hash(content, hashAlgo)

        val r = Arrays.copyOfRange(signature, 0, signature.size / 2)
        val s = Arrays.copyOfRange(signature, signature.size / 2, signature.size)

        val ecParameterSpec = ECNamedCurveTable.getParameterSpec(Config.ELIPTIC_CURVE)
        val affineX = signerPublicKey.w.affineX
        val affineY = signerPublicKey.w.affineY
        val curve = ecParameterSpec.curve

        val domainParameters = ECDomainParameters(curve, ecParameterSpec.g, ecParameterSpec.n, ecParameterSpec.h)
        val publicKeyParameters = ECPublicKeyParameters(curve.createPoint(affineX, affineY), domainParameters)

        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(false, publicKeyParameters)

        return ecdsaSigner.verifySignature(hash, BigInteger(1, r), BigInteger(1, s))

    }


    // SHA256withECDSA implementation
    // input content unlike createECDSASignatureWithHash - automatically hashes
    @Throws(NoSuchAlgorithmException::class)
    fun createECDSASignatureWithContent(
        signerPrivateKey: ECPrivateKey,
        content: ByteArray,
        hashAlgo: String
    ): ByteArray {

        val hash = hash(content, hashAlgo)
        val ecParameterSpec = ECNamedCurveTable.getParameterSpec(Config.ELIPTIC_CURVE)
        val curve = ecParameterSpec.curve
        val domainParameters = ECDomainParameters(curve, ecParameterSpec.g, ecParameterSpec.n, ecParameterSpec.h)
        val privateKeyParameters = ECPrivateKeyParameters(signerPrivateKey.s, domainParameters)

        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(true, privateKeyParameters)

        val bigIntegers = ecdsaSigner.generateSignature(hash)

        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            for (bigInteger in bigIntegers) {
                val tempBytes = bigInteger.toByteArray()
                if (tempBytes.size == 31) {
                    byteArrayOutputStream.write(0)
                } else if (tempBytes.size == 32) {
                    byteArrayOutputStream.write(tempBytes)
                } else {
                    byteArrayOutputStream.write(tempBytes, tempBytes.size - 32, 32)
                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return byteArrayOutputStream.toByteArray()

    }

    // NONEwithECDSA implementation
    // input "hash" not the content
    @Throws(IOException::class)
    fun createECDSASignatureWithHash(signerPrivateKey: ECPrivateKey, hash: ByteArray): ByteArray {

        val ecParameterSpec = ECNamedCurveTable.getParameterSpec(Config.ELIPTIC_CURVE)
        val curve = ecParameterSpec.curve
        val domainParameters = ECDomainParameters(curve, ecParameterSpec.g, ecParameterSpec.n, ecParameterSpec.h)
        val privateKeyParameters = ECPrivateKeyParameters(signerPrivateKey.s, domainParameters)

        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(true, privateKeyParameters)

        val bigIntegers = ecdsaSigner.generateSignature(hash)

        val byteArrayOutputStream = ByteArrayOutputStream()
        for (bigInteger in bigIntegers) {
            val tempBytes = bigInteger.toByteArray()
            println(tempBytes.size)
            if (tempBytes.size == 31) {
                byteArrayOutputStream.write(0)
            } else if (tempBytes.size == 32) {
                byteArrayOutputStream.write(tempBytes)
            } else {
                byteArrayOutputStream.write(tempBytes, tempBytes.size - 32, 32)
            }

        }

        return byteArrayOutputStream.toByteArray()

    }

    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        NoSuchProviderException::class
    )
    fun getAESKeyFromFile(fileName: String): SecretKey {

        return SecretKeySpec(Files.readAllBytes(File(fileName).toPath()), "AES")
    }

    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun generateAESIV(): ByteArray {
        val ivSize = 16
        val iv = ByteArray(ivSize)
        val random = SecureRandom()
        random.nextBytes(iv)
        return iv
    }

    fun getX509FromBytes(bytes: ByteArray): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X509")
        val bis = ByteArrayInputStream(bytes)
        val cert = certificateFactory.generateCertificate(bis) as X509Certificate
        bis.close()

        return cert
    }

    fun encryptAES(content: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(secretKey.encoded, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)
        return cipher.doFinal(content)
    }

    fun decryptAES(encrypted: ByteArray, secretKey: SecretKey, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(secretKey.encoded, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)
        return cipher.doFinal(encrypted)
    }

    fun generateECKeyPair(curve: String): KeyPair {
        Security.addProvider(BouncyCastleProvider())
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec(curve), SecureRandom())
        return keyGen.generateKeyPair()
    }

    //reference: https://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
    @Throws(NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class, NoSuchProviderException::class)
    fun generateRSAKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    @Throws(CertificateEncodingException::class)
    fun getSubjectCNFromX509Certificate(cert: X509Certificate): String {
        val x500name = JcaX509CertificateHolder(cert).subject
        val cn = x500name.getRDNs(BCStyle.CN)[0]
        return IETFUtils.valueToString(cn.first.value)
    }

    @Throws(CertificateEncodingException::class)
    fun getIssuerCNFromX509Certificate(cert: X509Certificate): String {
        val x500name = JcaX509CertificateHolder(cert).issuer
        val cn = x500name.getRDNs(BCStyle.CN)[0]
        return IETFUtils.valueToString(cn.first.value)
    }

    //reference: https://stackoverflow.com/questions/16412315/creating-custom-x509-v3-extensions-in-java-with-bouncy-castle
    @Throws(CertificateException::class, OperatorCreationException::class, IOException::class)
    fun issueCertificate(
        newPublicKey: PublicKey,
        issuerPublicKey: ECPublicKey,
        issuerPrivateKey: PrivateKey,
        noAfter: Date,
        subjectName: String,
        authorityName: String,
        subjectKeyIdentifierBytes: ByteArray,
        authorityKeyIdentifierBytes: ByteArray,
        ipAddress: InetAddress?,
        algo: String,
        isForSigning: Boolean
    ): X509Certificate {
        val builder = X509v3CertificateBuilder(
            X500Name("CN=$authorityName"),
            BigInteger.valueOf(Random().nextInt().toLong()),
            Date(),
            noAfter,
            X500Name("CN=$subjectName"),
            SubjectPublicKeyInfo.getInstance(newPublicKey.encoded)
        )

        val authorityKeyIdentifier = AuthorityKeyIdentifier(authorityKeyIdentifierBytes)
        builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier)
        if (isForSigning) {
            val subjectKeyIdentifier = SubjectKeyIdentifier(subjectKeyIdentifierBytes)
            builder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier)
        }

        if (isForSigning) {
            val usage = KeyUsage(KeyUsage.keyCertSign or KeyUsage.digitalSignature)
            builder.addExtension(Extension.keyUsage, false, usage.encoded)
        } else {
            val usage = KeyUsage(KeyUsage.digitalSignature)
            builder.addExtension(Extension.keyUsage, false, usage.encoded)
        }


        //		builder.addExtension(Extension.keyUsage,true,new KeyUsage(KeyUsage.digitalSignature|KeyUsage.keyEncipherment));

        if (ipAddress != null) {
            val generalName = arrayOf(GeneralName(GeneralName.iPAddress, ipAddress.hostAddress))
            builder.addExtension(Extension.subjectAlternativeName, false, DERSequence(generalName))
        }


        return JcaX509CertificateConverter().getCertificate(
            builder
                .build(JcaContentSignerBuilder(algo).setProvider("BC").build(issuerPrivateKey))
        )

    }


    @Throws(IOException::class)
    fun writePublicKeyToPEM(publicKey: PublicKey, fileName: String) {
        val writer = FileWriter(fileName)
        val pemWriter = PemWriter(writer)
        pemWriter.writeObject(PemObject("PUBLIC KEY", publicKey.encoded))
        pemWriter.close()
    }

    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        NoSuchProviderException::class
    )
    fun getPublicKeyFromPEM(file: File, algo: String): PublicKey {
        val reader = FileReader(file)
        val pemReader = PemReader(reader)
        val keyFactory = KeyFactory.getInstance(algo)
        val content = pemReader.readPemObject().content
        pemReader.close()

        return keyFactory.generatePublic(X509EncodedKeySpec(content))
    }

    @Throws(IOException::class)
    fun writePrivateKeyToPEM(privateKey: PrivateKey, fileName: String, algo: String) {
        val writer = FileWriter(fileName)
        val pemWriter = PemWriter(writer)
        pemWriter.writeObject(PemObject("$algo PRIVATE KEY", privateKey.encoded))
        pemWriter.close()
    }

    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        NoSuchProviderException::class
    )
    fun getPrivateFromPEM(fileName: String, algo: String): PrivateKey {
        val reader = FileReader(fileName)
        val pemReader = PemReader(reader)
        val keyFactory = KeyFactory.getInstance(algo)
        val content = pemReader.readPemObject().content
        pemReader.close()
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(content))
    }

    @Throws(IOException::class, CertificateEncodingException::class)
    fun writeX509ToDER(cert: X509Certificate, file: File) {
        val fos = FileOutputStream(file)
        fos.write(cert.encoded)
        fos.close()
    }

    //referece: https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.util.io.pem.PemReader
    @Throws(IOException::class, CertificateException::class, NoSuchProviderException::class)
    fun getX509FromDER(file: File): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X509")
        val fis = FileInputStream(file)
        val bis = ByteArrayInputStream(fis.readBytes())
        val cert = certificateFactory.generateCertificate(bis) as X509Certificate

        bis.close()

        return cert
    }


    fun getIssuerIdentifierFromX509Cert(cert: X509Certificate): ByteArray {
        val bytes = cert.getExtensionValue(Extension.authorityKeyIdentifier.id)
        val octets = ASN1OctetString.getInstance(bytes).octets
        val authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(octets)
        return authorityKeyIdentifier.keyIdentifier
    }

    // SHA256withECDSA implementation
    // input content unlike createRawECDSASignatureWithHash - automatically hashes
    @Throws(NoSuchAlgorithmException::class)
    fun verifyRawECDSASignatureWithContent(
        signerPublicKey: ECPublicKey,
        content: ByteArray,
        signature: ByteArray,
        hashAlgo: String,
        curveName: String
    ): Boolean {

        val hash = hash(content, hashAlgo)

        val r = Arrays.copyOfRange(signature, 0, signature.size / 2)
        val s = Arrays.copyOfRange(signature, signature.size / 2, signature.size)

        val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName)
        val affineX = signerPublicKey.w.affineX
        val affineY = signerPublicKey.w.affineY
        val curve = ecParameterSpec.curve

        val domainParameters = ECDomainParameters(curve, ecParameterSpec.g, ecParameterSpec.n, ecParameterSpec.h)
        val publicKeyParameters = ECPublicKeyParameters(curve.createPoint(affineX, affineY), domainParameters)

        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(false, publicKeyParameters)

        return ecdsaSigner.verifySignature(hash, BigInteger(1, r), BigInteger(1, s))

    }

    //https://stackoverflow.com/questions/33218674/how-to-make-a-bouncy-castle-ecpublickey
    fun getCompressedRawECPublicKey(publicKey: ECPublicKey): ByteArray {

        val affineX = publicKey.w.affineX
        val affineY = publicKey.w.affineY
        val curve = ECNamedCurveTable.getParameterSpec(Config.ELIPTIC_CURVE).curve

        return curve.createPoint(affineX, affineY).getEncoded(true)
    }

    //https://bitcoin.stackexchange.com/questions/44024/get-uncompressed-public-key-from-compressed-form
    @Throws(InvalidKeySpecException::class)
    fun getECPublicKeyFromCompressedRaw(raw: ByteArray): ECPublicKey? {
        val parameterSpec = ECNamedCurveTable.getParameterSpec(Config.ELIPTIC_CURVE)
        val point = parameterSpec.curve.decodePoint(raw)

        try {
            return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, parameterSpec)) as ECPublicKey
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return null // not expected
    }
}
