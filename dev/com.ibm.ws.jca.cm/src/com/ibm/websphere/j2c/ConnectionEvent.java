/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.j2c;

/**
 * ConnectionEvent extends the javax.resource.spi.ConnectionEvent to add additional constants
 * for connection event IDs.
 * 
 * @ibm-spi
 */
public class ConnectionEvent extends javax.resource.spi.ConnectionEvent {

    static final long serialVersionUID = 7709055559014574730L;

    private ConnectionEvent() {
        super(null, 0);
    }

    /**
     * Constant to indicate that only the connection the event was fired on
     * is to be destroyed, regardless of the purge policy.
     */
    public static final int SINGLE_CONNECTION_ERROR_OCCURRED = 51;

    /**
     * Constant to indicate that no message should be logged for this error, as it
     * was initiated by the application or by JMS
     */
    public static final int CONNECTION_ERROR_OCCURRED_NO_EVENT = 52;

}
