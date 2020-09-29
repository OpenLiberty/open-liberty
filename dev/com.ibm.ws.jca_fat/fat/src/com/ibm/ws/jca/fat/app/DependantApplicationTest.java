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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.jms.Topic;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ConnectionManager;
import com.ibm.websphere.simplicity.config.ContextService;
import com.ibm.websphere.simplicity.config.JCAGeneratedProperties;
import com.ibm.websphere.simplicity.config.JMSActivationSpec;
import com.ibm.websphere.simplicity.config.JMSConnectionFactory;
import com.ibm.websphere.simplicity.config.JMSQueue;
import com.ibm.websphere.simplicity.config.ResourceAdapter;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.context.ClassloaderContext;
import com.ibm.websphere.simplicity.config.context.JEEMetadataContext;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests of config update scenarios for JCA.
 */
@RunWith(FATRunner.class)
public class DependantApplicationTest {

    @Server("com.ibm.ws.jca.fat")
    public static LibertyServer server;

    private static final String fvtapp = "fvtapp";
    private static final String fvtweb = "fvtweb";

    private static ServerConfiguration originalServerConfig;

    @SuppressWarnings("unused")
    private static final String raPath = "${server.config.dir}/connectors/JCAFAT1.rar";
    private static final String raId = "FAT1";
    private static final Set<String> appNames = new TreeSet<String>(Arrays.asList(fvtapp, raId));

    private static final String[] EMPTY_EXPR_LIST = new String[0];
    private static final String[] FVTAPP_RECYCLE_EXPR_LIST = new String[] {
                                                                            "CWWKZ0009I.*" + fvtapp,
                                                                            "CWWKZ000[13]I.*" + fvtapp
    };
    private static final String[] FVTAPP_AND_RA_RECYCLE_EXPR_LIST = new String[] {
                                                                                   "CWWKZ0009I.*" + fvtapp,
                                                                                   "J2CA7009I.*" + raId,
                                                                                   "J2CA700[13]I.*" + raId,
                                                                                   "CWWKZ000[13]I.*" + fvtapp
    };
    private static String[] cleanUpExprs = EMPTY_EXPR_LIST;

    /**
     * Utility method to run a test on JCAFVTServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
        Log.info(getClass(), "runInServlet", "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } catch (IOException x) {
            throw x;
        } finally {
            con.disconnect();
        }
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

        originalServerConfig = server.getServerConfiguration().clone();
        server.addInstalledAppForValidation(fvtapp);
        server.startServer();
    }

    /**
     * After running each test, restore to the original configuration.
     *
     * @throws Exception
     */
    @After
    public void cleanUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);
        cleanUpExprs = EMPTY_EXPR_LIST;
        Log.info(getClass(), "cleanUpPerTest", "server configuration restored");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // English text should be avoided in regex below so that the tests can pass in any local
            server.stopServer("CNTR40(15|16)W:.*FVTMessageDrivenBean",
                              "J2CA8802E", // TODO : The message endpoint activation failed for resource adapter FAT1 due to exception: javax.resource.spi.InvalidPropertyException: destination
                              "J2CA8806E", // TODO : The administered object with id or JNDI name topic2 could not be found in the server configuration
                              "J2CA0045E:.*jms/cf1", // EXPECTED
                              "CWWKE0701E.*com.ibm.ws.jca", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                              "CWWKE0700W.*com.ibm.ws.jca", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                              "CWWKG0007W"); // TODO : The system could not delete C:\Users\IBM_ADMIN\Documents\workspace\build.image\wlp/usr/servers\com.ibm.ws.jca.fat\workarea\org.eclipse.osgi\9\data\configs\com.ibm.ws.jca.jmsQueue.properties_99!-1806458000
        } finally {
            if (originalServerConfig != null)
                server.updateServerConfiguration(originalServerConfig);
        }
    }

    /**
     * Increase the maximum pool size of a top level connection manager. Verify that the configuration update is honored
     * and check whether it causes application restart.
     */
    @AllowedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException") // test case intentionally runs out of connections to test pool size
    @Test
    public void testIncreaseMaxPoolSizeOfTopLevelConnectionManager() throws Exception {
        Log.entering(getClass(), "testIncreaseMaxPoolSizeOfTopLevelConnectionManager");

        // Remove the nested connection manager and use a top level connection manager instead.
        ServerConfiguration config = server.getServerConfiguration();
        ConnectionManager conMgr1 = new ConnectionManager();
        conMgr1.setId("conMgr1");
        conMgr1.setMaxPoolSize("1");
        conMgr1.setConnectionTimeout("0");
        config.addConnectionManager(conMgr1);
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        cf1.getConnectionManager().clear();
        cf1.setConnectionManagerRef(conMgr1.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, EMPTY_EXPR_LIST);

        runInServlet("testMaxPoolSize1&invokedBy=testIncreaseMaxPoolSizeOfTopLevelConnectionManager", fvtweb);
        runInServlet("setServletInstanceStillActive&invokedBy=testIncreaseMaxPoolSizeOfTopLevelConnectionManager", fvtweb);

        // Update the top level connectionManager
        conMgr1.setMaxPoolSize("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, EMPTY_EXPR_LIST);

        runInServlet("testMaxPoolSize2&invokedBy=testIncreaseMaxPoolSizeOfTopLevelConnectionManager", fvtweb);
        // TODO runInServlet("requireServletInstanceStillActive&invokedBy=testIncreaseMaxPoolSizeOfTopLevelConnectionManager", fvtweb);

        runInServlet("resetState", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testIncreaseMaxPoolSizeOfTopLevelConnectionManager");
    }

    /**
     * Increase the maximum pool size of a nested connection manager. Verify that the configuration update is honored
     * and check whether it causes application restart.
     */
    @AllowedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException") // test case intentionally runs out of connections to test pool size
    @Test
    public void testIncreaseMaxPoolSizeOfNestedConnectionManager() throws Exception {
        Log.entering(getClass(), "testIncreaseMaxPoolSizeOfNestedConnectionManager");

        runInServlet("testMaxPoolSize2&invokedBy=testIncreaseMaxPoolSizeOfNestedConnectionManager", fvtweb);
        runInServlet("setServletInstanceStillActive&invokedBy=testIncreaseMaxPoolSizeOfNestedConnectionManager", fvtweb);

        // Update a nested connectionManager
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        ConnectionManager conMgr = cf1.getConnectionManager().get(0);
        conMgr.setMaxPoolSize("3");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSizeGreaterThan2&invokedBy=testIncreaseMaxPoolSizeOfNestedConnectionManager", fvtweb);
        // TODO runInServlet("requireServletInstanceStillActive&invokedBy=testIncreaseMaxPoolSizeOfNestedConnectionManager", fvtweb);

        runInServlet("resetState", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testIncreaseMaxPoolSizeOfNestedConnectionManager");
    }

    @Test
    public void testUpdateActivationSpecConfigProperties() throws Exception {
        Log.entering(getClass(), "testUpdateActivationSpecConfigProperties");

        runInServlet("testActivationSpec", fvtweb);

        // Switch from authDataRef to userName="ACTV1USER" password="{xor}HhwLCW4PCBs="
        ServerConfiguration config = server.getServerConfiguration();
        JMSActivationSpec jmsActivationSpec = config.getJMSActivationSpecs().getById("fvtapp/fvtweb/FVTMessageDrivenBean");
        jmsActivationSpec.setAuthDataRef(null);
        JCAGeneratedProperties properties_FAT1 = jmsActivationSpec.getProperties_FAT1().get(0);
        properties_FAT1.setUserName("ACTV1USER");
        properties_FAT1.setPassword("{xor}HhwLCW4PCBs=");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec", fvtweb);

        // Update the destinationRef to point to a new destination (topic2)
        // <activationSpec id="fvtapp/fvtmdb.jar/FVTMessageDrivenBean">
        //   <properties.FAT1.jmsMessageListener destinationRef="queue2"...
        // </activationSpec>
        properties_FAT1.setDestinationRef("topic2");
        properties_FAT1.setDestinationType(Topic.class.getName());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpecsBothUsingTopic2", fvtweb);

        // Remove the destinationRef from FVTMessageDrivenBeanBindingOverride,
        // <jmsActivationSpec id="FVTMessageDrivenBeanBindingOverride">
        //   <properties.FAT1 destinationType="javax.jms.Topic"/>
        // </jmsActivationSpec>
        // which should still work because a destination-binding-name is specified in ibm-ejb-jar-bnd.xml
        JMSActivationSpec jmsActivationSpecBindingOverride = config.getJMSActivationSpecs().getById("FVTMessageDrivenBeanBindingOverride");
        properties_FAT1 = jmsActivationSpecBindingOverride.getProperties_FAT1().get(0);
        properties_FAT1.setDestinationRef(null);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpecBindings", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateActivationSpecConfigProperties");
    }

    @Test
    public void testUpdateConnectionFactoryConfigProperties() throws Exception {
        Log.entering(getClass(), "testUpdateConnectionFactoryConfigProperties");

        runInServlet("testConnectionFactoryClientIDDefault", fvtweb);

        // Update the clientID property
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        JCAGeneratedProperties properties_FAT1 = cf1.getProperties_FAT1().get(0);
        String defaultClientID = properties_FAT1.getClientID();
        properties_FAT1.setClientID("updatedClientID");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryClientIDUpdated", fvtweb);

        // Restore the original value
        properties_FAT1.setClientID(defaultClientID);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryClientIDDefault", fvtweb);

        // Switch the jndiName of the existing jmsConnectionFactory
        // and create another jmsConnectionFactory with the old JNDI name and updatedClientID
        String jms_cf1 = cf1.getJndiName();
        cf1.setJndiName("jms/cf1_old");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        JMSConnectionFactory cfNew = new JMSConnectionFactory();
        cfNew.setJndiName(jms_cf1);
        JCAGeneratedProperties propsNew = (JCAGeneratedProperties) properties_FAT1.clone();
        propsNew.setClientID("updatedClientID");
        cfNew.getProperties_FAT1().add(propsNew);
        config.getJMSConnectionFactories().add(cfNew);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, EMPTY_EXPR_LIST);

        runInServlet("testConnectionFactoryClientIDUpdated", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateConnectionFactoryConfigProperties");
    }

    @Test
    public void testUpdateConnectionFactoryUserName() throws Exception {
        Log.entering(getClass(), "testUpdateConnectionFactoryUserName");

        runInServlet("testConnectionFactoryUserDefault", fvtweb);

        // Add a containerAuthDataRef
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        cf1.setContainerAuthDataRef("activation1auth");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryUserUpdated1", fvtweb);

        // Switch to a different containerAuthDataRef
        AuthData newAuth = new AuthData();
        newAuth.setId("newAuth");
        newAuth.setUser("NEWUSER");
        newAuth.setPassword("NEWPASSWORD");
        config.getAuthDataElements().add(newAuth);
        cf1.setContainerAuthDataRef("newAuth");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryUserUpdated2", fvtweb);

        // Update the authData without touching the connection factory
        AuthData auth1 = config.getAuthDataElements().getById("activation1auth");
        newAuth.setUser(auth1.getUser());
        newAuth.setPassword(auth1.getPassword());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryUserUpdated1", fvtweb);

        // Remove containerAuthDataRef
        config.getAuthDataElements().remove(newAuth);
        cf1.setContainerAuthDataRef(null);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testConnectionFactoryUserDefault", fvtweb);

        cleanUpExprs = EMPTY_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateConnectionFactoryUserName");
    }

    /**
     * Make config updates to the connection manager, including removing it, adding a new one,
     * switching to a different one.
     *
     * @throws Exception if the test fails.
     */
    @AllowedFFDC("com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException") // test case intentionally runs out of connections to test pool size
    @Test
    public void testUpdateConnectionManager() throws Exception {
        Log.entering(getClass(), "testUpdateConnectionManager");

        runInServlet("testMaxPoolSize2", fvtweb);

        // Update a nested connectionManager
        ServerConfiguration config = server.getServerConfiguration();
        JMSConnectionFactory cf1 = config.getJMSConnectionFactories().getById("cf1");
        ConnectionManager conMgr = cf1.getConnectionManager().get(0);
        conMgr.setMaxPoolSize("1");
        conMgr.setConnectionTimeout("0");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSize1", fvtweb);

        // Remove the nested connectionManager
        cf1.getConnectionManager().remove(conMgr);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSizeGreaterThan2", fvtweb);

        // Add a reference to a new top-level connectionManager
        ConnectionManager conMgr1 = new ConnectionManager();
        conMgr1.setId("conMgr1");
        conMgr1.setMaxPoolSize("1");
        conMgr1.setConnectionTimeout("0");
        config.addConnectionManager(conMgr1);
        cf1.setConnectionManagerRef(conMgr1.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSize1", fvtweb);

        // Update a top-level connectionManager
        conMgr1.setMaxPoolSize("2");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSize2", fvtweb);

        // Switch from a top-level connectionManager to a nested connectionManager
        config.removeConnectionManagerById(conMgr1.getId());
        cf1.setConnectionManagerRef(null);
        conMgr = new ConnectionManager();
        conMgr.setMaxPoolSize("1");
        conMgr.setConnectionTimeout("0");
        cf1.getConnectionManager().add(conMgr);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testMaxPoolSize1", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateConnectionManager");
    }

    @Test
    public void testUpdateQueueConfigProperties() throws Exception {
        Log.entering(getClass(), "testUpdateQueueConfigProperties");

        runInServlet("testQueueNameDefault", fvtweb);

        // Update the queueName property
        ServerConfiguration config = server.getServerConfiguration();
        JMSQueue queue1 = config.getJMSQueues().getBy("jndiName", "jms/queue1");
        JCAGeneratedProperties properties_FAT1 = queue1.getProperties_FAT1().get(0);
        properties_FAT1.setQueueName("updatedQueueName");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testQueueNameUpdated", fvtweb);

        // Restore the original value
        properties_FAT1.setQueueName("queue1");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testQueueNameDefault", fvtweb);

        // Switch the jndiName of the existing jmsQueue
        // and create another jmsQueue with the old JNDI name and updatedQueueName
        String jms_queue1 = queue1.getJndiName();
        queue1.setJndiName("jms/queue1_old");
        JMSQueue queueNew = new JMSQueue();
        queueNew.setJndiName(jms_queue1);
        JCAGeneratedProperties propsNew = (JCAGeneratedProperties) properties_FAT1.clone();
        propsNew.setQueueName("updatedQueueName");
        queueNew.getProperties_FAT1().add(propsNew);
        config.getJMSQueues().add(queueNew);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_RECYCLE_EXPR_LIST);

        runInServlet("testQueueNameUpdated", fvtweb);

        cleanUpExprs = FVTAPP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateQueueConfigProperties");
    }

    @Test
    public void testUpdateContextService() throws Exception {
        Log.entering(getClass(), "testUpdateContextService");

        runInServlet("testActivationSpec", fvtweb);

        // Add a contextServiceRef
        ServerConfiguration config = server.getServerConfiguration();
        ContextService compContextSvc = new ContextService();
        compContextSvc.setId("compContextSvc");
        compContextSvc.getJEEMetadataContexts().add(new JEEMetadataContext());
        config.getContextServices().add(compContextSvc);
        ResourceAdapter resourceAdapter_FAT1 = config.getResourceAdapters().getById("FAT1");
        resourceAdapter_FAT1.setContextServiceRef(compContextSvc.getId());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_AND_RA_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec", fvtweb);

        // Switch to a nested contextService
        resourceAdapter_FAT1.setContextServiceRef(null);
        ContextService nestedContextSvc = new ContextService();
        nestedContextSvc.getClassloaderContexts().add(new ClassloaderContext());
        nestedContextSvc.getJEEMetadataContexts().add(new JEEMetadataContext());
        resourceAdapter_FAT1.getContextServices().add(nestedContextSvc);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_AND_RA_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec", fvtweb);

        // Remove the nested context service
        resourceAdapter_FAT1.getContextServices().clear();
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, FVTAPP_AND_RA_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec", fvtweb);

        cleanUpExprs = EMPTY_EXPR_LIST;
        Log.exiting(getClass(), "testUpdateContextService");
    }
}
