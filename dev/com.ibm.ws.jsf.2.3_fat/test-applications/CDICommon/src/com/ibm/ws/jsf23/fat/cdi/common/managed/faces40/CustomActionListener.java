/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.common.managed.faces40;

import java.io.IOException;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.faces40.ActionListenerBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;

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
        // Look up the ActionListenerBean using CDI
        BeanManager bm = CDI.current().getBeanManager();
        Set<?> actionListenerBeans = bm.getBeans("actionListenerBean");
        Bean<?> bean = (Bean<?>) actionListenerBeans.iterator().next();
        CreationalContext<?> ctx = bm.createCreationalContext(bean);

        ActionListenerBean testBean = (ActionListenerBean) bm
                        .getReference(bean, ActionListenerBean.class, ctx);

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
