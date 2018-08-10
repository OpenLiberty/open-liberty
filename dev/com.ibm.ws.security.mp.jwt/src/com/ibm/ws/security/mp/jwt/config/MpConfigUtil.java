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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.mp.jwt.MpJwtExtensionService;
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
    private final AtomicServiceReference<MpJwtExtensionService> mpJwtExtensionServiceRef;

    public MpConfigUtil(AtomicServiceReference<MpJwtExtensionService> mpJwtExtensionServiceRef) {
        this.mpJwtExtensionServiceRef = mpJwtExtensionServiceRef;
    }

    public Map<String, String> getMpConfig(HttpServletRequest req) {
        ClassLoader cl = getApplicationClassloader(req);
        return getMpConfigByClassLoader(cl);
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

    protected Map<String, String> getMpConfigByClassLoader(ClassLoader cl) {
        Map<String, String> map = new HashMap<String, String>();
        MpJwtExtensionService service = mpJwtExtensionServiceRef.getService();
        if (service != null) {
            String issuer = service.getConfigValue(cl, MpConstants.ISSUER, String.class);
            map.put(MpConstants.ISSUER, issuer);
            String publicKey = service.getConfigValue(cl, MpConstants.PUBLIC_KEY, String.class);
            map.put(MpConstants.PUBLIC_KEY, publicKey);
            String keyLocation = service.getConfigValue(cl, MpConstants.KEY_LOCATION, String.class);
            map.put(MpConstants.KEY_LOCATION, keyLocation);
        }
        return map;
    }
}
