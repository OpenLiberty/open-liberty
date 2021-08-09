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
package com.ibm.ws.ejbcontainer.async.fat.fafRemote.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.async.fat.fafRemote.ejb.CoLocDriver;
import com.ibm.ws.ejbcontainer.async.fat.fafRemote.ejb.StatelessRemote;

import componenttest.app.FATServlet;

/**
 * Tests EJB 3.1 Fire-and-forget Remote asynchronous method calls.
 * <p>
 *
 * <b>Test Matrix:</b>
 * <p>
 * <ul>
 * <li>testCoLocatedClient - this client calls a bean in server (CoLocDriverBean) which uses a Remote interface to call an asynch method on another Remote interface of another bean
 * (StatelessRemoteBean).
 * <li>testWebClient - this client directly calls the async method on the Remote interface of the StatelessRemoteBean.
 * </ul>
 */
@WebServlet("/AsyncFireAndForgetRemoteServlet")
@SuppressWarnings("serial")
public class AsyncFireAndForgetRemoteServlet extends FATServlet {

    private final static String CLASSNAME = AsyncFireAndForgetRemoteServlet.class.getName();

    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static <I> I lookupDefaultRemote(Class<I> interfaceClass, String beanName) throws NamingException {
        return interfaceClass.cast(FATHelper.lookupDefaultBindingEJBJavaGlobal(interfaceClass.getName(), "AsyncFafRemoteApp", "AsyncFafRemoteEJB", beanName));
    }

    /**
     * Test calling a method with an Asynchronous annotation at the method level
     * on an EJB 3.1 Stateless Session Bean, via the driver bean located with
     * the SSB on the server.
     */
    @Test
    public void testCoLocatedClient() throws Exception {

        // Lookup the test driver EJB
        CoLocDriver testDriver = lookupDefaultRemote(CoLocDriver.class, "CoLocDriverBean");

        // update results with bean creation
        assertNotNull("Asynch CoLoc driver bean created successfully", testDriver);

        testDriver.callAsyncMethod();

    } // end testCoLocatedClient()

    /**
     * Test calling a method with an Asynchronous annotation at the method level
     * on an EJB 3.1 Stateless Session Bean, directly from this class running
     * in the client container.
     */
    @Test
    public void testWebClient() throws Exception {

        final String meth = "testClientContainerClient";

        // Lookup the remote EJB
        StatelessRemote remoteBean = lookupDefaultRemote(StatelessRemote.class, "StatelessRemoteBean");

        // update results with bean creation
        assertNotNull("Failed to look up Async Stateless Remote Bean", remoteBean);

        // initialize work not done
        remoteBean.setAsyncWorkNotDone(); // 613496

        // call remote asynchronous bean method
        long t0 = System.currentTimeMillis();
        remoteBean.test_fireAndForget();
        long t1 = System.currentTimeMillis();
        long callTime = t1 - t0;
        svLogger.logp(Level.INFO, CLASSNAME, meth, "back from async method in " + callTime + "ms");

        // If the method had enough time to run and indicate it was done,
        // it must have blocked rather than run asynchronously
        String assertMsg = "Async method was not expected to have completed yet.  ";
        assertMsg += "It presumably blocked.  Control returned ";
        assertMsg += callTime + "ms after it was called.";

        assertFalse(assertMsg, remoteBean.asyncWorkWasDone());

        //Tell bean to continue
        remoteBean.awaitBarrier();
        //Wait for bean to finish
        remoteBean.awaitBarrier();

        assertTrue("Async work was NOT done", remoteBean.asyncWorkWasDone());

        // get current thread Id for comparison to bean method thread id
        long currentThreadId = Thread.currentThread().getId();

        assertFalse("Async Stateless Bean method completed on same thread", (remoteBean.getThreadId() == currentThreadId));
    }

}
