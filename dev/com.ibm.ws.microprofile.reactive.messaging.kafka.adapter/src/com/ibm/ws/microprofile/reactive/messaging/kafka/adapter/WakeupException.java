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
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

/**
 *
 */
public class WakeupException extends RuntimeException {

    private static final long serialVersionUID = -5770316885804917502L;

    /**
     * @param message
     */
    public WakeupException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public WakeupException(Throwable cause) {
        super(cause);
    }

}
