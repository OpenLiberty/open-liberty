/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.derby;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT test for DataSourceValidationMBean.validateConnections().
 *
 * The MBean is registered as an immediate DS component (immediate:=true)
 * in com.ibm.ws.jdbc, so it is present as soon as jdbc-4.x activates —
 * before any connection pool is initialised.
 *
 * Server config: jdbc-4.1 + jndi-1.0 + localConnector-1.0, one Derby
 * embedded datasource, no web application.
 */
@RunWith(FATRunner.class)
public class ConnectionManagerMBeanValidateTest {

    private static final Class<?> c = ConnectionManagerMBeanValidateTest.class;

    private static final String SERVER_NAME = "com.ibm.ws.jdbc.fat.derby.validateconn";
    private static final String CONNECTOR_ADDRESS_FILE = "com.ibm.ws.jmx.local.address";
    private static final String DATASOURCE_JNDI = "jdbc/dsfatValidate";

    /** ObjectName for the singleton DataSourceValidationMBean — registered by immediate DS component. */
    private static final String DS_VALIDATION_MBEAN =
        "WebSphere:type=com.ibm.ws.jdbc.DataSourceValidationMBean";

    /** Wildcard pattern for the per-pool ConnectionManagerMBean once it initialises. */
    private static final String CM_MBEAN_PATTERN =
        "WebSphere:type=com.ibm.ws.jca.cm.ConnectionManagerMBean,*,jndiName=" + DATASOURCE_JNDI;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static MBeanServerConnection mbsc;
    private static JMXConnector jmxConnector;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();

        // Wait up to 30 s for the localConnector address file to appear
        String serverRoot = server.getServerRoot();
        File connectorFile = new File(serverRoot + "/workarea/" + CONNECTOR_ADDRESS_FILE);
        for (int i = 0; i < 30 && !connectorFile.exists(); i++) {
            Log.info(c, "setUp", "Waiting for JMX connector address file...");
            Thread.sleep(1000);
        }
        assertTrue("JMX connector address file did not appear: " + connectorFile,
                   connectorFile.exists());

        String connectorAddr;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(connectorFile), "UTF-8"))) {
            connectorAddr = br.readLine();
        }
        assertNotNull("JMX connector address was null", connectorAddr);
        Log.info(c, "setUp", "JMX connector address: " + connectorAddr);

        jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(connectorAddr));
        mbsc = jmxConnector.getMBeanServerConnection();

        // DataSourceValidationMBeanImpl is an immediate DS component that activates with
        // the jdbc feature bundle.  It should be registered very quickly after startup,
        // but wait up to 30 s to be safe.
        ObjectName dsMBeanName = new ObjectName(DS_VALIDATION_MBEAN);
        for (int i = 0; i < 30; i++) {
            if (mbsc.isRegistered(dsMBeanName))
                break;
            Log.info(c, "setUp", "Waiting for DataSourceValidationMBean to register...");
            Thread.sleep(1000);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (jmxConnector != null)
                jmxConnector.close();
        } finally {
            server.stopServer();
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that DataSourceValidationMBean is registered at server startup
     * (before any pool is initialised).
     */
    @Test
    public void testDataSourceValidationMBeanIsRegistered() throws Exception {
        ObjectName name = new ObjectName(DS_VALIDATION_MBEAN);
        assertTrue("DataSourceValidationMBean should be registered at startup",
                   mbsc.isRegistered(name));
    }

    /**
     * Invokes DataSourceValidationMBean.validateConnections() and verifies that
     * a successful Derby connection result is returned — without requiring pool init.
     */
    @Test
    public void testValidateConnectionsByJndi() throws Exception {
        ObjectName mbean = new ObjectName(DS_VALIDATION_MBEAN);

        // Invoke with (jndiName, user, password)
        Object result = mbsc.invoke(mbean, "validateConnections",
                                    new Object[] { DATASOURCE_JNDI, null, null },
                                    new String[] { String.class.getName(),
                                                   String.class.getName(),
                                                   String.class.getName() });

        assertNotNull("validateConnections returned null", result);
        String resultStr = result.toString();
        Log.info(c, "testValidateConnectionsByJndi", "Result: " + resultStr);

        assertTrue("Expected 'successful=true' in result but got: " + resultStr,
                   resultStr.contains("successful=true"));
        assertTrue("Expected 'databaseProductName=Apache Derby' in result but got: " + resultStr,
                   resultStr.contains("databaseProductName=Apache Derby"));
        assertTrue("Expected 'jdbcDriverName=' in result but got: " + resultStr,
                   resultStr.contains("jdbcDriverName="));
        assertTrue("Expected 'jdbcDriverVersion=' in result but got: " + resultStr,
                   resultStr.contains("jdbcDriverVersion="));
    }

    /**
     * Verifies that DataSourceValidationMBean.validateConnections() triggers pool
     * initialisation as a side-effect (createResource + getConnection), which causes
     * the per-pool ConnectionManagerMBean to be registered.  Then invokes
     * ConnectionManagerMBean.validateConnections() directly on the pool MBean.
     *
     * The DataSourceValidationMBean is the correct way to test a connection before
     * the pool exists.  Once the pool has been primed by that MBean (or by any other
     * first use of the datasource), the ConnectionManagerMBean becomes available.
     */
    @Test
    public void testValidateConnectionsOnPoolMBean() throws Exception {
        // Step 1: use DataSourceValidationMBean to prime the pool.
        // createResource(null) → getConnection() → triggers ConnectionManagerMBean registration.
        ObjectName dsMBean = new ObjectName(DS_VALIDATION_MBEAN);
        Object primeResult = mbsc.invoke(dsMBean, "validateConnections",
                    new Object[] { DATASOURCE_JNDI, null, null },
                    new String[] { String.class.getName(),
                                   String.class.getName(),
                                   String.class.getName() });
        Log.info(c, "testValidateConnectionsOnPoolMBean", "Prime result: " + primeResult);
        assertTrue("Pool prime via DataSourceValidationMBean should succeed, got: " + primeResult,
                   primeResult.toString().contains("successful=true"));

        // Step 2: find the per-pool ConnectionManagerMBean.
        // Pool creation (and MBean registration) happens synchronously inside validateConnections
        // above, so it should be available immediately — but allow a few seconds for the OSGi
        // service → JMX MBeanServer path to complete.
        Set<ObjectName> names = null;
        for (int i = 0; i < 10; i++) {
            names = mbsc.queryNames(new ObjectName(CM_MBEAN_PATTERN), null);
            if (names != null && !names.isEmpty())
                break;
            Log.info(c, "testValidateConnectionsOnPoolMBean",
                     "Waiting for ConnectionManagerMBean, attempt " + (i + 1));
            Thread.sleep(1000);
        }

        // Log all MBeans with ConnectionManagerMBean type for diagnostics if the query fails
        Set<ObjectName> allCm = mbsc.queryNames(
            new ObjectName("WebSphere:type=com.ibm.ws.jca.cm.ConnectionManagerMBean,*"), null);
        Log.info(c, "testValidateConnectionsOnPoolMBean",
                 "All ConnectionManagerMBeans registered: " + allCm);

        assertNotNull("queryNames returned null", names);
        assertTrue("Expected at least one ConnectionManagerMBean for " + DATASOURCE_JNDI
                   + " but found none. All ConnectionManagerMBeans: " + allCm,
                   !names.isEmpty());

        // Step 3: invoke validateConnections on the pool MBean directly.
        ObjectName mbeanName = names.iterator().next();
        Log.info(c, "testValidateConnectionsOnPoolMBean", "Found pool MBean: " + mbeanName);

        Object result = mbsc.invoke(mbeanName, "validateConnections",
                                    new Object[] { null, null },
                                    new String[] { String.class.getName(),
                                                   String.class.getName() });

        assertNotNull("validateConnections on pool MBean returned null", result);
        String resultStr = result.toString();
        Log.info(c, "testValidateConnectionsOnPoolMBean", "Pool MBean result: " + resultStr);

        assertTrue("Expected 'successful=true' in result but got: " + resultStr,
                   resultStr.contains("successful=true"));
        assertTrue("Expected 'databaseProductName=Apache Derby' in result but got: " + resultStr,
                   resultStr.contains("databaseProductName=Apache Derby"));
    }
}
