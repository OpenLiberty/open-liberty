/*******************************************************************************
 * Copyright (c) 2017,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.postgresql.web;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ThreadLocalConnectionTestServlet")
public class ThreadLocalConnectionTestServlet extends FATServlet {
    private static final long serialVersionUID = -4499882586720934635L;
    private static final String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";

    private enum TLSConnectionType {
        shared, free
    };

    public static final String APP_NAME = "postgresqlApp";

    @Resource
    private UserTransaction tran;

    @Resource(name = "jdbc/ds2tls")
    DataSource ds2tls;

    @Resource(name = "jdbc/ds3tls")
    DataSource ds3tls;

    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void testTLSConnectionValidation() throws Throwable {

        String dsJndiName = "jdbc/ds3tls";
        int expectedSize = 2;

        tran.begin();
        try (Connection con1 = ds3tls.getConnection();) {
        } catch (Exception e) {
            System.out.println("Exception caught in testTLSConnectionValidation");
            e.printStackTrace();
        }
        tran.commit();
        System.out.println("***Pool after testTLSConnectionValidation***");
        checkTLSPoolSize(dsJndiName, 1, expectedSize, TLSConnectionType.free, true, false);

    }

    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void testTLSConnectionValidationAfterShutDown() throws Throwable {

        String dsJndiName = "jdbc/ds3tls";
        int expectedSize = 1;

        tran.begin();
        try (Connection con1 = ds3tls.getConnection();) {
        } catch (Exception e) {
            System.out.println("Exception caught in testTLSConnectionValidationAfterShutDown");
            e.printStackTrace();
        }
        tran.commit();
        System.out.println("***Pool after testTLSConnectionValidationAfterShutDown***");
        checkTLSPoolSize(dsJndiName, 1, expectedSize, TLSConnectionType.free, true, true);
    }

    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void testTLSConnectionValidationAfterRestart() throws Throwable {

        testTLSConnectionValidation();

    }

    /**
     * This function should be called at the end of the test
     */
    public void checkPoolAfterTestTLSConnectionValidation() throws Exception {
        String dsJndiName = "jdbc/ds3tls";
        int expectedSize = 2;

        checkTLSPoolSize(dsJndiName, 1, expectedSize, TLSConnectionType.free, true);
    }

//    public void testTLSConnectionValidationWithShutdown() throws Throwable {
//
//        int conCount = 1;
//        String dsJndiName = "jdbc/ds2tls";
//        int expectedSize = 1;
//
//        killPostGreSQLConnections(ds2tls);
//
//        for (int i = 0; i < conCount; i++) {
//            tran.begin();
//            Connection con1 = ds2tls.getConnection();
//            con1.close();
//            tran.commit();
//            //shutDownDerby(ds3tls);
//
//            System.out.println("***Pool after connection " + (i + 1));
//            //checkTLSPoolSize(dsJndiName, expectedSize, TLSConnectionType.free, true, false);
//        }
//
//    }
//
//    public void killPostGreSQLConnections(DataSource ds) throws Exception {
//        try (Connection con = ds.getConnection()) {
//            con.createStatement().execute("select pg_terminate_backend(pid) from pg_stat_activity where usename = \'" + POSTGRES_USER + "\'");
//        } catch (Exception e) {
//            System.out.println("killPostGreSQLConnections threw exception:");
//            e.printStackTrace();
//        }
//
//    }

//    public void shutDownDerby(DataSource dataSource) throws Exception {
//        ClassLoader loader;
//        String databaseName;
//        Connection con1 = dataSource.getConnection();
//        try {
//            // This test can only run against Derby Embedded
//            // because we take advantage of shutting down the Derby database
//            // in order to cause pooled connections to go bad.
//            if (con1.getMetaData().getDriverName().indexOf("Derby Embedded") < 0)
//                return;
//
//            // Find the databaseName from the URL.
//            String url = con1.getMetaData().getURL();
//            databaseName = url.substring("jdbc:derby:".length());
//
//            // Get the class loader used for the Derby driver
//            Clob clob = con1.createClob();
//            loader = clob.getClass().getClassLoader();
//            clob.free();
//
//        } finally {
//            con1.close();
//        }
//
//        long start = System.currentTimeMillis();
//
//        // shut down Derby
//        Class<?> EmbDS = Class.forName("org.apache.derby.jdbc.EmbeddedDataSource40", true, loader);
//        DataSource ds = (DataSource) EmbDS.newInstance();
//        EmbDS.getMethod("setDatabaseName", String.class).invoke(ds, databaseName);
//        EmbDS.getMethod("setShutdownDatabase", String.class).invoke(ds, "shutdown");
//        EmbDS.getMethod("setUser", String.class).invoke(ds, "dbuser1");
//        EmbDS.getMethod("setPassword", String.class).invoke(ds, "{xor}Oz0vKDtu");
//        try {
//            ds.getConnection().close();
//            throw new Exception("Failed to shut down Derby database: " + databaseName);
//        } catch (SQLException x) {
//            // expected for shutdown
//            System.out.println(System.currentTimeMillis() + ": Derby shutdown result: " + x.getMessage());
//        }
//    }

    private ConnectionManagerMBean getConnectionManagerBean(String jndiName) throws Exception {
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

    private String getPoolContent(String jndiName) throws Exception {
        ConnectionManagerMBean cmBean = getConnectionManagerBean(jndiName);
        return cmBean.showPoolContents();
    }

    private void checkTLSPoolSize(String dsJndiName, int expectedMarkedSize, int expectedSize, TLSConnectionType connType, boolean printPoolContents) throws Exception {
        checkTLSPoolSize(dsJndiName, expectedMarkedSize, expectedSize, connType, printPoolContents, true);
    }

    private void checkTLSPoolSize(String dsJndiName, int expectedMarkedSize, int expectedSize, TLSConnectionType connType, boolean printPoolContents,
                                  boolean doAssert) throws Exception {
        //Parse the poolContents to ensure the connection was created in thread local storage
        String poolContents = getPoolContent(dsJndiName);
        //debug convenience option
        if (printPoolContents) {
            System.out.println(poolContents);
        }

        String actualSize = null;
        Pattern pattern = Pattern.compile("No " + connType.toString() + " TLS connections");
        Matcher matcher = pattern.matcher(poolContents);
        int actualMarkedSize = 0;
        if (matcher.find() && !matcher.group().isEmpty()) {
            actualSize = "0";
        } else {
            Pattern pattern2 = Pattern.compile("Total number of connection in " + connType.toString() + " TLS pool:\\s+([0-9]+)");
            Matcher matcher2 = pattern2.matcher(poolContents);

            if (matcher2.find() && !matcher2.group().isEmpty()) {
                actualSize = matcher2.group(1);
            }

            Pattern pattern3 = Pattern.compile("Connection marked to be destroyed.  Waiting for transaction end and connection close");
            Matcher matcher3 = pattern3.matcher(poolContents);

            while (matcher3.find()) {
                actualMarkedSize++;
            }
        }

        if (actualSize == null) {
            System.out.println(poolContents);
            throw new Exception("Unable to determine " + connType + " TLS pool size.");
        }

        if (doAssert) {
            assertEquals("Total number of connection in free TLS pool", String.valueOf(expectedSize), actualSize);
            assertEquals("Total number of connections marked to be destroyed.", String.valueOf(expectedMarkedSize), String.valueOf(actualMarkedSize));
        }
    }
}
