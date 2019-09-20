/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@Singleton
public class AutoTimer {
    private int count;

    @Resource
    private SessionContext sessionContext;

    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    public int getRunCount() {
        return count;
    }

    // Timer runs every other second
    @Schedule(info = "MyAutomaticPersistentTimer", hour = "*", minute = "*", second = "*/2")
    public void run(Timer timer) {
        System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo());
    }
}
