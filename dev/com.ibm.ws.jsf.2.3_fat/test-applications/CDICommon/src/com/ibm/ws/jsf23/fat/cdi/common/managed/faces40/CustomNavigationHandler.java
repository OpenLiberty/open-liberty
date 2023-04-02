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

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.faces40.NavigationHandlerBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;

/**
 * Custom navigation handler that tests field and method injection. No constructor injection.
 */
public class CustomNavigationHandler extends NavigationHandler {

    // Field Injected Bean
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
        System.out.println("CustomNavigationHandler preDestroy called.");
    }

    //Method Injected bean
    @Inject
    public void setMethodBean(MethodBean bean) {
        _methodBean = bean;
    }

    /*
     * Looks for the next page. If is is the NavaigationHandlerFail page the
     * navigation is changed the navigation handler pass page. For all other
     * request the next page is assumed to be .xhtml.
     */
    @Override
    public void handleNavigation(FacesContext context, String fromAction, String outcome) {
        System.out.println("handleNavigation() fromAction = " + fromAction + ", outcome = " + outcome);
        String nextPage = "/" + outcome + ".xhtml";
        if (outcome != null) {
            if (outcome.equals("NavigationHandlerFail")) {

                nextPage = "/NavigationHandlerPass.xhtml";

                // Look up the navigationHandlerBean using CDI
                BeanManager bm = CDI.current().getBeanManager();
                Set<?> navigationHandlerBeans = bm.getBeans("navigationHandlerBean");
                Bean<?> bean = (Bean<?>) navigationHandlerBeans.iterator().next();
                CreationalContext<?> ctx = bm.createCreationalContext(bean);

                NavigationHandlerBean testBean = (NavigationHandlerBean) bm
                                .getReference(bean, NavigationHandlerBean.class, ctx);

                if (testBean != null) {

                    String outcomeData = ":NavigationHandler:";
                    outcomeData += _postConstruct;
                    if (_fieldBean != null) {
                        outcomeData += _fieldBean.getData();
                    } else {
                        outcomeData += ":FieldInjectionFailed:";
                    }

                    if (_methodBean == null) {
                        outcomeData += ":MethodInjectionFailed:";
                    } else {
                        outcomeData += _methodBean.getData();
                    }

                    testBean.setData(outcomeData);

                }

            }
            ViewHandler views = context.getApplication().getViewHandler();
            UIViewRoot view = views.createView(context, nextPage);
            context.setViewRoot(view);
            context.renderResponse();
        }

    }
}
