/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi.test.managedbean;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CounterUtil {

    private static List<String> msgList = new ArrayList<String>();

    public static void addToMsgList(String str) {
        msgList.add(str);
    }

    public static List<String> getMsgList() {
        return msgList;
    }

}
