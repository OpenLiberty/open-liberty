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
package com.ibm.ws.ejbcontainer.timer.np.v32.web.ejb;

import javax.ejb.AfterCompletion;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Stateful bean that keeps track of the number of automatic timers that
 * exist for a module. <p>
 *
 * Beans that have automatic timers should use this class to set the initial
 * count when starting, and then decrement the count when a timer is cancelled. <p>
 *
 * A stateful bean is used so that changes to the number of expected automatic
 * timers may be rolled back if the transaction that cancels an automatic
 * timer is rolled back.<p>
 **/
@Stateful
public class AutomaticTimerCountBean {

    private int committedExpectedAutomaticTimers = 0;
    private int uncommittedExpectedAutomaticTimers = 0;

    public int getExpectedAutomaticTimers() {
        return committedExpectedAutomaticTimers;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public int adjustExpectedAutomaticTimerCount(int delta) {
        uncommittedExpectedAutomaticTimers += delta;
        return uncommittedExpectedAutomaticTimers;
    }

    @AfterCompletion
    private void afterCompletion(boolean commit) {
        if (commit) {
            committedExpectedAutomaticTimers = uncommittedExpectedAutomaticTimers;
        } else {
            uncommittedExpectedAutomaticTimers = committedExpectedAutomaticTimers;
        }
    }
}
