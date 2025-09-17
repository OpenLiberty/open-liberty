/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.common.managed.jsf23;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23.ActionListenerBean;

/**
 * Custom action listener that tests field and method injection. No constructor injection.
 */
public class CustomActionListener implements ActionListener {

    // Field Injected bean
    @Inject
    @ManagedBeanType
    private FieldBean _fieldBean;
    //private final FieldBean _fieldBean = null;

    private MethodBean _methodBean = null;

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
     */
    @Override
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
        ActionListenerBean testBean = (ActionListenerBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("actionListenerBean");

        if (testBean != null) {

            String outcome = ":ActionListener:";
            outcome += _postConstruct;
            if (_fieldBean != null) {
                outcome += _fieldBean.getData();
            } else {
                outcome += ":FieldInjectionFailed:";
            }

            if (_methodBean == null)
                outcome += ":MethodInjectionFailed:";
            else
                outcome += _methodBean.getData();

            testBean.setData(outcome);
        }

        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("ActionListenerEnd.jsf");
        } catch (IOException e) {
            System.out.println("Could not redirect to ActionListenerEnd: " + e.getMessage());
        }

    }
}
