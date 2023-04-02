/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.fat.rar.message;

/**
 * <p>An exception indicating that the sendMessageWait method has been timed out.</p>
 */
public class MessageWaitTimeoutException extends RuntimeException {
    /**
     * Constructor for MessageWaitTimeoutException.
     */
    public MessageWaitTimeoutException() {
        super();
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     *
     * @param arg0
     */
    public MessageWaitTimeoutException(String arg0) {
        super(arg0);
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     *
     * @param arg0
     * @param arg1
     */
    public MessageWaitTimeoutException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Constructor for MessageWaitTimeoutException.
     *
     * @param arg0
     */
    public MessageWaitTimeoutException(Throwable arg0) {
        super(arg0);
    }
}