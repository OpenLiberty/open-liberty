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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
import componenttest.topology.impl.LibertyServer;

/**
 * General tests that involve updating configuration while the server is running.
 */
public class DynaCfgTest {

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
     * Utility method to run a test in a servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
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
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(dynamicConfigTestAppName);
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
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);
        cleanUpExprs = EMPTY_EXPR_LIST;
        Log.info(getClass(), "setUpPerTest", "server configuration restored");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKG0007W", // could not delete from workarea
                          "CWWKE0700W",
                          "CWWKE0701E.*NullPointerException", // Remove when FELIX-4682 is fixed and integrated
                          "CNTR4015W"); // EXPECTED : Warning for not having activation spec in server.xml for all beans on the application, this is OK
        if (originalServerConfig != null)
            server.updateServerConfiguration(originalServerConfig);
    }

    // Removed a separate test for removing an adminObject because it was duplicated in part of the following test

    // Test adding and removing an AdminObject
    @Test
    public void testAddingAdminObject_List() throws Exception {
        Log.entering(getClass(), "testAddingAdminObject_List");

        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        config.getAdminObjects().removeBy("jndiName", "eis/myAdminObject");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testAdminObject_NoList", dynamicConfigTestServlet);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testAdminObject_List", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testAddingAdminObject_List");
    }

    // Add, modify, then remove an activation spec
    @Test
    public void testAddModifyRemoveActivationSpec() throws Exception {
        Log.entering(getClass(), "testAddModifyRemoveActivationSpec");

        ServerConfiguration config = server.getServerConfiguration();

        // First, need a connection factory to trigger the MDB
        // add: <connectionFactory jndiName="eis/cf">
        //        <properties.dcra/>
        //      </connectionFactory>
        ConnectionFactory cf = new ConnectionFactory();
        cf.setJndiName("eis/cf");
        cf.getProperties_dcra().add(new JCAGeneratedProperties());
        config.getConnectionFactories().add(cf);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testActivationSpec_NoMessages", dynamicConfigTestServlet);

        // add: <activationSpec id="dynaCfgTestApp/fvtweb/DynaCfgMessageDrivenBean">
        //        <properties.dcra/>
        //      </activationSpec>
        ActivationSpec activationSpec = new ActivationSpec();
        activationSpec.setId("dynaCfgTestApp/fvtweb/DynaCfgMessageDrivenBean");
        JCAGeneratedProperties properties_dcra = new JCAGeneratedProperties();
        activationSpec.getProperties_dcra().add(properties_dcra);
        config.getActivationSpecs().add(activationSpec);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testActivationSpec_NoMessages", dynamicConfigTestServlet);

        // Enable the mdb-3.1 feature
        config.getFeatureManager().getFeatures().add("mdb-3.1");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        cleanUpExprs = APP_AND_RA_RECYCLE_EXPR_LIST;
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);

        runInServlet("testActivationSpec_MessageOn_0", dynamicConfigTestServlet);

        // modify: <properties.dcra messageFilterMax="100"/>
        properties_dcra.setMessageFilterMax("100");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec_MessageOn_0_100", dynamicConfigTestServlet);

        // modify: <properties.dcra messageFilterMax="50" messageFilterMin="5"/>
        properties_dcra.setMessageFilterMax("50");
        properties_dcra.setMessageFilterMin("5");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec_MessageOn_5_50", dynamicConfigTestServlet);

        // remove activationSpec
        config.getActivationSpecs().remove(activationSpec);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testActivationSpec_NoMessages", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testAddModifyRemoveActivationSpec");
    }

    // Add, modify, then remove a connection factory
    @Test
    public void testAddModifyRemoveConnectionFactory_CommonDataSource() throws Exception {
        Log.entering(getClass(), "testAddModifyRemoveConnectionFactory_CommonDataSource");

        runInServlet("testCommonDataSource_None", dynamicConfigTestServlet);

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

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testCommonDataSource_LoginTimeout_60", dynamicConfigTestServlet);

        // modify: <properties.dcra loginTimeout="80"/>
        properties_dcra.setLoginTimeout("80");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testCommonDataSource_LoginTimeout_80", dynamicConfigTestServlet);

        // modify: <properties.dcra/>
        properties_dcra.setLoginTimeout(null);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testCommonDataSource_LoginTimeout_100", dynamicConfigTestServlet);
        runInServlet("testDataSource_ContainerAuthData_None", dynamicConfigTestServlet);

        // add: <containerAuthData user="user1" password="{xor}Lyg7bg=="/>
        AuthData containerAuthData = new AuthData();
        containerAuthData.setUser("user1");
        containerAuthData.setPassword("{xor}Lyg7bg==");
        cf.getContainerAuthData().add(containerAuthData);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testDataSource_ContainerAuthData_User1", dynamicConfigTestServlet);

        // modify: <containerAuthData user="user2" password="{xor}Lyg7bQ=="/>
        containerAuthData.setUser("user2");
        containerAuthData.setPassword("{xor}Lyg7bQ==");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testDataSource_ContainerAuthData_User2", dynamicConfigTestServlet);

        // remove containerAuthData
        cf.getContainerAuthData().clear();

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testDataSource_ContainerAuthData_None", dynamicConfigTestServlet);

        // remove
        config.getConnectionFactories().removeBy("id", "cf");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, APP_RECYCLE_EXPR_LIST);

        runInServlet("testCommonDataSource_None", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testAddModifyRemoveConnectionFactory_CommonDataSource");
    }

    // Test using <application type="rar" .../> instead of <resourceAdapter .../>
    //@Test TODO re-enable once we solve the problem with the app not coming back up when resource adapter goes away
    public void testApplicationTypeRAR() throws Exception {
        Log.entering(getClass(), "testApplicationTypeRAR");

        runInServlet("testAdminObject_List", dynamicConfigTestServlet);

        ServerConfiguration config = server.getServerConfiguration();

        // Split these into 2 config updates because we cannot otherwise guarantee that the resourceAdapter remove will
        // be processed before the add of the application type=RAR.  If it occurs in the opposite order, it could fail due
        // to being a duplicate.

        // Replace
        //   <resourceAdapter id="dcra" location="${server.config.dir}/connectors/DynamicConfigRA.rar"/>
        ResourceAdapter resourceAdapter = config.getResourceAdapters().removeBy("id", dynamicConfigTestRarName);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null,
                                                 "CWWKZ0009I.*" + dynamicConfigTestAppName,
                                                 "J2CA7009I.*DynamicConfigRA");

        // with
        //   <application type="rar" location="${server.config.dir}/connectors/DynamicConfigRA.rar"/>
        Application application = new Application();
        application.setType("rar");
        application.setLocation(resourceAdapter.getLocation());
        config.getApplications().add(application);

        // and corresponding update of <properties.dcra.List> to <properties.DynamicConfigRA.List/>
        AdminObject adminObject = config.getAdminObjects().getById("myAdminObject");
        adminObject.getProperties_dcra_List().remove(0);
        adminObject.getProperties_DynamicConfigRA_List().add(new JCAGeneratedProperties());

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, "J2CA700[13]I.*DynamicConfigRA");

        runInServlet("testAdminObject_List", dynamicConfigTestServlet);

        cleanUpExprs = APP_RECYCLE_EXPR_LIST;

        Log.exiting(getClass(), "testApplicationTypeRAR");
    }

    // Test changing an AdminObject's JndiName
    @Test
    public void testChangingAdminObject_List() throws Exception {
        Log.entering(getClass(), "testChangingAdminObject_List");

        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        AdminObject adminObject = config.getAdminObjects().getBy("jndiName", "eis/myAdminObject");
        adminObject.setJndiName("eis/list2");
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testAdminObject_NoList", dynamicConfigTestServlet);
        runInServlet("testAdminObject_List2", dynamicConfigTestServlet);

        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
        Log.exiting(getClass(), "testChangingAdminObject_List");
    }

    // Switch the vendor properties of eis/myAdminObject from properties.dcra.List to properties.dcra.Date.
    // Then update a vendor property.
    @Test
    public void testChangingAdminObject_VendorProperties() throws Exception {
        Log.entering(getClass(), "testChangingAdminObject_VendorProperties");

        ServerConfiguration config = server.getServerConfiguration();
        AdminObject adminObject = config.getAdminObjects().getBy("jndiName", "eis/myAdminObject");

        // Switch the vendor properties from properties.dcra.List to properties.dcra.Date
        adminObject.getProperties_dcra_List().remove(0);
        JCAGeneratedProperties properties_dcra_Date = new JCAGeneratedProperties();
        properties_dcra_Date.setYear(Integer.toString(2013 - 1900));
        adminObject.getProperties_dcra_Date().add(properties_dcra_Date);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("testAdminObject_Date_2013_Jan_1", dynamicConfigTestServlet);

        // Update a vendor property
        properties_dcra_Date.setMonth(Integer.toString(Calendar.DECEMBER));
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        cleanUpExprs = APP_RECYCLE_EXPR_LIST;
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);

        runInServlet("testAdminObject_Date_2013_Dec_1", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testChangingAdminObject_VendorProperties");
    }

    // Part 1: Customize the properties.dcra.List adminObject properties to have no suffix.
    // Part 2: Customize the properties.dcra.List adminObject properties to have a "LinkedList" suffix.
    @Test
    public void testCustomizeAdminObject() throws Exception {
        Log.entering(getClass(), "testCustomizeAdminObject");

        runInServlet("testAdminObject_List", dynamicConfigTestServlet);

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

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        cleanUpExprs = APP_AND_RA_RECYCLE_EXPR_LIST;
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);

        runInServlet("testAdminObject_List", dynamicConfigTestServlet);

        // Part 2: Customize the properties.dcra.List adminObject properties to have a "LinkedList" suffix.

        customize.setSuffix("LinkedList");
        // this means that properties.dcra now gets used for Date,
        adminObject.getProperties_dcra().get(0).setYear(Integer.toString(2013 - 1900));
        // and properties.dcra.LinkedList is needed for a list
        AdminObject adminObjectLL = new AdminObject();
        adminObjectLL.setJndiName("eis/list2");
        adminObjectLL.getProperties_dcra_LinkedList().add(new JCAGeneratedProperties());
        config.getAdminObjects().add(adminObjectLL);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);

        runInServlet("testAdminObject_Date_2013_Jan_1", dynamicConfigTestServlet);
        runInServlet("testAdminObject_List2", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testCustomizeAdminObject");
    }

    @Test
    public void testRARemovalCF() throws Exception {
        Log.entering(getClass(), "testRARemovalCF");

        runInServlet("testRARUsable", dynamicConfigTestServlet);

        ServerConfiguration config = server.getServerConfiguration();
        config.getResourceAdapters().removeById(dynamicConfigTestRarName);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config); // Remove rar
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("dynaCfgTestApp"),
                                                 new String[] { "CWWKZ0009I.*dynaCfgTestApp",
                                                                "J2CA7009I.*dcra",
                                                                "CWWKZ0003I.*dynaCfgTestApp"
                                                 });
        cleanUpExprs = new String[] { "J2CA7001I.*dcra" };

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig); // Now add back rar
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanUpExprs);
        cleanUpExprs = EMPTY_EXPR_LIST;

        runInServlet("testRARUsable", dynamicConfigTestServlet);

        Log.exiting(getClass(), "testRARemovalCF");

    }
}
