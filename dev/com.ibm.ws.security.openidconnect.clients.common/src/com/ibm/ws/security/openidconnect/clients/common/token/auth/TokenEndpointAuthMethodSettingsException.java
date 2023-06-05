/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common.token.auth;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class TokenEndpointAuthMethodSettingsException extends Exception {

    public static final TraceComponent tc = Tr.register(TokenEndpointAuthMethodSettingsException.class);

    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final String authMethod;
    private final String nlsMessage;

    public TokenEndpointAuthMethodSettingsException(String clientId, String authMethod, String nlsMessage) {
        this.clientId = clientId;
        this.authMethod = authMethod;
        this.nlsMessage = nlsMessage;
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR", clientId, authMethod, nlsMessage);
    }

}
