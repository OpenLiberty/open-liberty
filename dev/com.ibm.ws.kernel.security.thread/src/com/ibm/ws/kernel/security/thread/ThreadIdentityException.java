/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.security.thread;

/**
 *
 */
public class ThreadIdentityException extends Exception {

    /**
     * @param e
     */
    public ThreadIdentityException(Exception e) {
        super(e);
    }

    /**  */
    private static final long serialVersionUID = -5708519766664441499L;
}
