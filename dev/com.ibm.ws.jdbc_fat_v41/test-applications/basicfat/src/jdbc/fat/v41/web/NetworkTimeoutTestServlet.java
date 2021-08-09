/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.web;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DERBY_EMBEDDED;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.dsprops.testrules.OnlyIfDataSourceProperties;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATDatabaseServlet;
import jdbc.fat.v41.slowdriver.TimeoutDataSource;

@WebServlet(urlPatterns = "/NetworkTimeoutTestServlet")
public class NetworkTimeoutTestServlet extends FATDatabaseServlet {
    private static final long serialVersionUID = 1204092494937033338L;
    private static final String tableName = "PEOPLE";

    /**
     * Special data source that implements TimeoutDataSource which can have a controlled latency.
     */
    @Resource(name = "jdbc/slowDS")
    DataSource slowDS;

    @Override
    public void init() throws ServletException {
        createTable(slowDS, tableName, "id int not null primary key, color varchar(30)");
    }

    @Test
    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "java.sql.SQLNonTransientConnectionException", "java.sql.SQLException" })
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testNetworkTimeoutSimple() throws Exception {

        Connection conn = slowDS.getConnection();
        try {
            setLatency(slowDS, 1000); // tolerable latency
            int initialTimeout = conn.getNetworkTimeout();
            System.out.println("got an initial timeout of " + initialTimeout);
            conn.setNetworkTimeout(getExec(), 10000);
            int nextTimeout = conn.getNetworkTimeout();
            if (nextTimeout != 10000)
                throw new Exception("Expected timeout to be 10000ms, but it was: " + nextTimeout);

            conn.getNetworkTimeout();
            setLatency(slowDS, 20000); // set to double the allowed network timeout
            try {
                conn.getNetworkTimeout();
                throw new Exception("Expected a network timeout but did not get one.");
            } catch (SQLException e) {
                // expected
            }

            setLatency(slowDS, 1000); // back to tolerable latency
            if (!conn.isClosed())
                throw new Exception("Connection should have been closed but it was not.");

        } finally {
            conn.close();
        }
    }

    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testNetworkTimeoutSharing() throws Exception {
        Connection conn1 = slowDS.getConnection();
        String conn1ID = null, conn2ID = null;
        int initialTimeout;
        try {
            conn1ID = getManagedConnectionID(conn1);
            initialTimeout = conn1.getNetworkTimeout();

            // Change the timeout to something else...
            int differentTimeout = (2000 == initialTimeout) ? 1000 : 2000;
            conn1.setNetworkTimeout(getExec(), differentTimeout);

            int timeout1 = conn1.getNetworkTimeout();
            if (timeout1 != differentTimeout)
                throw new Exception("Expected timeout to be " + differentTimeout + " but instead it was " + timeout1);

        } finally {
            conn1.close();
        }

        Connection conn2 = slowDS.getConnection();
        try {
            conn2ID = getManagedConnectionID(conn2);
            if (!conn1ID.equals(conn2ID))
                throw new Exception("Connections should have been shared but were not.  Conn1:" + conn1ID + "   Conn2:" + conn2ID);

            int timeout2 = conn2.getNetworkTimeout();
            if (initialTimeout != timeout2)
                throw new Exception("Expected timeout of " + initialTimeout + " but instead got timeout of " + timeout2);
        } finally {
            conn2.close();
        }
    }

    /**
     * Sets the simulated latency on the TimoutDataSource class. Use this method to
     * simulate latencies in conjunction with networkTimeout's.
     */
    private void setLatency(DataSource ds, int milliseconds) throws Exception {
        ds.unwrap(TimeoutDataSource.class).setLatency(milliseconds);
    }

    /**
     * Get the managed connection ID of a given Conneciton.
     */
    private String getManagedConnectionID(java.sql.Connection conn1) {
        for (Class<?> clazz = conn1.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field f1 = clazz.getDeclaredField("managedConn");
                f1.setAccessible(true);
                String mc1 = String.valueOf(f1.get(conn1));
                f1.setAccessible(false);
                return mc1;
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field 'managedConn' on " + conn1.getClass());
    }

    private Executor getExec() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }
}
