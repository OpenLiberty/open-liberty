/*******************************************************************************
 * Copyright (c) 2015,2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.osgi.internal.ejbdd;

import java.util.List;

import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.dd.ejb.Interceptor;

class InterceptorImpl implements Interceptor {

    private final String interceptorClassName;

    InterceptorImpl(String interceptorClassName) {
        this.interceptorClassName = interceptorClassName;
    }

    @Override
    public List<LifecycleCallback> getPostConstruct() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LifecycleCallback> getPreDestroy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnvEntry> getEnvEntries() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EJBRef> getEJBRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EJBRef> getEJBLocalRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ServiceRef> getServiceRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResourceRef> getResourceRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResourceEnvRef> getResourceEnvRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MessageDestinationRef> getMessageDestinationRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PersistenceContextRef> getPersistenceContextRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PersistenceUnitRef> getPersistenceUnitRefs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ContextService> getContextServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataSource> getDataSources() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<JMSConnectionFactory> getJMSConnectionFactories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<JMSDestination> getJMSDestinations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ManagedExecutor> getManagedExecutors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ManagedScheduledExecutor> getManagedScheduledExecutors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ManagedThreadFactory> getManagedThreadFactories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<MailSession> getMailSessions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionFactory> getConnectionFactories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AdministeredObject> getAdministeredObjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LifecycleCallback> getPostActivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LifecycleCallback> getPrePassivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InterceptorCallback> getAroundInvoke() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InterceptorCallback> getAroundTimeoutMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    @Override
    public List<LifecycleCallback> getAroundConstruct() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Description> getDescriptions() {
        throw new UnsupportedOperationException();
    }
}
