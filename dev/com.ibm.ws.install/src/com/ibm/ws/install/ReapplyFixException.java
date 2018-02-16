/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

/**
 * This exception indicates that an exception occurs during reapply fixes.
 */
public class ReapplyFixException extends InstallException {

    private static final long serialVersionUID = 5252063204794357335L;

    /**
     * Create Reapply Fix Exception with a Throwable and return code.
     *
     * @param message Exception message
     * @param cause Throwable cause of exception
     * @param rc Return Code
     */
    public ReapplyFixException(String message, Throwable cause, int rc) {
        super(message, cause, rc);
    }

    /**
     * Create Reapply Fix Exception with message only.
     *
     * @param message Exception message
     * @param runtimeException
     */
    public ReapplyFixException(String message) {
        super(message);
    }
}
