/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.wab.configure.WABConfiguration;

/*
 * this is part of configurable context root for this WAB
 */

@Component(configurationPid = "com.ibm.ws.security.openidconnect.client.webapp",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        name = "oidcClientWebappConfig",
        service = WABConfiguration.class,
        immediate = true,
        property = { "service.vendor=IBM" })
public class OidcClientWebappConfigImpl implements WABConfiguration {
    // yes it really is empty
    private static final TraceComponent tc = Tr.register(OidcClientWebappConfigImpl.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {

        String contextPath = (String) config.get(WABConfiguration.CONTEXT_PATH);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Context Path=" + contextPath);
        }
        OidcClientConfigImpl.setContextPath(contextPath);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        String contextPath = (String) config.get(WABConfiguration.CONTEXT_PATH);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Context Path=" + contextPath);
        }
        OidcClientConfigImpl.setContextPath(contextPath);
    }

    @Deactivate
    protected void deactivate() {

    }

}
