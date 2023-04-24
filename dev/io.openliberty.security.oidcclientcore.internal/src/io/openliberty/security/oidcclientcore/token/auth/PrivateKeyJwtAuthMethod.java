/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token.auth;

public class PrivateKeyJwtAuthMethod {

    private final String clientId;
    private final String tokenEndpoint;

    public PrivateKeyJwtAuthMethod(String clientId, String tokenEndpoint) {
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
    }

    public String createPrivateKeyJwt() {
        // TODO
        return null;
    }

}
