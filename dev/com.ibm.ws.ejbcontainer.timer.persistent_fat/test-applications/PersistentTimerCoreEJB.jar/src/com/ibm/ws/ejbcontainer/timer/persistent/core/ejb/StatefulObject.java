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

package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import javax.ejb.EJBLocalObject;

/**
 * Remote interface for a basic Stateful Session bean. It contains
 * methods to test TimerService access.
 **/
public interface StatefulObject extends EJBLocalObject {
    /**
     * Returns the results of testing performed in StatefulBean.setSessionContext(). <p>
     *
     * Since setSessionContext may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in setSesionContext method.
     **/
    public void verifySetSessionContextResults();

    /**
     * Returns the results of testing performed in StatefulBean.ejbCreate(). <p>
     *
     * Since ejbCreate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbCreate method.
     **/
    public void verifyEjbCreateResults();

    /**
     * Returns the results of testing performed in StatefulBean.ejbRemove(). <p>
     *
     * Since ejbRemove may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbRemove method.
     **/
    public void verifyEjbRemoveResults();

    /**
     * Returns the results of testing performed in StatefulBean.ejbActivate(). <p>
     *
     * Since ejbActivate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbActivate method.
     **/
    public void verifyEjbActivateResults();

    /**
     * Returns the results of testing performed in StatefulBean.ejbPassivate()}. <p>
     *
     * Since ejbPassivate may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in ejbPassivate method.
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
     * <li> Timer.getHandle() works
     * <li> TimerHandle.getTimer() works
     * <li> Timer.equals() works
     * <li> Timer.cancel() works
     * <li> NoSuchObjectLocalException occurs accessing canceled timer
     * </ol>
     */
    public void testTimerAccess();

    /**
     * Returns the results of testing performed in StatefulBean.afterBegin(). <p>
     *
     * Since afterBegin may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in afterBegin method.
     **/
    public void verifyAfterBeginResults();

    /**
     * Returns the results of testing performed in StatefulBean.beforeCompletion(). <p>
     *
     * Since beforeCompletion may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in beforeCompletion method.
     **/
    public void verifyBeforeCompletionResults();

    /**
     * Returns the results of testing performed in StatefulBean.afterCompletion(). <p>
     *
     * Since afterCompletion may not be called directly, the results are
     * stored in an instance variable for later retrieval. <p>
     *
     * @return Results of testing performed in afterCompletion method.
     **/
    public void verifyAfterCompletionResults();

    /**
     * Test that a Timer and TimerHandle will be serialized by the EJB Container
     * as part of the state of a Stateful Session bean. <p>
     *
     * A Timer and TimerHandle will be set as instance data during
     * beforeCompletion. They will be passivated after beforeCompletion,
     * and then activated when this method is called. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Timer is serialized successfully.
     * <li> TimerHandle is serialized successfully.
     * </ol>
     *
     * @return Results of testing performed during passivation/activation.
     */
    public void verifyTimerPassivationResults();

    public void setMessage(String message);

    public String getMessage();
}
