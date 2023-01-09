/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS.client.internal;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import io.openliberty.org.jboss.resteasy.common.client.LibertyResteasyClientBuilderImpl;
import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;

/**
 * Tracks and applies {@link ClientAsyncTaskWrapper} services
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ClientAsyncTaskWrapperComponent extends AsyncClientExecutorHelper {

    private static ClientAsyncTaskWrapper threadContextWrapper = new ThreadContextAsyncTaskWrapper();

    private final AtomicServiceReference<ExecutorService> executorServiceRef = new AtomicServiceReference<ExecutorService>("executorService");
    private final AtomicServiceReference<ExecutorService> managedExecutorServiceRef = new AtomicServiceReference<ExecutorService>("managedExecutorService");

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, fieldOption = FieldOption.REPLACE)
    private volatile List<ClientAsyncTaskWrapper> wrappers;

    @Activate
    protected void activate(ComponentContext cc) {
        instance.set(this);
        LibertyResteasyClientBuilderImpl.setAsyncClientExecutorHelper(this);
        executorServiceRef.activate(cc);
        managedExecutorServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        instance.compareAndSet(this, null);
        LibertyResteasyClientBuilderImpl.setAsyncClientExecutorHelper(null);
        executorServiceRef.deactivate(cc);
        managedExecutorServiceRef.deactivate(cc);
    }

    @Reference(name = "executorService", service = ExecutorService.class, target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutorService(ServiceReference<ExecutorService> ref) {
        executorServiceRef.setReference(ref);
    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> ref) {
        executorServiceRef.unsetReference(ref);
    }

    @Reference(name = "managedExecutorService", service = ExecutorService.class, target = "(id=DefaultManagedExecutorService)", policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setManagedExecutorService(ServiceReference<ExecutorService> ref) {
        managedExecutorServiceRef.setReference(ref);
    }

    protected void unsetManagedExecutorService(ServiceReference<ExecutorService> ref) {
        managedExecutorServiceRef.unsetReference(ref);
    }

    @Override
    public ExecutorService getExecutorService() {
        ExecutorService managedExecutorService = managedExecutorServiceRef.getService();
        if (managedExecutorService != null) {
            return managedExecutorService;
        }
        return executorServiceRef.getService();
    }

    @Override
    List<ClientAsyncTaskWrapper> getTaskWrappers() {
        return wrappers;
    }

    @Override
    ClientAsyncTaskWrapper getThreadContextWrapper() {
        return threadContextWrapper;
    }
}
