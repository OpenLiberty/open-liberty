/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jaspi.BridgeBuilderService;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesUtils;

@Component(service = { BridgeBuilderService.class },
           name = "com.ibm.ws.security.javaeesec",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class BridgeBuilderImpl implements BridgeBuilderService {

    private static final TraceComponent tc = Tr.register(BridgeBuilderImpl.class);

    private static final String JASPIC_LAYER_HTTP_SERVLET = "HttpServlet";

    @Activate
    protected void activate(ComponentContext cc) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    // Synchronized since checking if there is a provider and registering one need to be done as a single atomic operation.
    @Override
    public synchronized void buildBridgeIfNeeded(String appContext, AuthConfigFactory providerFactory) {
        AuthConfigProvider authConfigProvider = providerFactory.getConfigProvider(JASPIC_LAYER_HTTP_SERVLET, appContext, (RegistrationListener) null);
        if (authConfigProvider != null) {
            // A provider was registered already for this application context.
            return;
        }

        if (getModulePropertiesUtils().isHttpAuthenticationMechanism()) {
            // Create AuthConfigProvider, AuthConfig, AuthContext, and ServerAuthModule bridge.
            Map<String, String> props = new ConcurrentHashMap<String, String>();
            authConfigProvider = new AuthProvider(props, providerFactory);
            providerFactory.registerConfigProvider(authConfigProvider, JASPIC_LAYER_HTTP_SERVLET, appContext, PROVIDER_DESCRIPTION);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "HttpAuthenticationMechanism bean is not identified. JSR375 BridgeProvider is not enabled.");
            }
        }
    }

    @Override
    public boolean isProcessingNewAuthentication(HttpServletRequest req) {
        if (getModulePropertiesUtils().isHttpAuthenticationMechanism()) {
            AuthenticationParameters authParams = (AuthenticationParameters) req.getAttribute(JavaEESecConstants.SECURITY_CONTEXT_AUTH_PARAMS);
            if (authParams != null) {
                return authParams.isNewAuthentication();
            }
        }
        return false;
    }

    @Override
    public boolean isCredentialPresent(HttpServletRequest req) {
        if (getModulePropertiesUtils().isHttpAuthenticationMechanism()) {
            AuthenticationParameters authParams = (AuthenticationParameters) req.getAttribute(JavaEESecConstants.SECURITY_CONTEXT_AUTH_PARAMS);
            if (authParams != null) {
                return (authParams.getCredential() != null);
            }
        }
        return false;
    }

    protected ModulePropertiesUtils getModulePropertiesUtils() {
        return ModulePropertiesUtils.getInstance();
    }

}