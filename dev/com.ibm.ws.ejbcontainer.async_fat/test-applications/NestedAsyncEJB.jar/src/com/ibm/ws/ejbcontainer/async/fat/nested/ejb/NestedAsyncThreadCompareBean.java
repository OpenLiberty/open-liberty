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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.Singleton;

@Singleton
@Local(NestedAsyncThreadCompareLocal.class)
@LocalBean
public class NestedAsyncThreadCompareBean {

    private static final String CLASSNAME = NestedAsyncThreadCompareBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    // The MAX time limit we will wait before timing out
    private static final long MAX_WAIT = 5;

    // This latch is used to prevent innerFARMethod from updating the
    // threadIDList before outerFARMethod does.
    private CountDownLatch ivAsync1Latch;

    // This latch is used to prevent innermostFAFMethod from updating the
    // threadIDList before innerFARMethod does.
    private CountDownLatch ivAsync2Latch;

    // This ArrayList is used to keep track of the thread IDs for each of the
    // nested Asynchronous methods
    private ArrayList<Long> threadIDList = new ArrayList<Long>();

    public ArrayList<Long> testReset() {
        threadIDList = new ArrayList<Long>();
        return threadIDList;
    }

    @EJB
    private NestedAsyncThreadCompareBean selfBean;

    @Lock(READ)
    @Asynchronous
    public Future<ArrayList<Long>> outerFARMethod() {
        String methID = "outerFARMethod()";
        svLogger.info("--> Entering " + methID);

        ivAsync1Latch = new CountDownLatch(1);

        long outerFARMethodID = Thread.currentThread().getId();
        threadIDList.add(outerFARMethodID);
        svLogger.info("--> " + methID + ":Current threadIDList = " + threadIDList);
        svLogger.info("--> Calling nested async method innerFAFMethod().");
        Future<ArrayList<Long>> futureInnerFARMethodResult = selfBean.innerFARMethod(); // nested async method

        svLogger.info("--> " + methID + ":Opening ivAsync1Latch.");
        ivAsync1Latch.countDown();

        ArrayList<Long> returningThreadIDList = null;
        try {
            returningThreadIDList = futureInnerFARMethodResult.get(MAX_WAIT,
                                                                   TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            svLogger.info("--> " + methID + ":Caught unexpected exception: " + e);
            e.printStackTrace();
        } catch (ExecutionException e) {
            svLogger.info("--> " + methID + ":Caught unexpected exception: " + e);
            e.printStackTrace();
        } catch (TimeoutException e) {
            svLogger.info("--> "
                          + methID
                          + ":Caught TimeoutException. This means that the innerFARMethod "
                          + "did not return results before we reached our MAX_WAIT time of "
                          + MAX_WAIT + " Seconds.");
            e.printStackTrace();
        }

        svLogger.info("<-- " + methID + ":Returning results...");
        return new AsyncResult<ArrayList<Long>>(returningThreadIDList);
    }

    @Lock(READ)
    @Asynchronous
    public Future<ArrayList<Long>> innerFARMethod() {
        String methID = "innerFARMethod()";
        svLogger.info("--> Entering " + methID);
        boolean gateOpen;
        try {
            gateOpen = ivAsync1Latch.await(MAX_WAIT, TimeUnit.SECONDS);
            if (!gateOpen) {
                svLogger.info("--> " + methID
                              + ":**** The outerFARMethod did not execute "
                              + "ivAsync1Latch.countDown() in a timely manner.  "
                              + "This may cause the test to fail as the returned "
                              + "ArrayList may not have the expected results.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            svLogger.info("--> " + methID + ":Unexpected InterruptedException: "
                          + e);
        }

        ivAsync2Latch = new CountDownLatch(1);

        long innerFARMethodID = Thread.currentThread().getId();
        threadIDList.add(innerFARMethodID);
        svLogger.info("--> " + methID + ":Current threadIDList = " + threadIDList);
        svLogger.info("--> Calling innermost nested async method innermostFAFMethod().");
        Future<ArrayList<Long>> futureInnermostFARMethodResult = selfBean.innermostFARMethod(); // innermost nested async method

        svLogger.info("--> " + methID + ":Opening ivAsync2Latch.");
        ivAsync2Latch.countDown();

        ArrayList<Long> returningThreadIDList = null;
        try {
            returningThreadIDList = futureInnermostFARMethodResult.get(MAX_WAIT,
                                                                       TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            svLogger.info("--> " + methID + ":Caught unexpected exception: " + e);
            e.printStackTrace();
        } catch (ExecutionException e) {
            svLogger.info("--> " + methID + ":Caught unexpected exception: " + e);
            e.printStackTrace();
        } catch (TimeoutException e) {
            svLogger.info("--> "
                          + methID
                          + ":Caught TimeoutException. This means that the innermostFARMethod "
                          + "did not return results before we reached our MAX_WAIT time of "
                          + MAX_WAIT + " Seconds.");
            e.printStackTrace();
        }

        svLogger.info("<-- " + methID + ":Returning results...");

        return new AsyncResult<ArrayList<Long>>(returningThreadIDList);
    }

    @Lock(READ)
    @Asynchronous
    public Future<ArrayList<Long>> innermostFARMethod() {
        String methID = "innermostFARMethod()";
        svLogger.info("--> Entering " + methID);
        boolean gateOpen;
        try {
            gateOpen = ivAsync2Latch.await(MAX_WAIT, TimeUnit.SECONDS);
            if (!gateOpen) {
                svLogger.info("--> " + methID
                              + ":**** The innerFARMethod did not execute "
                              + "ivAsync2Latch.countDown() in a timely manner.  "
                              + "This may cause the test to fail as the returned "
                              + "ArrayList may not have the expected results.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            svLogger.info("--> " + methID + ":Unexpected InterruptedException: "
                          + e);
        }

        long innermostFARMethodID = Thread.currentThread().getId();
        threadIDList.add(innermostFARMethodID);
        svLogger.info("--> " + methID + ":Current threadIDList = " + threadIDList);

        svLogger.info("<-- " + methID + ":Returning results...");
        return new AsyncResult<ArrayList<Long>>(threadIDList);
    }

}
