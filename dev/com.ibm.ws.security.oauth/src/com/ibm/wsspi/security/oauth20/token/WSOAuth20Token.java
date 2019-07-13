/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.oauth20.token;

import java.security.SecurityPermission;
import java.util.Map;

public abstract interface WSOAuth20Token {
    public static final SecurityPermission GET_OAUTH_PERM =
            new SecurityPermission("wssapi.OAUTH20.getOAuth");

    public static final SecurityPermission UPDATE_OAUTH_PERM =
            new SecurityPermission("wssapi.OAUTH20.update" +
                    "OAuth");

    public String getUser();

    public String getTokenString();

    public String getClientID();

    public String[] getScope();

    public long getExpirationTime();

    public String getProperty(String key);

    public Object getAttribute(String key);

    @SuppressWarnings("unchecked")
    public Map getAttributes();

    public String getCacheKey();

    public boolean isValid();

    public String getProvider();

}
