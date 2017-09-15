/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ce.cm;

import java.sql.SQLTransientConnectionException;

/**
 * Used as a chained exception when unable to allocate a connection before the connection timeout is reached.
 * Would like to get rid of this and combine with top level exception, but could any application code be relying
 * on the chained exception?
 */
public class ConnectionWaitTimeoutException extends SQLTransientConnectionException {
    private static final long serialVersionUID = 5958695928250441720L;

    /**
     * Make a new ConnectionWaitTimeoutException.
     * 
     * @param message the exception message.
     */
    public ConnectionWaitTimeoutException(String message) {
        super(message);
    }
}