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
package com.ibm.ws.collector;

import java.util.List;

public interface Target {

    /*
     * The list passed here should be a list of formatted events.
     */
    public abstract void sendEvents(List<Object> formattedEvents);

    public abstract void close();
}
