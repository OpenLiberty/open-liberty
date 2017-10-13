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

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
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
import com.ibm.ws.security.javaeesec.authentication.mechanism.http.HAMProperties;

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

    @Override
    public void buildBridgeIfNeeded(String appContext, AuthConfigFactory providerFactory) {
        AuthConfigProvider authConfigProvider = providerFactory.getConfigProvider(JASPIC_LAYER_HTTP_SERVLET, appContext, (RegistrationListener) null);
        if (authConfigProvider != null) {
            // A provider was registered already for this application context.
            return;
        }

        if (isHAMIdentified()) {
            // Create AuthConfigProvider, AuthConfig, AuthContext, and ServerAuthModule bridge.
            Map<String, String> props = new ConcurrentHashMap<String, String>();
            authConfigProvider = new AuthProvider(props, providerFactory);
            providerFactory.registerConfigProvider(authConfigProvider, JASPIC_LAYER_HTTP_SERVLET, appContext, "Built-in JSR-375 Bridge Provider");
        }
    }

    private boolean isHAMIdentified() {
        boolean result = false;
        Instance<HAMProperties> hampInstance = getCDI().select(HAMProperties.class);
        if (hampInstance != null && !hampInstance.isUnsatisfied() && !hampInstance.isAmbiguous()) {
            Instance<HttpAuthenticationMechanism> beanInstance = getCDI().select(hampInstance.get().getImplementationClass());
            if (beanInstance != null && !beanInstance.isUnsatisfied() && !beanInstance.isAmbiguous()) {
                result = true;
            } else {
                Tr.error(tc, "JAVAEESEC_ERROR_NO_HAM");
            }
        } else {
            Tr.error(tc, "JAVAEESEC_ERROR_NO_HAM_PROPS");

        }
        return result;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

}