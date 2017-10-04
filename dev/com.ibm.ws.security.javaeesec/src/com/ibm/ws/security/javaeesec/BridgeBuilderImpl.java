/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jaspi.BridgeBuilderService;

@Component(service = { BridgeBuilderService.class },
           name = "com.ibm.ws.security.javaeesec",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class BridgeBuilderImpl implements BridgeBuilderService {

    private static final TraceComponent tc = Tr.register(BridgeBuilderImpl.class);

    private static final String JASPIC_LAYER_HTTP_SERVLET = "HttpServlet";

    private BeanManager beanManager;

    @Activate
    protected void activate(ComponentContext cc) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {}

    @FFDCIgnore(NamingException.class)
    @Override
    public void buildBridgeIfNeeded(String appContext, AuthConfigFactory providerFactory) {
        AuthConfigProvider authConfigProvider = providerFactory.getConfigProvider(JASPIC_LAYER_HTTP_SERVLET, appContext, (RegistrationListener) null);
        if (authConfigProvider != null) {
            // A provider was registered already for this application context.
            return;
        }

        try {
            beanManager = getBeanManager();
            Set<Bean<?>> httpAuthMechs = beanManager.getBeans(HttpAuthenticationMechanism.class);

            if (httpAuthMechs.size() == 1) {
                // Create AuthConfigProvider, AuthConfig, AuthContext, and ServerAuthModule bridge.
                Map<String, String> props = new ConcurrentHashMap<String, String>();
                authConfigProvider = new AuthProvider(props, providerFactory);
                providerFactory.registerConfigProvider(authConfigProvider, JASPIC_LAYER_HTTP_SERVLET, appContext, "Built-in JSR-375 Bridge Provider");
            } else {
                if (tc.isDebugEnabled()) {
                    StringBuffer names = new StringBuffer();
                    for (Bean<?> authMech : httpAuthMechs) {
                        names.append(authMech.getBeanClass().getName()).append(", ");
                    }
                    Tr.debug(tc, "Multiple HttpAuthenticationMechanism have been registered.  " + names.toString());
                }
                // TODO: Issue serviceability message
            }
        } catch (NamingException e) {
            // TODO: Issue serviceability message
        }
    }

    protected BeanManager getBeanManager() throws NamingException {
        return (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
    }

}