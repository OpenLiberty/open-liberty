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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.junit.Assert;

/**
 * Bean implementation class for Enterprise Bean: BasicStatelessLocal
 **/
@Singleton
@Remote(CoLocDriver.class)
public class CoLocDriverBean {

    public final static String CLASSNAME = CoLocDriverBean.class.getName();

    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variables for timeout values for performing work asynchronously **/
    public static int asyncTimeout = 5000;

    /**
     * The remote bean on which the asynchronous method is called
     */
    @EJB
    StatelessRemote ivRemoteBean;

    @Resource
    SessionContext ivContext;

    public CoLocDriverBean() {
    }

    /**
     * Test calling a method with an Asynchronous annotation at the method level
     * on an EJB 3.1 Stateless Session Bean.
     */
    public void callAsyncMethod() {

        final String meth = "callAsyncMethod";
        long currentThreadId = 0;

        // initialize work not done
        ivRemoteBean.setAsyncWorkNotDone(); // 613496

        // call remote asynchronous bean method
        long t0 = System.currentTimeMillis();
        ivRemoteBean.test_fireAndForget();
        long t1 = System.currentTimeMillis();
        long callTime = t1 - t0;
        svLogger.logp(Level.INFO, CLASSNAME, meth, "back from asynch method in " + callTime + "ms");

        svLogger.logp(Level.INFO, CLASSNAME, meth, "Other work being done");

        if (ivRemoteBean.asyncWorkWasDone()) {
            // implication is that the above call to test_fireAndForget()
            // blocked rather than was asynchronous since it had enough time
            // to run and indicate it was done
            Assert.fail("Async method was not expected to have completed yet.  It presumably blocked.  Control returned " + callTime + "ms after it was called.");
        }

        //Tell bean to continue
        ivRemoteBean.awaitBarrier();
        //Wait for bean to finish
        ivRemoteBean.awaitBarrier();

        assertTrue("Async method was expected to have completed", ivRemoteBean.asyncWorkWasDone());

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.logp(Level.INFO, CLASSNAME, meth, "Test threadId = " + currentThreadId);
        svLogger.logp(Level.INFO, CLASSNAME, meth, "Bean threadId = " + ivRemoteBean.getThreadId());

        assertFalse("Async Stateless Bean method completed on same thread", (ivRemoteBean.getThreadId() == currentThreadId));

    } // end callAsyncMethod()

}
