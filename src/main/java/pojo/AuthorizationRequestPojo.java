package pojo;


import java.util.Date;

public class AuthorizationRequestPojo {

    private MedicalOrgInfoPojo medicalOrgInfo;
    private Date noAfter;


    public MedicalOrgInfoPojo getMedicalOrgInfo() {
        return medicalOrgInfo;
    }

    public void setMedicalOrgInfo(MedicalOrgInfoPojo medicalOrgInfo) {
        this.medicalOrgInfo = medicalOrgInfo;
    }

    public Date getNoAfter() {
        return noAfter;
    }

    public void setNoAfter(Date noAfter) {
        this.noAfter = noAfter;
    }
}
