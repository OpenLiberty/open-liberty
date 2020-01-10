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

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.sql.DataSource;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every 30 seconds.
 */
@Singleton
public class AutomaticDatabase {
    private static final Class<AutomaticDatabase> c = AutomaticDatabase.class;

    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    @Resource(name = "DefaultDatasource") //Datasource used to create a new table
    private DataSource ds;

    private int count = -1; //Incremented with each execution of timer

    private boolean isTableCreated = false;

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

    /**
     * Runs ever 30 seconds. Automatically starts when application starts.
     */
    @Schedule(info = "Performing Database Operations", hour = "*", minute = "*", second = "*/30", persistent = true)
    public void run(Timer timer) {
        if (!isTableCreated)
            initTable();

        System.out.println("Running execution " + incrementCount(timer) + " of timer " + timer.getInfo());
    }

    private void initTable() {
        final String createTable = "CREATE TABLE AutomaticDatabase (name VARCHAR(64) NOT NULL PRIMARY KEY, count INT)";

        try (Connection conn = ds.getConnection()) {
            //See if table was created by another server
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, "AutomaticDatabase", null);
            while (rs.next()) {
                isTableCreated = true;
                return;
            }

            //If not, create it.
            try (PreparedStatement pstmt = conn.prepareStatement(createTable)) {
                pstmt.executeUpdate();
            }

            isTableCreated = true;
        } catch (SQLException e) {
            e.printStackTrace();
            fail(c.getName() + " caught exception when initializing table: " + e.getMessage());
        }
    }

    private void initCounter(Timer timer) {
        final String createRow = "INSERT INTO AutomaticDatabase VALUES(?,?)";

        //create count
        count = 1;

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(createRow)) {
                pstmt.setString(1, timer.getInfo().toString());
                pstmt.setInt(2, count);
                pstmt.execute();
            }
        } catch (SQLException e) {
            count = -1;
            e.printStackTrace();
            fail(c.getName() + " caught exception when creating table row: " + e.getMessage());
        }
    }

    /**
     * Increment count.
     */
    private int incrementCount(Timer timer) {
        if (count == -1) {
            this.initCounter(timer);
            return count;
        }

        final String modifyRow = "UPDATE AutomaticDatabase SET count = ? WHERE name = ?";

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(modifyRow)) {
                pstmt.setInt(1, ++count);
                pstmt.setString(2, timer.getInfo().toString());
                pstmt.execute();
            }
        } catch (SQLException e) {
            count--;
            e.printStackTrace();
            fail(c.getName() + " caught exception when incrementing count: " + e.getMessage());
        }

        return count;
    }
}
