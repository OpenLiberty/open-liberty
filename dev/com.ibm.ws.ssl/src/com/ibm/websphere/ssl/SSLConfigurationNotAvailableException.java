/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.websphere.ssl;

/**
 *
 */
public class SSLConfigurationNotAvailableException extends SSLException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public SSLConfigurationNotAvailableException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param cause
     */
    public SSLConfigurationNotAvailableException(Exception cause) {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message
     */
    public SSLConfigurationNotAvailableException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param message
     * @param cause
     */
    public SSLConfigurationNotAvailableException(String message, Exception cause) {
        super(message, cause);
    }

}
