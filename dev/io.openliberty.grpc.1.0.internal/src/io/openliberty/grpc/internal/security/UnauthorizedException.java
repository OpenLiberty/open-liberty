package io.openliberty.grpc.internal.security;

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public class UnauthorizedException extends java.lang.SecurityException{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
       /**
     * Constructor for this exception
     *
     * @param msg
     */
    public UnauthorizedException(String msg) {
        super(msg);
    }

    public UnauthorizedException() {
        super();
    }

    public UnauthorizedException(Throwable t) {
        super(t);
    }

    public UnauthorizedException(String msg, Throwable t) {
        super(msg, t);
    }

}
