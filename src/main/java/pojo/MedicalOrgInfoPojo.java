package pojo;

public class MedicalOrgInfoPojo {

    private String name;
    private byte[] ecPublicKey;
    private boolean keyDEREncoded; // true if DER encoded, false if raw (To not, simple calling getEncoded() gives DER encoded key)

    public MedicalOrgInfoPojo() {
    }

    public MedicalOrgInfoPojo(String name, byte[] ecPublicKey, boolean keyDEREncoded) {
        this.name = name;
        this.ecPublicKey = ecPublicKey;
        this.keyDEREncoded = keyDEREncoded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(byte[] ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public boolean isKeyDEREncoded() {
        return keyDEREncoded;
    }

    public void setKeyDEREncoded(boolean keyDEREncoded) {
        this.keyDEREncoded = keyDEREncoded;
    }
}
