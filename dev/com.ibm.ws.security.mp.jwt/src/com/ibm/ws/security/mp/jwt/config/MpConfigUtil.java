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
package com.ibm.ws.security.mp.jwt.config;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.mp.jwt.MpConfigProxyService;
import com.ibm.ws.security.mp.jwt.TraceConstants;

@Component(property = { "service.vendor=IBM" })
public class MpConfigUtil {
    private static TraceComponent tc = Tr.register(MpConfigUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static MpConfigProxyService mpConfigProxyService = null;

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void setMpConfigProxyService(MpConfigProxyService proxyService) {
        if (mpConfigProxyService == null) {
            mpConfigProxyService = proxyService;
        } else if (proxyService != null && (mpConfigProxyService.getVersion().compareTo(proxyService.getVersion()) < 0)) {
            mpConfigProxyService = proxyService;
        }
    }

    protected void unsetMpConfigProxyService(MpConfigProxyService proxyService) {
        mpConfigProxyService = null;
    }

    public MpConfigProperties getMpConfig(HttpServletRequest req) {
        if (mpConfigProxyService != null) {
            return mpConfigProxyService.getConfigProperties(getApplicationClassloader(req));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "MP JWT feature is not enabled.");
            }
        }
        return new MpConfigProperties();
    }

    protected ClassLoader getApplicationClassloader(HttpServletRequest req) {
        return req != null ? req.getServletContext().getClassLoader() : null;
    }
}
