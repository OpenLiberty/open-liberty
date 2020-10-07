/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
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
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.config.MpConstants;
import com.ibm.ws.security.mp.jwt.v11.config.TraceConstants;

@Component(service = MpConfigProxyService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "version=1.1", "service.ranking:Integer=11" }, name = "mpConfigProxyService")
public class MpConfigProxyServiceImpl implements MpConfigProxyService {

    public static final TraceComponent tc = Tr.register(MpConfigProxyServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static private String MP_VERSION = "1.1";

    public static Set<String> ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES = new HashSet<String>();
    static {
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.ISSUER);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.PUBLIC_KEY);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.KEY_LOCATION);
    }

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
    @Override
    public boolean isMpConfigAvailable() {
        return true;
    }

    /**
     * @return
     */
    @Override
    public <T> T getConfigValue(ClassLoader cl, String propertyName, Class<T> propertyType) throws IllegalArgumentException, NoSuchElementException {
        if (isAcceptableMpConfigProperty(propertyName)) {
            return getConfig(cl).getValue(propertyName, propertyType);
        }
        return null;
    }

    @Override
    public Set<String> getSupportedConfigPropertyNames() {
        return ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES;
    }

    protected boolean isAcceptableMpConfigProperty(String propertyName) {
        return ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.contains(propertyName);
    }

    protected Config getConfig(ClassLoader cl) {
        if (cl != null) {
            return ConfigProvider.getConfig(cl);
        } else {
            return ConfigProvider.getConfig();
        }
    }
}
