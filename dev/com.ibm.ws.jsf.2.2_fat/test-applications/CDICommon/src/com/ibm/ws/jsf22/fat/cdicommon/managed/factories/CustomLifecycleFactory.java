/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.cdicommon.managed.factories;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryAppBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryDepBean;

/**
 *
 */
public class CustomLifecycleFactory extends LifecycleFactory {

    private LifecycleFactory lf = null;

    // Field injected bean
    @Inject
    private FactoryAppBean fieldBean;

    // Method Injected bean
    private FactoryDepBean methodBean;

    public CustomLifecycleFactory(LifecycleFactory fac) {
        lf = fac;
    }

    String _postConstruct = ":PostConstructNotCalled";

    @PostConstruct
    public void start() {
        _postConstruct = ":PostConstructCalled";
    }

    @PreDestroy
    public void stop() {
        System.out.println(this.getClass().getSimpleName() + " preDestroy called.");
    }

    @Inject
    public void setMethodBean(FactoryDepBean bean) {
        methodBean = bean;
    }

    @Override
    public void addLifecycle(String lifecycleId, Lifecycle lifecycle) {
        lf.addLifecycle(lifecycleId, lifecycle);
    }

    @Override
    public Lifecycle getLifecycle(String lifecycleId) {

        String output = "Field Injected App Bean is NULL";

        if (fieldBean != null) {
            output = fieldBean.getName();
        }

        output += _postConstruct;

        if (methodBean != null) {
            methodBean.incrementAppCount();
            methodBean.addMessageNoLogJustFirst(this.getClass().getSimpleName(), "getLifecycle", output);
        }
        else {
            // No trace through normals ways, so just sys out.
            System.out.println("Method injection failed on CustomLifecycleFactory");
        }

        return lf.getLifecycle(lifecycleId);
    }

    @Override
    public Iterator<String> getLifecycleIds() {
        return lf.getLifecycleIds();
    }

}
