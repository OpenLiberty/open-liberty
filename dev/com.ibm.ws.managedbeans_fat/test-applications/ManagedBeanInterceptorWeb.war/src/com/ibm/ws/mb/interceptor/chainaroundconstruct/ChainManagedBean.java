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
package com.ibm.ws.mb.interceptor.chainaroundconstruct;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.mb.interceptor.injection.InjectedManagedBean;

@ManagedBean("ChainManagedBean")
@Interceptors({ ChainInterceptor1.class, ChainInterceptor2.class, ChainInterceptor3.class })
public class ChainManagedBean {

    private InjectedManagedBean ivInjection;
    private boolean ivAroundConstructCalled = false;

    public ChainManagedBean() {

    }

    @Inject
    public ChainManagedBean(InjectedManagedBean injection) {
        this.ivInjection = injection;
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
