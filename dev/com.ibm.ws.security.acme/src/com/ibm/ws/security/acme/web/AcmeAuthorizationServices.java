/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.web;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Component(service = AcmeAuthorizationServices.class, name = "com.ibm.ws.security.acme.web.AcmeAuthorizationServices", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class AcmeAuthorizationServices {
    private static TraceComponent tc = Tr.register(AcmeAuthorizationServices.class);

    @Activate
    protected void activate(ComponentContext cc) {
        Tr.info(tc, "**** JTM **** AcmeAuthorizationServices entered activate() method!");
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        Tr.info(tc, "**** JTM **** AcmeAuthorizationServices entered deactivate() method!");
    }

}