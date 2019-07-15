/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.interrupt;

/**
 * An exception which is thrown when registraion of an
 * {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
 * fails.
 *
 * @ibm-api
 * @ibm-was-base
 */
public class InterruptRegistrationException extends Exception {
    private static final long serialVersionUID = -4048875561283687517L;

    public InterruptRegistrationException(String message) {
        super(message);
    }

    public InterruptRegistrationException(String message, Throwable t) {
        super(message, t);
    }
}
