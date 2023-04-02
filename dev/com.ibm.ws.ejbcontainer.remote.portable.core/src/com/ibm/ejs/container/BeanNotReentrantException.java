/*******************************************************************************
 * Copyright (c) 1998, 2010 IBM Corporation and others.
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
package com.ibm.ejs.container;

public class BeanNotReentrantException extends java.rmi.RemoteException
{
    private static final long serialVersionUID = -4139033422889883913L;

    private transient boolean ivTimeout; // d653777.1

    public BeanNotReentrantException() {
        super();
    }

    public BeanNotReentrantException(String message) {
        super(message);
    }

    public BeanNotReentrantException(String message, boolean timeout) { // d653777.1
        this(message);
        ivTimeout = timeout;
    }

    public boolean isTimeout() { // d653777.1
        return ivTimeout;
    }
}
