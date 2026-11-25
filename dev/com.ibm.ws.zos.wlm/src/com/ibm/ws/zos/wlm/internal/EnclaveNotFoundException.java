/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

/**
 * An exception to indicate an enclave was not found in the ContextEnclaves map
 */
public class EnclaveNotFoundException extends Exception {

    /**
     * Serialization version
     */
    private static final long serialVersionUID = 2314704367860260286L;

    /**
     * constructor
     */
    public EnclaveNotFoundException() {
        super();
    }

    /**
     * Constructor
     *
     * @param message exception message
     */
    public EnclaveNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message a message
     * @param cause   nested cause
     */
    public EnclaveNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param cause nested cause
     */
    public EnclaveNotFoundException(Throwable cause) {
        super(cause);
    }
}
