/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package failovertimers.ejb.autotimer;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@Singleton
public class AutoCountingTimer {
    private int count;

    @Resource
    private SessionContext sessionContext;

    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    // Timer runs every other other second
    @Schedule(info = "AutomaticCountingTimer", hour = "*", minute = "*", second = "*/2")
    public void run(Timer timer) {
        String serverConfigDir = System.getProperty("server.config.dir");
        String wlpUserDir = System.getProperty("wlp.user.dir");
        String serverName = serverConfigDir.substring(wlpUserDir.length() + "servers/".length(), serverConfigDir.length() - 1);

        System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo() + " on " + serverName);
    }
}
