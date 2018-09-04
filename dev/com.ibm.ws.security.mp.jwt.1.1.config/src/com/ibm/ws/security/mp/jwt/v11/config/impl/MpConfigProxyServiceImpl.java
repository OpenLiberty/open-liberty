/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.v11.config.impl;

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.v11.config.TraceConstants;

@Component(service = MpConfigProxyService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = {"service.vendor=IBM", "version=1.1"}, name = "mpConfigProxyService")
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
    public String getVersion(){
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
        return getConfig(cl).getValue(propertyName, propertyType);
    }

    protected Config getConfig(ClassLoader cl) {
        if (cl != null) {
            return ConfigProvider.getConfig(cl);
        } else {
            return ConfigProvider.getConfig();
        }
    }
}
