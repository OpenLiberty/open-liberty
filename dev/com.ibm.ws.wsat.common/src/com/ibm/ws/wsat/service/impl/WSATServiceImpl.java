/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.service.impl;

import javax.xml.ws.Dispatch;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.wsat.tm.impl.TranManagerImpl;
import com.ibm.wsspi.webservices.wsat.WSATService;

/**
 *
 */
@Component(name = "com.ibm.ws.wsat.service.wsatservice",
           immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class WSATServiceImpl implements WSATService {

    @Activate
    protected void activate(ComponentContext cc) {
        WebClientImpl.setTccl(TranManagerImpl.getInstance().getThreadClassLoader(WebClientImpl.class));
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        TranManagerImpl.getInstance().destroyThreadClassLoader(WebClientImpl.getTccl());
        WebClientImpl.setTccl(null);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.webservices.wsat.WSATService#enableWSAT(java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void enableWSAT(Dispatch disp) throws Exception {
        // Disable this SPI first, because we don't need use policy to enable WS-AT transaction in this release
//        ((DispatchImpl) disp).getClient()
//                        .getOutInterceptors().add(new WSATPolicyOverrideInterceptor(false));
        throw new Exception("This SPI has been discarded, please do not use it any more");
    }

}
