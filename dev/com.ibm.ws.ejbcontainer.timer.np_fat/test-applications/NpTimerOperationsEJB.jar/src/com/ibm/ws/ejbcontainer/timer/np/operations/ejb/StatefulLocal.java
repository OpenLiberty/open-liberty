/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.operations.ejb;

/**
 * Local interface for a basic Stateful Session bean. It contains
 * methods to test TimerService access.
 **/
public interface StatefulLocal {
    /**
     * Verifies the results of testing performed in {@link StatefulBean#setSessionContext setSessionContext()}. <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifySetSessionContextResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#postConstruct postConstruct()}. <p>
     *
     * Since postConstruct may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyPostConstructResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#preDestroy preDestroy()}. <p>
     *
     * Since PreDestroy may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyPreDestroyResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#ejbActivate ejbActivate()}. <p>
     *
     * Since ejbActivate may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyEjbActivateResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#ejbPassivate ejbPassivate()}. <p>
     *
     * Since ejbPassivate may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyEjbPassivateResults();

    /**
     * Test getTimerService()/TimerService access from a method on a Stateful
     * Session bean. <p>
     *
     * This test method will confirm the following :
     * <ol>
     * <li> getTimerService() fails with IllegalStateException
     * <li> Timer.getTimeRemaining() works
     * <li> Timer.getInfo() works
     * <li> Timer.getHandle() fails with IllegalStateException
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    public void testTimerAccess();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#afterBegin afterBegin()}. <p>
     *
     * Since afterBegin may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyAfterBeginResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#beforeCompletion beforeCompletion()}. <p>
     *
     * Since beforeCompletion may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyBeforeCompletionResults();

    /**
     * Verifies the results of testing performed in {@link StatefulBean#afterCompletion afterCompletion()}. <p>
     *
     * Since afterCompletion may not be called directly, the results are
     * stored in an instance variable for later verification. <p>
     **/
    public void verifyAfterCompletionResults();

    /**
     * Test that a Timer will be serialized by the EJB Container
     * as part of the state of a Stateful Session bean. <p>
     *
     * A Timer will be set as instance data during
     * beforeCompletion. They will be passivated after beforeCompletion,
     * and then activated when this method is called. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Timer is serialized successfully.
     * </ol>
     */
    public void verifyTimerPassivationResults();

    public void remove();

    public void setMessage(String message);

    public String getMessage();
}
