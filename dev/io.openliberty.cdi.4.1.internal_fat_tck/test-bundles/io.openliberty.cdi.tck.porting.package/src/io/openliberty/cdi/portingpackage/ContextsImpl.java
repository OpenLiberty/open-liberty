/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi.portingpackage;

import org.jboss.cdi.tck.spi.Contexts;
import org.jboss.weld.Container;
import org.jboss.weld.context.ApplicationContext;
import org.jboss.weld.context.DependentContext;
import org.jboss.weld.context.ManagedContext;
import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.http.HttpRequestContext;
import org.jboss.weld.util.ForwardingContext;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import jakarta.enterprise.context.spi.Context;

/**
 * Porting pacakge Contexts impl for OpenLiberty.
 *
 * Adapted from the Weld porting package to use the contextId for the current application
 */
public class ContextsImpl implements Contexts<Context> {

    private static final ServiceCaller<CDIService> cdiService = new ServiceCaller<>(ContextsImpl.class, CDIService.class);

    /**
     * Get the contextId of the current application
     *
     * @return the contextId for the current application
     */
    private String getId() {
        return cdiService.run(CDIService::getCurrentApplicationContextID)
                         .orElseThrow(() -> new RuntimeException("CDIService not available"));
    }

    @Override
    public RequestContext getRequestContext() {
        return Container.instance(getId()).deploymentManager().instance().select(HttpRequestContext.class).get();
    }

    @Override
    public void setActive(Context context) {
        context = ForwardingContext.unwrap(context);
        if (context instanceof ManagedContext) {
            ((ManagedContext) context).activate();
        } else if (context instanceof ApplicationContext) {
            // No-op, always active
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setInactive(Context context) {
        context = ForwardingContext.unwrap(context);
        if (context instanceof ManagedContext) {
            ((ManagedContext) context).deactivate();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public DependentContext getDependentContext() {
        return Container.instance(getId()).deploymentManager().instance().select(DependentContext.class).get();
    }

    @Override
    public void destroyContext(Context context) {
        context = ForwardingContext.unwrap(context);
        if (context instanceof ManagedContext) {
            ManagedContext managedContext = (ManagedContext) context;
            managedContext.invalidate();
            managedContext.deactivate();
            managedContext.activate();
        } else if (context instanceof ApplicationContext) {
            ((ApplicationContext) context).invalidate();
        } else {
            throw new UnsupportedOperationException();
        }
    }
}