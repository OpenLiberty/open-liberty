/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import java.util.Map;
import java.util.NoSuchElementException;
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
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.v11.config.TraceConstants;

@Component(service = MpConfigProxyService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "version=1.1", "service.ranking:Integer=11" }, name = "mpConfigProxyService")
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

    /**
     * @return
     */
    @Sensitive
    @Override
    public <T> T getConfigValue(ClassLoader cl, String propertyName, Class<T> propertyType) throws IllegalArgumentException, NoSuchElementException {
        if (isAcceptableMpConfigProperty(propertyName)) {
            Optional<T> value = getConfig(cl).getOptionalValue(propertyName, propertyType);
            if (value != null && value.isPresent()) {
                return value.get();
            }
            return null;
        }
        return null;
    }

    /** return */
    @Sensitive
    @Override
    public MpConfigProperties getConfigProperties(ClassLoader cl) {
        Config config = getConfig(cl);
        Set<String> propertyNames = getSupportedConfigPropertyNames();
        MpConfigProperties mpConfigProps = new MpConfigProperties();

        for (String propertyName : propertyNames) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            if (value != null && value.isPresent()) {
                String valueString = value.get().trim();
                if (!valueString.isEmpty()) {
                    mpConfigProps.put(propertyName, valueString);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, propertyName + " is empty. Ignore it.");
                    }

                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, propertyName + " is not in mpConfig.");
                }
            }
        }

        return mpConfigProps;
    }

    @Override
    public Set<String> getSupportedConfigPropertyNames() {
        return MpConfigProperties.acceptableMpConfigPropNames11;
    }

    protected Config getConfig(ClassLoader cl) {
        if (cl != null) {
            return ConfigProvider.getConfig(cl);
        } else {
            return ConfigProvider.getConfig();
        }
    }
}
