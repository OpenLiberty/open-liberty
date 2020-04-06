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
package com.ibm.oauth.core.api.config;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuthComponentConfiguration implementation seeded with default configuration
 * values. Individual values can be overridden to customize the configuration.
 * 
 * Classes extending this class must implement getUniqueID(), and also provide
 * class names for OAUTH20_CLIENT_PROVIDER_CLASSNAME and
 * OAUTH20_TOKEN_CACHE_CLASSNAME, at a minimum if using OAuth 2.0 features of
 * the component.
 * 
 * Extending this base class rather than directly implementing
 * OAuthComponentConfiguration is highly recommended. New parameters added to
 * the OAuthComponentConfigurationConstants may receive corresponding update in
 * this class with reasonable default values, which minimizes future API
 * incompatibility.
 * 
 */
public abstract class SampleComponentConfiguration implements
        OAuthComponentConfiguration {

    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_MAX_AUTHORIZATION_GRANT_LIFETIME_SECONDS}
     * property: 604800
     */
    public static final String[] MAX_AUTHORIZATION_GRANT_LIFEIMTE_SECONDS = { "604800" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_CODE_LIFETIME_SECONDS}
     * property: 60
     */
    public static final String[] CODE_LIFETIME_SECONDS = { "60" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_CODE_LENGTH}
     * property: 30
     */
    public static final String[] CODE_LENGTH = { "30" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_TOKEN_LIFETIME_SECONDS}
     * property: 3600
     */
    public static final String[] TOKEN_LIFETIME_SECONDS = { "7200" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_ACCESS_TOKEN_LENGTH}
     * property: 40
     */
    public static final String[] ACCESS_TOKEN_LENGTH = { "40" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_ISSUE_REFRESH_TOKEN}
     * property: true
     */
    public static final String[] ISSUE_REFRESH_TOKEN = { "true" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_REFRESH_TOKEN_LENGTH}
     * property: 50
     */
    public static final String[] REFRESH_TOKEN_LENGTH = { "50" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME}
     * property to use internal default token type handler. You should
     * <b>not</b> use any other value for this property.
     */
    public static final String[] ACCESS_TOKENTYPEHANDLER_CLASSNAME = { "com.ibm.oauth.core.internal.oauth20.tokentype.impl.OAuth20TokenTypeHandlerBearerImpl" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_MEDIATOR_CLASSNAMES}
     * property: null, to use internal default mediator which does nothing.
     */
    public static final String[] MEDIATOR_CLASSNAMES = null;
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_ALLOW_PUBLIC_CLIENTS}
     * property: false
     */
    public static final String[] ALLOW_PUBLIC_CLIENTS = { "false" };
    /**
     * Sample config value for the
     * {@link OAuthComponentConfigurationConstants#OAUTH20_GRANT_TYPES_ALLOWED}
     * property: allows all grant and token types
     */
    public static final String[] GRANT_TYPES_ALLOWED = {
            OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
            OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
            OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
            OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD,
            OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };

    /**
     * Collection of configuration properties. The attribute is marked protected
     * so that subclasses can override or add other properties to this
     * configuration map.
     */
    protected Map<String, String[]> _config = new HashMap<String, String[]>();

    /**
     * Populates the configuration map with all our default values.
     */
    public SampleComponentConfiguration() {
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_MAX_AUTHORIZATION_GRANT_LIFETIME_SECONDS,
                        MAX_AUTHORIZATION_GRANT_LIFEIMTE_SECONDS);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_CODE_LIFETIME_SECONDS,
                        CODE_LIFETIME_SECONDS);
        _config.put(OAuthComponentConfigurationConstants.OAUTH20_CODE_LENGTH,
                CODE_LENGTH);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_TOKEN_LIFETIME_SECONDS,
                        TOKEN_LIFETIME_SECONDS);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKEN_LENGTH,
                        ACCESS_TOKEN_LENGTH);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_ISSUE_REFRESH_TOKEN,
                        ISSUE_REFRESH_TOKEN);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_REFRESH_TOKEN_LENGTH,
                        REFRESH_TOKEN_LENGTH);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME,
                        ACCESS_TOKENTYPEHANDLER_CLASSNAME);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                        MEDIATOR_CLASSNAMES);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                        ALLOW_PUBLIC_CLIENTS);
        _config
                .put(
                        OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                        GRANT_TYPES_ALLOWED);
    }

    public void putConfigPropertyValues(String property, String[] values) {
        _config.put(property, values);
    }

    public ClassLoader getPluginClassLoader() {
        // use my class loader for plugins
        return this.getClass().getClassLoader();
    }

    public String getConfigPropertyValue(String name) {
        String result = null;
        String[] vals = getConfigPropertyValues(name);
        if (vals != null && vals.length > 0) {
            result = vals[0];
        }
        return result;
    }

    public String[] getConfigPropertyValues(String name) {
        return _config.get(name);
    }

    public int getConfigPropertyIntValue(String name) {
        return Integer.parseInt(getConfigPropertyValue(name));
    }

    public boolean getConfigPropertyBooleanValue(String name) {
        return Boolean.parseBoolean(getConfigPropertyValue(name));
    }

    public abstract String getUniqueId();

}
