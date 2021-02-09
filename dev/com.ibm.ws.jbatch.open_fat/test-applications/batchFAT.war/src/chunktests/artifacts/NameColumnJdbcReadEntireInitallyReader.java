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

public class NameColumnJdbcReadEntireInitallyReader implements ItemReader {

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

    @Override
    public void open(Serializable checkpoint) throws Exception {
        ds = DataSource.class.cast(new InitialContext().lookup(jndi));
        if (checkpoint != null) {
            next = (Integer) checkpoint;
        }
        adjustedQuery = query += " WHERE id >= " + next + " ORDER BY id ASC";
        logger.fine("In open(), adjustedQuery = " + adjustedQuery);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    private Connection getConnection() throws Exception {
        return ds.getConnection("app", "app");
    }

    @Override
    public Object readItem() throws Exception {
        logger.entering("readItem", "In readItem(), about to execute query");
        if (!readYet) {
            logger.fine("In readItem(), about to execute query");
            final Connection conn = getConnection();
            try {
                final PreparedStatement preparedStatement = conn.prepareStatement(adjustedQuery,
                                                                                  ResultSet.TYPE_FORWARD_ONLY,
                                                                                  ResultSet.CONCUR_UPDATABLE,
                                                                                  ResultSet.HOLD_CURSORS_OVER_COMMIT);
                ResultSet resultSet = null;
                try {
                    resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()) {
                        logger.fine("In readItem(), adding to reader item cache");
                        items.add(resultSet.getString("name"));
                    }
                    if (items.isEmpty()) {
                        logger.fine("In readItem(), query matched nothing");
                        return null;
                    }
                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    preparedStatement.close();
                }
            } finally {
                logger.fine("In readItem(), closing connection");
                conn.close();
            }
            readYet = true;
        }
        if (items.isEmpty() && readYet) {
            logger.exiting("readItem", "No more items");
            return null;
        } else {
            next++;
            logger.fine("In readItem(), next = " + next);
            Object retVal = items.pop();
            logger.exiting("readItem", retVal.toString());
            return retVal;
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return next;
    }

}
