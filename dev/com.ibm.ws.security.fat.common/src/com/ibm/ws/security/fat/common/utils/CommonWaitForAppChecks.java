package com.ibm.ws.security.fat.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.MessageConstants;

public class CommonWaitForAppChecks {

    public static List<String> getSSLChannelReadyMsgs() throws Exception {
        List<String> waitForMessages = new ArrayList<String>();
        return getSSLChannelReadyMsgs(waitForMessages);
    }

    public static List<String> getSSLChannelReadyMsgs(List<String> waitForMessages) throws Exception {

        waitForMessages.add(MessageConstants.CWWKO0219I_SSL_CHANNEL_READY);

        return waitForMessages;
    }
}