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
package com.ibm.ws.cdi.client.fat.counting;


/**
 * The event fired by {@link CountBean} when it's warning level is reached.
 */
public class CountWarning {

    private final int count;

    public CountWarning(int count) {
        this.count = count;
    }

    /**
     * Returns the count at the time the warning was fired.
     * 
     * @return count
     */
    public int getCount() {
        return count;
    }

}
