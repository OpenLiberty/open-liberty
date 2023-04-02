/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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

package com.ibm.adapter.message;

/**
 * <p>A MessageException object indicates that users try to add a message which doesn't have
 * the right format.</p>
 */
public class MessageException extends RuntimeException {

    /**
     * Constructor for MessageException.
     */
    public MessageException() {
        super();
    }

    /**
     * Constructor for MessageException.
     *
     * @param arg0
     */
    public MessageException(String arg0) {
        super(arg0);
    }

    /**
     * Constructor for MessageException.
     *
     * @param arg0
     * @param arg1
     */
    public MessageException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Constructor for MessageException.
     *
     * @param arg0
     */
    public MessageException(Throwable arg0) {
        super(arg0);
    }

}
