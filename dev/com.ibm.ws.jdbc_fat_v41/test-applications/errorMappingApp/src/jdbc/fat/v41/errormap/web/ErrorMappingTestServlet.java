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

    @Resource(lookup = "jdbc/stateAndCode")
    DataSource stateAndCode;

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

    /**
     * Test that a non-configured error code does NOT map to a stale connection
     */
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

    /**
     * Verify that a user can configure a 'None' mapping for one of the builtin sqlstate mappings
     */
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

    /**
     * Verify that a datasource with no <identifyException> mappings properly identifies builtin mappings as stale connections
     */
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

    /**
     * Test that a datasource with several <identifyException> elements still properly identifies builtin error codes and sqlstates
     */
    @Test
    public void testBuiltinMappings() throws Exception {
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 1);

        // Force sqlState=55032. This should map to stale due to the builtin mappings in the generic DatabaseHelper
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("55032");

            System.out.println("Triggering sqlState=55032 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "55032", expected.getSQLState());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 0);

        // Force errorCode=45000. This should map to stale due to the builtin mappings in the derby-specific helper
        tx.begin();
        try (Connection con = manyMappings.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(45000);

            System.out.println("Triggering errorCode=45000 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 45000, expected.getErrorCode());
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/manyMappings", 0);
    }

    /**
     * Test a datasource that has multiple <identifyException> elements. All of the elements should be honored
     */
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

    /**
     * Test that we cannot lookup a datasource with an invalid <identifyException as="..."/> attribute
     */
    @Test
    public void testInvalidConfig_bogusTarget() throws Exception {
        try {
            InitialContext.doLookup("jdbc/invalid/bogusTarget");
            fail("Should not be able to lookup a datasource with <identifyException> with an invalid 'as' attribute");
        } catch (NamingException expected) {
            System.out.println("Caught expected exception: " + expected.getMessage());
        }
    }

    /**
     * Verify that if a datasource is configured with no sqlState or errorCode on an <identifyException> element
     * then it should not be accessible.
     */
    @Test
    public void testInvalidConfig_noStateOrCode() throws Exception {
        try {
            InitialContext.doLookup("jdbc/invalid/noStateOrCode");
            fail("Should not be able to lookup a datasource with <identifyException> with no 'sqlState' or 'errorCode' defined");
        } catch (NamingException expected) {
            System.out.println("Caught expected exception: " + expected.getMessage());
        }
    }

    /**
     * Test a datasource that has identifyMapping of errorCode=5001 and sqlState=E5001 mapped to stale.
     * If we get an exception with that exact errorCode and sqlState, it should map to a stale.
     */
    @Test
    public void testStateAndCode_01() throws Exception {
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/stateAndCode", 1);

        // Force a stale connection. The connection should be purged from the pool.
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(5001);
            customCon.setNextSqlState("E5001");

            System.out.println("Triggering errorCode=5001 sqlState=E5001 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 5001, expected.getErrorCode());
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "E5001", expected.getSQLState());
        } finally {
            tx.commit();
        }

        // The errorCode and sqlState should be mapped to "stale connection" which should evict the connection from the pool
        assertPoolSize("jdbc/stateAndCode", 0);
    }

    /**
     * Test a datasource that has identifyMapping of errorCode=5001 and sqlState=E5001 mapped to stale.
     * If we get an exception with only the matching errorCode, the exception should NOT map to stale.
     */
    @Test
    public void testStateAndCode_02() throws Exception {
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/stateAndCode", 1);

        // Force a stale connection. The connection should be purged from the pool.
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextErrorCode(5001);

            System.out.println("Triggering errorCode=5001 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), 5001, expected.getErrorCode());
        } finally {
            tx.commit();
        }

        // The exception should NOT map to "stale connection" and connection should still be in the pool
        assertPoolSize("jdbc/stateAndCode", 1);
    }

    /**
     * Test a datasource that has identifyMapping of errorCode=5001 and sqlState=E5001 mapped to stale.
     * If we get an exception with only the matching SQLState, the exception should NOT map to stale.
     */
    @Test
    public void testStateAndCode_03() throws Exception {
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            System.out.println("Got an initial connection");
        } finally {
            tx.commit();
        }
        assertPoolSize("jdbc/stateAndCode", 1);

        // Force a stale connection. The connection should be purged from the pool.
        tx.begin();
        try (Connection con = stateAndCode.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            customCon.setNextSqlState("E5001");

            System.out.println("Triggering sqlState=E5001 on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), "E5001", expected.getSQLState());
        } finally {
            tx.commit();
        }

        // The exception should NOT map to "stale connection" and connection should still be in the pool
        assertPoolSize("jdbc/stateAndCode", 1);
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
