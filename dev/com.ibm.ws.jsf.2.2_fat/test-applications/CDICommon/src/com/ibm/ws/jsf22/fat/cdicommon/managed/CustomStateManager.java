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
import javax.faces.application.StateManager;
import javax.faces.application.StateManagerWrapper;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.FieldBean;
import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf22.fat.cdicommon.beans.injected.MethodBean;

/**
 *
 */
public class CustomStateManager extends StateManagerWrapper {

    private StateManager sm = null;

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

    public CustomStateManager(StateManager man) {
        sm = man;

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
    public boolean isSavingStateInClient(FacesContext context) {

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

            FacesContext.getCurrentInstance().getExternalContext().log("JSF22: CustomStateManager isSavingStateInClient called: result- " + buf.toString());
        }
        return super.isSavingStateInClient(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.application.StateManagerWrapper#getWrapped()
     */
    @Override
    public StateManager getWrapped() {

        return sm;
    }

}
