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
package com.ibm.ws.security.authorization.jacc.ejb.impl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBService;

@Component(service = EJBService.class, immediate = true, name = "com.ibm.ws.security.authorization.jacc.ejb.ejbservice", configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class EJBServiceImpl implements EJBService {
    private static final TraceComponent tc = Tr.register(EJBServiceImpl.class);

    private static EJBSecurityPropagatorImpl esp = null;
    private static EJBSecurityValidatorImpl esv = null;

    public EJBServiceImpl() {}

    @Activate
    protected synchronized void activate(ComponentContext cc) {}

    @Deactivate
    protected synchronized void deactivate(ComponentContext cc) {}

    /** {@inheritDoc} */
    @Override
    public synchronized EJBSecurityPropagator getPropagator() {
        if (esp == null) {
            esp = new EJBSecurityPropagatorImpl();
        }
        return esp;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized EJBSecurityValidator getValidator() {
        if (esv == null) {
            esv = new EJBSecurityValidatorImpl();
        }
        return esv;
    }
}
