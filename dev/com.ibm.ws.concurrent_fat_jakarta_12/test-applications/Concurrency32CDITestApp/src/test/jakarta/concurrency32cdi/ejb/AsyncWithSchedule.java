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

import java.io.Serializable;
import java.time.Month;
import java.time.Year;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.SessionScoped;

/**
 * Bean with Asynchronous that also has Schedule on a bean method.
 * This must be rejected with UnsupportedOperationException.
 */
@Asynchronous
@SessionScoped
public class AsyncWithSchedule implements Serializable {
    private static final long serialVersionUID = 7391058645294L;

    /**
     * It is not valid for the method to be annotated Schedule
     * when the bean is annotated Asynchronous. Expect this method
     * to be rejected with UnsupportedOperationException.
     */
    @Schedule(months = Month.AUGUST,
              daysOfMonth = 12)
    public void runOnAugust12th() {
        System.out.println("Today is August 12th, " + Year.now());
    }

}
