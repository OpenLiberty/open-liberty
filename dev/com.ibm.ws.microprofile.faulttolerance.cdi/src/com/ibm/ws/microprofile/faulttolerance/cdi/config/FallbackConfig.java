/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;

public interface FallbackConfig {

    /**
     * Validate the Fallback policy
     */
    void validate();

    FallbackPolicy generatePolicy(InvocationContext context, BeanManager beanManager);

}