/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.session.app;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@SessionScoped
public class MySessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testData;

    @Inject
    private HttpSession session;

    @Inject
    private HttpServletRequest request;

//    Removed until #29434 is fixed
//    @Inject
//    private ServletContext context;

    @Inject
    private BeanManager beanManager;

    @Inject
    private Bean<MySessionBean> bean;

    @Inject
    private Conversation conversation;

    @Inject
    private Instance<Object> instance;

    @Inject
    private Event<Object> event;

    public String getTestData() {
        return testData;
    }

    public void setTestData(String testData) {
        this.testData = testData;
    }

    public void pokeBeans() {
        // Just call a method on each bean to make sure it's really there
        session.getId();
        request.getServletPath();
//      context.getContextPath(); //#29434
        beanManager.isScope(RequestScoped.class);
        bean.getBeanClass();
        conversation.getId();
        instance.isAmbiguous();
        event.select();
    }

}
