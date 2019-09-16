/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.singleton.ann.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.NoSuchObjectException;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.NoSuchEJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.omg.CORBA.OBJECT_NOT_EXIST;

import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.AnnHelper;
import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.BasicRmiSingleton;
import com.ibm.ws.ejbcontainer.remote.singleton.ann.shared.BasicSingleton;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Test that the proper exception is thrown when a Singleton session bean
 * fails to initialize, as described in section 4.8.4 of the EJB Specification. <p>
 *
 * Errors occurring during Singleton initialization are considered fatal
 * and must result in the discarding of the Singleton instance. Possible
 * initialization errors include injection failure, a system exception
 * thrown from a PostConstruct method, or the failure of a PostConstruct
 * method container-managed transaction to successfully commit. If a
 * singleton fails to initialize, attempted invocations on the Singleton
 * result in an exception as defined by Section 3.4.3 and Section 3.4.4. <p>
 *
 * The following test variations will cover the above mentioned initialization
 * failures :
 *
 * <ul>
 * <li> {@link #testClassLoadFailure testClassLoadFailure()}
 * <li> {@link #testConstructorFailure testConstructorFailure()}
 * <li> {@link #testInjectionFailure testInjectionFailure()}
 * <li> {@link #testPostConstructException testPostConstructException()}
 * <li> {@link #testPostConstructRollback testPostConstructRollback()}
 * <li> {@link #testDependsOnFailure testDependsOnFailure()}
 * </ul>
 *
 * Every test variation will cover the scenario with multiple Singleton session
 * beans, each exposing one of the following interfaces:
 *
 * <ul>
 * <li> No-Interface View (when possible)
 * <li> Local Business
 * <li> Remote Business
 * <li> Remote Business that extends java.rmi.Remote
 * </ul>
 *
 * Exception will be verified for multiple calls; where the first call should trigger
 * the initialization of the bean, and subsequent calls should also fail, though
 * should not attempt to initialize a second time. <p>
 */
@WebServlet("/InitializeFailureAnnServlet")
@SuppressWarnings("serial")
public class InitializeFailureServlet extends FATServlet {

    private static final String CLASSNAME = InitializeFailureServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String[] TYPE = { "", // No-Interface
                                           "Local", // Local Business
                                           "Remote" }; // Remote Business
    private static final String RMI_TYPE = "Rmi"; // extends java.rmi.Remote

    private static final String MSG1 = "An error occurred during initialization of singleton session bean SingletonApp#SingletonAnnEJB.jar#";
    private static final String MSG2 = "See nested exception";
    private static final String MSG3 = "An error occurred during a previous attempt to initialize the singleton session bean SingletonApp#SingletonAnnEJB.jar#";
    private static final String MSG4 = "Failed to initialize singleton session bean SingletonApp#SingletonAnnEJB.jar#";
    private static final String MSG5 = "SingletonApp#SingletonAnnEJB.jar#FailedDependency";

    /**
     * Test that when a Singleton session bean implementation class fails
     * to load, a NoSuchEJBException will be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     * Note: Testing the No-Interface view is not possible for this scenario.
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC("java.lang.ExceptionInInitializerError")
    public void testAnnClassLoadFailure() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedClassLoad";

        for (String type : TYPE) {
            // Not possible to get a reference to a No-InterfaceView.
            if (type.equals(""))
                continue;

            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG1 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG1 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                svLogger.info("--> Cause  : " + cause);
                assertNotNull("--> Cause is null", cause);
                cause = AnnHelper.getTypedCause(nsejb.getCause(), ExceptionInInitializerError.class);
                assertNotNull("--> nested exception is not ExceptionInInitializerError", cause);

                cause = cause.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof UnsupportedOperationException)) {
                    fail("--> nested exception is not UnsupportedOperationException : " + cause);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG1 + beanName)) {
                fail("--> Message text is not as expected : " + MSG1 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Test that when a Singleton session bean implementation class fails
     * in the constructor, a NoSuchEJBException will be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     * Note: Testing the No-Interface view is not possible for this scenario.
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC("java.lang.reflect.InvocationTargetException")
    public void testAnnConstructorFailure() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedConstructor";

        for (String type : TYPE) {
            // Not possible to get a reference to a No-InterfaceView.
            if (type.equals(""))
                continue;

            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG1 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG1 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof UnsupportedOperationException)) {
                    fail("--> nested exception is not UnsupportedOperationException : " + cause);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG1 + beanName)) {
                fail("--> Message text is not as expected : " + MSG1 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Test that when a Singleton session bean implementation class fails
     * during injection, a NoSuchEJBException will be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> No-Interface View
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException",
                    "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.ejb.EJBException" })
    public void testAnnInjectionFailure() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedInjection";

        for (String type : TYPE) {
            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG1 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG1 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof EJBException)) {
                    fail("--> nested exception is not EJBException : " + cause);
                }

                cause = cause.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof UnsupportedOperationException)) {
                    fail("--> nested exception is not UnsupportedOperationException : " + cause);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG1 + beanName)) {
                fail("--> Message text is not as expected : " + MSG1 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Test that when a Singleton session bean implementation class fails
     * during PostConstruct, a NoSuchEJBException will be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> No-Interface View
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC({ "java.lang.UnsupportedOperationException", "javax.ejb.EJBException" })
    public void testAnnPostConstructException() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedPostConstruct";

        for (String type : TYPE) {
            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG1 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG1 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof EJBException)) {
                    fail("--> nested exception is not EJBException : " + cause);
                }
                msg = cause.getMessage();
                if (msg == null || !msg.startsWith(MSG2)) {
                    fail("--> Message text is not as expected : " + MSG2 +
                         " : actual : " + msg);
                }

                cause = cause.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof UnsupportedOperationException)) {
                    fail("--> nested exception is not UnsupportedOperationException : " + cause);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG1 + beanName)) {
                fail("--> Message text is not as expected : " + MSG1 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Test that when a Singleton session bean implementation class fails
     * to commit the PostConstruct transaction, a NoSuchEJBException will
     * be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> No-Interface View
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC("javax.ejb.EJBTransactionRolledbackException")
    public void testAnnPostConstructRollback() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedPostConstructRB";

        for (String type : TYPE) {
            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG1 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG1 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof EJBTransactionRolledbackException)) {
                    fail("--> nested exception is not EJBTransactionRolledbackException : " +
                         cause);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG1 + beanName)) {
                fail("--> Message text is not as expected : " + MSG1 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Test that when a Singleton session bean implementation class fails
     * to initialize because a dependency bean fails to initialize, a
     * NoSuchEJBException will be thrown. <p>
     *
     * Each of the following interface types will be tested :
     * <ul>
     * <li> No-Interface View
     * <li> Local Business
     * <li> Remote Business
     * <li> Remote Business that extends java.rmi.Remote
     * </ul>
     *
     * The test will verify that on the first attempt to access the bean,
     * in addition to the NoSuchEJBException, meaningful nested exception
     * information will be available. <p>
     *
     * The test will also verify that subsequent attempts to access the bean
     * will continue to result in a NoSuchEJBException. <p>
     *
     * Note: For the rmi Remote interface, a NoSuchObjectException will
     * be thrown instead of NoSuchEJBException.
     **/
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException",
                    "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.ejb.EJBException" })
    public void testAnnDependsOnFailure() throws Exception {
        String beanName;
        BasicSingleton bean;
        BasicRmiSingleton rbean;
        String bean_prefix = "FailedDependsOn";

        for (String type : TYPE) {
            beanName = bean_prefix + type;
            bean = lookupDefault(BasicSingleton.class,
                                 beanName,
                                 type);

            svLogger.info("--> Attempting 1st method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG4 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG4 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNotNull("--> Cause is null", cause);
                svLogger.info("--> Cause  : " + cause);
                if (!(cause instanceof NoSuchEJBException)) {
                    fail("--> nested exception is not NoSuchEJBException : " + cause);
                }
                msg = cause.getMessage();
                if (msg == null || !msg.contains(MSG5)) {
                    fail("--> Message text is not as expected : " + MSG5 +
                         " : actual : " + msg);
                }
            }

            svLogger.info("--> Attempting 2nd method call on : " + beanName);
            try {
                bean.getBoolean();
                fail("--> Expected Exception was not thrown.");
            } catch (NoSuchEJBException nsejb) {
                svLogger.info("--> Caught : " + nsejb);
                String msg = nsejb.getMessage();
                if (msg == null || !msg.startsWith(MSG3 + beanName)) {
                    fail("--> Message text is not as expected : " + MSG3 + beanName +
                         " : actual : " + msg);
                }

                Throwable cause = nsejb.getCause();
                assertNull("--> Cause is not null", cause);
            }
        }

        beanName = bean_prefix + RMI_TYPE;
        rbean = lookupDefault(BasicRmiSingleton.class,
                              beanName,
                              RMI_TYPE);

        svLogger.info("--> Attempting 1st method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG4 + beanName)) {
                fail("--> Message text is not as expected : " + MSG4 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }

        svLogger.info("--> Attempting 2nd method call on : " + beanName);
        try {
            rbean.getBoolean();
            fail("--> Expected Exception was not thrown.");
        } catch (NoSuchObjectException nsobj) {
            svLogger.info("--> Caught : " + nsobj);
            String msg = nsobj.getMessage();
            if (msg == null || !msg.contains(MSG3 + beanName)) {
                fail("--> Message text is not as expected : " + MSG3 + beanName +
                     " : actual : " + msg);
            }

            Throwable cause = nsobj.getCause();
            assertNotNull("--> Cause is null", cause);
            svLogger.info("--> Cause  : " + cause);
            if (!(cause instanceof OBJECT_NOT_EXIST)) {
                fail("--> nested exception is not OBJECT_NOT_EXIST : " + cause);
            }
        }
    }

    /**
     * Common lookup method that knows how to lookup the default bindings for
     * No-Interface, Local, Remote, and RMI Remote interfaces.
     *
     * @param interfaceClass the interface class to be returned.
     * @param beanName the name of the EJB to lookup.
     * @param type the type of interface : "", "Local", "Remote", "Rmi"
     **/
    private static <T> T lookupDefault(Class<T> interfaceClass,
                                       String beanName,
                                       String type) throws Exception {
        T bean;

        svLogger.info("--> looking up EJB : " + beanName);
        if (type.equals("")) {
            bean = AnnHelper.lookupDefaultNoInterface(interfaceClass,
                                                      beanName);
        } else if (type.equals("Local")) {
            bean = AnnHelper.lookupDefaultLocal(interfaceClass,
                                                beanName);
        } else {
            bean = AnnHelper.lookupDefaultRemote(interfaceClass,
                                                 beanName);
        }
        assertTrue("--> Did not find EJB : " + beanName, bean != null);
        return bean;
    }
}
