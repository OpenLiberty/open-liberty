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
package io.openliberty.security.oidcclientcore.client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.oidcclientcore.authentication.AbstractFlow;
import io.openliberty.security.oidcclientcore.authentication.Flow;

public class Client {

    private final OidcClientConfig oidcClientConfig;

    public Client(OidcClientConfig oidcClientConfig) {
        this.oidcClientConfig = oidcClientConfig;
    }

    public ProviderAuthenticationResult startFlow(HttpServletRequest request, HttpServletResponse response) {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.startFlow(request, response);
    }

    public ProviderAuthenticationResult continueFlow(HttpServletRequest request, HttpServletResponse response) {
        Flow flow = AbstractFlow.getInstance(oidcClientConfig);
        return flow.continueFlow(request, response);
    }

    public void logout() {
        // TODO
    }

}
