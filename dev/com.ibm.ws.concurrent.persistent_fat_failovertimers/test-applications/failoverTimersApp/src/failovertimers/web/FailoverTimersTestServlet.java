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
package failovertimers.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FailoverTimersTestServlet")
public class FailoverTimersTestServlet extends FATServlet {
    private static final long POLL_INTERVAL_MS = 500;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource
    private DataSource ds;

    @Resource
    private UserTransaction tx;

    /**
     * Create (if not already created) a table where EJB timers can write data when they run indicating which
     * server executed the timer.
     */
    public static void createTables(DataSource ds) {
        try (Connection con = ds.getConnection(); Statement s = con.createStatement()) {
            s.execute("CREATE TABLE TIMERLOG(TIMERNAME VARCHAR(254) NOT NULL PRIMARY KEY, COUNT INT NOT NULL, SERVERNAME VARCHAR(254) NOT NULL)");
            System.out.println("Table created.");
        } catch (SQLException x) {
            System.out.println("Table might have already been created: " + x.getMessage());
        }
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

    @Override
    public void init(ServletConfig c) throws ServletException {
        createTables(ds);
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
