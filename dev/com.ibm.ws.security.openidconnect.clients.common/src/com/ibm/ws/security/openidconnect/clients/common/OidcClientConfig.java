/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import com.ibm.ws.security.common.structures.SingleTableCache;

public interface OidcClientConfig extends ConvergedClientConfig {

    public static final String ID_TOKEN_ONLY = "ID_TOKEN_ONLY";

    @Override
    String getGrantType(); // when the responseType is "code", we change the
                           // getGrantType to "authorization_code"

    boolean isValidateAccessTokenLocally();

    String getTrustAliasName();

    String getValidationEndpointUrl();

    int getInitialStateCacheCapacity();

    String getTrustStoreRef();

    public String getAuthFilterId();

    public String getValidationMethod();

    public String getJwtAccessTokenRemoteValidation();

    public String getHeaderName();

    public boolean isValidConfig(); // when the inboundPropagation is required
                                    // and the validationEndpointUrl is null,
                                    // it's a bad config instance

    /**
     * @return
     */
    public boolean isReAuthnOnAccessTokenExpire();

    /**
     * @return milliseconds
     */
    public long getReAuthnCushion();

    //public Cache getCache();

    @Override
    public String getResponseType(); // when the responseType is "code", we
                                     // change the getGrantType to
                                     // "authorization_code"

    /**
     * @return
     */
    public boolean isOidcclientRequestParameterSupported();

    /**
     * @return
     */
    public String jwtRef();

    public String[] getJwtClaims();

    /**
     * If a junction path is defined (goes after host and port but before context root)
     * insert it into the redirect url and return it.
     *
     * @param redirect_url
     * @return
     */
    @Override
    String getRedirectUrlWithJunctionPath(String redirect_url);

    public boolean requireExpClaimForIntrospection();

    public boolean requireIatClaimForIntrospection();

    public SingleTableCache getCache();

    public boolean getAccessTokenCacheEnabled();

    public long getAccessTokenCacheTimeout();

}
