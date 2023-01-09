/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import java.util.StringJoiner;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.UnsupportedResponseTypeException;

public abstract class AbstractFlow implements Flow {

    public static final String AUTHORIZATION_CODE_RESPONSE_TYPE = "code";

    public static String ALLOWED_RESPONSE_TYPES;
    static {
        StringJoiner joiner = new StringJoiner(",");
        joiner.add("'" + AUTHORIZATION_CODE_RESPONSE_TYPE + "'");
        ALLOWED_RESPONSE_TYPES = joiner.toString();
    }

    public static Flow getInstance(OidcClientConfig oidcClientConfig) throws UnsupportedResponseTypeException {
        String configResponseType = oidcClientConfig.getResponseType();
        if (AUTHORIZATION_CODE_RESPONSE_TYPE.equals(configResponseType)) {
            return new AuthorizationCodeFlow(oidcClientConfig);
        }
        throw new UnsupportedResponseTypeException(configResponseType, ALLOWED_RESPONSE_TYPES);
    }

}
