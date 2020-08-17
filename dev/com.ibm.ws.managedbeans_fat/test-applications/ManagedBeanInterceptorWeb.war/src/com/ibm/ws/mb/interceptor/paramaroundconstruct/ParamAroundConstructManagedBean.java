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
package com.ibm.ws.mb.interceptor.paramaroundconstruct;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

@ManagedBean("ParamAroundConstructManagedBean")
@Interceptors(ParamAroundConstructInterceptor.class)
public class ParamAroundConstructManagedBean {

    private InjectedManagedBean ivInjection;
    private boolean ivAroundConstructCalled = false;

    /**
     *
     */
    public ParamAroundConstructManagedBean() {
    }

    @Inject
    public ParamAroundConstructManagedBean(InjectedManagedBean injection) {
        this.ivInjection = injection;
    }

    //Have a parameter constructor that matches parameters in a .setParameter() test to make sure it still fails
    public ParamAroundConstructManagedBean(int arg) {

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
