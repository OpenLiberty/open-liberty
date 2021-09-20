/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.logging.Logger;

import javax.activity.ActivityCompletedException;
import javax.activity.ActivityRequiredException;
import javax.activity.InvalidActivityException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.AccessLocalException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRequiredLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.InvalidTransactionException;
import javax.transaction.TransactionRequiredException;
import javax.transaction.TransactionRolledbackException;

import org.junit.Ignore;
import org.junit.Test;
import org.omg.CORBA.portable.UnknownException;

import com.ibm.ejs.container.InvalidTransactionLocalException;
import com.ibm.ws.ejbcontainer.exception2x.ejb.TestEx;
import com.ibm.ws.ejbcontainer.exception2x.ejb.TestExHome;
import com.ibm.ws.ejbcontainer.exception2x.ejb.TestExLItf;
import com.ibm.ws.ejbcontainer.exception2x.ejb.TestExLItfHome;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>Exception2xTest
 *
 * <dt>Test Descriptions:
 * <dd>Exception test for EJB 2.x beans.
 *
 * <dt>Command options:
 * <dd>None
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>-option 1</TD>
 * <TD>Option 1 descriptions.</TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt>Test Matrix:
 * <dd>
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/Exception2xServlet")
public class Exception2xServlet extends FATServlet {
    private static final String CLASS_NAME = Exception2xServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    protected static TestExHome beanHome;
    protected static TestExLItfHome beanLocalHome;

    private static TestEx beanRef = null;
    private static TestExLItf beanLocalRef = null;

    @PostConstruct
    private void initializeBeans() {
        try {
            beanHome = (TestExHome) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/Exception2xBean/ExceptionEJB!com.ibm.ws.ejbcontainer.exception2x.ejb.TestExHome"),
                                                                TestExHome.class);
            beanLocalHome = (TestExLItfHome) new InitialContext().lookup("java:app/Exception2xBean/ExceptionEJB!com.ibm.ws.ejbcontainer.exception2x.ejb.TestExLItfHome");

            // create EJB instance
            svLogger.info("Create EJB instance");
            beanRef = beanHome.create(1);

            // create Local EJB instance
            svLogger.info("Create EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void removeBeans() {
        try {
            if (beanRef != null) {
                beanRef.remove();
            }

            if (beanLocalRef != null) {
                beanLocalRef.remove();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @testName: test1
     *
     * @assertion: An EJB method can throw a runtimeException. When that happens
     *             the client should receive a ServerException with a UncheckedException
     *             nested inside and the RuntimeException nested inside that.
     * @test_Strategy: Call the bean method that throws the RuntimeException.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testRemoteRuntimeException() throws Exception {
        try {
            beanRef.throwRuntimeException("test1");
            fail("Expected RuntimeException not thrown.");
        } catch (ServerException e) {
            svLogger.info("Expected ServerException caught = " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test2
     * @assertion: An EJB method can throw a TransactionRequiredException().
     *             When that happens the client should receive a TransactionRequiredException().
     * @test_Strategy: Call the bean method that throws the TransactionRequiredException().
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.TransactionRequiredException" })
    public void testRemoteTransactionRequiredException() throws Exception {
        try {
            beanRef.throwTransactionRequiredException("test2");
            fail("Expected TransactionRequiredException not thrown.");
        } catch (TransactionRequiredException e) {
            svLogger.info("Expected TransactionRequiredException caught : " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test3
     * @assertion: An EJB method can throw a TransactionRolledbackException.
     *             When that happens the client should receive a TransactionRolledbackException.
     * @test_Strategy: Call the bean method that throws the TransactionRolledbackException.
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.TransactionRolledbackException" })
    public void testRemoteTransactionRolledbackException() throws Exception {
        try {
            beanRef.throwTransactionRolledbackException("test3");
            fail("Expected TransactionRolledbackException not thrown.");
        } catch (TransactionRolledbackException e) {
            svLogger.info("Expected TransactionRolledbackException caught : " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test4
     * @assertion: An EJB method can throw a InvalidTransactionException.
     *             When that happens the client should receive a InvalidTransactionException.
     * @test_Strategy: Call the bean method that throws the InvalidTransactionException.
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.InvalidTransactionException" })
    public void testRemoteInvalidTransactionException() throws Exception {
        try {
            beanRef.throwInvalidTransactionException("test4");
            fail("Expected InvalidTransactionException not thrown.");
        } catch (InvalidTransactionException e) {
            svLogger.info("Expected InvalidTransactionException caught : " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test5
     * @assertion: An EJB method can throw a AccessException.
     *             When that happens the client should receive a AccessException.
     * @test_Strategy: Call the bean method that throws the AccessException.
     */
    @Test
    @ExpectedFFDC({ "java.rmi.AccessException" })
    public void testRemoteAccessException() throws Exception {
        try {
            beanRef.throwAccessException("test4");
            fail("Expected AccessException not thrown.");
        } catch (AccessException e) {
            svLogger.info("Expected AccessException caught : " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test1
     * @assertion: An EJB method can throw an ActivityRequiredException.
     *             When that happens the client should receive a ActivityRequiredException.
     * @test_Strategy: Call the bean method that throws the ActivityRequiredException.
     */
    @Ignore
    @Test
    public void testRemoteActivityRequiredException() throws Exception {
        try {
            beanRef.throwActivityRequiredException("test6");
            fail("Expected ActivityRequiredException not thrown.");
        } catch (ActivityRequiredException e) {
            svLogger.info("Expected ActivityRequiredException caught : " + e);
        }
    }

    /**
     * @testName: test7
     * @assertion: An EJB method can throw an InvalidActivityException..
     *             When that happens the client should receive a InvalidActivityException.
     * @test_Strategy: Call the bean method that throws the InvalidActivityException.
     */
    @Ignore
    @Test
    public void testRemoteInvalidActivityException() throws Exception {
        try {
            beanRef.throwInvalidActivityException("test7");
            fail("Expected InvalidActivityException not thrown.");
        } catch (InvalidActivityException e) {
            svLogger.info("Expected InvalidActivityException caught : " + e);
        }
    }

    /**
     * @testName: test8
     * @assertion: An EJB method can throw an ActivityCompletedException.
     *             When that happens the client should receive a ActivityCompletedException.
     * @test_Strategy: Call the bean method that throws the ActivityCompletedException.
     */
    @Ignore
    @Test
    public void testRemoteActivityCompletedException() throws Exception {
        try {
            beanRef.throwActivityCompletedException("test8");
            fail("Expected ActivityCompletedException not thrown.");
        } catch (ActivityCompletedException e) {
            svLogger.info("Expected ActivityCompletedException caught : " + e);
        }
    }

    /**
     * @testName: test9
     * @assertion: An EJB method can throw a NoSuchObjectException.
     *             When that happens the client should receive a NoSuchObjectException.
     * @test_Strategy: Call the bean method that throws the NoSuchObjectException.
     */
    @Test
    @ExpectedFFDC({ "java.rmi.NoSuchObjectException" })
    public void testRemoteNoSuchObjectException() throws Exception {
        try {
            beanRef.throwNoSuchObjectException("test9");
            fail("Expected NoSuchObjectException not thrown.");
        } catch (NoSuchObjectException e) {
            svLogger.info("Expected NoSuchObjectException caught : " + e);
        } finally {
            // recreate EJB instance
            svLogger.info("Recreate EJB instance");
            beanRef = beanHome.create(1);
        }
    }

    /**
     * @testName: test18
     * @assertion: A Local EJB method can throw a RuntimeException.
     *             When that happens the client should receive an EJBException.
     * @test_Strategy: Call the bean method that throws the RuntimeException.
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testLocalRuntimeException() throws Exception {
        try {
            beanLocalRef.throwRuntimeException("test18");
            fail("Expected EJBException not thrown.");
        } catch (EJBException e) {
            svLogger.info("Expected EJBException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test19
     * @assertion: A Local EJB method can throw a TransactionRequiredException.
     *             When that happens the client should receive a TransactionRequiredLocalException.
     * @test_Strategy: Call the bean method that throws the TransactionRequiredException.
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.TransactionRequiredException" })
    public void testLocalTransactionRequiredLocalException() throws Exception {
        try {
            beanLocalRef.throwTransactionRequiredException("test19");
            fail("Expected TransactionRequiredException not thrown.");
        } catch (TransactionRequiredLocalException e) {
            svLogger.info("Expected TransactionRequiredLocalException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test20
     * @assertion: A Local EJB method can throw a TransactionRolledbackException.
     *             When that happens the client should receive a TransactionRolledbackLocalException.
     * @test_Strategy: Call the bean method that throws the TransactionRolledbackException.
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.TransactionRolledbackException" })
    public void testLocalTransactionRolledbackLocalException() throws Exception {
        try {
            beanLocalRef.throwTransactionRolledbackException("test20");
            fail("Expected TransactionRolledbackLocalException not thrown.");
        } catch (TransactionRolledbackLocalException e) {
            svLogger.info("Expected TransactionRolledbackLocalException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test21
     * @assertion: A Local EJB method can throw a InvalidTransactionException.
     *             When that happens the client should receive a InvalidTransactionLocalException.
     * @test_Strategy: Call the bean method that throws the InvalidTransactionException.
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.InvalidTransactionException" })
    public void testLocalInvalidTransactionLocalException() throws Exception {
        try {
            beanLocalRef.throwInvalidTransactionException("test21");
            fail("Expected InvalidTransactionLocalException not thrown.");
        } catch (InvalidTransactionLocalException e) {
            svLogger.info("Expected InvalidTransactionLocalException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test22
     * @assertion: A Local EJB method can throw a AccessException.
     *             When that happens the client should receive a AccessLocalException.
     * @test_Strategy: Call the bean method that throws the AccessException.
     */
    @Test
    @ExpectedFFDC({ "java.rmi.AccessException" })
    public void testLocalAccessLocalException() throws Exception {
        try {
            beanLocalRef.throwAccessException("test22");
            fail("Expected AccessLocalException not thrown.");
        } catch (AccessLocalException e) {
            svLogger.info("Expected AccessLocalException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test23
     * @assertion: A local EJB method can throw a ActivityRequiredException.
     *             When that happens the client should receive a ActivityRequiredLocalException.
     * @test_Strategy: Call the bean method that throws the ActivityRequiredException.
     */
    @Ignore
    @Test
    public void testLocalActivityRequiredLocalException() throws Exception {
//        try {
        beanLocalRef.throwActivityRequiredException("test23");
        fail("Expected ActivityRequiredLocalException not thrown.");
//        } catch (ActivityRequiredLocalException e) {
//            svLogger.info("Expected ActivityRequiredLocalException caught : " + e);
//        }
    }

    /**
     * @testName: test24
     * @assertion: A Local EJB method can throw a InvalidActivityException.
     *             When that happens the client should receive a InvalidActivityLocalException.
     * @test_Strategy: Call the bean method that throws the InvalidActivityException.
     */
    @Ignore
    @Test
    public void testLocalInvalidActivityLocalException() throws Exception {
//        try {
        beanLocalRef.throwInvalidActivityException("test24");
        fail("Expected InvalidActivityLocalException not thrown.");
//        } catch (InvalidActivityLocalException e) {
//            svLogger.info("Expected InvalidActivityLocalException caught : " + e);
//        }
    }

    /**
     * @testName: test25
     * @assertion: A Local EJB method can throw a ActivityCompletedException.
     *             When that happens the client should receive a ActivityCompletedLocalException.
     * @test_Strategy: Call the bean method that throws the ActivityCompletedException.
     */
    @Ignore
    @Test
    public void testLocalActivityCompletedLocalException() throws Exception {
//        try {
        beanLocalRef.throwActivityCompletedException("test25");
        fail("Expected ActivityCompletedLocalException not thrown.");
//        } catch (ActivityCompletedLocalException e) {
//            svLogger.info("Expected ActivityCompletedLocalException caught : " + e);
//        }
    }

    /**
     * @testName: test26
     * @assertion: A Local EJB method can throw a NoSuchObjectException.
     *             When that happens the client should receive a NoSuchObjectLocalException.
     * @test_Strategy: Call the bean method that throws the NoSuchObjectException.
     */
    @Test
    @ExpectedFFDC({ "java.rmi.NoSuchObjectException" })
    public void testLocalNoSuchObjectLocalException() throws Exception {
        try {
            beanLocalRef.throwNoSuchObjectException("test26");
            fail("Expected NoSuchObjectLocalException not thrown.");
        } catch (NoSuchObjectLocalException e) {
            svLogger.info("Expected NoSuchObjectLocalException caught : " + e);
        } finally {
            // recreate local EJB instance
            svLogger.info("Recreate local EJB instance");
            beanLocalRef = beanLocalHome.create(2);
        }
    }

    /**
     * @testName: test35
     * @assertion: A call to create on local home can result in a unchecked exception
     *             occurring when ejbCreate is called. When that happens the client should
     *             receive a EJBException that wrappers the unchecked exception that occurred.
     * @test_Strategy: Call the create method on local home interface that causes
     *                 ejbCreate to throw a NullPointerException.
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void testLocalEjbCreateNullPointerException() throws Exception {
        try {
            beanLocalHome.create(35, 1);
            fail("No exception occured, one was expected.");
        } catch (EJBException e) {
            Exception ex = e.getCausedByException();
            assertNotNull("EJBException wrappers unchecked exception", ex);
            assertTrue("NPE was thrown", ex instanceof NullPointerException);
        }
    }

    /**
     * @testName: test37
     * @assertion: A call to create on remote home can result in a unchecked exception
     *             occurring when ejbCreate is called. When that happens the client should
     *             receive a RemoteException that wrappers the unchecked exception that occurred.
     * @test_Strategy: Call the create method on remote home interface that causes
     *                 ejbCreate to throw a NullPointerException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.CreateFailureException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void testRemoteEjbCreateNullPointerException() throws Exception {
        try {
            beanHome.create(37, 1);
            fail("No exception occured, one was expected.");
        } catch (RemoteException e) {
            Throwable t = findRootThrowable(e);
            assertNotNull("RemoteException wrappers unchecked exception", t);
            assertTrue("NPE was thrown", t instanceof NullPointerException);
        }
    }

    /**
     * Return root cause of a remote exception.
     */
    private Throwable findRootThrowable(RemoteException t) {
        Throwable root = null;
        Throwable next = t;

        while (next != null) {
            root = next;

            if (root instanceof RemoteException) {
                next = ((RemoteException) root).detail;
            } else if (root instanceof EJBException) {
                next = ((EJBException) root).getCausedByException();
            } else if (root instanceof NamingException) {
                next = ((NamingException) root).getRootCause();
            } else if (root instanceof InvocationTargetException) {
                next = ((InvocationTargetException) root).getTargetException();
            } else if (root instanceof UnknownException) {
                next = ((UnknownException) root).originalEx;
            } else {
                next = null;
            }
        }

        return root;
    }
}