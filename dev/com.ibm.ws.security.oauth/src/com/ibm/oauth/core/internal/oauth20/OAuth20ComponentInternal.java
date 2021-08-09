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
package com.ibm.oauth.core.internal.oauth20;

import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;

/**
 * Interface for internal-use-only methods for an OAuth 2.0 component
 */
public interface OAuth20ComponentInternal extends OAuth20Component {
    public OidcOAuth20ClientProvider getClientProvider();

    public OAuth20TokenCache getTokenCache();

    public OAuthStatisticsImpl getStatisticsImpl();

    public OAuth20ConfigProvider get20Configuration();
}
