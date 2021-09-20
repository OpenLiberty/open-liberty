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
package com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.AroundConstructOnBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.MyLocalInterface;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.PostConstructBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestException;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestExceptionErrorBeanAC;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestExceptionErrorBeanNoDefaultCtor;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestExceptionErrorBeanPostConstruct;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestNoProceedErrorBean;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestRuntimeException;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestUncheckedExceptionErrorBeanAC;
import com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestUncheckedExceptionErrorBeanPostConstruct;
import com.ibm.ejs.container.EJBConfigurationException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AroundConstructTest.
 * </dl>
 */
@WebServlet("/AroundConstructServlet")
public class AroundConstructServlet extends FATServlet {
    private static final long serialVersionUID = 7133638203504328737L;
    private static final String LOGGER_CLASS_NAME = AroundConstructServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);
    private static final String EJB_MODULE = "AroundConstructEJB.jar";

    @EJB
    StatelessBean bean;
    @EJB
    PostConstructBean pcbean;

    @EJB(beanName = "TestUncheckedExceptionErrorBeanCtor")
    MyLocalInterface mlibean;
    @EJB(beanName = "TestExceptionErrorBeanCtor")
    MyLocalInterface mlibean2;
    @EJB(beanName = "TestCaughtExceptionErrorBeanCtor")
    MyLocalInterface mlibean3;
    @EJB(beanName = "TestCaughtUncheckedExceptionErrorBeanCtor")
    MyLocalInterface mlibean4;

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
        // Verify nested PostConstructs don't return a value through InvocationContext.proceed
        pcbean.verifyLifeCycleNonNullReturn();
    }

    /**
     * Tests expected failure for AroundConstruct defined on enterprise bean class
     */
    @ExpectedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    @Test
    public void testAroundConstructOnErrorBean() throws Exception {
        String beanName = "AroundConstructOnBean";
        try {
            Object bean = new InitialContext().lookup("java:app/AroundConstructEJB/" + AroundConstructOnBean.class.getSimpleName());
            throw new IllegalStateException("expected lookup failure did not occur : " + bean);
        } catch (NamingException ex) {
            svLogger.info("caught expected " + ex);
            Throwable cause = getRootCause(ex);
            if (cause instanceof EJBConfigurationException) {
                String msg = cause.getMessage();
                assertTrue("Incorrect message text : " + msg, msg.startsWith("CNTR0249E: "));
                assertTrue("Incorrect message text : " + msg, msg.contains(beanName));
                assertTrue("Incorrect message text : " + msg, msg.contains(EJB_MODULE));
            } else {
                ex.printStackTrace(System.out);
                fail("Unexpected cause exception : " + cause);
            }
        }
    }

    /**
     * Tests expected failure for bean with no constructor
     */
    @ExpectedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    @Test
    public void testExceptionErrorBeanNoDefaultCtor() throws Exception {
        String beanName = "TestExceptionErrorBeanNoDefaultCtor";
        try {
            TestExceptionErrorBeanNoDefaultCtor bean = (TestExceptionErrorBeanNoDefaultCtor) new InitialContext().lookup("java:app/AroundConstructEJB/TestExceptionErrorBeanNoDefaultCtor");
            bean.method();
            throw new IllegalStateException("expected lookup failure did not occur : " + bean);
        } catch (NamingException ex) {
            svLogger.info("caught expected NamingException");
            Throwable cause = getRootCause(ex);
            if (cause instanceof EJBConfigurationException) {
                String msg = cause.getMessage();
                assertTrue("Incorrect message text : " + msg, msg.contains(beanName));
                assertTrue("Incorrect message text : " + msg, msg.contains(EJB_MODULE));
                assertTrue("Incorrect message text : " + msg, msg.contains("must have a public constructor"));
                assertTrue("Incorrect cause exception : " + cause.getCause(), cause.getCause() instanceof NoSuchMethodException);
            } else {
                ex.printStackTrace(System.out);
                fail("Unexpected cause exception : " + cause);
            }
        }
    }

    /**
     * Tests checked exception thrown by PostConstruct
     */
    @ExpectedFFDC("javax.ejb.EJBException")
    @Test
    public void testExceptionErrorBeanPostConstruct() throws Exception {
        String beanName = "TestExceptionErrorBeanPostConstruct";
        TestExceptionErrorBeanPostConstruct bean = (TestExceptionErrorBeanPostConstruct) new InitialContext().lookup("java:app/AroundConstructEJB/TestExceptionErrorBeanPostConstruct");
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
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
        TestUncheckedExceptionErrorBeanPostConstruct bean = (TestUncheckedExceptionErrorBeanPostConstruct) new InitialContext().lookup("java:app/AroundConstructEJB/TestUncheckedExceptionErrorBeanPostConstruct");
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * Tests checked exception thrown by bean's constructor.
     * The exception is NOT caught and swallowed in AroundConstruct
     * method. We expect to see TestException wrapped in
     * EJBException.
     */
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestException" })
    @Test
    public void testExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestExceptionErrorBeanCtor";
        try {
            mlibean2.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestException);
        }
    }

    /**
     * Tests unchecked exception thrown by bean's constructor.
     * The exception is NOT caught and swallowed in AroundConstruct
     * method. We expect to see TestRuntimeException wrapped in
     * EJBException.
     */
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestRuntimeException" })
    @Test
    public void testUncheckedExceptionErrorBeanCtor() throws Exception {
        String beanName = "TestUncheckedExceptionErrorBeanCtor";
        try {
            mlibean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
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
        try {
            mlibean3.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
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
        try {
            mlibean4.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
            assertTrue("Incorrect cause " + cause, cause instanceof TestRuntimeException);
        }
    }

    /**
     * Tests checked exception thrown by AroundConstruct interceptor method
     */
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestException" })
    @Test
    public void testCheckedExceptionErrorBeanAC() throws Exception {
        String beanName = "TestExceptionErrorBeanAC";
        TestExceptionErrorBeanAC bean = (TestExceptionErrorBeanAC) new InitialContext().lookup("java:app/AroundConstructEJB/TestExceptionErrorBeanAC");
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Throwable cause = e.getCausedByException();
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
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejbcontainer.interceptor.v32.aroundconstruct.ejb.TestRuntimeException" })
    @Test
    public void testUncheckedExceptionErrorBeanAC() throws Exception {
        String beanName = "TestUncheckedExceptionErrorBeanAC";
        TestUncheckedExceptionErrorBeanAC bean = (TestUncheckedExceptionErrorBeanAC) new InitialContext().lookup("java:app/AroundConstructEJB/TestUncheckedExceptionErrorBeanAC");
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            Exception cause = e.getCausedByException();
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
        TestNoProceedErrorBean bean = (TestNoProceedErrorBean) new InitialContext().lookup("java:app/AroundConstructEJB/TestNoProceedErrorBean");
        try {
            bean.method();
            fail(beanName + ".method completed normally");
        } catch (EJBException e) {
            String expected = "AroundConstruct interceptors for the TestNoProceedErrorBean bean in the AroundConstructEJB.jar module in the AroundConstructApp application did not call InvocationContext.proceed()";
            assertEquals("TestNoProceedErrorBean did not return expected exception message", expected, e.getMessage());
        }
    }

    private Throwable getRootCause(Throwable ex) {
        Throwable cause = ex.getCause();
        while (cause != null && !(cause instanceof EJBConfigurationException)) {
            cause = cause.getCause();
        }
        return cause;
    }

}
