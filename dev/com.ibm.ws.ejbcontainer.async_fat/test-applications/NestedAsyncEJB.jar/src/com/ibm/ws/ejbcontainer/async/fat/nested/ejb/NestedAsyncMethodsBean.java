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
package com.ibm.ws.ejbcontainer.async.fat.nested.ejb;

import static javax.ejb.LockType.READ;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.Singleton;

@Singleton
@Local(NestedAsyncMethodsLocal.class)
@LocalBean
public class NestedAsyncMethodsBean {

    private static final String CLASSNAME = NestedAsyncMethodsBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    // This latch is used to force getAsyncCallOrder1() to wait until all nested
    // async methods are called or the 5 second time limit is up
    private CountDownLatch ivTestLatch;

    // This latch is used to prevent innerFAFMethod from writing to the ArrayList
    // before outerFARMethod does.
    private CountDownLatch ivAsync1Latch;

    // This latch is used to prevent innermostFAFMethod from writing to the ArrayList
    // before innerFAFMethod does.
    private CountDownLatch ivAsync2Latch;

    // This ArrayList is used to keep track of the order in which the nested Asynchronous
    // methods were called
    private ArrayList<Integer> asyncCallOrder1 = new ArrayList<Integer>();

    public void testReset() {
        asyncCallOrder1 = new ArrayList<Integer>();
    }

    @EJB
    private NestedAsyncMethodsBean selfBean;

    public void setupTestLatch() {
        ivTestLatch = new CountDownLatch(3);
    }

    @Lock(READ)
    public ArrayList<Integer> getAsyncCallOrder1() {
        try {
            svLogger.info("--> Inside getAsyncCallOrder1(), will wait up to 5 seconds for the nested async methods to complete.");

            // Wait until all async methods are called the await method returns
            // false if we reach the timeout value specified
            boolean complete = ivTestLatch.await(5, TimeUnit.SECONDS);

            if (!complete) {
                svLogger.info("--> Timed out waiting for all of the nested async methods to be called.");
                throw new AssertionError("--> FAIL: Timed out waiting for all of the nested async methods to be called.");
            }

        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw new AssertionError("--> Unexpected InterruptedException thrown: " + ie);
        }
        return asyncCallOrder1;
    }

    @Lock(READ)
    @Asynchronous
    public Future<String> outerFARMethod(String msg) {
        String methID = "outerFARMethod()";
        svLogger.info("--> Entering " + methID);
        svLogger.info("--> " + methID + ":Current count = "
                      + ivTestLatch.getCount());

        ivAsync1Latch = new CountDownLatch(1);

        svLogger.info("--> Calling nested async method innerFAFMethod().");
        selfBean.innerFAFMethod(); // nested async method

        asyncCallOrder1.add(0); // should be able to do this work w/o having to wait for innerFAFMethod to complete
        ivTestLatch.countDown();
        svLogger.info("--> " + methID + ":Opening ivAsync1Latch.");
        ivAsync1Latch.countDown();
        svLogger.info("--> " + methID + ":Current count = "
                      + ivTestLatch.getCount());

        svLogger.info("<-- " + methID + ":Returning results...");
        return new AsyncResult<String>(msg);
    }

    @Lock(READ)
    @Asynchronous
    public void innerFAFMethod() {
        String methID = "innerFAFMethod()";
        svLogger.info("--> Entering " + methID);
        boolean gateOpen;
        try {
            gateOpen = ivAsync1Latch.await(1, TimeUnit.SECONDS);
            if (!gateOpen) {
                svLogger.info("--> " + methID
                              + ":**** The outerFARMethod did not execute "
                              + "ivAsync1Latch.countDown() in a timely manner.  "
                              + "This may cause the test to fail as the returned "
                              + "ArrayList may not have the expected order.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            svLogger.info("--> " + methID + ":Unexpected InterruptedException: "
                          + e);
        }

        ivAsync2Latch = new CountDownLatch(1);
        svLogger.info("--> Calling innermost nested async method innermostFAFMethod().");
        selfBean.innermostFAFMethod(); // innermost nested async method

        svLogger.info("--> " + methID + ":Adding to asyncCallOrder1.");
        asyncCallOrder1.add(1); // should be able to do this work w/o having to wait for innermostFAFMethod to complete
        svLogger.info("--> " + methID + ":Calling countdown.");
        ivTestLatch.countDown();
        svLogger.info("--> " + methID + ":Current count = "
                      + ivTestLatch.getCount());
        svLogger.info("--> " + methID + ":Opening ivAsync2Latch.");
        ivAsync2Latch.countDown();
        svLogger.info("<-- Exiting " + methID);
    }

    @Lock(READ)
    @Asynchronous
    public void innermostFAFMethod() {
        String methID = "innermostFAFMethod()";
        svLogger.info("--> Entering " + methID);
        boolean gateOpen;
        try {
            gateOpen = ivAsync2Latch.await(1, TimeUnit.SECONDS);
            if (!gateOpen) {
                svLogger.info("--> " + methID
                              + ":**** The innerFAFMethod did not execute "
                              + "ivAsync2Latch.countDown() in a timely manner.  "
                              + "This may cause the test to fail as the returned "
                              + "ArrayList may not have the expected order.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            svLogger.info("--> " + methID + ":Unexpected InterruptedException: "
                          + e);
        }

        svLogger.info("--> " + methID + ":Adding to asyncCallOrder1.");
        asyncCallOrder1.add(2);
        svLogger.info("--> " + methID + ":Calling countdown.");
        ivTestLatch.countDown();
        svLogger.info("--> " + methID + ":Current count = "
                      + ivTestLatch.getCount());

        svLogger.info("<-- Exiting " + methID);
    }
}
