/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

 package com.ibm.ws.jaxrs2x.defaultexceptionmapper;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.jaxrs.defaultexceptionmapper.DefaultExceptionMapper;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE, service = {JaxRsProviderRegister.class})
public class JaxRsDefaultExceptionMapperProviderRegister implements JaxRsProviderRegister {

    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        if (!clientSide) {
            providers.add(new DefaultExceptionMapper());
        }
    }

}
