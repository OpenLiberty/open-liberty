/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.v11.config.TraceConstants;

@Component(service = MpConfigProxyService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "version=1.1", "service.ranking:Integer=11" }, name = "mpConfigProxyService")
public class MpConfigProxyServiceImpl implements MpConfigProxyService {

    public static final TraceComponent tc = Tr.register(MpConfigProxyServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static private String MP_VERSION = "1.1";

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_11_CONFIG_PROXY_PROCESSED");
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_11_CONFIG_PROXY_MODIFIED");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "MPJWT_11_CONFIG_PROXY_DEACTIVATED");
    }

    /**
     * @return
     */
    @Override
    public String getVersion() {
        return MP_VERSION;
    }

    @Override
    public Set<String> getSupportedConfigPropertyNames() {
        return MpConfigProperties.acceptableMpConfigPropNames11;
    }

    @Override
    public MpConfigProxy getConfigProxy(ClassLoader cl) {
        Config config = cl != null ? ConfigProvider.getConfig(cl) : ConfigProvider.getConfig();

        return new MpConfigProxy() {
            @Override
            public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
                return config.getOptionalValue(propertyName, propertyType);
            }
        };
    }
}
