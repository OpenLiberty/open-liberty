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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

/**
 *
 */
public class MpConfigUtil {
    private static TraceComponent tc = Tr.register(MpConfigUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef;

    public MpConfigUtil(AtomicServiceReference<MpConfigProxyService> mpConfigProxyServiceRef) {
        this.mpConfigProxyServiceRef = mpConfigProxyServiceRef;
    }

    public Map<String, String> getMpConfig(HttpServletRequest req) {
        Map<String, String> map = new HashMap<String, String>();
        MpConfigProxyService service = mpConfigProxyServiceRef.getService();
        if (service != null) {
            return getMpConfigMap(service, getApplicationClassloader(req), map);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "mpJwt-1.1 feature is not enabled.");
            }
        }
        return map;
    }

    protected ClassLoader getApplicationClassloader(HttpServletRequest req) {
        ClassLoader cl = null;
        //HttpServletRequest req
        if (req instanceof SRTServletRequest) {
            SRTServletRequest servletRequest = (SRTServletRequest) req;
            IWebAppDispatcherContext webAppDispatchContext = servletRequest.getWebAppDispatcherContext();
            WebApp webApp = webAppDispatchContext.getWebApp();
            if (webApp != null) {
                cl = webApp.getClassLoader();
            }
        }
        return cl;
    }

    // no null check. make sure that the caller sets non null objects.
    protected Map<String, String> getMpConfigMap(MpConfigProxyService service, ClassLoader cl, Map<String, String> map) {
        Arrays.asList(MpConstants.ISSUER, MpConstants.PUBLIC_KEY, 
                MpConstants.AUDIENCES, MpConstants.KEY_LOCATION).forEach(s -> getMpConfig(service, cl, s, map));
        return map;
    }

    // no null check other than cl. make sure that the caller sets non null objects.
    @FFDCIgnore({ NoSuchElementException.class })
    protected Map<String, String> getMpConfig(MpConfigProxyService service, ClassLoader cl, String propertyName,  Map<String, String> map) {
        try {
            String value = service.getConfigValue(cl, propertyName, String.class).trim();
            if (!value.isEmpty()) {
                map.put(propertyName, value);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, propertyName + " is empty. Ignore it.");
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
