/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

/**
 * Thrown by UserRegistry when the mapCertificate() operation is not supported.
 */
public class CertificateMapNotSupportedException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public CertificateMapNotSupportedException(String msg) {
        super(msg);
    }

    public CertificateMapNotSupportedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
