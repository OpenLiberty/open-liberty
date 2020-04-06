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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.listeners;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.AsyncListenerType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.FieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.MethodBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors.OnCompleteType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors.StartAsyncType;

/**
 *
 */
public class CDIAsyncListener implements AsyncListener {

    private static final Logger LOG = Logger.getLogger(CDIAsyncListener.class.getName());

    @Inject
    @AsyncListenerType
    private FieldBean fieldBean;

    private MethodBean methodBean;
    private final ConstructorBean constructorBean;
    private String postConstruct;

    @Inject
    public CDIAsyncListener(ConstructorBean bean) {
        constructorBean = bean;
    }

    @PostConstruct
    void start() {
        postConstruct = ":postConstructCalled:";
    }

    @Inject
    public void setMethodBean(MethodBean bean) {
        methodBean = bean;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.AsyncListener#onComplete(javax.servlet.AsyncEvent)
     */
    @Override
    @OnCompleteType
    public void onComplete(AsyncEvent arg0) throws IOException {
        // TODO Auto-generated method stub

        PrintWriter out = arg0.getSuppliedResponse().getWriter();

        String intAttr = (String) arg0.getSuppliedRequest().getAttribute("OnCompleteInterceptor");

        if (intAttr != null && intAttr.equals(":OnCompleteInterceptor:")) {
            out.println("*** In AyncListener, onComplete :Interceptor was called:");
        } else {
            out.println("*** In AyncListener, onComplete :Interceptor was not called:");
        }

        LOG.info("*** In AyncListener, onComplete : OnCompleteInterceptor attribute = " + (String) arg0.getSuppliedRequest().getAttribute("OnCompleteInterceptor"));

        if (constructorBean == null) {
            out.println("*** In AyncListener, onComplete :ConstructorInjectFailed:");
        } else {
            out.println("*** In AyncListener, onComplete :" + constructorBean.getData());
        }

        if (postConstruct == null) {
            out.println("*** In AyncListener, onComplete :PostConstructFailed:");
        } else {
            out.println("*** In AyncListener, onComplete :" + postConstruct);
        }

        if (fieldBean == null) {
            out.println("*** In AyncListener, onComplete :FieldInjectFailed:");
        } else {
            out.println("*** In AyncListener, onComplete :" + fieldBean.getData());
        }

        if (methodBean == null) {
            out.println("*** In AyncListener, onComplete :MethodInjectFailed:");
        } else {
            out.println("*** In AyncListener, onComplete :" + methodBean.getData());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.AsyncListener#onError(javax.servlet.AsyncEvent)
     */
    @Override
    public void onError(AsyncEvent arg0) throws IOException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.AsyncListener#onStartAsync(javax.servlet.AsyncEvent)
     */
    @Override
    @StartAsyncType
    public void onStartAsync(AsyncEvent arg0) throws IOException {

        arg0.getAsyncContext().addListener(this, arg0.getSuppliedRequest(), arg0.getSuppliedResponse());

        PrintWriter out = arg0.getSuppliedResponse().getWriter();

        LOG.info("*** In AyncListener, onStartAsync : StartAsyncInterceptor attribute = " + (String) arg0.getSuppliedRequest().getAttribute("StartAsyncInterceptor"));

        String intAttr = (String) arg0.getSuppliedRequest().getAttribute("StartAsyncInterceptor");

        if (intAttr != null && intAttr.equals(":StartAsyncInterceptor:")) {
            out.println("*** In AyncListener, onStartAsync :Interceptor was called:");
        } else {
            out.println("*** In AyncListener, onSyartAsync :Interceptor was not called:");
        }

        if (constructorBean == null) {
            out.println("*** In AyncListener, onStartAsync :ConstructorInjectFailed:");
        } else {
            out.println("*** In AyncListener, onStartAsync :" + constructorBean.getData());
        }

        if (postConstruct == null) {
            out.println("*** In AyncListener, onStartAsync :PostConstructFailed:");
        } else {
            out.println("*** In AyncListener, onStartAsync :" + postConstruct);
        }

        if (fieldBean == null) {
            out.println("*** In AyncListener, onStartAsync :FieldInjectFailed:");
        } else {
            out.println("*** In AyncListener, onStartAsync :" + fieldBean.getData());
        }

        if (methodBean == null) {
            out.println("*** In AyncListener, onStartAsync :MethodInjectFailed:");
        } else {
            out.println("*** In AyncListener, onStartAsync :" + methodBean.getData());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
     */
    @Override
    public void onTimeout(AsyncEvent arg0) throws IOException {
        // TODO Auto-generated method stub

    }

}
