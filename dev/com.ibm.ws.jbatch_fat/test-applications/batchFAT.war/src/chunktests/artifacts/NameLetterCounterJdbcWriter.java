/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package chunktests.artifacts;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 *
 */
public class NameLetterCounterJdbcWriter implements ItemWriter {

    DataSource ds = null;

    @Inject
    @BatchProperty(name = "dsjndi")
    private String jndi;

    @Inject
    @BatchProperty
    private String writeTable;

    @Override
    // Don't use checkpoint 
    public void open(Serializable checkpoint) throws Exception {
        ds = DataSource.class.cast(new InitialContext().lookup(jndi));
    }

    @Override
    public void close() throws Exception {}

    @Override
    public void writeItems(List<Object> items) throws Exception {
        final Connection conn = getConnection();
        for (Object o : items) {
            String item = (String) o;
            PreparedStatement statement = conn.prepareStatement("INSERT INTO " + writeTable + "(name,lettercount) VALUES(?,?)");
            statement.setString(1, item);
            statement.setInt(2, item.length());
            statement.executeUpdate();
            statement.close();
        }
    }

    // Don't use checkpoint 
    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    private Connection getConnection() throws Exception {
        return ds.getConnection("app", "app");
    }
}
