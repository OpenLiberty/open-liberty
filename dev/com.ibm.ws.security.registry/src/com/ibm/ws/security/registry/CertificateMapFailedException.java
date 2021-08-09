/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

/**
 * Thrown when a UserRegistry can not successfully map a provided
 * certificate to an entry in the UserRegistry.
 */
public class CertificateMapFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    // Implementation note:
    // No default constructor should be provided.
    // A CertificateMapFailedException without a message is not helpful.

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public CertificateMapFailedException(String msg) {
        super(msg);
    }

    /**
     * @see java.lang.Exception#Exception(String, Throwable)
     */
    public CertificateMapFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
