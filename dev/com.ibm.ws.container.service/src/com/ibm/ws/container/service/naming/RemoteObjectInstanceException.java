/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.naming;

/**
 * This exception indicates that something went wrong while attempting
 * to acquire an object that is represented on a remote server.
 * The cause of this exception should include the information necessary
 * for understanding what went wrong. For example, it could be that
 * the client failed to communicate with the server which could result
 * in a RemoteException. Possibly the object does not exist on the
 * remote server, resulting in a NamingException. Or possibly the
 * object could not be re-constructed locally due to a
 * ClassNotFoundException, etc.
 */
public class RemoteObjectInstanceException extends Exception {
    private static final long serialVersionUID = 7057215313281670551L;

    public RemoteObjectInstanceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
