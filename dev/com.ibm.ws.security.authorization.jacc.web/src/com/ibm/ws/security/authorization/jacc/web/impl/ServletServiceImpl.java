/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.web.impl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.web.ServletService;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityValidator;

@Component(service = ServletService.class,
                immediate = true,
                name = "com.ibm.ws.security.authorization.jacc.web.servletservice",
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = { "service.vendor=IBM" })
public class ServletServiceImpl implements ServletService {
    private static final TraceComponent tc = Tr.register(ServletServiceImpl.class);

    private static WebSecurityPropagatorImpl wsp = null;
    private static WebSecurityValidatorImpl wsv = null;

    public ServletServiceImpl() {}

    @Activate
    protected synchronized void activate(ComponentContext cc) {}

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {}

    /** {@inheritDoc} */
    @Override
    public synchronized WebSecurityPropagator getPropagator() {
        if (wsp == null) {
            wsp = new WebSecurityPropagatorImpl();
        }
        return wsp;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized WebSecurityValidator getValidator() {
        if (wsv == null) {
            wsv = new WebSecurityValidatorImpl();
        }
        return wsv;
    }
}
