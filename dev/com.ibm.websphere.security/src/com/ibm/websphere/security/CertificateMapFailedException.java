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
 * Thrown to indicate that a error occurred while mapping the
 * specified certificate.
 * 
 * @ibm-spi
 */

public class CertificateMapFailedException extends WSSecurityException {

    private static final long serialVersionUID = -2745350089461368647L; //@vijaylax: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new CertificateMapFailedException with an empty description string.
     */
    public CertificateMapFailedException() {
        super();
    }

    /**
     * Create a new CertificateMapFailedException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public CertificateMapFailedException(String message) {
        super(message);
    }

    /**
     * Create a new CertificateMapFailedException with the Throwable root cause.
     * 
     * @param t the Throwable root cause.
     */
    public CertificateMapFailedException(Throwable t) {
        super(t);
    }

    /**
     * Create a new CertificateMapFailedException with the string description and Throwable root cause.
     * 
     * @param message the String describing the exception.
     * @param t the Throwable root cause.
     */
    public CertificateMapFailedException(String message, Throwable t) {
        super(message, t);
    }

}
