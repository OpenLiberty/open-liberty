/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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

package ejbapp1;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@Singleton
public class TimerTest {
    private final AtomicLong lastRun = new AtomicLong(-1);
    private final AtomicInteger numRun = new AtomicInteger(0);
    private final AtomicBoolean failed = new AtomicBoolean(false);

    @Schedule(hour = "*", minute = "*", second = "*/1", persistent = false)
    public void everyOneSeconds(Timer timer) {
        long nextTime = timer.getNextTimeout().getTime(); // time when the timer is scheduled to run
        long prevTime = lastRun.getAndSet(nextTime);
        if (prevTime == -1) {
            return;
        }

        long deltaMillis = nextTime - prevTime;

        System.out.println("TIMER - Ran again: " + deltaMillis);
        // if the delta is not 1000 millis then something is wrong since timer is scheduled to run every 1 sec.
        if (deltaMillis != 1000 && numRun.get() > 0) {
            // Note that we throw out the first run because of offset issues
            System.out.println("TIMER RUN TOO FAST: " + deltaMillis);
            failed.set(true);
        }

        if (numRun.addAndGet(1) == 5) {
            System.out.println("TIMER RUN 5 TIMES");
            if (failed.get()) {
                System.out.println("TIMER TEST - FAILED");
            } else {
                System.out.println("TIMER TEST - PASSED");
            }
            timer.cancel();
        }
    }
}
