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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBService;

@Component(service = EJBService.class,
                immediate = true,
                name = "com.ibm.ws.security.authorization.jacc.ejb.ejbservice",
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = { "service.vendor=IBM" })
public class EJBServiceImpl implements EJBService {
    private static final TraceComponent tc = Tr.register(EJBServiceImpl.class);

    private static EJBSecurityPropagatorImpl esp = null;
    private static EJBSecurityValidatorImpl esv = null;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

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

    @Reference(service = JavaEEVersion.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            ((EJBSecurityValidatorImpl) getValidator()).setEEVersion(0);
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            ((EJBSecurityValidatorImpl) getValidator()).setEEVersion(Integer.parseInt(major));
        }
        eeVersionRef = ref;
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            ((EJBSecurityValidatorImpl) getValidator()).setEEVersion(0);
        }
    }
}
