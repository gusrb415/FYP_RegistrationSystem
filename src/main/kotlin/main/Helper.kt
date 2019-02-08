package main

import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.ImageView
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import java.security.PublicKey
import java.security.PrivateKey
import java.security.cert.Certificate
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import viewmodel.Config
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.*


object Helper {

    var token = ""

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generatePublicKey(keyEncoded: ByteArray) : ECPublicKey {
        val kf = KeyFactory.getInstance("EC", Helper.getProvider())
        return kf.generatePublic(X509EncodedKeySpec(keyEncoded)) as ECPublicKey
    }

    fun getProvider() : String {
        return BouncyCastleProvider.PROVIDER_NAME
    }

    fun serializeToString(key: Key): String {
        return encodeToString(serialize(key))
    }

    fun serialize(any: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(any)

        return baos.toByteArray()
    }

    fun deserialize(serializedString: String): Any {
        return deserialize(decodeFromString(serializedString))
    }

    fun deserialize(byteArray: ByteArray): Any {
        val bais = ByteArrayInputStream(byteArray)
        val ois = ObjectInputStream(bais)
        return ois.readObject()
    }

    fun encodeToString(byteArray: ByteArray): String {
        return Base64.getEncoder().encodeToString(byteArray)
    }

    fun decodeFromString(base64String: String): ByteArray {
        return Base64.getDecoder().decode(base64String)
    }

    fun generateKeyPair(algorithm: String, provider: String = "BC"): KeyPair {
        val keyGen = KeyPairGenerator.getInstance(algorithm, provider)
        val keySize = if(algorithm == "ECDSA" || algorithm == "EC") 256 else 2048
        if(algorithm == "EC")
            keyGen.initialize(ECGenParameterSpec("secp256k1"))
        else
            keyGen.initialize(keySize)
        return keyGen.generateKeyPair()
    }

    fun mergeByteArrays(vararg bs: ByteArray): ByteArray {
        val mergedList = ArrayList<Byte>()
        for (bytes in bs) {
            for (aByte in bytes)
                mergedList.add(aByte)
        }

        val mergedArray = ByteArray(mergedList.size)

        for (i in mergedArray.indices)
            mergedArray[i] = mergedList[i]

        return mergedArray
    }

    fun longToBytes(x: Long): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(x)
        return buffer.array()
    }

    fun getAESKey(key: PrivateKey, index: String) : SecretKey {
        return generateSecretKey((getHash(key.encoded) + index).toCharArray())
    }

    fun getKey(key: Key): String {
        return String.format("%064x", BigInteger(1, key.encoded))
    }

    fun encrypt(text: ByteArray, key: PublicKey, algorithm: String): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(text)
    }

    fun decrypt(text: ByteArray, key: PrivateKey, algorithm: String): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(text)
    }

    fun generateSecretKey(password: CharArray, provider: String = "BC"): SecretKey {
        val keyFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", provider)
        val hmacKey = keyFact.generateSecret(
            PBEKeySpec(password, Hex.decode("0102030405060708090a0b0c0d0e0f10"), 1024, 256))
        return SecretKeySpec(hmacKey.encoded, "AES")
    }

    fun getHash(bytes: ByteArray, provider: String = "BC"): String {
        return byteToHex(MessageDigest.getInstance("SHA-256", provider).digest(bytes))
    }

    fun byteToHex(byteArray: ByteArray): String {
        return String(Hex.encode(byteArray))
    }

    fun getHash(string: String, provider: String = "BC"): String {
        return getHash(string.toByteArray(), provider)
    }

    fun exportToFile(fileName: String, path: String, input: String): Boolean {
        return try {
            if (!File(path).exists())
                File(path).mkdir()
            File("$path/$fileName").createNewFile()
            File("$path/$fileName").writeText(input)
            true
        } catch (e : Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun selfSign(keyPair: KeyPair, subjectDN: String): Certificate {
        val now = System.currentTimeMillis()
        val startDate = Date(now)
        val certSerialNumber = BigInteger(java.lang.Long.toString(now))
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.YEAR, 100)
        val endDate = calendar.time

        val dnName = X500Name(subjectDN)
        val signatureAlgorithm = "SHA256WithRSA"
        val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        val contentSigner = JcaContentSignerBuilder(signatureAlgorithm).setProvider(getProvider()).build(keyPair.private)
        val certificateHolder = X509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, subjectPublicKeyInfo)
            .build(contentSigner)

        return JcaX509CertificateConverter().getCertificate(certificateHolder)
    }

    fun loadFromPKCS12(fileName: String): KeyPair {
        val password = "MediRec".toCharArray()
        val pkcs12KeyStore = KeyStore.getInstance("PKCS12")
        FileInputStream("$fileName.pkcs12").use { fis -> pkcs12KeyStore.load(fis, password) }
        val param = KeyStore.PasswordProtection(password)
        val entry = pkcs12KeyStore.getEntry("MediRec", param) as? KeyStore.PrivateKeyEntry
            ?: throw KeyStoreException("That's not a private key!")
        val publicKey = entry.certificate.publicKey
        val privateKey = entry.privateKey
        return KeyPair(publicKey, privateKey)
    }

    fun storeToPKCS12(fileName: String, keyPair: KeyPair) {
        val password = "MediRec".toCharArray()
        val selfSignedCertificate = selfSign(keyPair, "CN=MediRec")
        val pkcs12KeyStore = KeyStore.getInstance("PKCS12")
        pkcs12KeyStore.load(null, null)
        val entry = KeyStore.PrivateKeyEntry(keyPair.private, arrayOf(selfSignedCertificate))
        val param = KeyStore.PasswordProtection(password)
        pkcs12KeyStore.setEntry("MediRec", entry, param)
        FileOutputStream("$fileName.pkcs12").use { fos -> pkcs12KeyStore.store(fos, password) }
    }

    fun exportToFile(fileName: String, path: String, input: Key): Boolean {
        return try {
            if (!File(path).exists())
                File(path).mkdir()
            val objectOutputStream = ObjectOutputStream(File("$path/$fileName").outputStream())
            objectOutputStream.writeObject(input)
            true
        } catch (e : Exception) {
            false
        }
    }

    fun importObjectFile(path: String): Key? {
        return try {
            val objectInputStream = ObjectInputStream(File(path).inputStream())
            objectInputStream.readObject() as Key
        } catch (e : Exception) {
            null
        }
    }

    fun importTextFile(path: String): String {
        return try {
            File(path).readText()
        } catch (e : Exception) {
            ""
        }
    }

    fun findPrivateKeyFiles(): List<String>{
        val list = listOf<String>()
        val directory = File("./keys")
        if(!directory.exists() || !directory.isDirectory || directory.list() == null)
            return list
        return directory.list().toList()
    }

    fun generateSignature(privateKey: PrivateKey, input: ByteArray, algorithm: String, provider: String = "BC"): ByteArray {
        val signature = Signature.getInstance(algorithm, provider)
        signature.initSign(privateKey)
        signature.update(input)
        return signature.sign()
    }

    fun verifySignature(publicKey: PublicKey, input: ByteArray, encSignature: ByteArray, algorithm: String, provider: String = "BC"): Boolean {
        val signature = Signature.getInstance(algorithm, provider)
        signature.initVerify(publicKey)
        signature.update(input)
        return signature.verify(encSignature)
    }

    fun drawQRCode(qrView: ImageView, str: String) {
        val qrCodeWriter = QRCodeWriter()
        val width = Config.QRCODE_WIDTH.toInt()
        val height = Config.QRCODE_HEIGHT.toInt()

        val bufferedImage: BufferedImage
        try {
            val byteMatrix = qrCodeWriter.encode(str, BarcodeFormat.QR_CODE, width, height)
            bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            bufferedImage.createGraphics()

            val graphics = bufferedImage.graphics as Graphics2D
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.color = Color.BLACK

            for (i in 0 until height) {
                for (j in 0 until width) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1)
                    }
                }
            }
            qrView.image = SwingFXUtils.toFXImage(bufferedImage, null)
            println("Success...")
        } catch (e: WriterException) {
            println("Failed to create a QR Code")
        }
    }
}