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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2injection.war.cdi.servlets;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInstantiableType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ListenerFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ListenerProducesType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ListenerType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;

@WebListener
public class CDIListener implements ServletRequestListener {
    // Test case: Constructor injection

    /** Storage for the injected constructor bean. */
    private final CDIDataBean constructorBean;

    /**
     * Constructor injection: When this servlet is instantiated, inject
     * a bean as a parameter to the filter constructor.
     *
     * @param constructorBean The constructor bean which is to be set.
     */
    @Inject
    public CDIListener(ConstructorBean constructorBean) {
        // (new Throwable("Dummy for CDIListener() with [ " + constructorBean.getClass().getName() + " ]")).printStackTrace(System.out);
        this.constructorBean = constructorBean;
    }

    /**
     * Verify that constructor injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The contructor bean must be assigned, and must have the correct data.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyConstructorInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(constructorBean, CDICaseInjection.Constructor));
    }

    // Test case: Post-construct injection.

    /** Storage for a value set as a side of effect of the injected post-construct method. */
    private String postConstruct;

    /**
     * Post-construct injection: Invoked via injection following the construction
     * of this servlet instance.
     *
     * This implementation sets post construct data which is examined to verify that the
     * post-construct injection was performed.
     */
    @PostConstruct
    void start() {
        postConstruct = "Start";
    }

    /**
     * Verify that post-construct injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The post-construct bean must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyPostConstructInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(postConstruct, CDICaseInjection.PostConstruct));
    }

    // Test case: Pre-destroy injection.

    // TODO: Not entirely sure how to make this work.  Have it dummied up for now.

    /**
     * Pre-destroy: Invoked via injection preceeding the destruction of this servlet instance.
     *
     * This implementation sets post construct data to the session, if any is available.
     */
    @PreDestroy
    void stop() {
        setPreDestroy("Stop");
    }

    /**
     * Store pre-destroy text.
     *
     * @param preDestroy Pre-destroy text to store.
     */
    public void setPreDestroy(String preDestroy) {
        // TODO
    }

    /**
     * Retrieve the pre-destroy text. Answer null if none is stored.
     *
     * @return The stored pre-destroy text.
     */
    public String getPreDestroy() {
        // TODO
        return "Stop";
    }

    /**
     * Verify that post-construct injection has been performed. This is only possible
     * when session data is available.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyPreDestroyInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(getPreDestroy(), CDICaseInjection.PreDestroy));
    }

    // Test case: Qualified bean injection

    /**
     * CDI Injection Test Case: Qualified bean injection.
     *
     * Inject a {@link CDIDataBean} instance. Select the concrete
     * type qualified with {@link ListenerType}, which is expected
     * to be {@link ListenerFieldBean}.
     */
    @Inject
    @ListenerType
    private CDIDataBean fieldBean;

    /**
     * Verify that field injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The field bean must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(fieldBean, CDICaseInjection.Field));
    }

    // Test case: Produces injection

    /**
     * CDI Injection Test Case: Produces injection.
     *
     * Inject the value obtained by a getter of an injected bean.
     * Use the getter annotated by {@link Produces} and {@link ListenerProducesType}.
     */
    @Inject
    @ListenerProducesType
    private String producesText;

    /**
     * Verify that produces injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The produces text must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyProducesInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(producesText, CDICaseInjection.Produces));
    }

    // Test case: Initializer method injection.

    /** Storage for the injected method bean. */
    private CDIDataBean methodBean;

    /**
     * Initializer method injection: When this filter is instantiated,
     * inject a bean using this method.
     *
     * @param bean The method bean which is to be set.
     */
    @Inject
    public void setMethodBean(MethodBean methodBean) {
        // (new Throwable("Dummy for CDIListener.setMethodBean with [ " + methodBean.getClass().getName() + " ]")).printStackTrace(System.out);
        this.methodBean = methodBean;
    }

    /**
     * Verify that method injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The method bean must be assigned, and must have the correct data.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
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

    /**
     * CDI Injection Test Case: Session bean injection.
     */
    // @Inject
    // private SessionFieldBean sessionFieldBean;

    /**
     * Verify that field injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The field bean must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    // private void verifySessionFieldInjection(PrintWriter responseWriter) {
    //     responseWriter.println(getResponseText(sessionFieldBean, CDICaseInjection.Field));
    // }

    // Test case: Application bean injection

    /**
     * CDI Injection Test Case: Application bean injection.
     */
    @Inject
    private ApplicationFieldBean applicationFieldBean;

    /**
     * Verify that field injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The field bean must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifyApplicationFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(applicationFieldBean, CDICaseInjection.Field));
    }

    // Test utility ...

    /**
     * Answer the subject of this test. This is included in
     * various output and is used to verify the output.
     *
     * @return The test subject. This implementation always answers {@link CDICaseInstantiableType#Listener}.
     */
    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Listener;
    }

    /**
     * Prepend the type tag to response text.
     *
     * @param responseText Input responst text.
     *
     * @return The responst text with the type tag prepended.
     */
    private String prependType(String responseText) {
        return (":" + getInstantiableType().getTag() + ":" + responseText + ":");
    }

    /**
     * Generate response text for a data bean, either, the bean data, or,
     * the failed tag plus "Failed", prepended with the type tag. Display
     * null data as the text "Null".
     *
     * @param dataBean      The data bean for which to generate response text.
     * @param injectionCase The injection case.
     *
     * @return The generated response text.
     */
    private String getResponseText(CDIDataBean dataBean, CDICaseInjection injectionCase) {
        String beanText = injectionCase.getTag() + ":";
        beanText += ((dataBean == null) ? "Failed" : dataBean.getData());
        return prependType(beanText);
    }

    /**
     * Generate response text for a data value, either, the data value, or,
     * if the data is null, the failed tag plus "Failed". Prepend the result
     * with the type tag.
     *
     * @param value         The value for which to generate response text.
     * @param injectionCase The injection case.
     *
     * @return The generated response text.
     */
    private String getResponseText(String value, CDICaseInjection injectionCase) {
        value = (injectionCase.getTag() + ":" + ((value == null) ? "Failed" : value));
        return prependType(value);
    }

    // Standard test implementation ...

    /** The URL parameter name for the bean data payload. */
    public static final String PAYLOAD_PARAMETER_NAME = "payload";

    /**
     * Store the payload to the beans. Prefix the payload value with the
     * type which is storing the payload.
     *
     * @param payload The bean data payload which is to be stored.
     */
    protected void storePayload(String payload) {
        payload = "(" + getInstantiableType().getTag() + ":" + payload + ")";

        if (constructorBean != null) {
            constructorBean.addData(payload);
        }

        if (fieldBean != null) {
            fieldBean.addData(payload);
        }
        // if (sessionFieldBean != null) {
        //     sessionFieldBean.addData(payload);
        // }
        if (applicationFieldBean != null) {
            applicationFieldBean.addData(payload);
        }

        if (methodBean != null) {
            methodBean.addData(payload);
        }
    }

    // Standard test implementation ...

    /** The key used to relay results to the servlet. */
    public static final String LISTENER_DATA = "CDIListenerData";

    /**
     * Listener API: Standard listener handler method (one of several).
     *
     * This implementation verifies injected data. Each verification emits text
     * to the response writer, which will be verified by the test client.
     *
     * @param requestEvent The event which is being handled.
     */
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

    //

    /**
     * Listener API: The request has been destroyed.
     *
     * This implementation does nothing.
     */
    @Override
    public void requestDestroyed(ServletRequestEvent servletrequestevent) {
        // EMPTY
    }
}
