/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.providers.security;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;
import com.ibm.ws.jaxrs21.security.LibertyAuthFilter;

@Component(immediate=true)
public class SecurityAnnoProviderRegister implements JaxRsProviderRegister {

    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {

        if (!clientSide) {
            //add one built-in ContainerRequestFilter to handle basic security
            LibertyAuthFilter laf = new LibertyAuthFilter();
            providers.add(laf);
        }
    }
}
