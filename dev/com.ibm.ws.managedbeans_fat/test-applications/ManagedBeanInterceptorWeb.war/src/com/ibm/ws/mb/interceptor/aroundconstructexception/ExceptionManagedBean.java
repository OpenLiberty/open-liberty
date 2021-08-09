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
package com.ibm.ws.mb.interceptor.aroundconstructexception;

import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

@ManagedBean("ExceptionManagedBean")
@Interceptors(ExceptionInterceptor.class)
public class ExceptionManagedBean {

    private static final String CLASS_NAME = ExceptionManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static boolean svThrowException = true;

    private InjectedManagedBean ivInjection;
    private boolean ivAroundConstructCalled = false;

    public ExceptionManagedBean() {
    }

    @Inject
    public ExceptionManagedBean(InjectedManagedBean injection) throws ConstructorException {
        svLogger.info("Exception Constructor Called");
        this.ivInjection = injection;
        if (svThrowException == true) {
            throw new ConstructorException();
        }
    }

    public static void setThrowException(boolean args) {
        svThrowException = args;
    }

    public InjectedManagedBean getInjection() {
        return ivInjection;
    }

    public boolean verifyAroundConstructCalled() {
        return ivAroundConstructCalled;
    }

    public void setAroundConstructCalled(boolean arg) {
        ivAroundConstructCalled = arg;
    }

}
