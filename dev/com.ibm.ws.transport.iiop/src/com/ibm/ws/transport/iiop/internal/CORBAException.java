/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.internal;

/**
 * @version $Revision: 465172 $ $Date: 2006-10-18 01:16:14 -0700 (Wed, 18 Oct 2006) $
 */
public class CORBAException extends Exception {
    private static final long serialVersionUID = 1L;

    public CORBAException() {
        super();
    }

    public CORBAException(String message) {
        super(message);
    }

    public CORBAException(String message, Throwable cause) {
        super(message, cause);
    }

    public CORBAException(Throwable cause) {
        super(cause);
    }
}
