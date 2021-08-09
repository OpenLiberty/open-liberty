/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.callback;

import java.io.Serializable;

import javax.security.auth.callback.Callback;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
*
*/
public class JwtTokenCallback implements Callback, Serializable {
    private static final long serialVersionUID = 1L;
    private String credToken;

    public JwtTokenCallback() {
        super();
    }

    public void setToken(@Sensitive String token) {
        this.credToken = new String(token);
    }

    public @Sensitive String getToken() {
        return credToken;
    }

}
