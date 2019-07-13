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
package com.ibm.ws.security.openidconnect.server.plugins;

/**
 *  simple holder for oidc provider name and the name of it's oauth provider
 */
public class ProviderInfo {
    private final String name;
    private final String OauthProviderName;

    public ProviderInfo(String oidcProviderName, String oauthProviderName) {
        name = oidcProviderName;
        OauthProviderName = oauthProviderName;
    }

    public String getName() {
        return name;
    }

    public String getOauthProviderName() {
        return OauthProviderName;
    }

}
