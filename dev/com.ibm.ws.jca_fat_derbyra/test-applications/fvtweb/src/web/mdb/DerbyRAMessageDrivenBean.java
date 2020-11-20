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
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.resource.ResourceException;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.sql.DataSource;

@MessageDriven
public class DerbyRAMessageDrivenBean implements MessageListener {
    @Resource(lookup = "eis/ds1", shareable = false)
    private DataSource ds1;

    // Map of test case name to a connection that it intentionally left open
    private static final Map<String, Connection> leakedConnections = new TreeMap<String, Connection>();

    @Override
    public Record onMessage(Record record) throws ResourceException {
        System.out.println("onMessage for " + record);

        MappedRecord m = (MappedRecord) record;
        Object key = m.get("key");
        Object newValue = m.get("newValue");
        Object oldValue = m.get("previousValue");

        String testNameForConnectionLeak = null;
        // look for special instructions from individual tests
        if ("mdbtestHandleListClosesConnectionLeakedFromMDB".equals(key)) {
            if ("LeakConnection".equals(newValue))
                testNameForConnectionLeak = (String) key;
            else if ("ExpectConnectionClosed".equals(newValue)) {
                Connection leakedCon = leakedConnections.get(key);
                try {
                    oldValue = leakedCon.isClosed();
                    System.out.println("Expecting connection to be closed by the HandleList. Did we find it closed? " + oldValue);
                } catch (SQLException x) {
                    throw new ResourceException(x);
                }
            } else
                System.out.println("Setting new value to " + newValue);
        }

        // Write the previous value to a database table
        try {
            Connection con = ds1.getConnection();
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("insert into TestActivationSpecTBL values ('" + key + "', '" + oldValue + "')");
                } catch (SQLIntegrityConstraintViolationException x) {
                    stmt.executeUpdate("update TestActivationSpecTBL set oldValue='" + oldValue + "' where id='" + key + "'");
                }
                stmt.close();
            } finally {
                if (testNameForConnectionLeak == null)
                    con.close();
                else {
                    System.out.println("MDB intentionally avoids closing connection " + con);
                    leakedConnections.put(testNameForConnectionLeak, con);
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
