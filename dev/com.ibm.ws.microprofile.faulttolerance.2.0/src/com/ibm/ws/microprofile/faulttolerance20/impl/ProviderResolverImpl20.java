/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.microprofile.faulttolerance.impl.AbstractProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;
import com.ibm.ws.microprofile.faulttolerance.spi.context.ContextService;

@Component(service = { FaultToleranceProviderResolver.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ProviderResolverImpl20 extends AbstractProviderResolverImpl {

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ContextService contextService;

    @Override
    public <R> ExecutorBuilder<R> newExecutionBuilder() {
        return new ExecutorBuilderImpl20<>(contextService, policyExecutorProvider, getScheduledExecutorService());
    }

}
