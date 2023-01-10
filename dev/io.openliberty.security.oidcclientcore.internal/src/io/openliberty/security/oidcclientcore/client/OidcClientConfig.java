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
package io.openliberty.security.oidcclientcore.client;

import java.util.Set;

import com.ibm.websphere.ras.ProtectedString;

/**
 * Featureless OIDC Client Configuration.
 */
public interface OidcClientConfig {

    String getProviderURI();

    OidcProviderMetadata getProviderMetadata();

    String getClientId();

    ProtectedString getClientSecret();

    ClaimsMappingConfig getClaimsMappingConfig();

    default LogoutConfig getLogoutConfig() {
        return new LogoutConfig() {
        };
    }

    String getRedirectURI();

    boolean isRedirectToOriginalResource();

    Set<String> getScope();

    String getResponseType();

    String getResponseMode();

    String getPromptParameter();

    String getDisplayParameter();

    boolean isUseNonce();

    boolean isUseSession();

    // TODO: Change to List<NameValuePair>
    String[] getExtraParameters();

    int getJwksConnectTimeout();

    int getJwksReadTimeout();

    boolean isTokenAutoRefresh();

    int getTokenMinValidity();

}