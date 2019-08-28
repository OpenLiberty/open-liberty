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
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.ActionListenerBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ConstructorBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.FieldBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ManagedBeanType;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.MethodBean;
import  com.ibm.ws.jsf22.fat.cdicommon.interceptors.TestActionListener;

/**
 *
 */
public class CustomActionListener implements ActionListener {

    // Field Injected bean
    @Inject
    @ManagedBeanType
    private FieldBean _fieldBean;
    //private final FieldBean _fieldBean = null;

    private ConstructorBean _constructorBean = null;
    private MethodBean _methodBean = null;

    // Constructor Injected bean
    @Inject
    public CustomActionListener(ConstructorBean bean) {
        _constructorBean = bean;
    }

    String _postConstruct = ":PostConstructNotCalled:";

    @PostConstruct
    public void start() {
        _postConstruct = ":PostConstructCalled:";
    }

    @PreDestroy
    public void stop() {
        System.out.println("CustomActionListener preDestroy called.");
    }

    // Method Injected bean
    @Inject
    public void setMethodBean(MethodBean bean) {
        _methodBean = bean;
    }

    /*
     * Looks for the actionListenreBean which will be found if the bean has already been used or is the
     * subject of the action. If is is the actionListenreBean is found it is updated with information which
     * indicates whether or not the various injections have passed.
     * The @TestActionListener annotation is added to cause this method to be intercepted by
     * TestActionListenerInterceptor
     */
    @Override
    @TestActionListener
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
        ActionListenerBean testBean = (ActionListenerBean) FacesContext.getCurrentInstance().
                        getExternalContext().getSessionMap().get("actionListenerBean");

        if (testBean != null) {

            String outcome = ":ActionListener:";
            outcome += _postConstruct;
            if (_fieldBean != null) {
                outcome += _fieldBean.getData();
            } else {
                outcome += ":FieldInjectionFailed:";
            }

            if (_constructorBean != null) {
                outcome += _constructorBean.getData();
            } else {
                outcome += ":ConstructorInjectionFailed:";
            }

            if (_methodBean == null)
                outcome += ":MethodInjectionFailed:";
            else
                outcome += _methodBean.getData();

            testBean.setData(outcome);
        }

    }
}
