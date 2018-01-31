/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import java.util.EventObject;

/**
 * This abstract class is for Install Event notifications
 */
public abstract class InstallEvent extends EventObject {

    private static final long serialVersionUID = -1991865252974291860L;

    /**
     * Creates an Install event object
     *
     * @param notificationType Event notification type
     */
    public InstallEvent(String notificationType) {
        super(notificationType);
    }

}
