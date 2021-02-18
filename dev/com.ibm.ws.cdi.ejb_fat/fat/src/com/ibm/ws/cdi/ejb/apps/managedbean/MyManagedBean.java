/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.managedbean;

import java.util.List;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyCDIInterceptorBinding;
import com.ibm.ws.cdi.ejb.apps.managedbean.interceptors.MyNonCDIInterceptor;

/**
 *
 */
@ManagedBean
@MyCDIInterceptorBinding
@Interceptors({ MyNonCDIInterceptor.class })
public class MyManagedBean {

    private List<String> msgList;

    @Resource(name = "myBeanName")
    String myBeanName;

    @Inject
    MyEJBBeanLocal myEjbBean;

    public List<String> getMsgList() {

        if (this.msgList == null) {
            this.msgList = myEjbBean.getMsgList();
        }
        return this.msgList;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void doPostConstruct() {
        CounterUtil.addToMsgList(this.getClass().getSuperclass().getSimpleName() + " called postConstruct()");
    }

    @SuppressWarnings("unused")
    @PreDestroy
    private void doPreDestroy() {
        System.out.println("@PreDestory called " + this.getClass().getSuperclass().getSimpleName());
    }

}
