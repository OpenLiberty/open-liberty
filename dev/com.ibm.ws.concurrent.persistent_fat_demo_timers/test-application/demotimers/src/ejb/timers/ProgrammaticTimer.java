/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb.timers;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * This class uses the @Timeout annotation.
 * The method decorated with @Timeout will be run each iteration of
 * the timer. The timer is scheduled programmatically by calling the setTimer() method.
 */
@Stateless
public class ProgrammaticTimer {

    @Resource
    TimerService timerService;

    private int count;

    /**
     * Creates a timer that runs every 5 minutes,
     * and then cancel's itself after 6 runs (30 minutes).
     */
    public Timer setTimer() {
        final TimerConfig config = new TimerConfig();
        config.setInfo("Created Programmatically and Running Every 5 minute(s)");
        config.setPersistent(true);
        ScheduleExpression exp = new ScheduleExpression().hour("*").minute("*/5").second("0");
        return timerService.createCalendarTimer(exp, config);
    }

    @Timeout
    public void run(Timer timer) {
        System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo());

        if (count >= 6) //30 minutes
            timer.cancel();
    }
}
