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
package com.ibm.ws.security.internal;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Represents the securityConfiguration XML element.
 * This separation is done so that components which use Security need
 * not be aware of multiple security configurations.
 */
public class SecurityConfiguration {
    private static final TraceComponent tc = Tr.register(SecurityConfiguration.class);

    static final String CFG_KEY_AUTHENTICATION_REF = "authenticationRef";
    static final String CFG_KEY_AUTHORIZATION_REF = "authorizationRef";
    static final String CFG_KEY_USERREGISTRY_REF = "userRegistryRef";

    private volatile String cfgAuthenticationRef;
    private volatile String cfgAuthorizationRef;
    private volatile String cfgUserRegistryRef;

    protected void activate(Map<String, Object> properties) {
        setAndValidateProperties((String) properties.get(CFG_KEY_AUTHENTICATION_REF),
                                 (String) properties.get(CFG_KEY_AUTHORIZATION_REF),
                                 (String) properties.get(CFG_KEY_USERREGISTRY_REF));
    }

    protected void modify(Map<String, Object> newProperties) {
        setAndValidateProperties((String) newProperties.get(CFG_KEY_AUTHENTICATION_REF),
                                 (String) newProperties.get(CFG_KEY_AUTHORIZATION_REF),
                                 (String) newProperties.get(CFG_KEY_USERREGISTRY_REF));
    }

    protected void deactivate() {
        cfgAuthenticationRef = null;
        cfgAuthorizationRef = null;
        cfgUserRegistryRef = null;
    }

    /**
     * @throw Translated IllegalArgumentException representing a missing attribute error
     */
    private void throwIllegalArgumentExceptionMissingAttribute(String attributeName) {
        Tr.error(tc, "SECURITY_CONFIG_ERROR_MISSING_ATTRIBUTE", attributeName);
        throw new IllegalArgumentException(Tr.formatMessage(tc, "SECURITY_CONFIG_ERROR_MISSING_ATTRIBUTE", attributeName));
    }

    /**
     * Sets and validates the configuration properties. If any of the
     * configuration properties are not set, an IllegalArgumentException
     * is thrown.
     * 
     * @param cfgAuthentication
     * @param cfgAuthorization
     * @param cfgUserRegistry
     * @throws IllegalArgumentException if any of the configuration elements
     *             are not set
     */
    private void setAndValidateProperties(String cfgAuthentication,
                                          String cfgAuthorization,
                                          String cfgUserRegistry) {
        if ((cfgAuthentication == null) || cfgAuthentication.isEmpty()) {
            throwIllegalArgumentExceptionMissingAttribute(CFG_KEY_AUTHENTICATION_REF);
        }
        this.cfgAuthenticationRef = cfgAuthentication;

        if ((cfgAuthorization == null) || cfgAuthorization.isEmpty()) {
            throwIllegalArgumentExceptionMissingAttribute(CFG_KEY_AUTHORIZATION_REF);
        }
        this.cfgAuthorizationRef = cfgAuthorization;

        if ((cfgUserRegistry == null) || cfgUserRegistry.isEmpty()) {
            throwIllegalArgumentExceptionMissingAttribute(CFG_KEY_USERREGISTRY_REF);
        }
        this.cfgUserRegistryRef = cfgUserRegistry;
    }

    /**
     * @return AuthenticationService id for this configuration, will not be null.
     */
    String getAuthenticationServiceId() {
        return cfgAuthenticationRef;
    }

    /**
     * @return AuthorizationService id for this configuration, will not be null.
     */
    String getAuthorizationServiceId() {
        return cfgAuthorizationRef;
    }

    /**
     * @return UserRegistryService id for this configuration, will not be null.
     */
    String getUserRegistryServiceId() {
        return cfgUserRegistryRef;
    }

}
