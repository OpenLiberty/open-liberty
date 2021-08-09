/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal.exception;

import javax.net.ssl.SSLException;

/**
 * Socket has already been established and therefore
 * a user cannot set this parameter.
 */
public class SocketEstablishedSSLException extends SSLException {

    /** Serialization ID string */
    private static final long serialVersionUID = 5731482978051458363L;

    /**
     * @param arg0
     */
    public SocketEstablishedSSLException(String arg0) {
        super(arg0);
    }

}
