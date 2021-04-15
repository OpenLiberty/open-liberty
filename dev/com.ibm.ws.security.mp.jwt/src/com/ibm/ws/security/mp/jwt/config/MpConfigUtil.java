/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.config;

import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class MpConfigUtil {
    private static TraceComponent tc = Tr.register(MpConfigUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef;

    public MpConfigUtil(AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef) {
        this.mpConfigProxyServiceRef = mpConfigProxyServiceRef;
    }

    public MpConfigProperties getMpConfig(HttpServletRequest req) {
        MpConfigProperties map = new MpConfigProperties();
        MpConfigProxyService service = mpConfigProxyServiceRef.getService();
        if (service != null) {
            return getMpConfigMap(service, getApplicationClassloader(req), map);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "MP JWT feature is not enabled.");
            }
        }
        return map;
    }

    protected ClassLoader getApplicationClassloader(HttpServletRequest req) {
        return req != null ? req.getServletContext().getClassLoader() : null;
    }

    // no null check. make sure that the caller sets non null objects.
    protected MpConfigProperties getMpConfigMap(MpConfigProxyService service, ClassLoader cl, MpConfigProperties map) {
        Set<String> supportedMpConfigPropNames = service.getSupportedConfigPropertyNames();
        supportedMpConfigPropNames.forEach(s -> getMpConfig(service, cl, s, map));
        return map;
    }

    // no null check other than cl. make sure that the caller sets non null objects.
    @FFDCIgnore({ NoSuchElementException.class })
    protected MpConfigProperties getMpConfig(MpConfigProxyService service, ClassLoader cl, String propertyName, MpConfigProperties map) {
        try {
            String value = service.getConfigValue(cl, propertyName, String.class);
            if (value != null) {
                value = value.trim();
            }
            if (value != null && !value.isEmpty()) {
                map.put(propertyName, value);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, propertyName + " is empty or null. Ignore it.");
                }
            }
        } catch (NoSuchElementException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, propertyName + " is not in mpConfig.");
            }
        }
        return map;
    }
}
