/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.priority.helpers;

public class RelativePriority {

    /*
     * Alternatives have the concept of high vs. low priority.
     * The highest priority alternative will be used.
     */
    public static final int HIGH_PRIORITY = 100;
    public static final int LOW_PRIORITY = 10;

    /*
     * Interceptors and Decorators use priority for order.
     * Lower priorities are called first.
     */
    public static final int FIRST = 1;

    private RelativePriority() {}
}
