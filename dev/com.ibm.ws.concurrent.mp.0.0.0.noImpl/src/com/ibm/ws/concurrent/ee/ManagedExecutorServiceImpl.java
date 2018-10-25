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

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.enterprise.concurrent.ManagedExecutorService;

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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.concurrent.service.AbstractManagedExecutorService;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPid = "com.ibm.ws.concurrent.managedExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutorService.class, ResourceFactory.class, ApplicationRecycleComponent.class },
           reference = @Reference(name = "ApplicationRecycleCoordinator", service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService" })
@Trivial
public class ManagedExecutorServiceImpl extends AbstractManagedExecutorService {
    @Activate
    @Override
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        super.activate(context, properties);
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Modified
    @Override
    protected void modified(final ComponentContext context, Map<String, Object> properties) {
        super.modified(context, properties);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Override
    protected void setConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.setConcurrencyPolicy(svc);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Override
    protected void setContextService(ServiceReference<WSContextService> ref) {
        super.setContextService(ref);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    @Override
    protected void setLongRunningPolicy(ConcurrencyPolicy svc) {
        super.setLongRunningPolicy(svc);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(component.name=com.ibm.ws.transaction.context.provider)")
    @Override
    protected void setTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.setTransactionContextProvider(ref);
    }

    @Override
    protected void unsetConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.unsetConcurrencyPolicy(svc);
    }

    @Override
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        super.unsetContextService(ref);
    }

    @Override
    protected void unsetLongRunningPolicy(ConcurrencyPolicy svc) {
        super.unsetLongRunningPolicy(svc);
    }

    @Override
    protected void unsetTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.unsetTransactionContextProvider(ref);
    }
}
