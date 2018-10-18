/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.ee;

import javax.enterprise.concurrent.ContextService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.service.AbstractContextService;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Subclass of com.ibm.ws.concurrent.internal.ContextServiceImpl to be used with Java 7,
 * where the MicroProfile Concurrency interfaces (which require Java 8) are unavailable.
 *
 * The purpose of this class is to define ContextServiceImpl as an OSGi service component
 * that provides only the EE Concurrency spec and not MicroProfile Concurrency.
 */
@Component(name = "com.ibm.ws.context.service",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ContextService.class, WSContextService.class, ApplicationRecycleComponent.class },
           property = { "creates.objectClass=javax.enterprise.concurrent.ContextService" })
@Trivial
public class ContextServiceImpl extends AbstractContextService {
    private String name; // TODO temporarily exists to accommodate test case

    @Activate
    @Override
    protected void activate(ComponentContext context) {
        super.activate(context);
        name = super.name;
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Modified
    @Override
    protected void modified(ComponentContext context) {
        super.modified(context);
        name = super.name;
    }

    @Override
    @Reference(name = "baseInstance",
               service = ContextService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    protected void setBaseInstance(ServiceReference<ContextService> ref) {
        super.setBaseInstance(ref);
    }

    @Override
    @Reference(name = "threadContextManager",
               service = WSContextService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC,
               target = "(component.name=com.ibm.ws.context.manager)")
    protected void setThreadContextManager(WSContextService svc) {
        super.setThreadContextManager(svc);
    }

    @Override
    protected void unsetBaseInstance(ServiceReference<ContextService> ref) {
        super.unsetBaseInstance(ref);
    }

    @Override
    protected void unsetThreadContextManager(WSContextService svc) {
        super.unsetThreadContextManager(svc);
    }
}
