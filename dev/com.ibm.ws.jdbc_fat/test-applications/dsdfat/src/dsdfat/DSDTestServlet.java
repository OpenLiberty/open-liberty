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
package dsdfat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

// Has the same name as a DataSourceDefinition in the basicfat web module
@DataSourceDefinition(
                      name = "java:comp/env/jdbc/dsfat8",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "${shared.resource.dir}/data/derbyfat",
                      isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
                      loginTimeout = 88,
                      maxIdleTime = 2,
                      maxPoolSize = 1,
                      properties = {
                                     "connectionTimeout=0",
                                     "containerAuthDataRef=derbyAuth1",
                                     "createDatabase=create",
                                     "onConnect=DECLARE GLOBAL TEMPORARY TABLE TEMP8 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED",
                                     "queryTimeout=1m20s",
                                     "reapTime=2000ms"
                      })
public class DSDTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Resource(lookup = "java:comp/env/jdbc/dsfat8")
    DataSource ds8; // two-phase, Derby only

    /**
     * Two servlets (this and DataSourceTestServlet) both declare DataSourceDefinitions
     * with the same name: java:comp/env/jdbc/ds8
     *
     * This is possible because they are declared in java:comp and are in different modules.
     * Verify that the one declared here matches the loginTimeout that we set for it.
     */
    public void testDuplicateJNDINames() throws Throwable {

        // loginTimeout = 88
        int loginTimeout = ds8.getLoginTimeout();
        if (loginTimeout != 88)
            throw new Exception("Wrong loginTimeout for DataSourceDefinition with java:comp/env/jdbc/ds8 in DSDServlet: " + loginTimeout);

        Connection con = ds8.getConnection();
        try {
            // user = dbuser1
            DatabaseMetaData metadata = con.getMetaData();
            String user = metadata.getUserName();
            if (!"dbuser1".equalsIgnoreCase(user))
                throw new Exception("Expecting user=dbuser1, not " + user);

            // databaseName = ${shared.resource.dir}/data/derbyfat
            String url = metadata.getURL();
            if (!url.contains("derbyfat"))
                throw new Exception("Expecting url to point to derbyfat database. Instead: " + url);

            // isolationLevel = TRANSACTION_SERAIALIZABLE (8)
            int isolationLevel = con.getTransactionIsolation();
            if (isolationLevel != Connection.TRANSACTION_SERIALIZABLE)
                throw new Exception("Expecting isolationLevel = 8, not " + isolationLevel);

            // onConnect creates table TEMP8
            Statement stmt = con.createStatement();
            int updateCount = stmt.executeUpdate("insert into SESSION.TEMP8 values ('This table should have been created by onConnect SQL.')");
            if (updateCount != 1)
                throw new Exception("Expected to insert 1 entry. Instead: " + updateCount);

            // queryTimeout = 1m20s (80)
            ResultSet result = stmt.executeQuery("values (current_date)");
            if (!result.next())
                throw new Exception("Query didn't return a result");
            int queryTimeout = stmt.getQueryTimeout();
            if (queryTimeout != 80)
                throw new Exception("Expecting queryTimeout = 80 seconds (from 1m20s). Instead: " + queryTimeout);

            // maxPoolSize = 1
            long start = System.currentTimeMillis();
            try {
                ds8.getConnection().close();
                throw new Exception("Should not be able to exceed maxPoolSize of 1");
            } catch (SQLTransientConnectionException x) {
                // connectionTimeout = 0 (immediate)
                // Timeout should occur immediately, but let's allow lots of buffer for slow machines (Observed 7.4 second delay logging FFDC)
                long duration = System.currentTimeMillis() - start;
                if (duration > 15000)
                    throw new Exception("Connection attempt should time out immediately, not " + duration + "ms");
            }
        } finally {
            con.close();
        }
    }
}
