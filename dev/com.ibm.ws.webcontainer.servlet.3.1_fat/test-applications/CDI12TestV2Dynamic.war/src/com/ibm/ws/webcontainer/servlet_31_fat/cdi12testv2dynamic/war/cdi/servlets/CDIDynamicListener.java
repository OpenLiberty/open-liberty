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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2dynamic.war.cdi.servlets;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInstantiableType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ListenerProducesType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ListenerType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;

/**
 * CDI Testing: CDI Listener (Dynamic case)
 *
 * CDIListener with static registration removed.
 */
// @WebListener
public class CDIDynamicListener implements ServletRequestListener {
    // Test case: Constructor injection

    private final CDIDataBean constructorBean;

    @Inject
    public CDIDynamicListener(ConstructorBean constructorBean) {
        this.constructorBean = constructorBean;
    }

    private void verifyConstructorInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(constructorBean, CDICaseInjection.Constructor));
    }

    // Test case: Post-construct injection.

    private String postConstruct;

    @PostConstruct
    void start() {
        postConstruct = "Start";
    }

    private void verifyPostConstructInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(postConstruct, CDICaseInjection.PostConstruct));
    }

    // Test case: Pre-destroy injection.

    // TODO: Not entirely sure how to make this work.  Have it dummied up for now.

    @PreDestroy
    void stop() {
        setPreDestroy("Stop");
    }

    public void setPreDestroy(String preDestroy) {
        // TODO
    }

    public String getPreDestroy() {
        // TODO
        return "Stop";
    }

    private void verifyPreDestroyInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(getPreDestroy(), CDICaseInjection.PreDestroy));
    }

    // Test case: Qualified bean injection

    @Inject
    @ListenerType
    private CDIDataBean fieldBean;

    private void verifyFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(fieldBean, CDICaseInjection.Field));
    }

    // Test case: Produces injection

    @Inject
    @ListenerProducesType
    private String producesText;

    private void verifyProducesInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(producesText, CDICaseInjection.Produces));
    }

    // Test case: Initializer method injection.

    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(MethodBean methodBean) {
        // (new Throwable("Dummy for CDIListener.setMethodBean with [ " + methodBean.getClass().getName() + " ]")).printStackTrace(System.out);
        this.methodBean = methodBean;
    }

    private void verifyMethodInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(methodBean, CDICaseInjection.Method));
    }

    // Test case: Session bean injection

    // Stack Dump = org.jboss.weld.context.ContextNotActiveException:
    // WELD-001303: No active contexts for scope type javax.enterprise.context.SessionScoped
    //
    // Then:
    // "Session scoped contexts are only active during servlet calls with the service() method, or when executing a servlet filter."
    //
    // From:
    // http://stackoverflow.com/questions/15496374/weld-001303-no-active-contexts-for-scope-type-javax-enterprise-context-sessionsc

    // @Inject
    // private SessionFieldBean sessionFieldBean;

    // private void verifySessionFieldInjection(PrintWriter responseWriter) {
    //     responseWriter.println(getResponseText(sessionFieldBean, CDICaseInjection.Field));
    // }

    // Test case: Application bean injection

    @Inject
    private ApplicationFieldBean applicationFieldBean;

    private void verifyApplicationFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(applicationFieldBean, CDICaseInjection.Field));
    }

    // Test utility ...

    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Listener;
    }

    private String prependType(String responseText) {
        return (":" + getInstantiableType().getTag() + ":" + responseText + ":");
    }

    private String getResponseText(CDIDataBean dataBean, CDICaseInjection injectionCase) {
        String beanText = injectionCase.getTag() + ":";
        beanText += ((dataBean == null) ? "Failed" : dataBean.getData());
        return prependType(beanText);
    }

    private String getResponseText(String value, CDICaseInjection injectionCase) {
        value = (injectionCase.getTag() + ":" + ((value == null) ? "Failed" : value));
        return prependType(value);
    }

    // Standard test implementation ...

    public static final String PAYLOAD_PARAMETER_NAME = "payload";

    protected void storePayload(String payload) {
        payload = "(" + getInstantiableType().getTag() + ":" + payload + ")";

        if (constructorBean != null) {
            constructorBean.addData(payload);
        }
        if (fieldBean != null) {
            fieldBean.addData(payload);
        }
        if (applicationFieldBean != null) {
            applicationFieldBean.addData(payload);
        }
        if (methodBean != null) {
            methodBean.addData(payload);
        }
    }

    // Listener API

    public static final String LISTENER_DATA = "CDIListenerData";

    @Override
    public void requestInitialized(ServletRequestEvent requestEvent) {
        ServletRequest servletRequest = requestEvent.getServletRequest();

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        PrintWriter responseWriter = new PrintWriter(responseStream);

        responseWriter.println(prependType("Entry"));

        String usePayload = servletRequest.getParameter(PAYLOAD_PARAMETER_NAME);
        if (usePayload != null) {
            responseWriter.println(prependType("Payload=" + usePayload));
            storePayload(usePayload);
        }

        // Verify the Servlet injections.

        verifyConstructorInjection(responseWriter);
        verifyPostConstructInjection(responseWriter);
        verifyPreDestroyInjection(responseWriter);

        verifyFieldInjection(responseWriter);
        // verifySessionFieldInjection(responseWriter);
        verifyApplicationFieldInjection(responseWriter);

        verifyProducesInjection(responseWriter);

        verifyMethodInjection(responseWriter);

        responseWriter.println(prependType("Exit"));

        responseWriter.flush();
        responseWriter.close();

        String responseText = responseStream.toString();

        ServletContext requestContext = servletRequest.getServletContext();

        requestContext.setAttribute(LISTENER_DATA, responseText);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletrequestevent) {
        // EMPTY
    }
}
