package pojo;


public class PatientInfoPojo {

    private long timestamp;
    private byte[] ecPublicKey;
    private byte[] encryptedInfo;
    private byte[] signature;
    private boolean keyDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded key)
    private boolean signatureDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded signature)

    public PatientInfoPojo() {
    }

    public PatientInfoPojo(long timestamp, byte[] ecPublicKey, byte[] encryptedInfo, byte[] signature, boolean keyDEREncoded, boolean signatureDEREncoded) {
        this.timestamp = timestamp;
        this.ecPublicKey = ecPublicKey;
        this.encryptedInfo = encryptedInfo;
        this.signature = signature;
        this.keyDEREncoded = keyDEREncoded;
        this.signatureDEREncoded = signatureDEREncoded;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(byte[] ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public byte[] getEncryptedInfo() {
        return encryptedInfo;
    }

    public void setEncryptedInfo(byte[] encryptedInfo) {
        this.encryptedInfo = encryptedInfo;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public boolean isKeyDEREncoded() {
        return keyDEREncoded;
    }

    public void setKeyDEREncoded(boolean keyDEREncoded) {
        this.keyDEREncoded = keyDEREncoded;
    }

    public boolean isSignatureDEREncoded() {
        return signatureDEREncoded;
    }

    public void setSignatureDEREncoded(boolean signatureDEREncoded) {
        this.signatureDEREncoded = signatureDEREncoded;
    }
}
