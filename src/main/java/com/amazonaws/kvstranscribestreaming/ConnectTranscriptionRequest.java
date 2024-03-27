package com.amazonaws.kvstranscribestreaming;


public class ConnectTranscriptionRequest {
    String streamARN = null;

    String startFragmentNum = null;

    String connectContactId = null;

    public ConnectTranscriptionRequest(String streamARN, String startFragmentNum, String connectContactId) {
        this.streamARN = streamARN;
        this.startFragmentNum = startFragmentNum;
        this.connectContactId = connectContactId;
    }

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
}
