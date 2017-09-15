/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ce.j2c;

import javax.resource.spi.ResourceAllocationException;

/**
 * Indicates that a connection request has waited for ConnectionWaitTimeout but a
 * connection did become free, and MaxConnections has been reached.
 * 
 * @ibm-api
 */
public class ConnectionWaitTimeoutException extends ResourceAllocationException {
    private static final long serialVersionUID = 7973811692690774902L;

    /**
     * Constructs a ResourceAllocationException with the specified reason.
     * 
     * @param reason The reason for the timeout exception.
     */
    public ConnectionWaitTimeoutException(String reason) {
        super(reason);
    }

    /**
     * Constructs a ResourceAllocationException with the specified reason and error code
     * 
     * @param reason The reason for the timeout exception.
     * @param errorCode
     */
    public ConnectionWaitTimeoutException(String reason, String errorCode) {
        super(reason, errorCode);
    }
}
