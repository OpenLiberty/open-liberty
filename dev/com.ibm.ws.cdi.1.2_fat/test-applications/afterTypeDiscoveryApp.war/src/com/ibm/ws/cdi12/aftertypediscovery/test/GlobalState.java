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
package com.ibm.ws.cdi12.aftertypediscovery.test;

import java.util.LinkedList;
import java.util.List;

public class GlobalState {

    private static List<String> output = new LinkedList<String>();

    public static List<String> getOutput() {
        System.out.println("out" + output.size());
        return output;
    }

    public static void addOutput(String s) {
        System.out.println(s);
        output.add(s);
    }

}
