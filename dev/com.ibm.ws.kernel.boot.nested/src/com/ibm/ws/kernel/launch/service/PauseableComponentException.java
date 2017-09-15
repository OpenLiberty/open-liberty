/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

/**
 * A ServiceNotRegisteredException is thrown to indicate issues with services implementing
 * the {@link PausableComponent} interface.
 */
public class PauseableComponentException extends Exception {
    /** Serial UID. */
    private static final long serialVersionUID = -5586603988141501090L;

    /**
     * Constructs a new instance with null as its detail message and cause.
     */
    public PauseableComponentException() {
        super();
    }

    /**
     * Constructs a new instance with the specified detail message.
     *
     * @param message The detail message.
     */
    public PauseableComponentException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     *
     * @param throwable The cause of type Throwable.
     */
    public PauseableComponentException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructs a new instance with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param throwable The cause of type Throwable.
     */
    public PauseableComponentException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
