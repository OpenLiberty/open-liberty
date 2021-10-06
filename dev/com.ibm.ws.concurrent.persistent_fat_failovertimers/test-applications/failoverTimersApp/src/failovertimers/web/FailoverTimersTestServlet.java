/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package failovertimers.web;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import componenttest.app.FATServlet;
import failovertimers.ejb.stateless.StatelessProgrammaticTimersBean;
import failovertimers.ejb.stateless.StatelessTxSuspendedBean;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FailoverTimersTestServlet")
public class FailoverTimersTestServlet extends FATServlet {
    private static final long POLL_INTERVAL_MS = 500;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * List of timers that will intentionally fail on this server.
     */
    public static final Set<String> TIMERS_TO_FAIL = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * List of timers that will roll back their own execution when run on this server.
     */
    public static final Set<String> TIMERS_TO_ROLL_BACK = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    @Resource
    private DataSource ds;

    @Resource
    private UserTransaction tx;

    /**
     * Allow executions of the specified timer to succeed on this server.
     */
    public void allowTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        TIMERS_TO_FAIL.remove(timerName);
    }

    /**
     * Allow executions of the specified timer to commit on this server.
     */
    public void allowTimerCommit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        TIMERS_TO_ROLL_BACK.remove(timerName);
    }

    /**
     * Force executions of the specified timer to fail on this server.
     */
    public void disallowTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        TIMERS_TO_FAIL.add(timerName);
    }

    /**
     * Writes the name of the server upon which the automatic timer is running to the server output.
     */
    public void findServerWhereTimerRuns(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");

        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("SELECT SERVERNAME FROM TIMERLOG WHERE TIMERNAME=?");
            st.setString(1, timerName);

            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS)) {
                ResultSet result = st.executeQuery();
                if (result.next()) {
                    response.getWriter().println("Timer last ran on server: " + result.getString(1) + '.');
                    return;
                }
            }
        }

        response.getWriter().println("Timer did not run within allotted interval.");
    }

    /**
     * Force executions of the specified timer to roll back on this server.
     */
    public void forceRollbackForTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        TIMERS_TO_ROLL_BACK.add(timerName);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        final String createTable = "CREATE TABLE TIMERLOG (TIMERNAME VARCHAR(254) NOT NULL PRIMARY KEY, COUNT INT NOT NULL, SERVERNAME VARCHAR(254) NOT NULL)";
        boolean tableCreated = false;
        try (Connection con = ds.getConnection(); Statement s = con.createStatement()) {
            s.execute(createTable);
            tableCreated = true;
        } catch (SQLException x) {
            System.out.println("Table might have already been created: " + x.getMessage());
        }
        System.out.println("Was TIMERLOG table created? " + tableCreated);
    }

    /**
     * Cancel all timers that were scheduled by the StatelessProgrammaticTimersBean.
     */
    public void testCancelStatelessProgrammaticTimers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        StatelessProgrammaticTimersBean bean = InitialContext
                        .doLookup("java:global/failoverTimersApp/StatelessProgrammaticTimersBean!failovertimers.ejb.stateless.StatelessProgrammaticTimersBean");
        bean.cancelTimers();
    }

    /**
     * Cancel all timers that were scheduled by the StatelessTxSuspendedBean.
     */
    public void testCancelStatelessTxSuspendedTimers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        StatelessTxSuspendedBean bean = InitialContext.doLookup("java:global/failoverTimersApp/StatelessTxSuspendedBean!failovertimers.ejb.stateless.StatelessTxSuspendedBean");
        bean.cancelTimers();
    }

    /**
     * Programmatically schedule an EJB persistent timer that runs
     * at the specified interval and with the specific initial delay.
     */
    public void testScheduleStatelessTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        long initialDelayMS = Long.parseLong(request.getParameter("initialDelayMS"));
        long intervalMS = Long.parseLong(request.getParameter("intervalMS"));

        StatelessProgrammaticTimersBean bean = InitialContext
                        .doLookup("java:global/failoverTimersApp/StatelessProgrammaticTimersBean!failovertimers.ejb.stateless.StatelessProgrammaticTimersBean");
        bean.scheduleTimer(initialDelayMS, intervalMS, timerName);
    }

    /**
     * Programmatically schedule an EJB persistent timer that runs without a global transaction on the thread
     * at the specified interval and with the specific initial delay.
     */
    public void testScheduleStatelessTxSuspendedTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        long initialDelayMS = Long.parseLong(request.getParameter("initialDelayMS"));
        long intervalMS = Long.parseLong(request.getParameter("intervalMS"));

        StatelessTxSuspendedBean bean = InitialContext.doLookup("java:global/failoverTimersApp/StatelessTxSuspendedBean!failovertimers.ejb.stateless.StatelessTxSuspendedBean");
        bean.scheduleTimer(initialDelayMS, intervalMS, timerName);
    }

    /**
     * Verify that the specified persistent timer fails over to the specified server within a reasonable amount of time.
     */
    public void testTimerFailover(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String timerName = request.getParameter("timer");
        String expectedServerName = request.getParameter("server");

        String serverName = null;

        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("SELECT SERVERNAME FROM TIMERLOG WHERE TIMERNAME=?");
            st.setString(1, timerName);

            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS)) {
                ResultSet result = st.executeQuery();
                if (result.next()) {
                    serverName = result.getString(1);
                    if (expectedServerName.equals(serverName))
                        return;
                }
            }
        }

        fail("Timer last ran on server: " + serverName);
    }
}
