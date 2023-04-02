/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.security.jaspi;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.http.HttpServletRequest;

/**
 * Bridge to create the AuthConfigProvider, AuthConfig, AuthContext, and ServerAuthModule needed for JSR-375.
 */
public interface BridgeBuilderService {

    public static final String PROVIDER_DESCRIPTION = "Bridge Provider for JSR-375 Java EE Security API";

    /**
     * @param appContext
     * @param providerFactory
     *
     */
    void buildBridgeIfNeeded(String appContext, AuthConfigFactory providerFactory);

    /**
     * returns true if JSR-375 HttpAuthenticationMechansim is available and newAuthentication attribute is set as true in AuthenticationParameters.
     *
     * @param HttpServletRequest
     *
     */
    boolean isProcessingNewAuthentication(HttpServletRequest req);

    /**
     * returns true if JSR-375 HttpAuthenticationMechansim is available and credential object is set in AuthenticationParameters.
     * otherwise return false;
     */
    boolean isCredentialPresent(HttpServletRequest req);
}
