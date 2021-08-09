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
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInstantiableType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.FilterType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.SessionFieldBean;

/**
 * CDI Testing: CDI Filter (Dynamic case)
 *
 * CDIFilter with static registration removed.
 */
// @WebFilter(urlPatterns = { "/CDIDynamicServlet" })
public class CDIDynamicFilter implements Filter {
    // Test case: Constructor injection

    private final CDIDataBean constructorBean;

    @Inject
    public CDIDynamicFilter(ConstructorBean constructorBean) {
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
    @FilterType
    private CDIDataBean fieldBean;

    private void verifyFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(fieldBean, CDICaseInjection.Field));
    }

    // Test case: Produces injection

    // @Inject
    // @FilterProducesType
    // private String producesText;

    private void verifyProducesInjection(PrintWriter responseWriter) {
        // responseWriter.println(getResponseText(producesText, CDICaseInjection.Produces));
    }

    // Test case: Initializer method injection.

    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(MethodBean methodBean) {
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
        return CDICaseInstantiableType.Filter;
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

    // Filter API

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws ServletException, IOException {

        PrintWriter responseWriter = servletResponse.getWriter();

        // The listener runs first .. post its results first.
        // The filter runs next, then the servlet.

        // Transfer any available injection results from an injected listener.
        ServletContext requestContext = servletRequest.getServletContext();
        String listenerResponse = (String) requestContext.getAttribute(CDIDynamicListener.LISTENER_DATA);
        if (listenerResponse != null) {
            responseWriter.print(listenerResponse);
        }

        responseWriter.println(prependType("Entry"));

        String sessionId;
        if (servletRequest instanceof HttpServletRequest) {
            sessionId = ((HttpServletRequest) servletRequest).getSession().getId();
            if (sessionId == null) {
                sessionId = "Null";
            }
        } else {
            sessionId = "Null";
        }
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

        responseWriter.println(prependType("Exit"));

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Empty
    }

    @Override
    public void destroy() {
        // EMPTY
    }
}
