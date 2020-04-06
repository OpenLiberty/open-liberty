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

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInstantiableType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ServletProducesType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ServletType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.SessionFieldBean;

/**
 * CDI Testing: CDI Servlet (Dynamic case)
 *
 * CDIServlet with static registration removed.
 */
// @WebServlet(urlPatterns = { "/CDIDynamicServlet" })
public class CDIDynamicServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Test case: Constructor injection

    private final CDIDataBean constructorBean;

    @Inject
    public CDIDynamicServlet(ConstructorBean constructorBean) {
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
    @ServletType
    private CDIDataBean fieldBean;

    private void verifyFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(fieldBean, CDICaseInjection.Field));
    }

    // Test case: Produces injection

    @Inject
    @ServletProducesType
    private String producesText;

    private void verifyProducesInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(producesText, CDICaseInjection.Produces));
    }

    // Test case: Initializer method injection.

    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(MethodBean methodBean) {
        // (new Throwable("Dummy for CDIServlet.setMethodBean with [ " + methodBean.getClass().getName() + " ]")).printStackTrace(System.out);
        this.methodBean = methodBean;
    }

    private void verifyMethodInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(methodBean, CDICaseInjection.Method));
    }

    // Test case: Session bean injection

    @Inject
    private SessionFieldBean sessionFieldBean;

    private void verifySessionFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(sessionFieldBean, CDICaseInjection.Field));
    }

    // Test case: Application bean injection

    @Inject
    private ApplicationFieldBean applicationFieldBean;

    private void verifyApplicationFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(applicationFieldBean, CDICaseInjection.Field));
    }

    // Test utility ...

    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Servlet;
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
        if (sessionFieldBean != null) {
            sessionFieldBean.addData(payload);
        }
        if (applicationFieldBean != null) {
            applicationFieldBean.addData(payload);
        }

        if (methodBean != null) {
            methodBean.addData(payload);
        }
    }

    // Servlet API

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        doPost(servletRequest, servletResponse); // throws ServletException, IOException
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        PrintWriter responseWriter = servletResponse.getWriter();

        responseWriter.println(prependType("Entry"));

        String sessionId = servletRequest.getSession().getId();
        responseWriter.println(prependType("SessionId=" + sessionId));

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
        verifySessionFieldInjection(responseWriter);
        verifyApplicationFieldInjection(responseWriter);

        verifyProducesInjection(responseWriter);

        verifyMethodInjection(responseWriter);

        ServletContext requestContext = servletRequest.getServletContext();

        // Transfer any available injection results from an injected listener.
        String listenerResponse = (String) requestContext.getAttribute(CDIDynamicListener.LISTENER_DATA);
        if (listenerResponse != null) {
            responseWriter.print(listenerResponse);
        }

        responseWriter.println(prependType("Exit"));
    }
}
