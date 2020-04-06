/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ucp.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolXADataSource;

@DataSourceDefinitions(value = {
                                 @DataSourceDefinition(
                                                       name = "java:comp/env/jdbc/dsdUCPDS",
                                                       className = "oracle.ucp.jdbc.PoolDataSourceImpl",
                                                       url = "${env.URL}",
                                                       isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
                                                       maxIdleTime = 30,
                                                       minPoolSize = 1,
                                                       initialPoolSize = 1, //This property should be allowed when using UCP
                                                       user = "${env.USER}",
                                                       password = "${env.PASSWORD}",
                                                       maxPoolSize = 3,
                                                       maxStatements = 9,
                                                       properties = {
                                                                      "connectionTimeout=1", //Liberty property, shouldn't take effect
                                                                      "connectionWaitTimeout=30", //UCP property - should be used
                                                                      "validationTimeout=30",
                                                                      "connectionFactoryClassName=oracle.jdbc.pool.OracleDataSource"
                                                       }),
                                 @DataSourceDefinition(
                                                       name = "java:comp/env/jdbc/dsdXAUCPDS",
                                                       className = "oracle.ucp.jdbc.PoolXADataSourceImpl",
                                                       url = "${env.URL}",
                                                       initialPoolSize = 1,
                                                       user = "${env.USER}",
                                                       password = "${env.PASSWORD}",
                                                       maxStatements = 10,
                                                       properties = {
                                                                      "validationTimeout=30"
                                                       })
})

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleUCPTestServlet")
public class OracleUCPTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/ucpDS", shareable = false)
    private DataSource ucpDS;

    @Resource(lookup = "jdbc/oracleDS", shareable = false)
    private DataSource oracleDS;

    @Resource(lookup = "jdbc/ucpXADS", shareable = false)
    private DataSource ucpXADS;

    @Resource(lookup = "jdbc/ucpDS", shareable = true)
    private DataSource sharedUCPDS;

    @Resource(lookup = "jdbc/ucpDSEmbeddedConMgr", shareable = false)
    private DataSource ucpDSEmbeddedConMgr;

    @Resource(lookup = "java:comp/env/jdbc/dsdUCPDS", shareable = false)
    private DataSource dsdUCPDS;

    @Resource(lookup = "java:comp/env/jdbc/dsdXAUCPDS", shareable = false)
    private DataSource dsdXAUCPDS;

    @Resource(lookup = "jdbc/ucpDSAuthData")
    private DataSource ucpDSAuthData;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    @Resource
    private UserTransaction tran;

    /**
     * Basic test that we can get and use a connection when using Oracle UCP
     */
    @Test
    public void testOracleUCP() throws Exception {
        Connection con = ucpDS.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 1);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("maroon", result.getString(1));
            assertFalse(result.next());
        } finally {
            con.close();
        }
    }

    /**
     * Test that you cannot use UCP as a ConnectionPoolDataSource since Oracle doesn't implement
     * a ConnectionPoolDataSource for UCP.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLNonTransientException" })
    public void testOracleUCPConnectionPoolDS() throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            ctx.lookup("jdbc/ucpConnectionPoolDS");
            fail("Should not be able to use Connection Pool DataSource when using UCP.");
        } catch (Exception ex) {
            System.out.println("Caught exception: " + ex);
            //expected
        }
    }

    /**
     * Basic test that we can get and use a connection when using Oracle UCP with an XA datasource.
     */
    @Test
    public void testOracleUCPXADS() throws Exception {
        Connection con = ucpXADS.getConnection();

        assertTrue(ucpXADS.isWrapperFor(PoolXADataSource.class));

        try {
            PreparedStatement ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 1);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("maroon", result.getString(1));
            assertFalse(result.next());
        } finally {
            con.close();
        }
    }

    /**
     * Test that you cannot use UCP as a java.sql.Driver since Oracle doesn't implement
     * the Driver interface for UCP.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLNonTransientException" })
    public void testOracleUCPDriverDS() throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            ctx.lookup("jdbc/ucpDriverDS");
            fail("Should not be able to use Driver when using UCP.");
        } catch (Exception ex) {
            System.out.println("Caught exception: " + ex);
            //expected
        }
    }

    /**
     * Test that using UCP with XA, connections can be properly enlisted in a transaction and
     * rolled back/committed as appropriate. Uses two resources to ensure that we are actually
     * using XA and not just getting the 1PC optimization.
     */
    @Test
    public void testOracleUCPXATranEnlistment() throws Exception {
        Connection con = ucpXADS.getConnection();
        Connection con2 = null;

        try {
            con2 = ucpXADS.getConnection();
            tran.begin();

            //Add a new row to the db
            PreparedStatement ps = con.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 2);
            ps.setString(2, "gold");
            ps.executeUpdate();
            ps.close();

            //Add a new row to the db using the second 2PC resource
            ps = con2.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 3);
            ps.setString(2, "green");
            ps.executeUpdate();
            ps.close();

            tran.commit();

            //ensure our updates were committed
            ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 2);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("gold", result.getString(1));
            assertFalse(result.next());

            ps = con2.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 3);
            result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("green", result.getString(1));
            assertFalse(result.next());

            //now add another row the db, but rollback the transaction
            tran.begin();

            ps = con.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 4);
            ps.setString(2, "blue");
            ps.executeUpdate();
            ps.close();

            tran.rollback();

            //ensure our update was rolled back
            ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 4);
            result = ps.executeQuery();

            assertFalse(result.next());

        } finally {
            con.close();
            if (con2 != null)
                con2.close();
        }
    }

    /**
     * Test that connection sharing behaves as normal when using UCP. You should be able to take advantage
     * of serial reuse with UCP since the connection is not actually closed until a transaction boundary
     * is crossed.
     */
    @Test
    public void testOracleUCPConnectionSharing() throws Exception {
        Connection con = sharedUCPDS.getConnection();
        Connection con2 = null;

        try {
            //use the connection
            PreparedStatement ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 1);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("maroon", result.getString(1));
            assertFalse(result.next());

            con.close();
            assertEquals("Pool size should still be 1 after connection is closed since we have not left the transaction scope",
                         1, getPoolSize("jdbc/ucpDS"));

            con2 = sharedUCPDS.getConnection();

            assertEquals("Pool size should be 1 since we should be using the shared connection",
                         1, getPoolSize("jdbc/ucpDS"));

        } finally {
            con.close();
            if (con2 != null)
                con2.close();
        }

    }

    /**
     * Basic test that we can get and use a connection when using Oracle UCP with an embedded connection
     * manager.
     */
    @Test
    public void testOracleUCPEmbeddedConMgr() throws Exception {
        Connection con = ucpDSEmbeddedConMgr.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 1);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("maroon", result.getString(1));
            assertFalse(result.next());
        } finally {
            con.close();
        }
    }

    /**
     * This tests that the proper max connections value is enforced. The Liberty connection manager is
     * configured with max connections = 2, which should be ignored since UCP is being used. UCP is configured
     * with max connections 3, which should be enforced.
     */
    @Test
    public void testOracleUCPMaxConnections() throws Exception {
        Connection con1 = ucpDS.getConnection();
        Connection con2 = null;
        Connection con3 = null;

        try {
            con2 = ucpDS.getConnection();
            //Should be able to get this connection since the Liberty config is ignored
            con3 = ucpDS.getConnection();

            //Need to request the fourth connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(() -> {
                try (Connection con4 = ucpDS.getConnection()) {
                    return true;
                }
            });

            //Wait 10 seconds, assume we would normally would obtain a connection in that amount of time
            try {
                fail("The task should not have completed, instead returned " + future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con3.close();
            assertTrue(future.get(5, TimeUnit.MINUTES));

        } finally {
            con1.close();
            con2.close();
        }
    }

    /**
     * That that if a minPoolSize is configured on a connection manager used by Oracle UCP
     * the value is ignored.
     */
    @Test
    public void testOracleUCPMinConnections() throws Exception {
        Connection con = ucpDSEmbeddedConMgr.getConnection();
        con.close();

        assertEquals("Connection pool should be empty", 0, getPoolSize("jdbc/ucpDSEmbeddedConMgr"));
    }

    /**
     * This tests that the proper max connections value is enforced when using an embedded connection
     * manager. The Liberty connection manager is configured with max connections = 2, which
     * should be ignored since UCP is being used. UCP is configured with max connections 3, which should
     * be enforced.
     */
    @Test
    public void testOracleUCPMaxConnectionsEmbedded() throws Exception {
        Connection con1 = ucpDSEmbeddedConMgr.getConnection();
        Connection con2 = null;
        Connection con3 = null;

        try {
            con2 = ucpDSEmbeddedConMgr.getConnection();
            //Should be able to get this connection since the Liberty config is ignored
            con3 = ucpDSEmbeddedConMgr.getConnection();

            //Need to request the fourth connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(() -> {
                try (Connection con4 = ucpDSEmbeddedConMgr.getConnection()) {
                    return true;
                }
            });

            //Wait 10 seconds, assume that would normally would obtain a connection in that amount of time
            try {
                fail("The task should not have completed, instead returned " + future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con3.close();
            assertTrue(future.get(5, TimeUnit.MINUTES));

        } finally {
            con1.close();
            con2.close();
        }
    }

    /**
     * Test that an Oracle DataSource using the Liberty Connection Manager and an Oracle DataSource
     * using UCP can be used simultaneously.
     */
    @Test
    public void testUCPAndLibertyConnPool() throws Exception {
        //first get a UCP connection
        Connection ucpCon = ucpDS.getConnection();
        Connection con1 = oracleDS.getConnection();

        try {

            //Need to request the second connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(() -> {
                try (Connection con2 = oracleDS.getConnection()) {
                    return true;
                }
            });

            //Wait 10 seconds, assume that would normally would obtain a connection in that amount of time
            try {
                fail("The task should not have completed, instead returned " + future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con1.close();
            assertTrue(future.get(5, TimeUnit.MINUTES));

        } finally {
            con1.close();
            ucpCon.close();
        }
    }

    /**
     * Test that the minPoolSize property is not utilized by the Liberty connection manager
     * when using UCP with DataSourceDefinition
     */
    @Test
    public void testUCPDSDMinConnections() throws Exception {
        Connection con = dsdUCPDS.getConnection();
        con.close();

        assertEquals("Connection pool should be empty", 0, getPoolSize("java.comp/env/jdbc/dsdUCPDS"));
    }

    /**
     * Test that using UCP with XA in DataSourceDefinition, connections can be properly enlisted
     * in a transaction and rolled back/committed as appropriate. Uses two resources to ensure
     * that we are actually using XA and not just getting the 1PC optimization.
     */
    @Test
    public void testOracleUCPXATranEnlistmentDSD() throws Exception {
        Connection con = dsdXAUCPDS.getConnection();
        Connection con2 = null;

        try {
            con2 = dsdXAUCPDS.getConnection();
            tran.begin();

            //Add a new row to the db
            PreparedStatement ps = con.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 100);
            ps.setString(2, "gray");
            ps.executeUpdate();
            ps.close();

            //Add a new row to the db using the second 2PC resource
            ps = con2.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 101);
            ps.setString(2, "purple");
            ps.executeUpdate();
            ps.close();

            tran.commit();

            //ensure our updates were committed
            ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 100);
            ResultSet result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("gray", result.getString(1));
            assertFalse(result.next());

            ps = con2.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 101);
            result = ps.executeQuery();

            assertTrue(result.next());
            assertEquals("purple", result.getString(1));
            assertFalse(result.next());

            //now add another row the db, but rollback the transaction
            tran.begin();

            ps = con.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 102);
            ps.setString(2, "yellow");
            ps.executeUpdate();
            ps.close();

            tran.rollback();

            //ensure our update was rolled back
            ps = con.prepareStatement("SELECT COLOR FROM COLORTABLE WHERE ID=?");
            ps.setInt(1, 102);
            result = ps.executeQuery();

            assertFalse(result.next());

        } finally {
            con.close();
            if (con2 != null)
                con2.close();
        }
    }

    /**
     * Test that when using the maxStatements property of DataSource definition the value
     * is properly divided by the maxPoolSize.
     */
    @Test
    public void testUCPMaxStatements() throws Exception {
        //TODO remove this restriction once Oracle release a JDBC 4.3 compliant driver
        boolean atLeastJava9 = false;
        //Oracle driver and UCP for JDBC 4.2 is not compatible with Java 9+
        try {
            Class.forName("java.lang.Runtime$Version"); // added in Java 9
            atLeastJava9 = true;
        } catch (ClassNotFoundException x) {
            atLeastJava9 = false;
        }

        if (atLeastJava9) {
            System.out.println("Skipping testUCPMaxStatements because we are running on java 9 or greater");
            return;
        }
        PoolDataSource poolDataSourceDSD = dsdUCPDS.unwrap(PoolDataSource.class);
        assertEquals("The maxStatements should be divided by the maxPoolSize", 3, poolDataSourceDSD.getMaxStatements());

        PoolXADataSource poolDataSourceXADSD = dsdXAUCPDS.unwrap(PoolXADataSource.class);
        assertEquals("The maxStatements should be 0 since maxPoolSize is not defined", 0, poolDataSourceXADSD.getMaxStatements());

        PoolDataSource poolDataSource = ucpDS.unwrap(PoolDataSource.class);
        assertEquals("maxStatements shouldn't be divided when not using DataSourceDef", 5, poolDataSource.getMaxStatements());
    }

    /**
     * Tests that UCP properties that share a name with their Liberty equivalents are
     * correctly passed to the UCP driver when using DataSourceDefintiion.
     */
    @Test
    public void testDataSourceDefProps() throws Exception {
        //TODO remove this restriction once Oracle release a JDBC 4.3 compliant driver
        boolean atLeastJava9 = false;
        //Oracle driver and UCP for JDBC 4.2 is not compatible with Java 9+
        try {
            Class.forName("java.lang.Runtime$Version"); // added in Java 9
            atLeastJava9 = true;
        } catch (ClassNotFoundException x) {
            atLeastJava9 = false;
        }

        if (atLeastJava9) {
            System.out.println("Skipping testDataSourceDefProps because we are running on java 9 or greater");
            return;
        }
        PoolDataSource pds = dsdUCPDS.unwrap(PoolDataSource.class);

        assertEquals("maxIdleTime not set on UCP", 30, pds.getMaxIdleTime());

        assertEquals("maxPoolSize not set on UCP", 3, pds.getMaxPoolSize());

        assertEquals("minPoolSize not set on UCP", 1, pds.getMinPoolSize());

        //initialPoolSize doesn't have a Liberty equivalent so ensure it is allowed through
        assertEquals("initialPoolSize not set on UCP", 1, pds.getInitialPoolSize());
    }

    /**
     * Tests that when a connection manager is shared between two datasources an
     * exception is thrown as that is not a supported configuration
     */
    @Test
    @ExpectedFFDC({ "java.lang.UnsupportedOperationException" })
    public void testSharingConMgr() throws Exception {
        InitialContext ctx = new InitialContext();
        try {
            ctx.lookup("jdbc/ucpDSSameConMgr");
            fail("Lookup should fail");
        } catch (Exception ex) {
            //expected
        }
    }

    /**
     * Test that the appropriate connection manager properties (currently just
     * enableSharingForDirectLookups) are still enforced when using UCP
     */
    @Test
    public void testOtherConnMgrPropsApplied() throws Exception {
        //do a direct lookup of ucpDS
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("jdbc/ucpDS");

        //get and close a connection
        Connection con = ds.getConnection();
        con.close();

        //since we have enableSharingForDirectLookups=false, the connection should be closed
        //in Liberty and returned to UCP, otherwise it would remain in the shared pool
        assertEquals("There should be 0 connections in the pool", 0, getPoolSize("jdbc/ucpDS"));
    }

    /**
     * Test that when supplied with a user and password in a get connection request the
     * credentials are honored by the UCP driver
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException", "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void getConnectionUserPass() throws Exception {
        try {
            ucpDS.getConnection("wrongUser", "wrongPassword");
            fail("Credentials with the connection request were not honored");
        } catch (Exception ex) {
            //expected
        }
    }

    /**
     * Test that when supplied with a user and password in containerAuthData the
     * credentials are honored by the UCP driver, even though a user and password
     * are also required as ds props
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException", "javax.resource.spi.ResourceAllocationException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testAuthData() throws Exception {
        try {
            ucpDSAuthData.getConnection();
            fail("User/password from containerAuthData not honored");
        } catch (Exception ex) {
            //expected
        }
    }

    //Used by config update tests to verify we are using a UCP datasource and
    //using the UCP rather than Liberty connection manager config
    public void testUsingUCP() throws Exception {
        assertTrue(oracleDS.isWrapperFor(PoolDataSource.class));
        Connection con1 = oracleDS.getConnection();
        Connection con2 = null;

        try {
            //Should be able to get a second connection as we've switched to using UCP config
            con2 = oracleDS.getConnection();

            //Need to request the third connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(() -> {
                try (Connection con3 = oracleDS.getConnection()) {
                    return true;
                }
            });

            //Wait 10 seconds, assume that would normally would obtain a connection in that amount of time
            try {
                fail("The task should not have completed, instead returned " + future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con2.close();
            assertTrue(future.get(5, TimeUnit.MINUTES));

        } finally {
            con1.close();
            con2.close();
        }
    }

    //Used by config update tests to verify we are using a non UCP datasource and
    //using the Liberty rather than UCP connection manager config
    public void testUsingLibertyConnPool() throws Exception {
        assertFalse(oracleDS.isWrapperFor(PoolDataSource.class));
        Connection con1 = oracleDS.getConnection();

        try {
            //Need to request the second connection async since the getConnection request should hang
            //until there is room in the pool
            Future<Boolean> future = executor.submit(() -> {
                try (Connection con2 = oracleDS.getConnection()) {
                    return true;
                }
            });

            //Wait 10 seconds, assume that would normally would obtain a connection in that amount of time
            try {
                fail("The task should not have completed, instead returned " + future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException ex) {
                //expected
            }

            //Now try to close one of the connections, which should allow the other task to complete
            con1.close();
            assertTrue(future.get(5, TimeUnit.MINUTES));

        } finally {
            con1.close();
        }
    }

    //Used by testUpdateDSType
    public void testUsingPoolDataSource() throws Exception {
        assertTrue(ucpXADS.isWrapperFor(PoolDataSource.class));
        assertFalse(ucpXADS.isWrapperFor(PoolXADataSource.class));
    }

    private ObjectInstance getMBeanObjectInstance(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    private int getPoolSize(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectInstance bean = getMBeanObjectInstance(jndiName);
        System.out.println("Found " + bean.getObjectName().toString());
        String contents = (String) mbs.invoke(bean.getObjectName(), "showPoolContents", null, null);
        System.out.println("   " + contents.replace("\n", "\n   "));

        return Integer.parseInt((String) mbs.getAttribute(bean.getObjectName(), "size"));
    }
}
