/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;

public abstract class AbstractFlow implements Flow {

    public static final String AUTHORIZATION_CODE_RESPONSE_TYPE = "code";
    public static final String IMPLICIT_RESPONSE_TYPE = "token";

    public static Flow getInstance(OidcClientConfig oidcClientConfig) {
        String configResponseType = oidcClientConfig.getResponseType();
        String[] responseTypes = configResponseType.split(" ");
        for (String responseType : responseTypes) {
            if (IMPLICIT_RESPONSE_TYPE.equals(responseType)) {
                return new ImplicitFlow(oidcClientConfig);
            }
        }
        return new AuthorizationCodeFlow(oidcClientConfig);
    }

}
