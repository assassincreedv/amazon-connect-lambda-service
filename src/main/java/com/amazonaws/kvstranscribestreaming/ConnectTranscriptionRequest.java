package com.amazonaws.kvstranscribestreaming;


public class ConnectTranscriptionRequest {
    String phone = null;

    String streamARN = null;

    String startFragmentNum = null;

    String connectContactId = null;

    String accountARN = null;

    public ConnectTranscriptionRequest(String phone, String streamARN, String startFragmentNum, String connectContactId, String accountARN) {
        this.phone = phone;
        this.streamARN = streamARN;
        this.startFragmentNum = startFragmentNum;
        this.connectContactId = connectContactId;
        this.accountARN = accountARN;
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getStreamARN() {
        return streamARN;
    }

    public void setStreamARN(String streamARN) {
        this.streamARN = streamARN;
    }

    public String getStartFragmentNum() {
        return startFragmentNum;
    }

    public void setStartFragmentNum(String startFragmentNum) {
        this.startFragmentNum = startFragmentNum;
    }

    public String getConnectContactId() {
        return connectContactId;
    }

    public void setConnectContactId(String connectContactId) {
        this.connectContactId = connectContactId;
    }

    public String getAccountARN() { return accountARN; }

    public void setAccountARN(String accountARN) { this.accountARN = accountARN; }
}
