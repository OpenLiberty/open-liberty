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
package com.ibm.ws.microprofile.faulttolerance20.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.microprofile.faulttolerance.impl.ProviderResolverImpl;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutorBuilder;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProviderResolver;

@Component(service = { FaultToleranceProviderResolver.class }, property = { "service.vendor=IBM" }, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ProviderResolverImpl20 extends ProviderResolverImpl {

    @Override
    public <T, R> ExecutorBuilder<T, R> newExecutionBuilder() {
        return new ExecutorBuilderImpl20<>(contextService, policyExecutorProvider, getScheduledExecutorService());
    }

}
