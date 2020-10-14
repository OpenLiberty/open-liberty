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
package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Exception thrown when an RRS service encounters an error using the native
 * registry.
 */
public class RegistryException extends Exception {
    /** Serial version */
    private static final long serialVersionUID = 7396851550780231950L;

    /** The return code from the registry service. */
    private final int returnCode;

    /**
     * Constructor.
     *
     * @param explanation An explanation of the registry error.
     * @param returnCode  The return code from the registry service.
     */
    public RegistryException(String explanation, int returnCode) {
        super(explanation);
        this.returnCode = returnCode;
    }

    /**
     * Gets the return code from the registry service.
     *
     * @return The return code from the failing registry service.
     */
    @Trivial
    public final int getReturnCode() {
        return returnCode;
    }
}
