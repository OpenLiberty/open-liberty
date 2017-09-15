/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security;

/**
 * Thrown to indicate that the certificate mapping for the
 * specified certificate is not supported.
 * 
 * @ibm-spi
 */

public class CertificateMapNotSupportedException extends WSSecurityException {

    private static final long serialVersionUID = -2990393675191692512L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new CertificateMapNotSupportedException with an empty description string.
     */
    public CertificateMapNotSupportedException() {
        super();
    }

    /**
     * Create a new CertificateMapNotSupportedException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public CertificateMapNotSupportedException(String message) {
        super(message);
    }

    /**
     * Create a new CertificateMapNotSupportedException with the Throwable root cause.
     * 
     * @param t the Throwable root cause.
     */
    public CertificateMapNotSupportedException(Throwable t) {
        super(t);
    }

    /**
     * Create a new CertificateMapNotSupportedException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public CertificateMapNotSupportedException(String message, Throwable t) {
        super(message, t);
    }

}
