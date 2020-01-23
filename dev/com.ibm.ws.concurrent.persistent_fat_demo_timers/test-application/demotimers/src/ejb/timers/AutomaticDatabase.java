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

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every 30 seconds.
 */
@Stateless
public class AutomaticDatabase {
    private static final Class<AutomaticDatabase> c = AutomaticDatabase.class;

    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    @Resource(shareable = false)
    private DataSource ds;

    private int count = -1; //Incremented with each execution of timer

    private static final String name = "DatabaseTimer";

    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Get the value of count.
     */
    public int getRunCount() {
        return count;
    }

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void initTableAndRow() {
        try {
            initTable();
        } catch (SQLException e) {
            e.printStackTrace();
            fail(c.getName() + " caught exception when initializing table: " + e.getMessage());
        }

        try {
            initRow();
        } catch (SQLException e) {
            e.printStackTrace();
            fail(c.getName() + " caught exception when creating table row: " + e.getMessage());
        }
    }

    private void initTable() throws SQLException {
        final String createTable = "CREATE TABLE AUTOMATICDATABASE (name VARCHAR(64) NOT NULL PRIMARY KEY, count INT)";

        try (Connection conn = ds.getConnection()) {
            //See if table was already created
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, "AUTOMATICDATABASE", null);
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equalsIgnoreCase("AUTOMATICDATABASE"))
                    return;
            }

            //If not, create it.
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTable);
            }
        }
    }

    private void initRow() throws SQLException {
        final String createRow = "INSERT INTO AUTOMATICDATABASE VALUES(?,?)";

        //create count
        count = 0;

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(createRow)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, count);
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Runs ever 30 seconds. Automatically starts when application starts.
     */
    @Schedule(info = "Performing Database Operations", hour = "*", minute = "*", second = "*/30", persistent = true)
    public void run(Timer timer) {
        System.out.println("Running execution " + incrementCount(timer) + " of timer " + timer.getInfo());
    }

    /**
     * Increment count.
     */
    private int incrementCount(Timer timer) {
        final String modifyRow = "UPDATE AUTOMATICDATABASE SET count = ? WHERE name = ?";

        count++;

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(modifyRow)) {
                pstmt.setInt(1, count);
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            count--;
            e.printStackTrace();
            fail(c.getName() + " caught exception when incrementing count: " + e.getMessage());
        }

        return count;
    }
}
