/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from Apache BatchEE and modified
package chunktests.artifacts;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/*
 * This isn't going to work in a global tran
 */
public class NameColumnJdbcCursorHoldReader implements ItemReader {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    private final LinkedList<Object> items = new LinkedList<Object>();

    DataSource ds = null;
    boolean readYet = false;

    private String query = "SELECT name FROM intable";
    private String adjustedQuery;

    @Inject
    @BatchProperty(name = "dsjndi")
    private String jndi;

    int next = 1;

    private ResultSet rs = null;
    private PreparedStatement preparedStatement = null;
    private Connection conn = null;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        ds = DataSource.class.cast(new InitialContext().lookup(jndi));
        if (checkpoint != null) {
            next = (Integer) checkpoint;
        }
        adjustedQuery = query += " WHERE id >= " + next + " ORDER BY id ASC";
        logger.fine("In open(), adjustedQuery = " + adjustedQuery);

        executeQuery();
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        cleanupJDBC();
    }

    private void getConnection() throws Exception {
        conn = ds.getConnection("app", "app");
    }

    @Override
    public Object readItem() throws Exception {
        if (rs.next()) {
            next++;
            conn.commit();
            Object item = rs.getString("name");
            logger.fine("In readItem(), returning item: " + item);
            return item;
        } else {
            logger.fine("In readItem(), returning null");
            return null;
        }
    }

    private void executeQuery() throws Exception {

        logger.fine("In getResultSet(), about to execute query");

        try {
            getConnection();

            preparedStatement = conn.prepareStatement(adjustedQuery,
                                                      ResultSet.TYPE_FORWARD_ONLY,
                                                      ResultSet.CONCUR_UPDATABLE,
                                                      ResultSet.HOLD_CURSORS_OVER_COMMIT);
            rs = preparedStatement.executeQuery();
            conn.commit();

        } catch (Exception e) {
            cleanupJDBC();
        }

    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return next;
    }

    private void cleanupJDBC() throws Exception {
        if (rs != null) {
            rs.close();
        }
        preparedStatement.close();
        conn.close();
    }

}
