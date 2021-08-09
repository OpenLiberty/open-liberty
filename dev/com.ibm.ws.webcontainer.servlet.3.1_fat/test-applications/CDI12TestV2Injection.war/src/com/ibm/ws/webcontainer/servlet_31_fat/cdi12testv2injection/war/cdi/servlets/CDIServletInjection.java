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

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInstantiableType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ConstructorBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ServletFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ServletProducesType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ServletType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.SessionFieldBean;

/**
 * CDI Injection servlet.
 *
 * Two injection test suites are currently implemented. One, CDI injection to Servlet. Two,
 * CDI injection to Filter.
 *
 * The same servlet (this servlet) is used for both tests. Servlet injection is always active.
 * Filter injection is active only for the URL pattern "/CDIServletFiltered".
 *
 * CDI is specified by JSR 299 (formerly known as Web Beans) and by the related specifications
 * JSR 330 (Dependency Injection for Java) and JSR 316 (the Java EE 6 platform specification).
 *
 * For CDI information, see {@linkplain "http://docs.oracle.com/javaee/6/tutorial/doc/gjbnr.html"}.
 *
 * See also {@linkplain "https://docs.oracle.com/javaee/7/tutorial/partcdi.htm#GJBNR"}.
 *
 * For servlet filter information, see {@linkplain "http://docs.oracle.com/cd/B14099_19/web.1012/b14017/filters.htm"}.
 *
 * For CDI qualifier information, see {@linkplain "https://blogs.oracle.com/arungupta/entry/totd_161_java_ee_6"}.
 * In particular:
 *
 * <quote>
 * The CDI specification (JSR-299) defines "Qualifier" as a means to uniquely identify
 * one of the multiple implementations of the bean type to be injected. The spec defines
 * certain built-in qualifiers (\@Default, \@Any, \@Named, \@New) and new qualifiers can
 * be easily defined as well. This Tip Of The Day (TOTD) discusses the in-built qualifiers
 * and how they can be used.
 * </quote> *
 * This type provides the basic pattern for CDI injection tests. These
 * injections are performed:
 *
 * For CDI {@link Produces} information, see {@linkplain "http://docs.oracle.com/javaee/6/tutorial/doc/gjdid.html"}.
 * See also {@linkplain "http://docs.oracle.com/javaee/6/tutorial/doc/gkgkv.html"}.
 *
 * <ul>
 * <li>Bean injection (see {@link #fieldBean}.
 * <li>
 * </ul>
 *
 * Per {@plainlink "http://docs.oracle.com/javaee/6/tutorial/doc/gjebj.html"}:
 *
 * <quote>
 * CDI redefines the concept of a bean beyond its use in other Java technologies,
 * such as the JavaBeans and Enterprise JavaBeans (EJB) technologies. In CDI,
 * a bean is a source of contextual objects that define application state and/or
 * logic. A Java EE component is a bean if the lifecycle of its instances may be
 * managed by the container according to the lifecycle context model defined in
 * the CDI specification.
 *
 * More specifically, a bean has the following attributes:
 *
 * <ul>
 * <li>A (nonempty) set of bean types
 * <li>A (nonempty) set of qualifiers (see Using Qualifiers)
 * <li>A scope (see Using Scopes)
 * <li>Optionally, a bean EL name (see Giving Beans EL Names)
 * <li>A set of interceptor bindings
 * <li>A bean implementation
 * </ul>
 *
 * A bean type defines a client-visible type of the bean. Almost any Java type may be a bean type of a bean.
 *
 * <ul>
 * <li>A bean type may be an interface, a concrete class, or an abstract class and may be declared final or have final methods.
 * <li>A bean type may be a parameterized type with type parameters and type variables.
 * <li>A bean type may be an array type. Two array types are considered identical only if the element type is identical.
 * <li>A bean type may be a primitive type. Primitive types are considered to be identical to their corresponding wrapper types in java.lang.
 * <li>A bean type may be a raw type.
 * </ul>
 *
 * Per {@linkplain "http://docs.oracle.com/javaee/6/tutorial/doc/gjbbk.html"}:
 *
 * <ul>
 * <li>\@RequestScoped - A user’s interaction with a web application in a single HTTP request.
 * <li>\@SessionScoped - A user’s interaction with a web application across multiple HTTP requests.
 * <li>\@ApplicationScoped - Shared state across all users’ interactions with a web application.
 * <li>\@Dependent - The default scope if none is specified; it means that an object exists to serve exactly one client (bean) and has the same lifecycle as that client (bean).
 * <li>\@ConversationScoped - A user’s interaction with a JavaServer Faces application, within explicit developer-controlled boundaries that extend the scope across multiple
 * invocations of the JavaServer Faces lifecycle. All long-running conversations are scoped to a particular HTTP servlet session and may not cross session boundaries.
 * </ul>
 */
@WebServlet(urlPatterns = { "/CDIInjection" })
public class CDIServletInjection extends HttpServlet {
    /** Standard serialization ID. */
    private static final long serialVersionUID = 1L;

    // Test case: Constructor injection

    /** Storage for the injected constructor bean. */
    private final CDIDataBean constructorBean;

    /**
     * Constructor injection: When this servlet is instantiated, inject
     * a bean as a parameter to the servlet constructor.
     *
     * @param constructorBean The constructor bean which is to be set.
     */
    @Inject
    public CDIServletInjection(ConstructorBean constructorBean) {
        // (new Throwable("Dummy for CDIServlet() with [ " + constructorBean.getClass().getName() + " ]")).printStackTrace(System.out);
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
     * type qualified with {@link ServletType}, which is expected
     * to be {@link ServletFieldBean}.
     */
    @Inject
    @ServletType
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
     * Use the getter annotated by {@link Produces} and {@link ServletProducesType}.
     */
    @Inject
    @ServletProducesType
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
     * Initializer method injection: When this servlet is instantiated,
     * inject a bean using this method.
     *
     * See JSR 299 section 3.9.
     *
     * The parameter may be qualified, in which case an instance of the qualifier
     * specific bean subtype will be injected. This text uses no qualifier,
     * meaning, the default type of {@link ServletMethodBean} will be injected.
     *
     * @param bean The method bean which is to be set.
     */
    @Inject
    public void setMethodBean(MethodBean methodBean) {
        // (new Throwable("Dummy for CDIServlet.setMethodBean with [ " + methodBean.getClass().getName() + " ]")).printStackTrace(System.out);
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

    /**
     * CDI Injection Test Case: Session bean injection.
     */
    @Inject
    private SessionFieldBean sessionFieldBean;

    /**
     * Verify that field injection has been performed. Emit text to the
     * the response writer to indicate whether the injection was performed.
     *
     * The field bean must be assigned and must have the correct value.
     *
     * @param responseWriter The writer to which to emit verification output.
     */
    private void verifySessionFieldInjection(PrintWriter responseWriter) {
        responseWriter.println(getResponseText(sessionFieldBean, CDICaseInjection.Field));
    }

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
     * @return The test subject. This implementation always answers {@link CDICaseInstantiableType#Servlet}.
     */
    public CDICaseInstantiableType getInstantiableType() {
        return CDICaseInstantiableType.Servlet;
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

    /**
     * HttpServlet API: Handle a GET type request.
     *
     * This implementation forwards the call to {@link #doGet}.
     *
     * @param servletRequest  The request which is being handled.
     * @param servletResponse The response for the request.
     *
     * @throws ServletException Thrown in case of an error processing the request.
     * @throws IOException      Thrown in case of an error processing the request.
     */
    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        doPost(servletRequest, servletResponse); // throws ServletException, IOException
    }

    /**
     * HttpServlet API: Handle a POST request.
     *
     * This implementation verifies injected data. Each verification emits text
     * to the response writer, which will be verified by the test client.
     *
     * @param servletRequest  The request which is being handled.
     * @param servletResponse The response for the request.
     *
     * @throws ServletException Thrown in case of an error processing the request.
     * @throws IOException      Thrown in case of an error processing the request.
     */
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
        String listenerResponse = (String) requestContext.getAttribute(CDIListener.LISTENER_DATA);
        if (listenerResponse != null) {
            responseWriter.print(listenerResponse);
        }

        responseWriter.println(prependType("Exit"));
    }
}
