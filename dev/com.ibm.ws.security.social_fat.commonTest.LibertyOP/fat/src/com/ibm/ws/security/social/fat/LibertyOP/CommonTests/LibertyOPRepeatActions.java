/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.LibertyOP.CommonTests;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

public class LibertyOPRepeatActions {
    protected static final String USERINFO_ENDPOINT = Constants.USERINFO_ENDPOINT;
    protected static final String INTROSPECTION_ENDPOINT = Constants.INTROSPECTION_ENDPOINT;

    public static UserApiEndpoint usingUserInfo() {

        return new UserApiEndpoint(USERINFO_ENDPOINT);
    }

    public static UserApiEndpoint usingIntrospect() {

        return new UserApiEndpoint(INTROSPECTION_ENDPOINT);
    }
}
