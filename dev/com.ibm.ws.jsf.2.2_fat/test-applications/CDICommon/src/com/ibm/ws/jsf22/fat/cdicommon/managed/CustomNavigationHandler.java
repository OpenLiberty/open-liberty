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
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import  com.ibm.ws.jsf22.fat.cdicommon.beans.NavigationHandlerBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ConstructorBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.FieldBean;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.ManagedBeanType;
import  com.ibm.ws.jsf22.fat.cdicommon.beans.injected.MethodBean;
import  com.ibm.ws.jsf22.fat.cdicommon.interceptors.TestNavigationHandler;

/**
 *
 */
public class CustomNavigationHandler extends NavigationHandler {

    // Field Injected Bean
    @Inject
    @ManagedBeanType
    private FieldBean _fieldBean;
    //private final FieldBean _fieldBean = null;

    private ConstructorBean _constructorBean = null;
    private MethodBean _methodBean = null;

    // Constructor Injected bean
    @Inject
    public CustomNavigationHandler(ConstructorBean bean) {
        super();
        _constructorBean = bean;
        System.out.println("CustomNavigationHandler : Injected constructor");
    }

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
     * The @TestNavigationHandler annotation is added to cause this method to be intercepted by
     * TestNavigationHandlerInterecptor
     */
    @Override
    @TestNavigationHandler
    public void handleNavigation(FacesContext context, String fromAction, String outcome) {
        // TODO Auto-generated method stub

        System.out.println("handleNavigation() fromAction = " + fromAction + ", outcome = " + outcome);
        String nextPage = "/" + outcome + ".xhtml";
        if (outcome != null) {
            if (outcome.equals("NavigationHandlerFail")) {

                nextPage = "/NavigationHandlerPass.xhtml";

                NavigationHandlerBean testBean = (NavigationHandlerBean) FacesContext.getCurrentInstance().
                                getExternalContext().getSessionMap().get("navigationHandlerBean");

                if (testBean != null) {

                    String outcomeData = ":NavigationHandler:";
                    outcomeData += _postConstruct;
                    if (_fieldBean != null) {
                        outcomeData += _fieldBean.getData();
                    } else {
                        outcomeData += ":FieldInjectionFailed:";
                    }

                    if (_constructorBean != null) {
                        outcomeData += _constructorBean.getData();
                    } else {
                        outcomeData += ":ConstructorInjectionFailed:";
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
