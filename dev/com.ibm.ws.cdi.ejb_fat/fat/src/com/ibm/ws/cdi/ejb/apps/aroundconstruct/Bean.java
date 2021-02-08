/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import static com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger.ConstructorType.INJECTED;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.DirectlyIntercepted;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorOneBinding;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorTwoBinding;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.NonCdiInterceptor;
import com.ibm.ws.cdi.ejb.utils.Intercepted;

@RequestScoped
@Intercepted
@InterceptorOneBinding
@InterceptorTwoBinding
@Interceptors({ NonCdiInterceptor.class })
public class Bean {
    public Bean() {} // necessary to be proxyable

    @DirectlyIntercepted
    @Inject
    public Bean(AroundConstructLogger logger) {
        logger.setConstructorType(INJECTED);
    }

    public void doSomething() {}
}
