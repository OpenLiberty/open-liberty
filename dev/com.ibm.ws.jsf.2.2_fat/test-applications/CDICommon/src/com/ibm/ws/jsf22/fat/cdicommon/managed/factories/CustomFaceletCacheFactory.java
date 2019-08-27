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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryAppBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryDepBean;

/**
 *
 */
public class CustomFaceletCacheFactory extends FaceletCacheFactory {

    private FaceletCacheFactory fch = null;

    // Field injected bean
    @Inject
    private FactoryAppBean fieldBean;

    // Method Injected bean
    private FactoryDepBean methodBean;

    public CustomFaceletCacheFactory(FaceletCacheFactory fac) {
        fch = fac;
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
    public FaceletCache getFaceletCache() {
        String output = "Field Injected App Bean is NULL";

        if (fieldBean != null) {
            output = fieldBean.getName();
        }

        output += _postConstruct;

        if (methodBean != null) {
            methodBean.incrementAppCount();
            methodBean.logFirst(FacesContext.getCurrentInstance().getExternalContext(), this.getClass().getSimpleName(), "getFaceletCache", output);
        }
        else {
            FacesContext.getCurrentInstance().getExternalContext().log("CustomFaceletCacheFactory method injection failed.");
        }

        return fch.getFaceletCache();
    }

}
