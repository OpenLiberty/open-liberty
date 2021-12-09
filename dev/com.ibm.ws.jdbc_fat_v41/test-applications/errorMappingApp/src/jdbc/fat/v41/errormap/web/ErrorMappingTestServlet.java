/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
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

    /**
     * Verifies that SQL state 08888 does not indicate StaleConnection (for config update tests)
     */
    public void testIdentify08888NotStale() throws Exception {
        assertNotStale("jdbc/errorMap", "08888", null);
    }

    /**
     * Verifies that SQL state 08888 indicates StaleConnection (for config update tests)
     */
    public void testIdentify08888Stale() throws Exception {
        assertStale("jdbc/errorMap", "08888", null);
    }

    /**
     * Verifies that error code 1234 does not indicate StaleConnection (for config update tests)
     */
    public void testIdentify1234NotStale() throws Exception {
        assertNotStale("jdbc/errorMap", null, 1234);
    }

    /**
     * Verify that once a connection goes stale (as indicated by server.xml config)
     * it is removed from the connection pool.
     */
    @Test
    public void testIdentify1234Stale() throws Exception {
        assertStale("jdbc/errorMap", null, 1234);
    }

    /**
     * Test that a non-configured error code does NOT map to a stale connection
     */
    @Test
    public void testUnmappedError() throws Exception {
        assertNotStale("jdbc/errorMap", null, 5555);
    }

    /**
     * Verify that a user can configure a 'None' mapping for one of the builtin sqlstate mappings
     */
    @Test
    public void testRemovedMapping() throws Exception {
        assertNotStale("jdbc/removeMapping", "08001", null);
    }

    /**
     * Test removing a mapping for only a specific sqlcode and sqlstate
     * Normally SQLState 08003 maps to stale, but here we have explicitly configured state=08003 && code=1234
     * to map to "None" to remove the mapping
     */
    @Test
    public void testRemovedMapping_stateAndCode() throws Exception {
        assertNotStale("jdbc/removeMapping", "08003", 1234);
    }

    /**
     * Verify that a datasource with no <identifyException> mappings properly identifies builtin mappings as stale connections
     */
    @Test
    public void testNoMappings() throws Exception {
        assertStale("jdbc/noMappings", "08001", null);

        assertNotStale("jdbc/noMappings", "08002", null);
    }

    /**
     * Test that a datasource with several <identifyException> elements still properly identifies builtin error codes and sqlstates
     */
    @Test
    public void testBuiltinMappings() throws Exception {
        assertStale("jdbc/manyMappings", "55032", null);

        assertStale("jdbc/manyMappings", null, 45000);
    }

    /**
     * Test a datasource that has multiple <identifyException> elements. All of the elements should be honored
     */
    @Test
    public void testManyMappings() throws Exception {
        assertStale("jdbc/manyMappings", null, -8);

        assertNotStale("jdbc/manyMappings", null, 1010);

        assertStale("jdbc/manyMappings", "E1111", null);
    }

    public void testInvalidConfig_noTarget() throws Exception {
        // Due to the way required config attributes work in Liberty, the invalid element will be ignored but an error message will be in logs
        InitialContext.doLookup("jdbc/invalid/noTarget");
    }

    /**
     * Test that we can lookup a datasource with an invalid <identifyException as="..."/> attribute,
     * and legacy exception values do not identify as stale when legacy function is not enabled.
     */
    @Test
    public void testInvalidConfig_bogusTarget() throws Exception {
        assertNotStale("jdbc/invalid/bogusTarget", null, 1234);
        assertNotStale("jdbc/invalid/bogusTarget", "SCE99", null);
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
        assertStale("jdbc/stateAndCode", "E5001", 5001);
    }

    /**
     * Test a datasource that has identifyMapping of errorCode=5001 and sqlState=E5001 mapped to stale.
     * If we get an exception with only the matching errorCode, the exception should NOT map to stale.
     */
    @Test
    public void testStateAndCode_02() throws Exception {
        assertNotStale("jdbc/stateAndCode", null, 5001);
    }

    /**
     * Test a datasource that has identifyMapping of errorCode=5001 and sqlState=E5001 mapped to stale.
     * If we get an exception with only the matching SQLState, the exception should NOT map to stale.
     */
    @Test
    public void testStateAndCode_03() throws Exception {
        assertNotStale("jdbc/stateAndCode", "E5001", null);
    }

    private void assertStale(String jndiName, String sqlState, Integer errorCode) throws Exception {
        DataSource ds = InitialContext.doLookup(jndiName);
        tx.begin();
        try (Connection con = ds.getConnection()) {
            System.out.println("Got an initial connection from " + jndiName);
        } finally {
            tx.commit();
        }
        assertPoolSize(jndiName, 1);

        tx.begin();
        try (Connection con = ds.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            if (sqlState != null)
                customCon.setNextSqlState(sqlState);
            if (errorCode != null)
                customCon.setNextErrorCode(errorCode);

            System.out.println("Triggering SQLException with sqlState=" + sqlState + " and errorCode=" + errorCode + " on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            if (sqlState != null)
                assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), sqlState, expected.getSQLState());
            if (errorCode != null)
                assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), (int) errorCode, expected.getErrorCode());
        } finally {
            tx.commit();
        }

        // Pool should be empty because error should have mapped to stale
        assertPoolSize(jndiName, 0);
    }

    private void assertNotStale(String jndiName, String sqlState, Integer errorCode) throws Exception {
        DataSource ds = InitialContext.doLookup(jndiName);
        tx.begin();
        try (Connection con = ds.getConnection()) {
            System.out.println("Got an initial connection from " + jndiName);
        } finally {
            tx.commit();
        }
        assertPoolSize(jndiName, 1);

        tx.begin();
        try (Connection con = ds.getConnection()) {
            ErrorMapConnection customCon = con.unwrap(ErrorMapConnection.class);
            if (sqlState != null)
                customCon.setNextSqlState(sqlState);
            if (errorCode != null)
                customCon.setNextErrorCode(errorCode);

            System.out.println("Triggering SQLException with sqlState=" + sqlState + " and errorCode=" + errorCode + " on connection");
            customCon.createStatement();
        } catch (SQLException expected) {
            if (sqlState != null)
                assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), sqlState, expected.getSQLState());
            if (errorCode != null)
                assertEquals("Did not get expected exception. Message was: " + expected.getMessage(), (int) errorCode, expected.getErrorCode());
        } finally {
            tx.commit();
        }

        // connection should still be in pool because exception should NOT map to stale
        assertPoolSize(jndiName, 1);
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
