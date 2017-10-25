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
package com.ibm.ws.cdi.ejb.constructor.test;

/**
 *
 */
public class StaticState {

    private static StringBuilder state = new StringBuilder();

    public static void append(String str) {
        state.append(str);
    }

    public static String getOutput() {
        return state.toString();
    }
}
