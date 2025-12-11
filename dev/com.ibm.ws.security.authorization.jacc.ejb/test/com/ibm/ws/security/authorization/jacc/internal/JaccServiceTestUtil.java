/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import io.openliberty.security.authorization.jacc.internal.proxy.ProviderServiceProxyImpl;
import io.openliberty.security.authorization.jacc.internal.proxy.ProxyTestUtil;

public class JaccServiceTestUtil {

    /**
     * @param pcm
     */
    public static JaccServiceImpl createJaccService(PolicyConfigurationManager pcm) {
        return new JaccServiceImpl(pcm);
    }

    /**
     * @param jaccService
     * @param jaccProviderServiceProxyRef
     * @param jaccProviderServiceRef
     * @param wsLocationAdminRef
     * @param cc
     */
    public static void initJaccService(JaccServiceImpl jaccService, ServiceReference<ProviderServiceProxy> jaccProviderServiceProxyRef,
                                       ServiceReference<ProviderService> jaccProviderServiceRef, ServiceReference<WsLocationAdmin> wsLocationAdminRef,
                                       ComponentContext cc) {
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        ProviderServiceProxyImpl providerServiceProxy = new ProviderServiceProxyImpl();
        ProxyTestUtil.setProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);
    }
}
