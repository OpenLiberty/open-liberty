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
package fat.derbyra.resourceadapter;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.sql.XAConnection;

/**
 * Notifies message driven beans that a value in a DerbyMap was replaced.
 */
public class DerbyMessageWork implements Work {
    private final DerbyActivationSpec activationSpec;
    private final Object key, value, previous;
    private static boolean tableCreated = false;

    DerbyMessageWork(DerbyActivationSpec activationSpec, Object key, Object value, Object previous) {
        this.activationSpec = activationSpec;
        this.key = key;
        this.value = value;
        this.previous = previous;
    }

    private static synchronized void initTable(Connection con) throws SQLException {
        if (!tableCreated) {
            Statement stmt = con.createStatement();
            stmt.execute("create table TestActivationSpecRecoveryTBL (id varchar(50) not null primary key, description varchar(150))");
            stmt.close();
            tableCreated = true;
        }
    }

    @Override
    public void release() {}

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        System.out.println("Work impl will notify MDB for " + key + ", value " + previous + " updated to " + value);

        for (MessageEndpointFactory mef : activationSpec.messageEndpointFactories)
            try {
                MappedRecord record = new DerbyMappedRecord();
                record.setRecordName("MapValueReplaced");
                record.setRecordShortDescription("A value in DerbyMap was replaced with a new value.");
                record.put("key", key);
                record.put("newValue", value);
                record.put("previousValue", previous);

                // Fail xa commit/rollback only for tests that specify a key of mdbtestRecovery
                AtomicInteger successLimit = "mdbtestRecovery".equals(key) ? new AtomicInteger(0) : null;
                DerbyResourceAdapter ra = (DerbyResourceAdapter) activationSpec.getResourceAdapter();
                XAConnection xaCon = ra.xaDataSource.getXAConnection("ActvSpecUser", "ActvSpecPwd"); // XA connection is implicitly closed by DerbyXAResource upon successful commit/rollback
                DerbyXAResource xaRes = new DerbyXAResource(xaCon.getXAResource(), successLimit, activationSpec, xaCon);

                Connection con = xaCon.getConnection();
                initTable(con);

                MessageEndpoint endpoint = mef.createEndpoint(xaRes);
                MessageListener listener = (MessageListener) endpoint;
                Method onMessage = MessageListener.class.getMethod("onMessage", Record.class);
                endpoint.beforeDelivery(onMessage);
                record = (MappedRecord) listener.onMessage(record);

                System.out.println("Response from MDB has record name " + record.getRecordName() +
                                   " and description: " + record.getRecordShortDescription() +
                                   ". Content is " + record);

                // Write the response to the database under the same transaction
                try {
                    PreparedStatement ps = con.prepareStatement("insert into TestActivationSpecRecoveryTBL values (?,?)");
                    try {
                        ps.setString(1, key.toString());
                        ps.setString(2, record.getRecordShortDescription());
                        ps.executeUpdate();
                        System.out.println("inserted " + record.getRecordShortDescription());
                    } catch (SQLIntegrityConstraintViolationException x) {
                        ps.close();
                        ps = con.prepareStatement("update TestActivationSpecRecoveryTBL set description=? where id=?");
                        ps.setString(1, record.getRecordShortDescription());
                        ps.setString(2, key.toString());
                        ps.executeUpdate();
                        System.out.println("updated " + record.getRecordShortDescription());
                    }
                    ps.close();
                } finally {
                    con.close();
                }

                endpoint.afterDelivery();
                endpoint.release();
            } catch (ResourceException x) {
                x.printStackTrace();
                throw new RuntimeException(x);
            } catch (NoSuchMethodException x) {
                x.printStackTrace();
                throw new RuntimeException(x);
            } catch (SQLException x) {
                x.printStackTrace();
                throw new RuntimeException(x);
            }
    }
}
