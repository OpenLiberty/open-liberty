/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.Intercept;

@RequestScoped
public class MyBeanInjectionString {

    String s = "bean injection";

    static String interceptorString = "";

    public static void setInterceptorString(String s) {
        interceptorString = s;
    }

    @Override
    @Intercept
    public String toString() {
        return interceptorString + s;
    }
}
