package pojo;


public class VotingPojo {

    private AuthorityInfoPojo beneficiary;
    private boolean add; // true:add, false:remove
    private int agree;
    private int disagree;
    private boolean voted;

    public AuthorityInfoPojo getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(AuthorityInfoPojo beneficiary) {
        this.beneficiary = beneficiary;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public int getAgree() {
        return agree;
    }

    public void setAgree(int agree) {
        this.agree = agree;
    }

    public int getDisagree() {
        return disagree;
    }

    public void setDisagree(int disagree) {
        this.disagree = disagree;
    }

    public boolean isVoted() {
        return voted;
    }

    public void setVoted(boolean voted) {
        this.voted = voted;
    }

}
