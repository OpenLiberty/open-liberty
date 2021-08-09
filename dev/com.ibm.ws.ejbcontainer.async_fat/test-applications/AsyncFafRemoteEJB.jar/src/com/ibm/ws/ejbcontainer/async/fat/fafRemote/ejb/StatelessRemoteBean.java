/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.fafRemote.ejb;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean: StatelessRemote
 **/
@Stateless
@Remote(StatelessRemote.class)
public class StatelessRemoteBean {

    public final static String CLASSNAME = StatelessRemoteBean.class.getName();

    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for timeout value for performing work asynchronously **/
    public static int asyncTimeout = 5000;

    public static CyclicBarrier svBeanBarrier = new CyclicBarrier(2);

    /**
     * Static variable for thread bean is executing on for comparison to caller
     * thread
     **/
    public static long beanThreadId = 0;

    @Asynchronous
    public void test_fireAndForget() {

        svLogger.info("Entering test_fireAndForget");

        // initialize to say work was not done yet
        StatelessRemoteBean.asyncWorkDone = false;

        // save threadId value to static variable for verification method executed
        // on different thread
        StatelessRemoteBean.beanThreadId = Thread.currentThread().getId();

        svLogger.info("threadId: " + StatelessRemoteBean.beanThreadId);

        //Ensure calling test isn't blocked by waiting for it
        awaitBarrier();

        // set static variable for work completed to true
        StatelessRemoteBean.asyncWorkDone = true;

        //Tell calling test we are done
        awaitBarrier();

        svLogger.info("Exiting test_fireAndForget");

    } // end test_fireAndForget

    public boolean asyncWorkWasDone() {
        return StatelessRemoteBean.asyncWorkDone;
    }

    public void setAsyncWorkNotDone() {
        StatelessRemoteBean.asyncWorkDone = false;
    }

    public void awaitBarrier() {
        try {
            svLogger.info("Barrier Await");
            svBeanBarrier.await(asyncTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public long getThreadId() {
        return beanThreadId;
    }

    public StatelessRemoteBean() {
    }

}
