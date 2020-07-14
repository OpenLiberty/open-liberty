/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.executor.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.microprofile.faulttolerance.impl.AbstractProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(service = { FaultToleranceProviderResolver.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ProviderResolverImpl extends AbstractProviderResolverImpl {

    /**
     * Reference to the context service for this managed executor service.
     */
    @Reference
    protected WSContextService contextService;

    @Override
    public <R> ExecutorBuilder<R> newExecutionBuilder() {
        ExecutorBuilderImpl<R> ex = new ExecutorBuilderImpl<R>(contextService, policyExecutorProvider, getScheduledExecutorService());
        return ex;
    }
}
