/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.config;

import com.ibm.oauth.core.api.audit.OAuthAuditHandler;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;

public interface OAuth20ConfigProvider {

    public OidcOAuth20ClientProvider getClientProvider();

    public OAuth20TokenCache getTokenCache();

    public int getMaxAuthGrantLifetimeSeconds();

    public int getCodeLifetimeSeconds();

    public int getCodeLength();

    public int getTokenLifetimeSeconds();

    public int getAccessTokenLength();

    public boolean isIssueRefreshToken();

    public int getRefreshTokenLength();

    public OAuth20TokenTypeHandler getTokenTypeHandler();

    public OAuth20TokenTypeHandler getIDTokenTypeHandler(); // oidc10

    public OAuth20GrantTypeHandlerFactory getGrantTypeHandlerFactory(); // oidc10

    public OAuth20ResponseTypeHandlerFactory getResponseTypeHandlerFactory(); // oidc10

    public OAuth20Mediator getMediators();

    public boolean isAllowPublicClients();

    public boolean isGrantTypeAllowed(String grantType);

    public OAuthAuditHandler getAuditHandler();

}
