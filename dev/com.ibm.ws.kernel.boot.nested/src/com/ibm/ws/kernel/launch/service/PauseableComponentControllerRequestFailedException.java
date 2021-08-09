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
 * Exception representing no pauseable components during a pause or resume request
 */
public class PauseableComponentControllerRequestFailedException extends Exception {

    /**
     * Constructs a new instance with null as its detail message and cause.
     */
    public PauseableComponentControllerRequestFailedException() {
        super();
    }

    /**
     * Constructs a new instance with the specified detail message.
     *
     * @param message The detail message.
     */
    public PauseableComponentControllerRequestFailedException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with the specified cause.
     *
     * @param throwable The cause of type Throwable.
     */
    public PauseableComponentControllerRequestFailedException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructs a new instance with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param throwable The cause of type Throwable.
     */
    public PauseableComponentControllerRequestFailedException(String message, Throwable throwable) {
        super(message, throwable);
    }

}