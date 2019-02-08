package pojo;


public class PatientInfoContentPojo {

    private byte[] encryptedInfo;

    public PatientInfoContentPojo() {
    }

    public PatientInfoContentPojo(byte[] encryptedInfo) {
        this.encryptedInfo = encryptedInfo;
    }

    public byte[] getEncryptedInfo() {
        return encryptedInfo;
    }

    public void setEncryptedInfo(byte[] encryptedInfo) {
        this.encryptedInfo = encryptedInfo;
    }

}
