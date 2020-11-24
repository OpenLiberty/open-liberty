/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.mdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.resource.ResourceException;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.sql.DataSource;

@MessageDriven
public class DerbyRAMessageDrivenBean implements MessageListener {
    @Resource(lookup = "eis/ds5", shareable = true)
    private DataSource ds5;

    // Map of test case name to a connection that it intentionally left open
    private static final AtomicReference<Connection> cachedConnectionRef = new AtomicReference<>();

    @Override
    public Record onMessage(Record record) throws ResourceException {
        System.out.println("onMessage for " + record);

        MappedRecord m = (MappedRecord) record;
        Object key = m.get("key");
        Object newValue = m.get("newValue");
        Object oldValue = m.get("previousValue");

        // Write the previous value to a database table
        try {
            Connection con;
            if ("UseCachedConnection".equals(newValue)) {
                con = cachedConnectionRef.get();
                if (con.isClosed()) {
                    oldValue = "CachedConnectionIsClosed";
                    con = ds5.getConnection();
                } else {
                    oldValue = "CachedConnectionIsNotClosed";
                }
            } else {
                con = ds5.getConnection();
            }
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestActivationSpecTBL values ('" + key + "', '" + oldValue + "')");
                } catch (SQLIntegrityConstraintViolationException x) {
                    stmt.executeUpdate("update TestActivationSpecTBL set oldValue='" + oldValue + "' where id='" + key + "'");
                }
                stmt.close();
            } finally {
                if ("CacheConnection".equals(newValue)) {
                    System.out.println("MDB intentionally caches connection " + con);
                    cachedConnectionRef.set(con);
                } else {
                    cachedConnectionRef.set(null);
                    con.close();
                }
            }
        } catch (SQLException x) {
            throw new ResourceException(x);
        }
        m.clear();
        m.setRecordShortDescription(key + ": " + oldValue + " --> " + newValue);
        return m;
    }
}
