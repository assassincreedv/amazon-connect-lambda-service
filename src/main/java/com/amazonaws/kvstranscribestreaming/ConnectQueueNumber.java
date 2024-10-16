package com.amazonaws.kvstranscribestreaming;


public class ConnectQueueNumber {
    String outBoundNum = null;

    public ConnectQueueNumber() {}

    public ConnectQueueNumber(String outBoundNum) {
        this.outBoundNum = outBoundNum;
    }

    public String getOutBoundNum() { return outBoundNum; }

    public void setOutBoundNum(String outBoundNum) { this.outBoundNum = outBoundNum; }
}
