/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.app;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConnectionManager;
import com.ibm.websphere.simplicity.config.JMSConnectionFactory;
import com.ibm.websphere.simplicity.config.JavaPermission;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ConnectionManagerMBeanTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat")
    public static LibertyServer server;

    private static ServerConfiguration originalServerConfig;
    private static final String fvtweb = "fvtweb";
    private static final String fvtapp = "fvtapp";
    private static final Set<String> appNames = Collections.singleton(fvtapp);

    private void runTest() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    private void runTest(String testName) throws Exception {
        runTest(server, fvtweb, testName);
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // Build jars that will be in the RAR
        JavaArchive JCAFAT1_jar = ShrinkWrap.create(JavaArchive.class, "JCAFAT1.jar");
        JCAFAT1_jar.addPackage("fat.jca.resourceadapter.jar1");

        JavaArchive JCAFAT2_jar = ShrinkWrap.create(JavaArchive.class, "JCAFAT2.jar");
        JCAFAT2_jar.addPackage("fat.jca.resourceadapter.jar2");
        JCAFAT2_jar.add(JCAFAT1_jar, "/", ZipExporter.class);

        // Build the resource adapter
        ResourceAdapterArchive JCAFAT1_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "JCAFAT1.rar");
        JCAFAT1_rar.as(JavaArchive.class).addPackage("fat.jca.resourceadapter");
        JCAFAT1_rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/ra.xml"));
        JCAFAT1_rar.addAsLibrary(JCAFAT2_jar);
        ShrinkHelper.exportToServer(server, "connectors", JCAFAT1_rar);

        // Build the web module and application
        WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, fvtweb + ".war");
        fvtweb_war.addPackage("web");
        fvtweb_war.addPackage("web.mdb");
        fvtweb_war.addPackage("web.mdb.bindings");
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-ejb-jar-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"));

        EnterpriseArchive fvtapp_ear = ShrinkWrap.create(EnterpriseArchive.class, fvtapp + ".ear");
        fvtapp_ear.addAsModule(fvtweb_war);
        ShrinkHelper.addDirectory(fvtapp_ear, "lib/LibertyFATTestFiles/fvtapp");
        ShrinkHelper.exportToServer(server, "apps", fvtapp_ear);

        if (JakartaEE9Action.isActive()) {
            /*
             * Need to update the destination type of the topic to ensure it matches the Jakarta FQN.
             */
            ServerConfiguration clone = server.getServerConfiguration().clone();
            clone.getJMSActivationSpecs().getById("FVTMessageDrivenBeanBindingOverride").getProperties_FAT1().get(0).setDestinationType("jakarta.jms.Topic");

            for (JavaPermission perm : clone.getJavaPermissions()) {
                if (perm.getSignedBy() != null && perm.getSignedBy().startsWith("javax.resource.spi")) {
                    perm.setSignedBy(perm.getSignedBy().replace("javax.", "jakarta."));
                }
                if (perm.getName() != null && perm.getName().startsWith("javax.resource.spi")) {
                    perm.setName(perm.getName().replace("javax.", "jakarta."));
                }
            }

            server.updateServerConfiguration(clone);
        }

        originalServerConfig = server.getServerConfiguration().clone();
        server.addInstalledAppForValidation(fvtapp);
        server.startServer();
    }

    /**
     * Before running each test, restore to the original configuration.
     *
     * @throws Exception
     */
    @Before
    public void setUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        Log.info(getClass(), "setUpPerTest", "server configuration restored");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("J2CA0086W.*State:STATE_TRAN_WRAPPER_INUSE", // EXPECTED: One test intentionally leaves an open connection
                              "CWWKE0701E.*com.ibm.ws.jca.resourceAdapter.properties", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                              "CWWKE0700W.*com.ibm.ws.jca.resourceAdapter.properties", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                              "CWWKG0007W"); // could not delete from workarea
        } finally {
            if (originalServerConfig != null)
                server.updateServerConfiguration(originalServerConfig);
        }
    }

    @Test
    public void testPoolSizeDelegated() throws Exception {
        // Run a test that calls purge on connections that are in state ActiveInTransactionToBePurged
        // which will let the servlet teardown code cleanup the connections
        runTest("testPoolSizeDelegatedBEFORE");

        // Run another servlet method and verify that the connections were cleaned
        runTest("testPoolSizeDelegatedAFTER");
    }

    /**
     * Gets a connection (which should create a bean) and gets the pool contents.
     */
    @Test
    public void testMBeanCreation() throws Exception {
        runTest();
    }

    /**
     * Purges the connections from a connection pool, using the MBean "purgePoolContents".
     */
    @Test
    public void testMBeanPurge() throws Exception {
        runTest();
    }

    /**
     * Purge the connections from a connection pool, using the MBean "purgePoolContents".
     * The purge occurs while a connection is in-use in a transaction, we ensure that
     * the connection is purged after the transaction ends.
     */
    @Test
    public void testMBeanPurgeDuringTransaction() throws Exception {
        runTest();
    }

    /**
     * Purges the connections from a connection pool, using the MBean "purgePoolContents", with the "immediate" option.
     */
    @Test
    public void testMBeanPurgeImmediate() throws Exception {
        runTest();
    }

    /**
     * Purges the connections from a connection pool, using the MBean "purgePoolContents", with the "immediate" option.
     * The purge occurs while a connection is in-use in a transaction, we ensure that
     * the connection is purged after the transaction ends.
     */
    @Test
    public void testMBeanPurgeImmediateDuringTransaction() throws Exception {
        runTest();
    }

    /**
     * Two resource references point to the same connection pool, one shared, one unshared. This test makes a connection on each,
     * ensures that both show up, and purges both.
     */
    @Test
    public void testMBeanPurgeTwoResourceRef() throws Exception {
        runTest();
    }

    /**
     * Begin by verifying the bean exists.
     * Then, remove the ConnectionFactory from config, and verify the bean is gone.
     * Lastly, add the ConnectionFactory back, and make sure the bean has reappeared.
     */
    @Test
    public void testConfigChangeDestroyMBean() throws Throwable {
        //Verify bean exists:
        System.out.println("---> testConfigChangeDestroyMBean begins");
        runTest("testMBeanCreation");

        String method = "testConfigChangeAddConnectionManager";
        // Remove a connectionManager
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf = config.getJMSConnectionFactories().getById("cf1");
        try {
            assertTrue(config.getJMSConnectionFactories().remove(cf));

            try {
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(config);
                server.waitForConfigUpdateInLogUsingMark(appNames);
                server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");
            } catch (Throwable x) {
                System.out.println("Failure during " + method + " with the following config:");
                System.out.println(config);
                throw x;
            }

            runTest("testMBeanIsMissing");

            // Add the connectionManager back
            config.getJMSConnectionFactories().add(cf);

            try {

                server.setMarkToEndOfLog();
                server.updateServerConfiguration(config);
                server.waitForConfigUpdateInLogUsingMark(appNames);
                //Make sure we're back.
                runTest("testMBeanCreation");
            } catch (Throwable x) {
                System.out.println("Failure during " + method + " with the following config:");
                System.out.println(config);
                throw x;
            }
        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalServerConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
        System.out.println("<--- testConfigChangeDestroyMBean is successful");
    }

    /**
     * This test removes a connection factory, and puts a new connectionFactory in place with the same JNDI name as the prior one.
     * Then we test to make sure that the MBean server has picked up the new ConnectionFactory.
     *
     * This test currently doesn't work because of a bug in JNDI. See testJNDIBreaksWhenIDoThisStrangeConfigThing for details.
     */
//    @Test
    public void testRemovalAndReplacementOfConnectionFactory() throws Exception {
        //Get the showPoolContents of the Mbean
        StringBuilder poolContents = runTestWithResponse(server, fvtweb, "testGetConnectionFactoryPoolContents");
        //Change the poolSize on the element
        try {
            // Remove the connection factory.
            ServerConfiguration config = server.getServerConfiguration();
            JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
            config.getJMSConnectionFactories().remove(cf1);

            // Update the config, to make sure the server registers it gone.
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);
            server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");
            runTest("testMBeanIsMissing");
            //Give new connectionFactory the same JNDI Name
            config = server.getServerConfiguration();
            JMSConnectionFactory cf2 = new JMSConnectionFactory();

//            cf2.getConnectionManager().add(config.getConnectionManagerById("externalConnectionManager"));
            cf2.setConnectionManagerRef("externalConnectionManager");
            cf2.setJndiName(cf1.getJndiName());
            config.getJMSConnectionFactories().add(cf2);

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);

            //Get the showPoolContents again
            StringBuilder poolContentsAfterChange = runTestWithResponse(server, fvtweb, "testGetConnectionFactoryPoolContents");
            //Compare for changes: Different poolSize, same everything else
            System.out.println(poolContents + "\n\n\n" + poolContentsAfterChange);

            //Parse out changes
            Pattern getMaxPoolLine = Pattern.compile("maxPoolSize=\\d+");
            Matcher originalPCMatcher = getMaxPoolLine.matcher(poolContents);
            Matcher afterPCMatcher = getMaxPoolLine.matcher(poolContentsAfterChange);

            originalPCMatcher.find();
            afterPCMatcher.find();

            int originalPoolSize = Integer.parseInt(originalPCMatcher.group().substring(12));
            int afterPoolSize = Integer.parseInt(afterPCMatcher.group().substring(12));

            assertTrue("Original Pool Size was expected to be 2, but was found to be " + originalPoolSize, originalPoolSize == 2);
            assertTrue("Pool size after changes should be 20, but was found to be " + afterPoolSize, afterPoolSize == 20);

        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalServerConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }

    }

    /**
     * This test gets the contents of a pool, makes a config change to the pool size (which should be a non-destructive change)
     * and then checks that the poolContents dump reads the new data.
     */
    @Test
    public void testNonDestructiveChangeToPool() throws Exception {
        //Get the showPoolContents of the Mbean
        StringBuilder poolContents = runTestWithResponse(server, fvtweb, "testGetConnectionFactoryPoolContents");
        //Change the poolSize on the element
        try {
            // Change maxPoolSize to 1
            ServerConfiguration config = server.getServerConfiguration();
            JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
            ConnectionManager conMgr = cf1.getConnectionManager().get(0);
            conMgr.setMaxPoolSize("1");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames);
            server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");
            //Get the showPoolContents again
            StringBuilder poolContentsAfterChange = runTestWithResponse(server, fvtweb, "testGetConnectionFactoryPoolContents");
            //Compare for changes: Different poolSize, same everything else
            System.out.println(poolContents + "\n\n\n" + poolContentsAfterChange);

            //Parse out changes
            Pattern getMaxPoolLine = Pattern.compile("maxPoolSize=\\d+");
            Matcher originalPCMatcher = getMaxPoolLine.matcher(poolContents);
            Matcher afterPCMatcher = getMaxPoolLine.matcher(poolContentsAfterChange);

            originalPCMatcher.find();
            afterPCMatcher.find();

            int originalPoolSize = Integer.parseInt(originalPCMatcher.group().substring(12));
            int afterPoolSize = Integer.parseInt(afterPCMatcher.group().substring(12));

            assertTrue("Original Pool Size was expected to be 2, but was found to be " + originalPoolSize, originalPoolSize == 2);
            assertTrue("Pool size after changes should be 1, but was found to be " + afterPoolSize, afterPoolSize == 1);

        } finally {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalServerConfig);
            server.waitForConfigUpdateInLogUsingMark(appNames);
        }
    }

    /**
     * Test to make sure that a JMS Connection Factory can be removed from the configuration
     * and re-added while the server is running.
     */
    @Test
    public void testJNDILookupConnectionFactoryDynamic() throws Exception {
        //Look up a connection Factory
        runTest("testJNDILookupConnectionFactory");

        //Remove it
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        assertTrue(config.getJMSConnectionFactories().remove(cf1));

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);
        server.waitForStringInLogUsingMark(".*CWWKZ000[13]I.*");
        // Change the id of the factory so that it is slightly different than the
        // original xml.
        config = server.getServerConfiguration();
        cf1.setId("cf2");
        config.getJMSConnectionFactories().add(cf1);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runTest("testJNDILookupConnectionFactory");
    }

}
