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
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryAppBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.factory.FactoryDepBean;

/**
 *
 */
public class CustomRenderKitFactory extends RenderKitFactory {

    private RenderKitFactory rkf = null;

    private final boolean calledOnce = false;

    // Field injected bean
    @Inject
    private FactoryAppBean fieldBean;

    // Method Injected bean
    private FactoryDepBean methodBean;

    public CustomRenderKitFactory(RenderKitFactory fac) {
        rkf = fac;
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
    public void addRenderKit(String renderKitId, RenderKit renderKit) {
        rkf.addRenderKit(renderKitId, renderKit);
    }

    @Override
    public RenderKit getRenderKit(FacesContext context, String renderKitId) {
        String output = "Field Injected App Bean is NULL";

        if (fieldBean != null) {
            output = fieldBean.getName();
        }

        output += _postConstruct;

        if (methodBean != null) {
            methodBean.incrementAppCount();
            methodBean.logFirst(FacesContext.getCurrentInstance().getExternalContext(), this.getClass().getSimpleName(), "getRenderKit", output);
        }
        else {
            FacesContext.getCurrentInstance().getExternalContext().log("CustomRenderKitFactory method injection failed.");
        }

        return rkf.getRenderKit(context, renderKitId);
    }

    @Override
    public Iterator<String> getRenderKitIds() {
        return rkf.getRenderKitIds();
    }

}
