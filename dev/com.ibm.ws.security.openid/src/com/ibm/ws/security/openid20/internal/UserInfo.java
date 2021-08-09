/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.internal;

/**
 * OpenId attribute data
 */
public class UserInfo {
    private String alias;
    private String uriType;
    private int count;
    private boolean required;

    UserInfo(String alias, String uriType, int count, boolean required) {
        this.alias = alias;
        this.uriType = uriType;
        this.count = count;
        this.required = required;
    }

    public String getAlias() {
        return alias;
    }

    public int getCount() {
        return count;
    }

    public boolean getRequired() {
        return required;
    }

    public String getType() {
        return uriType;
    }
}