/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.mp.jwt.v12.config.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import io.openliberty.security.mp.jwt.v12.config.TraceConstants;

@Component(service = MpConfigProxyService.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "version=1.2", "service.ranking:Integer=12" }, name = "mpConfigProxyService")
public class MpConfigProxyServiceImpl extends com.ibm.ws.security.mp.jwt.v11.config.impl.MpConfigProxyServiceImpl implements MpConfigProxyService {

    public static final TraceComponent tc = Tr.register(MpConfigProxyServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static private String MP_VERSION = "1.2";

    public static Set<String> ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES = new HashSet<String>();
    static {
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.PUBLIC_KEY_ALG);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.DECRYPT_KEY_LOCATION);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.VERIFY_AUDIENCES);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.TOKEN_HEADER);
        ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.add(MpConstants.TOKEN_COOKIE);
    }

    @Override
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_12_CONFIG_PROXY_PROCESSED");
    }

    @Override
    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        Tr.info(tc, "MPJWT_12_CONFIG_PROXY_MODIFIED");
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "MPJWT_12_CONFIG_PROXY_DEACTIVATED");
    }

    @Override
    public String getVersion() {
        return MP_VERSION;
    }

    @Override
    public Set<String> getSupportedConfigPropertyNames() {
        Set<String> allSupportedProps = super.getSupportedConfigPropertyNames();
        allSupportedProps.addAll(ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES);
        return allSupportedProps;
    }

    @Override
    protected boolean isAcceptableMpConfigProperty(String propertyName) {
        if (ACCEPTABLE_MP_CONFIG_PROPERTY_NAMES.contains(propertyName)) {
            return true;
        }
        return super.isAcceptableMpConfigProperty(propertyName);
    }

}
