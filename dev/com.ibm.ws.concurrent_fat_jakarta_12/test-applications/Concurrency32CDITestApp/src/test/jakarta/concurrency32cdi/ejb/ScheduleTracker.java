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

import java.util.function.Function;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

/**
 * An EJB that provides access to trackers for methods annotated Schedule.
 */
@Singleton
public class ScheduleTracker implements Function<String, Object> {

    @Inject
    AutoScheduler autoScheduler;

    /**
     * Returns the tracking object for the given Schedule method
     *
     * @param methodName name of a bean method annotated Schedule
     * @return the tracking object
     */
    @Override
    public Object apply(String methodName) {
        return autoScheduler.trackerOf(methodName);
    }
}
