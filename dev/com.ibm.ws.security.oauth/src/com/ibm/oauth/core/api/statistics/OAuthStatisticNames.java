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
package com.ibm.oauth.core.api.statistics;

/**
 * List of all known named statistics supported by the component
 */
public interface OAuthStatisticNames {

    /*
     * OAuth 2.0 primary API interfaces
     */
    public final static String OAUTH20_PROCESS_AUTHORIZATION = "stats.oauth20.processAuthorization";
    public final static String OAUTH20_PROCESS_TOKEN = "stats.oauth20.processToken";
    public final static String OAUTH20_PROCESS_RESOURCE = "stats.oauth20.processResource";
    public final static String OAUTH20_PROCESS_APP_TOKEN = "stats.oauth20.processAppToken";

    /*
     * OAuth configuration extensions interfaces
     */
    public final static String OAUTH_CONFIG_GETUNIQUEID = "stats.oauth.config.getUniqueId";
    public final static String OAUTH_CONFIG_GETPROPERTY = "stats.oauth.config.getProperty";

    /*
     * OAuth 2.0 customer extensions interfaces
     */
    public final static String OAUTH20_CLIENTPROVIDER_EXISTS = "stats.oauth20.clientProvider.exists";
    public final static String OAUTH20_CLIENTPROVIDER_GETCLIENT = "stats.oauth20.clientProvider.getClient";
    public final static String OAUTH20_CLIENTPROVIDER_VALIDATECLIENT = "stats.oauth20.clientProvider.validateClient";
    public final static String OAUTH20_MEDIATOR_MEDIATEAUTHORIZE = "stats.oauth20.mediator.mediateAuthorize";
    public final static String OAUTH20_MEDIATOR_MEDIATETOKEN = "stats.oauth20.mediator.mediateToken";
    public final static String OAUTH20_MEDIATOR_MEDIATERESOURCE = "stats.oauth20.mediator.mediateResource";
    public final static String OAUTH20_MEDIATOR_MEDIATEAUTHORIZE_EXCEPTION = "stats.oauth20.mediator.mediateAuthorize.exception";
    public final static String OAUTH20_MEDIATOR_MEDIATETOKEN_EXCEPTION = "stats.oauth20.mediator.mediateToken.exception";
    public final static String OAUTH20_MEDIATOR_MEDIATERESOURCE_EXCEPTION = "stats.oauth20.mediator.mediateResource.exception";
    public final static String OAUTH20_TOKENCACHE_ADD = "stats.oauth20.tokenCache.add";
    public final static String OAUTH20_TOKENCACHE_GET = "stats.oauth20.tokenCache.get";
    public final static String OAUTH20_TOKENCACHE_REMOVE = "stats.oauth20.tokenCache.remove";
}
