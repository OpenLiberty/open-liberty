/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.exception;

public class OAuthProviderException extends Exception {

    private static final long serialVersionUID = 401L;

    public OAuthProviderException(Exception e) {
        super(e);
    }

    public OAuthProviderException(String msg) {
        super(msg);
    }

}
