/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.basic;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionRolledbackException;

import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/BasicRemoteTestServlet")
@SuppressWarnings("serial")
public class BasicRemoteTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(BasicRemoteTestServlet.class.getName());

    @Resource
    private ORB orb;

    @EJB(beanName = "BusinessRemoteStatelessBean")
    private BusinessRMI businessRMIStateless;
    @EJB(beanName = "BusinessRemoteSingletonBean")
    private BusinessRMI businessRMISingleton;
    @EJB(beanName = "BusinessRemoteStatelessBean")
    private BusinessRemote businessRemoteStateless;
    @EJB(beanName = "BusinessRemoteSingletonBean")
    private BusinessRemote businessRemoteSingleton;

    @Test
    public void testBusinessRMIInjection() throws Exception {
        businessRMIStateless.test();
    }

    @Test
    public void testBusinessRemoteInjection() throws Exception {
        businessRemoteStateless.test();
    }

    private void testBusinessRMILookup(String jndiNamePrefix) throws Exception {
        BusinessRMI bean = (BusinessRMI) new InitialContext().lookup(jndiNamePrefix + '!' + BusinessRMI.class.getName());
        bean.test();
    }

    @Test
    public void testBusinessRMILookup() throws Exception {
        testBusinessRMILookup("java:module/BusinessRemoteStatelessBean");
        testBusinessRMILookup("java:app/BasicRemote/BusinessRemoteStatelessBean");
        testBusinessRMILookup("java:global/BasicRemote/BusinessRemoteStatelessBean");
    }

    private Object cosNamingResolve(String... names) throws Exception {
        NamingContext context = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        logger.info("resolved initial reference to NameService");
        for (int i = 0; i < names.length - 1; i++) {
            logger.info("resolving context " + names[i] + " relative to " + context);
            context = NamingContextHelper.narrow(context.resolve(new NameComponent[] { new NameComponent(names[i], "") }));
        }

        logger.info("resolving object " + names[names.length - 1] + " relative to " + context);
        return context.resolve(new NameComponent[] { new NameComponent(names[names.length - 1], "") });
    }

    @SuppressWarnings("unchecked")
    private static <T> T narrow(Object from, Class<T> to) {
        return (T) PortableRemoteObject.narrow(from, to);
    }

    @Test
    public void testBusinessRMICosNamingResolve() throws Exception {
        BusinessRMI bean = narrow(cosNamingResolve("ejb", "global", "BasicRemote", "BusinessRemoteStatelessBean!" + BusinessRMI.class.getName()), BusinessRMI.class);
        bean.test();
    }

    private void testBusinessRemoteLookup(String jndiName) throws Exception {
        BusinessRemote bean = (BusinessRemote) new InitialContext().lookup(jndiName + '!' + BusinessRemote.class.getName());
        bean.test();
    }

    @Test
    public void testBusinessRemoteLookup() throws Exception {
        testBusinessRemoteLookup("java:module/BusinessRemoteStatelessBean");
        testBusinessRemoteLookup("java:app/BasicRemote/BusinessRemoteStatelessBean");
        testBusinessRemoteLookup("java:global/BasicRemote/BusinessRemoteStatelessBean");
    }

    @Test
    public void testBusinessRemoteCosNamingResolve() throws Exception {
        BusinessRemote bean = narrow(cosNamingResolve("ejb", "global", "BasicRemote", "BusinessRemoteStatelessBean!" + BusinessRemote.class.getName()), BusinessRemote.class);
        bean.test();
    }

    @Test
    public void testBusinessRMISingleton() throws Exception {
        businessRMISingleton.test();
    }

    @Test
    public void testBusinessRemoteSingleton() throws Exception {
        businessRemoteSingleton.test();
    }

    private void testBusinessRMIStateful(BusinessRMIStateful bean) throws Exception {
        bean.initialize(true);
        if (!bean.getValueAndRemove()) {
            throw new IllegalStateException("expected value to be set");
        }
    }

    private void testBusinessRemoteStateful(BusinessRemoteStateful bean) throws Exception {
        bean.initialize(true);
        if (!bean.getValueAndRemove()) {
            throw new IllegalStateException("expected value to be set");
        }
    }

    @Test
    public void testBusinessRMIStateful() throws Exception {
        BusinessRMIStateful bean = (BusinessRMIStateful) new InitialContext().lookup("java:module/BusinessRemoteStatefulBean!" + BusinessRMIStateful.class.getName());
        testBusinessRMIStateful(bean);
    }

    @Test
    public void testBusinessRemoteStateful() throws Exception {
        BusinessRemoteStateful bean = (BusinessRemoteStateful) new InitialContext().lookup("java:module/BusinessRemoteStatefulBean!" + BusinessRemoteStateful.class.getName());
        testBusinessRemoteStateful(bean);
    }

    @Test
    public void testBusinessRMICosNamingResolveStateful() throws Exception {
        BusinessRMIStateful bean = narrow(cosNamingResolve("ejb", "global", "BasicRemote", "BusinessRemoteStatefulBean!" + BusinessRMIStateful.class.getName()),
                                          BusinessRMIStateful.class);
        testBusinessRMIStateful(bean);
    }

    @Test
    public void testBusinessRemoteCosNamingResolveStateful() throws Exception {
        BusinessRemoteStateful bean = narrow(cosNamingResolve("ejb", "global", "BasicRemote", "BusinessRemoteStatefulBean!" + BusinessRemoteStateful.class.getName()),
                                             BusinessRemoteStateful.class);
        testBusinessRemoteStateful(bean);
    }

    @Test
    public void testBusinessRMIAppException() throws Exception {
        try {
            businessRMIStateless.testAppException();
            throw new IllegalStateException("expected TestAppException");
        } catch (TestAppException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    public void testBusinessRemoteAppException() throws Exception {
        try {
            businessRemoteStateless.testAppException();
            throw new IllegalStateException("expected TestAppException");
        } catch (TestAppException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    @ExpectedFFDC("com.ibm.ws.ejbcontainer.remote.fat.basic.TestSystemException")
    public void testBusinessRMISystemException() throws Exception {
        try {
            businessRMIStateless.testSystemException();
            throw new IllegalStateException("expected RemoteException");
        } catch (RemoteException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    @ExpectedFFDC("com.ibm.ws.ejbcontainer.remote.fat.basic.TestSystemException")
    public void testBusinessRemoteSystemException() throws Exception {
        try {
            businessRemoteStateless.testSystemException();
            throw new IllegalStateException("expected RemoteException");
        } catch (EJBException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    @ExpectedFFDC({
                    "java.lang.reflect.InvocationTargetException",
                    "com.ibm.ws.ejbcontainer.remote.fat.basic.TestRollbackException",
                    "javax.transaction.RollbackException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException",
    })
    public void testBusinessRMITransactionException() throws Exception {
        try {
            businessRMIStateless.testTransactionException();
            throw new IllegalStateException("expected TransactionRolledbackException");
        } catch (TransactionRolledbackException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    @Test
    @ExpectedFFDC({
                    "java.lang.reflect.InvocationTargetException",
                    "com.ibm.ws.ejbcontainer.remote.fat.basic.TestRollbackException",
                    "javax.transaction.RollbackException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException",
    })
    public void testBusinessRemoteTransactionException() throws Exception {
        try {
            businessRemoteStateless.testTransactionException();
            throw new IllegalStateException("expected TransactionRolledbackException");
        } catch (EJBTransactionRolledbackException e) {
            logger.info("caught expected exception: " + e);
        }
    }

    /**
     * Call a remote method with parameter/return values that should use
     * write_value/read_value. The "WAS EJB 3" marshalling actually uses
     * writeAbstractObject/read_abstract_interface, so a stub/tie mismatch will
     * result in an OutOfMemoryError.
     */
    @Test
    public void testBusinessRMIWriteValue() throws Exception {
        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = businessRMIStateless.testWriteValue(expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    /**
     * Call a remote method with parameter/return values that should use
     * write_value/read_value. The "WAS EJB 3" marshalling actually uses
     * writeAbstractObject/read_abstract_interface, so a stub/tie mismatch will
     * result in an OutOfMemoryError.
     */
    @Test
    public void testBusinessRemoteWriteValue() throws Exception {
        List<?> expected = new ArrayList<Object>(Arrays.asList("a", "b", "c"));
        List<?> actual = businessRemoteStateless.testWriteValue(expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("actual=" + actual + " != expected=" + expected);
        }
    }

    @Test
    public void testBusinessRMIAsyncVoid() throws Exception {
        businessRMIStateless.setupAsyncVoid();
        businessRMIStateless.testAsyncVoid();
        long actual = businessRMIStateless.awaitAsyncVoidThreadId();
        long current = Thread.currentThread().getId();
        if (actual == current) {
            throw new IllegalStateException("actual=" + actual + " == current=" + current);
        }
    }

    @Test
    public void testBusinessRemoteAsyncVoid() throws Exception {
        businessRemoteStateless.setupAsyncVoid();
        businessRemoteStateless.testAsyncVoid();
        long actual = businessRemoteStateless.awaitAsyncVoidThreadId();
        long current = Thread.currentThread().getId();
        if (actual == current) {
            throw new IllegalStateException("actual=" + actual + " == current=" + current);
        }
    }

    @Test
    public void testBusinessRMIAsyncFuture() throws Exception {
        businessRemoteStateless.setupAsyncFuture(1);
        Future<Long> future = businessRMIStateless.testAsyncFuture();
        long actual = future.get();
        long current = Thread.currentThread().getId();
        if (actual == current) {
            throw new IllegalStateException("actual=" + actual + " == current=" + current);
        }
    }

    @Test
    public void testBusinessRemoteAsyncFuture() throws Exception {
        businessRemoteStateless.setupAsyncFuture(1);
        Future<Long> future = businessRemoteStateless.testAsyncFuture();
        long actual = future.get();
        long current = Thread.currentThread().getId();
        if (actual == current) {
            throw new IllegalStateException("actual=" + actual + " == current=" + current);
        }
    }

    @Test
    public void testOrbAndHandleDelegateAvailableMarshalling() throws Exception {
        DeserializeOrbAccessChecker checker = new DeserializeOrbAccessChecker();
        checker.verifyState();
        List<?> expected = new ArrayList<Object>(Arrays.asList(checker));
        List<?> actual = businessRemoteStateless.testWriteValue(expected);
        checker = (DeserializeOrbAccessChecker) actual.get(0);
        checker.verifyState();
    }

    // @Test - Called from RemoteTests after configuration change
    public void testAsyncConfigMaxUnclaimedRemoteResults() throws Exception {
        final int maxUnclaimedRemoteResults = 1;

        // Schedule maxUnclaimedRemoteResults + 1 async method calls.
        businessRMIStateless.setupAsyncFuture(maxUnclaimedRemoteResults + 1);

        List<Future<Long>> futures = new ArrayList<Future<Long>>();
        for (int i = 0; i < maxUnclaimedRemoteResults + 1; i++) {
            Future<Long> future = businessRMIStateless.testAsyncFuture();
            logger.info("future" + i + " = " + future);
            futures.add(future);
        }

        // Wait for all the results to be done.
        businessRMIStateless.awaitAsyncFuture();

        // Give the async methods time to complete post invoke processing
        // and make the async results available; server won't remove one
        // until both become available.
        TimeUnit.SECONDS.sleep(3);

        // Try to get both results; one should fail.
        int failedIndex = -1;

        for (int i = 0; i < futures.size(); i++) {
            try {
                logger.info("future" + i + ".get() = " + futures.get(i).get());
            } catch (Exception e) {
                // At most one result should fail.
                if (e.getCause() instanceof NoSuchObjectException && failedIndex == -1) {
                    logger.info("future" + i + ".isDone() threw expected exception: " + e);
                    failedIndex = i;
                } else {
                    throw e;
                }
            }
        }

        if (failedIndex == -1)
            throw new IllegalStateException("expected one of the futures to fail in either isDone or get");

        return;
    }

    // @Test - Called from RemoteTests after configuration change
    public void testAsyncConfigUnclaimedRemoteResultTimeout() throws Exception {
        final long unclaimedRemoteResultTimeoutSeconds = 1;

        // Schedule a single async method call.
        businessRMIStateless.setupAsyncFuture(1);
        Future<Long> future = businessRMIStateless.testAsyncFuture();

        // Wait for it to be done.
        businessRMIStateless.awaitAsyncFuture();

        // Give the server time to remove the result.
        TimeUnit.SECONDS.sleep(unclaimedRemoteResultTimeoutSeconds + 5);

        // Expect the result to be unavailable.
        try {
            future.get();
            throw new IllegalStateException("expected future.get() exception");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NoSuchObjectException) {
                logger.info("future.get() threw expected exception: " + e);
            } else {
                throw e;
            }
        }
        return;
    }
}