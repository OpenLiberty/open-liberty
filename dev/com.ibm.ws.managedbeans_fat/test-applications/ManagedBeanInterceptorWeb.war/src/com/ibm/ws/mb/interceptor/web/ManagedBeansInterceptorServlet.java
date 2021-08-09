/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mb.interceptor.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.mb.interceptor.aroundconstruct.AroundConstructInterceptor;
import com.ibm.ws.mb.interceptor.aroundconstruct.AroundConstructInterceptor.TestType;
import com.ibm.ws.mb.interceptor.aroundconstruct.AroundConstructManagedBean;
import com.ibm.ws.mb.interceptor.aroundconstruct.AroundConstructMethodBean;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ChainExceptionManagedBean;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ChainExceptionManagedBean.ChainExceptionTestType;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ConstructorException;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ExceptionInterceptor;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ExceptionInterceptor.ExceptionTestType;
import com.ibm.ws.mb.interceptor.aroundconstructexception.ExceptionManagedBean;
import com.ibm.ws.mb.interceptor.aroundconstructexception.InterceptorException;
import com.ibm.ws.mb.interceptor.chainaroundconstruct.ChainManagedBean;
import com.ibm.ws.mb.interceptor.paramaroundconstruct.ParamAroundConstructInterceptor;
import com.ibm.ws.mb.interceptor.paramaroundconstruct.ParamAroundConstructInterceptor.ParamTestType;
import com.ibm.ws.mb.interceptor.paramaroundconstruct.ParamAroundConstructManagedBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ManagedBeansInterceptorServlet")
public class ManagedBeansInterceptorServlet extends FATServlet {

    private static final String CLASS_NAME = ManagedBeansInterceptorServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final String managedBeanLookup = "java:module" + "/" + "AroundContructManagedBean";
    private final String methodBeanLookup = "java:module" + "/" + "AroundContructMethodBean";
    private final String paramBeanLookup = "java:module" + "/" + "ParamAroundConstructManagedBean";
    private final String chainBeanLookup = "java:module" + "/" + "ChainManagedBean";
    private final String exceptionBeanLookup = "java:module" + "/" + "ExceptionManagedBean";
    private final String chainExceptionBeanLookup = "java:module" + "/" + "ChainExceptionManagedBean";

    @Resource
    AroundConstructManagedBean ivACMB;

    /**
     * Test that @AroundConstruct interceptor is invoked when a managed bean is created
     * --Test for default constructor and parameter constructor
     *
     * <ul>
     * <li>Verify that InvocationContext.getTarget() is null before .proceed();
     * <li>Verify that InvocationContext.getTarget() returns the associated target instance after proceed.
     * </ul>
     */
    @Test
    public void testBasicAroundConstruct() throws Exception {
        AroundConstructInterceptor.setTestType(TestType.DEFAULT);

        //Test @Resource
        svLogger.info("@Resource Test");
        assertEquals("@Resource AroundConstruct called wrong number of times", 1, ivACMB.getAroundConstructCount());

        //Test Lookup
        svLogger.info("Lookup Test");
        AroundConstructManagedBean lookupMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("lookup() AroundConstruct called wrong number of times", 1, lookupMB.getAroundConstructCount());

        //Non Default Constructor
        svLogger.info("Param Constructor Test");
        ParamAroundConstructInterceptor.setTestType(ParamTestType.DEFAULT);
        ParamAroundConstructManagedBean lookupPACMB = (ParamAroundConstructManagedBean) new InitialContext().lookup(paramBeanLookup);
        assertTrue("Param AroundConstruct not called", lookupPACMB.verifyAroundConstructCalled());

        assertNotNull("Injected Bean is Null", lookupPACMB.getInjection());

    }

    /**
     * Test that instance is not created if InvocationContext.proceed() is not invoked in @AroundConstruct
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "org.jboss.weld.exceptions.CreationException" })
    public void testAroundConstructNoProceed() throws Exception {
        AroundConstructInterceptor.setTestType(TestType.NOPROCEED);
        try {
            @SuppressWarnings("unused")
            AroundConstructManagedBean lookupMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
            fail("AroundConstruct noProceed() did not throw expected exception");
        } catch (NamingException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
        }
    }

    /**
     * Test that InvocationContext.getMethod() returns null in @AroundConstruct
     */
    @Test
    public void testAroundConstructGetMethod() throws Exception {
        AroundConstructInterceptor.setTestType(TestType.GETMETHOD);
        AroundConstructManagedBean lookupMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("lookup() AroundConstruct called wrong number of times", 1, lookupMB.getAroundConstructCount());
    }

    /**
     * Test that InvocationContext.getTimer() returns null in @AroundConstruct
     */
    @Test
    public void testAroundConstructGetTimer() throws Exception {
        AroundConstructInterceptor.setTestType(TestType.GETTIMER);
        AroundConstructManagedBean lookupMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("lookup() AroundConstruct called wrong number of times", 1, lookupMB.getAroundConstructCount());
    }

    /**
     * test InvocationContext.getConstructor()
     *
     * <ul>
     * <li>Verify that InvocationContext.getConstructor() returns the constructor in @AroundConstruct
     * <li>Verify that returned constructor is correct one for default constructed bean and constructor injected bean
     * <li>Verify that InvocationContext.getConstructor() returns null for @PostConstruct @PreDestroy and @AroundInvoke
     * </ul>
     */
    @Test
    public void testInvocationContextGetConstructor() throws Exception {
        svLogger.info("Default getConstructor Test");
        AroundConstructInterceptor.setTestType(TestType.GETCONSTRUCT);
        AroundConstructManagedBean lookupMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("lookup() AroundConstruct called wrong number of times", 1, lookupMB.getAroundConstructCount());

        svLogger.info("Param getConstructor Test");
        ParamAroundConstructInterceptor.setTestType(ParamTestType.GETCONSTRUCT);
        ParamAroundConstructManagedBean lookupPACMB = (ParamAroundConstructManagedBean) new InitialContext().lookup(paramBeanLookup);
        assertTrue("Param AroundConstruct not called", lookupPACMB.verifyAroundConstructCalled());

        //test getConstructor returns null in all other interceptor methods
        //@PostConstruct
        svLogger.info("PostConstruct Test");
        AroundConstructInterceptor.setTestType(TestType.POSTCONSTRUCT);
        AroundConstructManagedBean postConstructMB = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("postConstruct called wrong number of times", 1, postConstructMB.getPostConstructCount());

        //@PreDestroy
        svLogger.info("PreDestroy Test");
        AroundConstructInterceptor.setTestType(TestType.PREDESTROY);
        invokePreDestroy();
        assertTrue("PreDestroy not called", AroundConstructManagedBean.verifyPreDestroyCalled());

        //@AroundInvoke
        svLogger.info("AroundInvoke Test");
        AroundConstructInterceptor.setTestType(TestType.AROUNDINVOKE);
        assertEquals("AroundInvoke called wrong number of times", 1, lookupMB.getBusinessMethodCount());
    }

    /**
     * Test InvocationContext.getParameters() and InvocationContext.setParameters() in @AroundConstruct
     *
     * <ul>
     * <li>Verify that .getParameters() returns an Object[] of length 0 for default constructor
     * <li>Verify that .setParameters() only works when passing in an Object[] of length 0 or null for default constructor
     * <li>Verify that .getParameters() returns an Object[] of correct length with correct parameter types for parameter constructors
     * <li>Verify that .setParameters() with correct parameter types works in parameter constructors
     * <li>Verify that .setParameters() with incorrect parameter types and/or incorrect number of parameters throws IllegalStateException for parameter constructors
     * <li>Verify that bean is created with the parameters set in .setParameters() when method is invoked before .proceed()
     * </ul>
     */
    @Test
    public void testAroundConstructContructorParameters() throws Exception {
        svLogger.info("Default Constructor getParameters Test");
        AroundConstructInterceptor.setTestType(TestType.PARAMSTEST);
        AroundConstructManagedBean bean = (AroundConstructManagedBean) new InitialContext().lookup(managedBeanLookup);
        assertEquals("lookup() AroundConstruct called wrong number of times", 1, bean.getAroundConstructCount());

        svLogger.info("Param Constructor getParameters Test");
        ParamAroundConstructInterceptor.setTestType(ParamTestType.GETPARAMS);
        ParamAroundConstructManagedBean bean2 = (ParamAroundConstructManagedBean) new InitialContext().lookup(paramBeanLookup);
        assertTrue("Param AroundConstruct not called", bean2.verifyAroundConstructCalled());

        svLogger.info("Param Constructor setParameters Test");
        ParamAroundConstructInterceptor.setTestType(ParamTestType.SETPARAMS);
        ParamAroundConstructManagedBean bean3 = (ParamAroundConstructManagedBean) new InitialContext().lookup(paramBeanLookup);
        assertTrue("Param AroundConstruct not called", bean3.verifyAroundConstructCalled());

        //Make sure bean was created with injected bean created in .setParameters()
        assertEquals("Created bean did not have injected bean set in .setParemeters()", bean3.getInjection().getID(), -1);

    }

    /**
     * Test @AroundConstruct behavior with multiple interceptors
     *
     * <ul>
     * <li>Verify that instance is created after last interceptor calls proceed
     * <li>Verify that .getPrameters() in later interceptors returns what is set with setParameters() in earlier interceptors
     * </ul>
     */
    @Test
    public void testAroundConstructInterceptorChain() throws Exception {
        svLogger.info("ChainArouncConstruct");

        ChainManagedBean bean = (ChainManagedBean) new InitialContext().lookup(chainBeanLookup);

        //Make sure bean was created with injected bean created in last .setParameters() call
        assertEquals("Created bean did not have injected bean set in .setParemeters()", bean.getInjection().getID(), -2);

    }

    /**
     * Test that @AroundConstruct is not called if interceptor is bound only to a business method
     */
    @Test
    public void testAroundConstructBusinessMethod() throws Exception {
        AroundConstructMethodBean lookupMB = (AroundConstructMethodBean) new InitialContext().lookup(methodBeanLookup);
        lookupMB.businessMethod();
    }

    /**
     * Test @AroundConstruct Exception Handling
     *
     * Verify that a bean with an interceptor fails construction
     * when the Interceptor does not catch the constructor exception <br>
     *
     * Verify that a bean still fails construction if the interceptor catches the
     * exception but does nothing <br>
     *
     * Verify that an interceptor can catch and recover from an exception thrown
     * from the bean's constructor <br>
     *
     * Verify that InvocationTarget.getTarget() returns null after first .proceed() fails <br>
     *
     * Verify that bean is constructed with parameters set in .setParameters() when
     * construction fails the first time <br>
     *
     * Verify that the Interceptor can catch and throw a new exception <br>
     *
     * Verify that even if calling .proceed() works, throwing an exception afterwards
     * in the interceptor causes bean creation to fail <br>
     *
     * Verify that in a chain of interceptors, the second and third chain can ignore
     * the exception and the first interceptor can catch and recover <br>
     *
     * Verify that in a chain of interceptors, after the first chain catches the exception,
     * calling .proceed() calls the constructor and not the second interceptor again <br>
     *
     * Verify that in a chain of interceptors, the first interceptor will catch an InterceptorException
     * instead of a ConstructorException if the third interceptor catches the ConstructorException
     * and throws a new InterceptorException <br>
     */
    @SuppressWarnings("unused")
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException",
                    "com.ibm.ws.mb.interceptor.aroundconstructexception.InterceptorException",
                    "com.ibm.ws.mb.interceptor.aroundconstructexception.ConstructorException" })
    public void testAroundConstructExceptions() throws Exception {

        svLogger.info("Exception Interceptor Recover Test");
        ExceptionInterceptor.setTestType(ExceptionTestType.RECOVER);
        ExceptionManagedBean exceptionBean = (ExceptionManagedBean) new InitialContext().lookup(exceptionBeanLookup);
        assertTrue("Exception AroundConstruct not called", exceptionBean.verifyAroundConstructCalled());

        //Make sure bean was created with injected bean created before Constructor Exception
        assertEquals("Created bean did not have injected bean set before Constructor Exception", -1, exceptionBean.getInjection().getID());

        try {
            svLogger.info("Exception Interceptor Throw Test");
            ExceptionInterceptor.setTestType(ExceptionTestType.THROW);
            ExceptionManagedBean exceptionBean2 = (ExceptionManagedBean) new InitialContext().lookup(exceptionBeanLookup);
            fail("Lookup of ExceptionManagedBean should have failed");
        } catch (NamingException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
            assertEquals("Expected Exception has unexpected cause", ConstructorException.class, e.getCause().getClass());
        }

        try {
            svLogger.info("Exception Interceptor Catch and Rethrow Test");
            ExceptionInterceptor.setTestType(ExceptionTestType.CATCHANDRETHROW);
            ExceptionManagedBean exceptionBean3 = (ExceptionManagedBean) new InitialContext().lookup(exceptionBeanLookup);
            fail("Lookup of ExceptionManagedBean should have failed");
        } catch (NamingException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
            assertEquals("Expected Exception has unexpected cause", ConstructorException.class, e.getCause().getClass());
        }

        try {
            svLogger.info("Exception Interceptor Throw New Test");
            ExceptionInterceptor.setTestType(ExceptionTestType.THROWNEW);
            ExceptionManagedBean exceptionBean4 = (ExceptionManagedBean) new InitialContext().lookup(exceptionBeanLookup);
            fail("Lookup of ExceptionManagedBean should have failed");
        } catch (NamingException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
            assertEquals("Expected Exception has unexpected cause", InterceptorException.class, e.getCause().getClass());
        }

        ExceptionManagedBean exceptionBean5 = null;
        try {
            svLogger.info("Exception Interceptor Construct Then Throw Test");
            ExceptionInterceptor.setTestType(ExceptionTestType.CONSTRUCTTHENTHROW);
            exceptionBean5 = (ExceptionManagedBean) new InitialContext().lookup(exceptionBeanLookup);
            fail("Lookup of ExceptionManagedBean should have failed");
        } catch (NamingException e) {
            svLogger.log(Level.INFO, "Expected Exception", e);
            assertNull("Bean should not have been created when exception was thrown after proceed was successful", exceptionBean5);
        }

        svLogger.info("Chain Exception Interceptor Recover Test");
        ChainExceptionManagedBean.setTestType(ChainExceptionTestType.CHAIN1RECOVER);
        ChainExceptionManagedBean chainBean = (ChainExceptionManagedBean) new InitialContext().lookup(chainExceptionBeanLookup);
        assertTrue("ChainException Around Construct not called", chainBean.verifyAroundConstructCalled());

        svLogger.info("Chain Exception Interceptor Throw New Test");
        ChainExceptionManagedBean.setTestType(ChainExceptionTestType.CHAIN3THROWNEW);
        ChainExceptionManagedBean chainBean2 = (ChainExceptionManagedBean) new InitialContext().lookup(chainExceptionBeanLookup);
        assertTrue("ChainException Around Construct not called", chainBean2.verifyAroundConstructCalled());
    }

    /**
     * Recursive call to trigger creation of multiple bean instances until one is destroyed.
     */
    private void invokePreDestroy() throws Exception {
        int count = 0;
        long startTime = System.currentTimeMillis();
        while (!AroundConstructManagedBean.verifyPreDestroyCalled()) {
            if (System.currentTimeMillis() - startTime > (5 * 60 * 1000)) {
                svLogger.info("PreDestroy not invoked after 5 minutes");
                return;
            }
            //after 1k bean lookups start waiting a second between lookups
            if (count > 1000)
                Thread.sleep(1000);
            new InitialContext().lookup(managedBeanLookup);
            count++;
        }
    }

}
