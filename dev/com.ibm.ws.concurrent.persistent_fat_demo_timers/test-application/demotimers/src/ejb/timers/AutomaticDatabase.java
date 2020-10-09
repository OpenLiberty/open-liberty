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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.sql.DataSource;

/**
 * This class uses the @Schedule annotation.
 * Using this annotation will start the timer immediately on start and will run every 30 seconds.
 */
@Stateless
public class AutomaticDatabase {
    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    @Resource(shareable = false)
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
     * Returns the number of executions of the task.
     *
     * @return -1: row does not exist. 0: timer has not run. Otherwise, number of times timer has run
     */
    public int getRunCount() {
        final String getCount = "SELECT count FROM AUTOMATICDATABASE WHERE name = ?";
        int count = -1;

        try (Connection conn = ds.getConnection(); PreparedStatement pstmt = conn.prepareStatement(getCount);) {
            pstmt.setString(1, name);
            ResultSet results = pstmt.executeQuery();
            count = results.next() ? results.getInt(1) : 0;
        } catch (SQLException e) {
            //could be called before table is initialized
        }
        return count;
    }

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void initTable() throws SQLException {
        final String createTable = "CREATE TABLE AUTOMATICDATABASE (name VARCHAR(64) NOT NULL PRIMARY KEY, count INT)";

        boolean isTableCreated = false;

        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement();) {
            stmt.execute(createTable);
            isTableCreated = true;
        } catch (SQLException e) {
            System.out.println("Table might have already been created: " + e.getMessage());
        }

        System.out.println("Was AUTOMATICDATABASE table created? " + isTableCreated);
    }

    /**
     * Runs ever 30 seconds. Automatically starts when application starts.
     */
    @Schedule(info = "Performing Database Operations", hour = "*", minute = "*", second = "*/30", persistent = true)
    public void run(Timer timer) {

        final String modifyRow = "UPDATE AUTOMATICDATABASE SET count = count+1 WHERE name = ?";
        final String createRow = "INSERT INTO AUTOMATICDATABASE VALUES(?,?)";

        try (Connection con = ds.getConnection()) {
            boolean found;
            try {
                PreparedStatement stmt = con.prepareStatement(modifyRow);
                stmt.setString(1, name);
                found = stmt.executeUpdate() == 1;
            } catch (SQLException x) {
                found = false;
            }

            if (!found) { // insert new entry
                PreparedStatement stmt = con.prepareStatement(createRow);
                stmt.setString(1, name);
                stmt.setInt(2, 1);
                stmt.executeUpdate();
            }

            System.out.println("Running execution " + (++count) + " of timer " + timer.getInfo());
        } catch (SQLException x) {
            System.out.println("Timer " + name + " failed.");
            x.printStackTrace(System.out);
            throw new RuntimeException(x);
        }

    }
}
