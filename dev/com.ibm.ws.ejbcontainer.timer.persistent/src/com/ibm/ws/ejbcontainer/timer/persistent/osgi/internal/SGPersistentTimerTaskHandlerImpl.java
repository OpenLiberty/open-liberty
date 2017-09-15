/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.ejb.TaskLocker;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;

/**
 * Extends the core PersistentTimerTaskHandler implementation to provide
 * integration with the Liberty profile PersistentExecutor Service for
 * Singleton session beans that will run in a global transaction.
 **/
final class SGPersistentTimerTaskHandlerImpl extends PersistentTimerTaskHandlerImpl implements TaskLocker {

    private static final long serialVersionUID = -8200752857441853748L;

    private transient EJSDeployedSupport sgLockMethodContext;

    /**
     * Constructor for expiration based persistent timers. Expiration based timers are
     * either "single-action" timers that run just once at a specific time (expiration),
     * or "interval" timers that run initially at a specific time (expiration) and then
     * repeat at a designated interval. <p>
     *
     * Automatic timers cannot be based on an initial expiration. <p>
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer
     * @param expiration The point in time at which the timer must expire.
     * @param interval The number of milliseconds that must elapse between timer expiration notifications.
     *            A negative value indicates this is a single-action timer.
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected SGPersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                               Date expiration, long interval) {
        super(j2eeName, info, expiration, interval);
    }

    /**
     * Constructor for calendar based persistent timers (not automatic).
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected SGPersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                               ParsedScheduleExpression parsedSchedule) {
        super(j2eeName, info, parsedSchedule);
    }

    /**
     * Constructor for automatic calendar based persistent timers.
     *
     * @param j2eeName identity of the Timer bean that is the target of the associated task.
     * @param info the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     * @param methodId timeout callback method identifier; must be a non-zero value
     * @param methodame timeout callback method name; used for validation
     * @param className timeout callback class name; used for validation (null if defined in XML)
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    @Trivial
    protected SGPersistentTimerTaskHandlerImpl(J2EEName j2eeName, @Sensitive Serializable info,
                                               ParsedScheduleExpression parsedSchedule,
                                               int methodId,
                                               String methodName,
                                               String className) {
        super(j2eeName, info, parsedSchedule, methodId, methodName, className);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from interface com.ibm.ws.concurrent.persistent.ejb.TaskLocker
    //
    // --------------------------------------------------------------------------
    @Override
    public void lock() {
        if (sgLockMethodContext != null) {
            throw new IllegalStateException("lock method already called");
        }

        // Activates and locks singleton beans that are configured to run in global transaction
        sgLockMethodContext = lockSingleton();
    }

    @Override
    public void unlock() {
        // unlock beans that were locked prior to calling timeout
        if (sgLockMethodContext != null) {
            try {
                unlockSingleton(sgLockMethodContext);
            } catch (Throwable ex) {
                // The transaction is already over, so don't propagate any failure;
                // rely on generated FFDC/trace to capture and log details
            } finally {
                sgLockMethodContext = null;
            }
        }
    }

}
