/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.providers.security;

import java.util.List;
import java.util.Set;

import org.apache.cxf.jaxrs.security.SimpleAuthorizingFilter;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.jaxrs20.security.LibertyAuthFilter;
import com.ibm.ws.jaxrs20.security.LibertySimpleAuthorizingInterceptor;

@Component(immediate=true)
public class SecurityAnnoProviderRegister implements JaxRsProviderRegister {

    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {

        if (!clientSide) {
            if (features.contains("appSecurity-2.0") || features.contains("appSecurity-1.0") || features.contains("appSecurity-3.0")) {
                //add one built-in ContainerRequestFilter to handle basic security
                LibertyAuthFilter laf = new LibertyAuthFilter();
                LibertySimpleAuthorizingInterceptor in = new LibertySimpleAuthorizingInterceptor();
                laf.setInterceptor(in);
                providers.add(laf);
            }
        }
    }

}
