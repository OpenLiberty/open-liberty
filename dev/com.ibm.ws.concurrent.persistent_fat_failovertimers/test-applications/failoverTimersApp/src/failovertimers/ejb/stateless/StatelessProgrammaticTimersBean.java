/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package failovertimers.ejb.stateless;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import failovertimers.web.FailoverTimersTestServlet;

@Stateless
public class StatelessProgrammaticTimersBean {
    @Resource
    private DataSource ds;

    @Resource
    private SessionContext sessionContext;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void initTable() {
        final String createTable = "CREATE TABLE TIMERLOG (TIMERNAME VARCHAR(254) NOT NULL PRIMARY KEY, COUNT INT NOT NULL, SERVERNAME VARCHAR(254) NOT NULL)";
        boolean isTableCreated = false;
        try (Connection con = ds.getConnection(); Statement s = con.createStatement()) {
            s.execute(createTable);
            isTableCreated = true;
        } catch (SQLException x) {
            System.out.println("Table might have already been created: " + x.getMessage());
        }
        System.out.println("Was TIMERLOG table created? " + isTableCreated);
    }

    public void cancelTimers() {
        TimerService timerService = sessionContext.getTimerService();
        for (Timer t : timerService.getTimers())
            t.cancel();
    }

    @Timeout
    public void execTimer(Timer timer) {
        String serverConfigDir = System.getProperty("server.config.dir");
        String wlpUserDir = System.getProperty("wlp.user.dir");
        String serverName = serverConfigDir.substring(wlpUserDir.length() + "servers/".length(), serverConfigDir.length() - 1);
        String timerName = timer.getInfo().toString();

        if (FailoverTimersTestServlet.TIMERS_TO_FAIL.contains(timerName)) {
            System.out.println("Timer " + timerName + " is not allowed to run on " + serverName);
            throw new CompletionException("Intentionally failing timer " + timerName + " for testing purposes", null);
        }

        System.out.println("Running timer " + timerName + " on " + serverName);

        try (Connection con = ds.getConnection()) {
            boolean found;
            try {
                PreparedStatement stmt = con.prepareStatement("UPDATE TIMERLOG SET SERVERNAME=?, COUNT=COUNT+1 WHERE TIMERNAME=?");
                stmt.setString(1, serverName);
                stmt.setString(2, timerName);
                found = stmt.executeUpdate() == 1;
            } catch (SQLException x) {
                found = false;
            }
            if (!found) { // insert new entry
                PreparedStatement stmt = con.prepareStatement("INSERT INTO TIMERLOG VALUES (?,?,?)");
                stmt.setString(1, timerName);
                stmt.setInt(2, 1);
                stmt.setString(3, serverName);
                stmt.executeUpdate();
            }
        } catch (SQLException x) {
            System.out.println("Timer " + timerName + " failed.");
            x.printStackTrace(System.out);
            throw new RuntimeException(x);
        }

        if (FailoverTimersTestServlet.TIMERS_TO_ROLL_BACK.contains(timerName)) {
            System.out.println("Timer " + timerName + " can only roll back on " + serverName);
            sessionContext.setRollbackOnly();
        }

        if (timerName.startsWith("Long_Running_Timer_"))
            try {
                long delay = 8;
                System.out.println("Timer " + timerName + " about to wait " + delay + " seconds before ending.");
                TimeUnit.SECONDS.sleep(delay);
            } catch (InterruptedException x) {
                throw new CompletionException(x);
            }
    }

    public Timer scheduleTimer(long initialDelayMS, long intervalMS, String name) {
        TimerService timerService = sessionContext.getTimerService();
        return timerService.createIntervalTimer(initialDelayMS, intervalMS, new TimerConfig(name, true));
    }
}
