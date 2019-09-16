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
package com.ibm.ws.jsf22.fat.cdicommon.managed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.FieldBean;
import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.MethodBean;

/**
 *
 */
public class CustomResourceHandler extends ResourceHandlerWrapper {

    private ResourceHandler rh = null;

    private boolean calledOnce = false;

    // Field Injected bean
    @Inject
    @ManagedBeanType
    private FieldBean fieldBean;

    private MethodBean methBean;

    // Method Injected bean
    @Inject
    public void setMethodBean(MethodBean bean) {
        methBean = bean;
    }

    public CustomResourceHandler(ResourceHandler fac) {
        rh = fac;
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

    @Override
    public boolean isResourceRequest(FacesContext context)
    {

        if (!calledOnce) {
            calledOnce = true;
            StringBuffer buf = new StringBuffer();

            if (methBean != null) {
                buf.append(methBean.getData());
            }
            else {
                buf.append("Method Bean is null");
            }
            if (fieldBean != null) {
                buf.append(fieldBean.getData());
            }
            else {
                buf.append("Field bean is null.");
            }

            buf.append(_postConstruct);

            buf.append(":" + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());

            FacesContext.getCurrentInstance().getExternalContext().log("JSF22: CustomResourceHandler libraryExists called: result- " + buf.toString());
        }
        return super.isResourceRequest(context);

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.application.ResourceHandlerWrapper#getWrapped()
     */
    @Override
    public ResourceHandler getWrapped() {
        return rh;
    }

}
