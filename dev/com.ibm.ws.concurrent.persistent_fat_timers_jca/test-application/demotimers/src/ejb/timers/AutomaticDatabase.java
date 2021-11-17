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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every 30 seconds.
 */
@Stateless
public class AutomaticDatabase {
    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    @Resource(lookup = "jdbc/derbyds", shareable = true)
    private DataSource ds;

    private int count = 0; //Incremented with each execution of timer

    private static final String name = "DatabaseTimer";

    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Returns count
     */
    public int getRunCount() {
        return count;
    }

    /**
     * Runs ever 30 seconds. Automatically starts when application starts.
     */
    @Schedule(info = "Performing Database Operations", hour = "*", minute = "*", second = "*/15", persistent = true)
    public void run(Timer timer) {
        try (Connection con = ds.getConnection()) {
            count++;
            System.out.println("KJA1017 - connection count " + count);
        } catch (SQLException x) {
            System.out.println("Timer " + name + " failed.");
            x.printStackTrace(System.out);
        }
    }
}
