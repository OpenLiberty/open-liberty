/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mb.interceptor.aroundconstruct;

import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.interceptor.Interceptors;

/**
 * Testing @AroundConstruct Interceptor on a method
 */
@ManagedBean("AroundContructMethodBean")
public class AroundConstructMethodBean {

    private static final String CLASS_NAME = AroundConstructMethodBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public AroundConstructMethodBean() {

    }

    @Interceptors(AroundConstructMethodInterceptor.class)
    public void businessMethod() {
        svLogger.info("BusinessMethod Called");
    }
}
