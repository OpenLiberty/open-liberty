/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

/**
 *
 */
public class ProvisioningException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public ProvisioningException() {}

    /**
     * @param message
     */
    public ProvisioningException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ProvisioningException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
