/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.logging.collector;

public interface Formatter {

    /*
     * Collector will implement this method to return a formatted
     * event. Event should be formatted in such a way that it is fit to be consumed
     * by target.
     */
    public abstract Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength);
}
