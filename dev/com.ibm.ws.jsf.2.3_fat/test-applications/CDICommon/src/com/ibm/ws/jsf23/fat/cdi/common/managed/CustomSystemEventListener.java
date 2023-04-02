/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.common.managed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.factory.FactoryAppBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.factory.FactoryDepBean;

/**
 * Custom system event listener that tests field and method injection. No constructor injection.
 */
public class CustomSystemEventListener implements SystemEventListener {

    // Field injected bean
    @Inject
    private FactoryAppBean fieldBean;

    // Method Injected bean
    private FactoryDepBean methodBean;

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
    public boolean isListenerForSource(Object source) {

        return true;
    }

    @Override
    public void processEvent(SystemEvent event) {

        StringBuffer buf = new StringBuffer();

        if (fieldBean != null) {
            buf.append(fieldBean.getName());
        } else {
            buf.append("Field Bean was not injected.");
        }

        buf.append(_postConstruct);

        if (methodBean != null) {
            methodBean.incrementAppCount();
            methodBean.logFirst(FacesContext.getCurrentInstance().getExternalContext(),
                                this.getClass().getSimpleName(), "processEvent", buf.toString());
        } else {
            FacesContext.getCurrentInstance().getExternalContext().log("CustomSystemEventListener method injection failed: " + buf.toString());

        }

    }
}