/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.MyLocalInterface;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.PostConstructBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.StatelessBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestException;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestExceptionErrorBeanAC;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestExceptionErrorBeanPostConstruct;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestNoProceedErrorBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestRuntimeException;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestUncheckedExceptionErrorBeanAC;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestUncheckedExceptionErrorBeanPostConstruct;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AroundConstructXmlTest.
 * </dl>
 */
@WebServlet("/AroundConstructXmlServlet")
public class AroundConstructXmlServlet extends FATServlet {
    private static final long serialVersionUID = 7133638203504328737L;

    private static final String APPLICATION_NAME = "AroundConstructXmlApp";
    private static final String EJB_MODULE_NAME = "AroundConstructXmlEJB";
    private static final String EJB_MODULE = "AroundConstructXmlEJB.jar";

    /**
     * Tests that AroundConstruct and PostConstruct interact properly,
     * including InvocationContext getContextData.
     * Context data added in AroundConstruct should not exist in
     * getContextData of PostConstruct.
     *
     * Ensures we get expected results in AroundConstruct method for
     * InvocationContext getConstructor, getMethod, getTarget, getTimer,
     * getParameters, and proceed
     *
     * Before Proceed:
     * getTarget: null
     * getMethod: null
     * getTimer: null
     * getConstructor: Constructor<?>
     * getParameters: Object[]
     *
     * proceed(): null
     *
     * After Proceed:
     * getTarget: Object (StatelessBean, eg.)
     * getMethod: null
     * getTimer: null
     * getConstructor: Constructor<?>
     * getParameters: Object[]
     */
    @Test
    public void testInterceptors() throws Exception {
        StatelessBean bean = lookupLocalBean("StatelessBean", StatelessBean.class);

        // Verify PostConstruct was called which checks if interceptor's postConstruct was called.
        bean.verifyPostConstruct();

        // Verify interceptor's AroundInvoke was called.
        bean.verifyInterceptorAroundInvoke();

        bean.verifyInterceptorAroundConstruct();
    }

    /**
     * Tests that PostConstruct interceptor methods cannot return a value
     * through InvocationContext.proceed().
     */
    @Test
    public void testPostConstructInterceptorsReturnValue() throws Exception {
        PostConstructBean pcbean = lookupLocalBean("PostConstructBean", PostConstructBean.class);

        // Verify nested PostConstructs don't return a value through InvocationContext.proceed
        pcbean.verifyLifeCycleNonNullReturn();
    }

    /**
     * Tests checked exception thrown by PostConstruct
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testExceptionErrorBeanPostConstruct() throws Exception {
        String beanName = "TestExceptionErrorBeanPostConstruct";
        TestExceptionErrorBeanPostConstruct bean = lookupLocalBean(beanName, TestExceptionErrorBeanPostConstruct.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestException);
        }
    }

    /**
     * Tests unchecked exception thrown by PostConstruct
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testUncheckedExceptionErrorBeanPostConstruct() throws Exception {
        String beanName = "TestUncheckedExceptionErrorBeanPostConstruct";
        TestUncheckedExceptionErrorBeanPostConstruct bean = lookupLocalBean(beanName, TestUncheckedExceptionErrorBeanPostConstruct.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * Tests checked exception thrown by bean's constructor.
     * The exception is NOT caught and swallowed in AroundConstruct
     * method. We expect to see TestException wrapped in
     * EJBException.
     */
    @ExpectedFFDC({ "javax.enterprise.inject.CreationException", "javax.ejb.EJBException" })
    @Test
    public void testExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestExceptionErrorBeanCtor";
        MyLocalInterface bean = lookupLocalBean(beanName, MyLocalInterface.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestException);
        }
    }

    /**
     * Tests unchecked exception thrown by bean's constructor.
     * The exception is NOT caught and swallowed in AroundConstruct
     * method. We expect to see TestRuntimeException wrapped in
     * EJBException.
     */
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestRuntimeException" })
    @Test
    public void testUncheckedExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestUncheckedExceptionErrorBeanCtor";
        MyLocalInterface bean = lookupLocalBean(beanName, MyLocalInterface.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * Tests checked exception thrown by bean's constructor.
     * The exception is caught and swallowed in AroundConstruct
     * method. We expect to see TestException wrapped in
     * EJBException.
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testCaughtExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestCaughtExceptionErrorBeanCtor";
        MyLocalInterface bean = lookupLocalBean(beanName, MyLocalInterface.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestException);
        }
    }

    /**
     * Tests unchecked exception thrown by bean's constructor.
     * The exception is caught and swallowed in AroundConstruct
     * method. We expect to see TestRuntimeException wrapped in
     * EJBException.
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testCaughtUncheckedExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestCaughtUncheckedExceptionErrorBeanCtor";
        MyLocalInterface bean = lookupLocalBean(beanName, MyLocalInterface.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * Tests checked exception thrown by AroundConstruct interceptor method
     */
    @ExpectedFFDC({ "org.jboss.weld.exceptions.WeldException", "javax.ejb.EJBException" })
    @Test
    public void testCheckedExceptionErrorBeanAC() throws Exception {
        String beanName = "TestExceptionErrorBeanAC";
        TestExceptionErrorBeanAC bean = lookupLocalBean(beanName, TestExceptionErrorBeanAC.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Throwable cause = ex.getCausedByException();
            // Allow WELD to nest the TestException in a WeldException
            if (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertTrue("Incorrect cause " + cause, cause instanceof TestException);
        }
    }

    /**
     * Tests unchecked exception thrown by AroundConstruct interceptor method
     */
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.xml.ejb.TestRuntimeException" })
    @Test
    public void testUncheckedExceptionErrorBeanAC() throws Exception {
        String beanName = "TestUncheckedExceptionErrorBeanAC";
        TestUncheckedExceptionErrorBeanAC bean = lookupLocalBean(beanName, TestUncheckedExceptionErrorBeanAC.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            Exception cause = ex.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * [2.2] If the InvocationContext.proceed method is not invoked by an interceptor method,
     * the target instance will not be created.
     *
     * Don't call proceed() and ensure exception is thrown
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testNoProceedErrorBean() throws Exception {
        String beanName = "TestNoProceedErrorBean";
        TestNoProceedErrorBean bean = lookupLocalBean(beanName, TestNoProceedErrorBean.class);
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException ex) {
            String expected = "AroundConstruct interceptors for the TestNoProceedErrorBean bean in the " + EJB_MODULE +
                              " module in the " + APPLICATION_NAME + " application did not call InvocationContext.proceed()";
            assertEquals("TestNoProceedErrorBean did not return expected exception message", expected, ex.getMessage());
        }
    }

    private <T> T lookupLocalBean(String beanName, Class<T> interfaceClass) throws NamingException {
        Object bean = new InitialContext().lookup("java:app/" + EJB_MODULE_NAME + "/" + beanName);
        return interfaceClass.cast(bean);
    }

}
