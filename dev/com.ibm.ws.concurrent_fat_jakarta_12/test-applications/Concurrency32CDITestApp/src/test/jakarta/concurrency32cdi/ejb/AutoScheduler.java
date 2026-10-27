/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency32cdi.ejb;

import java.util.concurrent.LinkedBlockingQueue;

import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Bean with methods that are automatically scheduled to run per their
 * Schedule annotations.
 */
@ApplicationScoped
public class AutoScheduler {

    /**
     * Tracks the results of executions of the lookUpEvery4Seconds method.
     */
    private final LinkedBlockingQueue<Object> lookUpEvery4SecondsQueue = //
                    new LinkedBlockingQueue<>();

    // Seconds at which methods aim to run:
    //     02      06      10      14      18      22      26      30      34      38      42      46      50      54      58

    @Schedule(cron = "2/4 * * * JAN-DEC *")
    public Void lookUpEvery4Seconds() throws NamingException {
        try {
            lookUpEvery4SecondsQueue.add(InitialContext //
                            .doLookup("java:module/env/TestEntry"));
        } catch (NamingException x) {
            lookUpEvery4SecondsQueue.add(x);
            throw x;
        }
        return null; // continue running
    }

    /**
     * Returns the tracking object for the given Schedule method
     *
     * @param methodName name of a bean method annotated Schedule
     * @return the tracking object
     */
    public Object trackerOf(String methodName) {
        if ("lookUpEvery4Seconds".equals(methodName))
            return lookUpEvery4SecondsQueue;
        else
            throw new IllegalArgumentException(methodName +
                                               " is not a method of " +
                                               getClass().getName());
    }
}
