/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.dynamicConfig;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ActivationSpec;
import com.ibm.websphere.simplicity.config.AdminObject;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ConnectionFactory;
import com.ibm.websphere.simplicity.config.Customize;
import com.ibm.websphere.simplicity.config.JCAGeneratedProperties;
import com.ibm.websphere.simplicity.config.ResourceAdapter;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General tests that involve updating configuration while the server is running.
 */
@RunWith(FATRunner.class)
public class DynaCfgTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat.dynamicConfig")
    public static LibertyServer server;

    private static final String dynamicConfigTestAppName = "dynaCfgTestApp";
    private static final String dynamicConfigTestServlet = "fvtweb";
    private static final String dynamicConfigTestRarName = "dcra";
    private static final Set<String> appNames = new TreeSet<String>(Arrays.asList(dynamicConfigTestAppName, dynamicConfigTestRarName));

    private static ServerConfiguration originalServerConfig;

    private static final String[] EMPTY_EXPR_LIST = new String[0];
    private static final String[] APP_RECYCLE_EXPR_LIST = new String[] {
                                                                         "CWWKZ0009I.*" + dynamicConfigTestAppName,
                                                                         "CWWKZ0003I.*" + dynamicConfigTestAppName
    };
    private static final String[] APP_AND_RA_RECYCLE_EXPR_LIST = new String[] {
                                                                                "CWWKZ0009I.*" + dynamicConfigTestAppName,
                                                                                "J2CA7009I.*dcra",
                                                                                "J2CA700[13]I.*dcra",
                                                                                "CWWKZ0003I.*" + dynamicConfigTestAppName
    };
    private static String[] cleanUpExprs = EMPTY_EXPR_LIST;

    /**
     * Utility method to run test on server given testName
     */
    private void runTest(String testName) throws Exception {
        runTest(server, dynamicConfigTestServlet, testName);
    }

    /**
     * Utility method to update config and wait for cleanUpExprs in logs
     */
    private void updateConfig(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        //Create ear
        WebArchive war = ShrinkHelper.buildDefaultApp(dynamicConfigTestServlet, "web", "web.mdb");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, dynamicConfigTestAppName + ".ear")
                        .addAsModule(war);
        ShrinkHelper.addDirectory(ear, "test-applications/dynaCfgTestApp/resources");
        ShrinkHelper.exportToServer(server, "apps", ear);

        //Create rar
        ShrinkHelper.defaultRar(server, dynamicConfigTestRarName, "com.ibm.test.dynamicconfigadapter");

        server.startServer();
        originalServerConfig = server.getServerConfiguration().clone();
    }

    /**
     * Before running each test, restore to the original configuration.
     *
     * @throws Exception
     */
    @Before
    public void setUpPerTest() throws Exception {
        //Reset cleanup expressions to an empty list
        cleanUpExprs = EMPTY_EXPR_LIST;
        //Update server to original config
        updateConfig(originalServerConfig);
        //Log restoration
        Log.info(getClass(), "setUpPerTest", "server configuration restored");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKG0007W", // could not delete from workarea
                              "CWWKE0700W",
                              "CWWKE0701E.*NullPointerException", // Remove when FELIX-4682 is fixed and integrated
                              "CNTR4015W"); // EXPECTED : Warning for not having activation spec in server.xml for all beans on the application, this is OK
            if (originalServerConfig != null)
                server.updateServerConfiguration(originalServerConfig);
        }
    }

    // Removed a separate test for removing an adminObject because it was duplicated in part of the following test
    // Test adding and removing an AdminObject
    @Test
    public void testAddingAdminObject_List() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getAdminObjects().removeBy("jndiName", "eis/myAdminObject");

        updateConfig(config);
        runTest("testAdminObject_NoList");

        updateConfig(originalServerConfig);
        runTest("testAdminObject_List");
    }

    // Add, modify, then remove an activation spec
    @Test
    public void testAddModifyRemoveActivationSpec() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        // First, need a connection factory to trigger the MDB
        // add: <connectionFactory jndiName="eis/cf">
        //        <properties.dcra/>
        //      </connectionFactory>
        ConnectionFactory cf = new ConnectionFactory();
        cf.setJndiName("eis/cf");
        cf.getProperties_dcra().add(new JCAGeneratedProperties());
        config.getConnectionFactories().add(cf);
        updateConfig(config);
        runTest("testActivationSpec_NoMessages");

        // add: <activationSpec id="dynaCfgTestApp/fvtweb/DynaCfgMessageDrivenBean">
        //        <properties.dcra/>
        //      </activationSpec>
        ActivationSpec activationSpec = new ActivationSpec();
        activationSpec.setId("dynaCfgTestApp/fvtweb/DynaCfgMessageDrivenBean");
        JCAGeneratedProperties properties_dcra = new JCAGeneratedProperties();
        activationSpec.getProperties_dcra().add(properties_dcra);
        config.getActivationSpecs().add(activationSpec);
        updateConfig(config);
        runTest("testActivationSpec_NoMessages");

        // Enable the mdb-3.2 feature
        config.getFeatureManager().getFeatures().add("mdb-3.2");
        cleanUpExprs = APP_AND_RA_RECYCLE_EXPR_LIST;
        updateConfig(config);
        runTest("testActivationSpec_MessageOn_0");

        // modify: <properties.dcra messageFilterMax="100"/>
        properties_dcra.setMessageFilterMax("100");
        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
        updateConfig(config);
        runTest("testActivationSpec_MessageOn_0_100");

        // modify: <properties.dcra messageFilterMax="50" messageFilterMin="5"/>
        properties_dcra.setMessageFilterMax("50");
        properties_dcra.setMessageFilterMin("5");
        updateConfig(config);
        runTest("testActivationSpec_MessageOn_5_50");

        // remove activationSpec
        config.getActivationSpecs().remove(activationSpec);
        updateConfig(config);
        runTest("testActivationSpec_NoMessages");
    }

    // Add, modify, then remove a connection factory
    @Test
    public void testAddModifyRemoveConnectionFactory_CommonDataSource() throws Exception {
        runTest("testCommonDataSource_None");

        ServerConfiguration config = server.getServerConfiguration();

        // add: <connectionFactory id="cf" jndiName="eis/${id}">
        //        <properties.dcra loginTimeout="60"/>
        //      </connectionFactory>
        JCAGeneratedProperties properties_dcra = new JCAGeneratedProperties();
        properties_dcra.setLoginTimeout("60");
        ConnectionFactory cf = new ConnectionFactory();
        cf.setId("cf");
        cf.setJndiName("eis/${id}");
        cf.getProperties_dcra().add(properties_dcra);
        config.getConnectionFactories().add(cf);
        updateConfig(config);
        runTest("testCommonDataSource_LoginTimeout_60");

        // modify: <properties.dcra loginTimeout="80"/>
        properties_dcra.setLoginTimeout("80");
        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
        updateConfig(config);
        runTest("testCommonDataSource_LoginTimeout_80");

        // modify: <properties.dcra/>
        properties_dcra.setLoginTimeout(null);
        updateConfig(config);
        runTest("testCommonDataSource_LoginTimeout_100");
        runTest("testDataSource_ContainerAuthData_None");

        // add: <containerAuthData user="user1" password="{xor}Lyg7bg=="/>
        AuthData containerAuthData = new AuthData();
        containerAuthData.setUser("user1");
        containerAuthData.setPassword("{xor}Lyg7bg==");
        cf.getContainerAuthData().add(containerAuthData);
        updateConfig(config);
        runTest("testDataSource_ContainerAuthData_User1");

        // modify: <containerAuthData user="user2" password="{xor}Lyg7bQ=="/>
        containerAuthData.setUser("user2");
        containerAuthData.setPassword("{xor}Lyg7bQ==");
        updateConfig(config);
        runTest("testDataSource_ContainerAuthData_User2");

        // remove containerAuthData
        cf.getContainerAuthData().clear();
        updateConfig(config);
        runTest("testDataSource_ContainerAuthData_None");

        // remove
        config.getConnectionFactories().removeBy("id", "cf");
        updateConfig(config);
        runTest("testCommonDataSource_None");
    }

    // Test using <application type="rar" .../> instead of <resourceAdapter .../>
    //@Test TODO re-enable once we solve the problem with the app not coming back up when resource adapter goes away
    public void testApplicationTypeRAR() throws Exception {
        runTest("testAdminObject_List");

        ServerConfiguration config = server.getServerConfiguration();

        // Split these into 2 config updates because we cannot otherwise guarantee that the resourceAdapter remove will
        // be processed before the add of the application type=RAR.  If it occurs in the opposite order, it could fail due
        // to being a duplicate.

        // Replace
        //   <resourceAdapter id="dcra" location="${server.config.dir}/connectors/dcra.rar"/>
        ResourceAdapter resourceAdapter = config.getResourceAdapters().removeBy("id", dynamicConfigTestRarName);
        cleanUpExprs = new String[] { "CWWKZ0009I.*" + dynamicConfigTestAppName, "J2CA7009I.*dcra" };
        updateConfig(config);

        // with
        //   <application type="rar" location="${server.config.dir}/connectors/dcra.rar"/>
        Application application = new Application();
        application.setType("rar");
        application.setLocation(resourceAdapter.getLocation());
        config.getApplications().add(application);

        // and corresponding update of <properties.dcra.List> to <properties.dcra.List/>
        AdminObject adminObject = config.getAdminObjects().getById("myAdminObject");
        adminObject.getProperties_dcra_List().remove(0);
        adminObject.getProperties_DynamicConfigRA_List().add(new JCAGeneratedProperties());

        cleanUpExprs = new String[] { "J2CA700[13]I.*dcra" };
        updateConfig(config);
        runTest("testAdminObject_List");

        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
    }

    // Test changing an AdminObject's JndiName
    @Test
    public void testChangingAdminObject_List() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        AdminObject adminObject = config.getAdminObjects().getBy("jndiName", "eis/myAdminObject");
        adminObject.setJndiName("eis/list2");
        updateConfig(config);

        runTest("testAdminObject_NoList");
        runTest("testAdminObject_List2");

        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
    }

    // Switch the vendor properties of eis/myAdminObject from properties.dcra.List to properties.dcra.Date.
    // Then update a vendor property.
    @Test
    public void testChangingAdminObject_VendorProperties() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        AdminObject adminObject = config.getAdminObjects().getBy("jndiName", "eis/myAdminObject");

        // Switch the vendor properties from properties.dcra.List to properties.dcra.Date
        adminObject.getProperties_dcra_List().remove(0);
        JCAGeneratedProperties properties_dcra_Date = new JCAGeneratedProperties();
        properties_dcra_Date.setYear(Integer.toString(2013 - 1900));
        adminObject.getProperties_dcra_Date().add(properties_dcra_Date);
        updateConfig(config);

        runTest("testAdminObject_Date_2013_Jan_1");

        // Update a vendor property
        properties_dcra_Date.setMonth(Integer.toString(Calendar.DECEMBER));
        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
        updateConfig(config);

        runTest("testAdminObject_Date_2013_Dec_1");
    }

    // Part 1: Customize the properties.dcra.List adminObject properties to have no suffix.
    // Part 2: Customize the properties.dcra.List adminObject properties to have a "LinkedList" suffix.
    @Test
    public void testCustomizeAdminObject() throws Exception {
        runTest("testAdminObject_List");

        ServerConfiguration config = server.getServerConfiguration();

        // Part 1: Customize the properties.dcra.List adminObject properties to have no suffix.

        // <resourceAdapter id="dcra" location=...
        //   <customize implementation="java.util.LinkedList" suffix=""/>
        ResourceAdapter resourceAdapter = config.getResourceAdapters().getBy("id", dynamicConfigTestRarName);
        Customize customize = new Customize();
        customize.setImplementation("java.util.LinkedList");
        customize.setSuffix("");
        resourceAdapter.getCustomizes().add(customize);

        // <adminObject id="myAdminObject" jndiName="eis/myAdminObject">
        //   <properties.dcra/>
        AdminObject adminObject = config.getAdminObjects().getBy("id", "myAdminObject");
        adminObject.getProperties_dcra_List().remove(0);
        adminObject.getProperties_dcra().add(new JCAGeneratedProperties());

        cleanUpExprs = APP_AND_RA_RECYCLE_EXPR_LIST;
        updateConfig(config);

        runTest("testAdminObject_List");

        // Part 2: Customize the properties.dcra.List adminObject properties to have a "LinkedList" suffix.

        customize.setSuffix("LinkedList");
        // this means that properties.dcra now gets used for Date,
        adminObject.getProperties_dcra().get(0).setYear(Integer.toString(2013 - 1900));
        // and properties.dcra.LinkedList is needed for a list
        AdminObject adminObjectLL = new AdminObject();
        adminObjectLL.setJndiName("eis/list2");
        adminObjectLL.getProperties_dcra_LinkedList().add(new JCAGeneratedProperties());
        config.getAdminObjects().add(adminObjectLL);

        updateConfig(config);

        runTest("testAdminObject_Date_2013_Jan_1");
        runTest("testAdminObject_List2");
    }

    @Test
    public void testRARemovalCF() throws Exception {
        runTest("testRARUsable");
        ServerConfiguration config = server.getServerConfiguration();
        //remove rar
        config.getResourceAdapters().removeById(dynamicConfigTestRarName);

        cleanUpExprs = new String[] { "CWWKZ0009I.*dynaCfgTestApp",
                                      "J2CA7009I.*dcra",
                                      "CWWKZ0003I.*dynaCfgTestApp" };
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config); // Remove rar
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("dynaCfgTestApp"), cleanUpExprs);
        cleanUpExprs = new String[] { "J2CA7001I.*dcra" };
        updateConfig(originalServerConfig);

        cleanUpExprs = EMPTY_EXPR_LIST;
        runTest("testRARUsable");

    }
}
