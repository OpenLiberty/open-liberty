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
package com.ibm.ws.managedobject;

/**
 * Thrown if an error occur while creating a managed object or managed object
 * factory.
 */
public class ManagedObjectException extends Exception {
    private static final long serialVersionUID = 2189215815982016336L;

    public ManagedObjectException(Throwable t) {
        super(t);
    }

    public ManagedObjectException(String message, Throwable t) {
        super(message, t);
    }

    public ManagedObjectException(String message) {
        super(message);
    }
}
