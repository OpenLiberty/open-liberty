/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspi;

import java.security.Security;

import javax.security.auth.message.config.AuthConfigFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Wrapper to ensure that if the authconfigprovider.factory java.security
 * property is not set, we will use our JASPI implementation. This came
 * about from scenarios where a customer could be migrating their java.secrity
 * file and either remove this property, or copy a file that does not have
 * it thereby removing it.
 */
public class AuthConfigFactoryWrapper
{
    private static final TraceComponent tc = Tr.register(AuthConfigFactoryWrapper.class);
    static final String JASPI_PROVIDER_REGISTRY = "com.ibm.ws.security.jaspi.ProviderRegistry";
    static boolean initialized = false;

    /**
     * Wraps {@link AuthConfigFactory#getFactory()}.
     * If {@link AuthConfigFactory#DEFAULT_FACTORY_SECURITY_PROPERTY} is
     * not set, use our internal JASPI implementation: {@link #JASPI_PROVIDER_REGISTRY}.
     * 
     * @return AuthConfigFactory instance
     */
    public static AuthConfigFactory getFactory() {
        if (!initialized) {
            setFactoryImplementation();
        }
        return AuthConfigFactory.getFactory();
    }

    /**
     * Make sure we don't dump multiple messages to the logs by only
     * initializing once.
     */
    synchronized static void setFactoryImplementation() {
        if (initialized)
            return;
        initialized = true;
        String authConfigProvider = Security.getProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY);
        if (authConfigProvider == null || authConfigProvider.isEmpty()) {
            Tr.info(tc, "JASPI_DEFAULT_AUTH_CONFIG_FACTORY", new Object[] { JASPI_PROVIDER_REGISTRY });
            Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, JASPI_PROVIDER_REGISTRY);
        } else if (!authConfigProvider.equals(JASPI_PROVIDER_REGISTRY)) {
            Tr.info(tc, "JASPI_AUTH_CONFIG_FACTORY", new Object[] { authConfigProvider });
        }
    }
}
