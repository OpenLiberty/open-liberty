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
package jdbc.fat.v41.errormap.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.annotation.Resource;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean;

import componenttest.app.FATServlet;
import jdbc.fat.v41.errormap.driver.ErrorMapConnection;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ErrorMappingTestServlet")
public class ErrorMappingTestServlet extends FATServlet {

    @Resource
    UserTransaction tx;

    @Resource(lookup = "jdbc/errorMap")
    DataSource ds;

    @Resource(lookup = "jdbc/removeMapping")
    DataSource removeMapping;

    @Resource(lookup = "jdbc/noMappings")
    DataSource noMappings;

    @Resource(lookup = "jdbc/manyMappings")
    DataSource manyMappings;

    /**
     * Verify that once a connection goes stale (as indicated by server.xml config)
     * it is removed from the connection pool.
     */
    @Test
    public void testStaleConnection() throws Exception {

        tx.begin();
        try (Connection con = ds.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/errorMap", 1);

        // Force a stale connection. The connection should be purged from the pool.
        tx.begin();
        try (Connection con = ds.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(1234);

            System.out.println("Triggering errorCode 1234 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 1234, expected.getErrorCode());
        } finally {
            tx.commit();
        }

        // The errorCode should be mapped to "stale connection" which should evict the connection from the pool
        assertPoolSize("jdbc/errorMap", 0);
    }

    @Test
    public void testUnmappedError() throws Exception {
        tx.begin();
        try (Connection con = ds.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/errorMap", 1);

        // Force a connection error. It will not be purged from the pool because it is not mapped to a stale connection
        tx.begin();
        try (Connection con = ds.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(5555);

            System.out.println("Triggering errorCode 5555 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 5555, expected.getErrorCode());
        } finally {
            tx.commit();
        }

        // Since errorCode 5555 is not mapped to a stale connection, it should still be in the pool
        assertPoolSize("jdbc/errorMap", 1);
    }

    @Test
    public void testRemovedMapping() throws Exception {
        tx.begin();
        try (Connection con = removeMapping.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/removeMapping", 1);

        // Force a connection error for sqlState '08001'. Normally it would map to a connection error according to the built-in
        // mappings we have, but since the mapping has been removed it will not be purged from the pool
        tx.begin();
        try (Connection con = removeMapping.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("08001");

            System.out.println("Triggering sqlState '08001' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "08001", expected.getSQLState());
        } finally {
            tx.commit();
        }

        // Since the error mapping for sqlState '08001' was removed, the connection should still be in the pool
        assertPoolSize("jdbc/removeMapping", 1);
    }

    @Test
    public void testNoMappings() throws Exception {
        tx.begin();
        try (Connection con = noMappings.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/noMappings", 1);

        // Force a connection error for sqlState '08001'. This should map to a stale connection and be removed from the pool
        tx.begin();
        try (Connection con = noMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("08001");

            System.out.println("Triggering sqlState '08001' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "08001", expected.getSQLState());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/noMappings", 0);

        // Force a connection error for sqlState '08002'. This should NOT map to a stale connection and remain in the pool
        tx.begin();
        try (Connection con = noMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("08002");

            System.out.println("Triggering sqlState '08002' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "08002", expected.getSQLState());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/noMappings", 1);
    }

    @Test
    public void testManyMappings() throws Exception {
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 1);

        // Force a connection error for errorCode -8. This should map to a stale connection and be removed from the pool
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(-8);

            System.out.println("Triggering errorCode '-8' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), -8, expected.getErrorCode());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 0);

        // Force a connection error for sqlState '1010'. This should NOT map to a stale connection and remain in the pool
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(1010);

            System.out.println("Triggering errorCode '1010' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 1010, expected.getErrorCode());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 1);

        // Force a connection error for sqlState 'E1111'. This should map to a stale connection and be removed from the pool
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("E1111");

            System.out.println("Triggering sqlState 'E1111' on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "E1111", expected.getSQLState());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 0);
    }

    public void testInvalidConfig_noTarget() throws Exception {
        // Due to the way required config attributes work in Liberty, the invalid element will be ignored but an error message will be in logs
        InitialContext.doLookup("jdbc/invalid/noTarget");
    }

    @Test
    public void testInvalidConfig_bogusTarget() throws Exception {
        try {
            InitialContext.doLookup("jdbc/invalid/bogusTarget");
            fail("Should not be able to lookup a datasource with <identifyException> with an invalid 'as' attribute");
        } catch (NamingException expected) {
            System.out.println("Caught expected exception: " + expected.getMessage());
        }
    }

    @Test
    public void testInvalidConfig_noStateOrCode() throws Exception {
        try {
            InitialContext.doLookup("jdbc/invalid/noStateOrCode");
            fail("Should not be able to lookup a datasource with <identifyException> with no 'sqlState' or 'errorCode' defined");
        } catch (NamingException expected) {
            System.out.println("Caught expected exception: " + expected.getMessage());
        }
    }

    @Test
    public void testInvalidConfig_stateAndCode() throws Exception {
        try {
            InitialContext.doLookup("jdbc/invalid/stateAndCode");
            fail("Should not be able to lookup a datasource with <identifyException> with a 'sqlState' and 'errorCode' defined");
        } catch (NamingException expected) {
            System.out.println("Caught expected exception: " + expected.getMessage());
        }
    }

    private static void assertPoolSize(String jndiName, int expectedSize) throws Exception {
        ConnectionManagerMBean mbean = getConnectionManagerBean(jndiName);
        assertEquals("Connection pool did not contain the expected number of connections: " + mbean.showPoolContents(),
                     expectedSize, mbean.getSize());
    }

    private static ConnectionManagerMBean getConnectionManagerBean(String jndiName) throws Exception {
        final String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return JMX.newMBeanProxy(mbs, s.iterator().next().getObjectName(), ConnectionManagerMBean.class);
    }

}
