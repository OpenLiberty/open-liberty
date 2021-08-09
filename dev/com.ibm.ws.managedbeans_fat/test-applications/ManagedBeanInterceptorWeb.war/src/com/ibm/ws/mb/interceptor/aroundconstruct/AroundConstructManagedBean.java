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

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.Interceptors;

/**
 *
 */
@ManagedBean("AroundContructManagedBean")
@Interceptors(AroundConstructInterceptor.class)
public class AroundConstructManagedBean {

    private int ivAroundConstructCalled = 0;
    private int ivBusinessMethodCalled = 0;
    private int ivPostConstructCalled = 0;
    private static volatile boolean svPreDestroyCalled = false;

    /**
     *
     */
    public AroundConstructManagedBean() {

    }

    public AroundConstructManagedBean(String constructMessage) {

    }

    @PostConstruct
    public void initialize() {

    }

    @PreDestroy
    public void unInitialize() {

    }

    public int getAroundConstructCount() {
        return ivAroundConstructCalled;
    }

    public void setAroundConstructCalled() {
        this.ivAroundConstructCalled++;
    }

    public int getBusinessMethodCount() {
        return ivBusinessMethodCalled;
    }

    public void setBusinessMethodCalled() {
        this.ivBusinessMethodCalled++;
    }

    public int getPostConstructCount() {
        return ivPostConstructCalled;
    }

    public void setPostConstructCalled() {
        this.ivPostConstructCalled++;
    }

    public static boolean verifyPreDestroyCalled() {
        return svPreDestroyCalled;
    }

    public static void setPreDestroyCalled() {
        AroundConstructManagedBean.svPreDestroyCalled = true;
    }

}
