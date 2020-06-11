/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.mix.ejb;

import static javax.ejb.TransactionManagementType.BEAN;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

@Stateful
@ExcludeDefaultInterceptors
@TransactionManagement(BEAN)
public class StatefulInterceptorInjectionBean implements StatefulInterceptorInjectionLocal {

    @Override
    @Interceptors({ AnnotationInjectionInterceptor.class })
    public Throwable getAnnotationInterceptorResults() {
        return new Throwable();
    }

    // Tests injection through the <interceptor> definition
    @Override
    @Interceptors({ XMLInjectionInterceptor.class })
    public Throwable getXMLInterceptorResults() {
        return new Throwable();
    }

    // Tests injection through the bean class itself
    @Override
    @Interceptors({ XMLInjectionInterceptor2.class })
    public Throwable getXMLInterceptorResults2() {
        return new Throwable();
    }

    @Override
    @Remove
    public void finish() {

    }
}
