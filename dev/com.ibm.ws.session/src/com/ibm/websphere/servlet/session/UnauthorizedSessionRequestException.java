/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.session;

/**
 * The UnauthorizedSessionRequestException is thrown when a user
 * attempts to access a session owned by another user.
 * 
 * @ibm-api
 */
public class UnauthorizedSessionRequestException extends RuntimeException {

    private static final long serialVersionUID = -636334438049529270L;

    public UnauthorizedSessionRequestException() {
        super();
    }

    public UnauthorizedSessionRequestException(String s) {
        super(s);
    }
}
