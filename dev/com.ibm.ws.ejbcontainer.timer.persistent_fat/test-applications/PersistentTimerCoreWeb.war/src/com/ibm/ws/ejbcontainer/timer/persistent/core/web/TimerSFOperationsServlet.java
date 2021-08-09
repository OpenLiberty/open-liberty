/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.web;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatefulHome;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.StatefulObject;

import componenttest.app.FATServlet;

/**
 * Test to exercise the EJB Container Timer Service using Stateful Session
 * beans. Verifies the "Allowed Operations" table in the EJB spcification
 * for Stateful Session beans.
 */
@WebServlet("/TimerSFOperationsServlet")
@SuppressWarnings("serial")
public class TimerSFOperationsServlet extends FATServlet {

    private static final Logger svLogger = Logger.getLogger(TimerSFOperationsServlet.class.getName());

    /**
     * Test getTimerService()/TimerService access from setSessionContext on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceSetSessionContext() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - setSessionContext may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------

        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        svLogger.info("Enter EJB method : setSessionContext()");

        StatefulObject sf = SFHome.create();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifySetSessionContextResults();

        svLogger.info("Exit EJB method : setSessionContext()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbCreate on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceEJBCreate() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - ejbCreate may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        svLogger.info("Enter EJB method : ejbCreate()");

        StatefulObject sf = SFHome.create("ejbCreate");

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyEjbCreateResults();

        svLogger.info("Exit EJB method : ejbCreate()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbRemove on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceEJBRemove() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - ejbRemove may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("ejbRemove");

        svLogger.info("Enter EJB method : ejbRemove()");

        // Now remove the bean to execute ejbRemove....
        sf.remove();

        // Create another bean to extract the results...
        sf = SFHome.create("NoTest");

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyEjbRemoveResults();

        svLogger.info("Exit EJB method : ejbRemove()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbActivate on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceEJBActivate() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - ejbActivate may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("ejbActivate");

        svLogger.info("Enter EJB method : ejbActivate()-----------------");

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyEjbActivateResults();

        svLogger.info("Exit EJB method : ejbActivate()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from ejbPassivate on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceEJBPassivate() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - ejbPassivate may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.

        svLogger.info("Enter EJB method : ejbPassivate()");

        StatefulObject sf = SFHome.create("ejbPassivate");

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyEjbPassivateResults();

        svLogger.info("Exit EJB method : ejbPassivate()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from a business method on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Test
    public void testSFTimerServiceBeanMethod() throws Exception {
        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("testTimerAccess");

        // --------------------------------------------------------------------
        // Execute the test by calling a method on the bean... append results
        // --------------------------------------------------------------------

        svLogger.info("Enter EJB method : testTimerAccess()");

        sf.testTimerAccess();

        svLogger.info("Exit EJB method : testTimerAccess()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from afterBegin on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Test
    public void testSFTimerServiceAfterBegin() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - afterBegin may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("afterBegin");

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        svLogger.info("Enter EJB method : afterBegin()");

        sf.verifyAfterBeginResults();

        svLogger.info("Exit EJB method : afterBegin()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from beforeCompletion on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    @Test
    public void testSFTimerServiceBeforeCompletion() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - beforeCompletion may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("beforeCompletion");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and beforeCompletion.
        // --------------------------------------------------------------------
        svLogger.info("Enter EJB method : beforeCompletion()");

        sf.getMessage();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyBeforeCompletionResults();

        svLogger.info("Exit EJB method : beforeCompletion()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test getTimerService()/TimerService access from afterCompletion on a
     * Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() fails with IllegalStateException
     * <li> Timer.getInfo() fails with IllegalStateException
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> Timer.cancel() fails with IllegalStateException
     * </ol>
     */
    @Test
    public void testSFTimerServiceAfterCompletion() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - afterCompletion may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("afterCompletion");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and afterCompletion.
        // --------------------------------------------------------------------
        svLogger.info("Enter EJB method : afterCompletion()");

        sf.getMessage();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyAfterCompletionResults();

        svLogger.info("Exit EJB method : afterCompletion()");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }

    /**
     * Test that a Timer and TimerHandle will be serialized by the EJB Container
     * as part of the state of a Stateful Session bean. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Timer is serialized successfully.
     * <li> TimerHandle is serialized successfully.
     * </ol>
     */
    @Test
    public void testSFTimerSerializationWhenPassivated() throws Exception {
        // --------------------------------------------------------------------
        // Execute the test by creating instance of the bean... append results
        // - passivation/activation may not be called directly....
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        StatefulHome SFHome = (StatefulHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/Stateful");

        // Create a bean to execute the test.
        StatefulObject sf = SFHome.create("TimerPassivation");

        svLogger.info("Enter EJB method : passivation/activation");

        // --------------------------------------------------------------------
        // Call any method to force a transaction and beforeCompletion, which
        // will create a Timer to be passivated....
        // --------------------------------------------------------------------
        sf.getMessage();

        // --------------------------------------------------------------------
        // Retrieve the test results from the created bean.
        // --------------------------------------------------------------------
        sf.verifyTimerPassivationResults();

        svLogger.info("Exit EJB method : passivation/activation");

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        // --------------------------------------------------------------------
        sf.remove();
    }
}
