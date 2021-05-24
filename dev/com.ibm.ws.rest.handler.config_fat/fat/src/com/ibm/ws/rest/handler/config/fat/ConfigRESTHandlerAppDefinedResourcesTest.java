/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.config.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ConfigRESTHandlerAppDefinedResourcesTest extends FATServletClient {
    private static final String APP_NAME = "AppDefResourcesApp";
    private static final String DerbyVersion = "10.11.1.1";

    @Server("com.ibm.ws.rest.handler.config.appdef.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "AppDefResourcesEJB.jar").addPackage("test.resthandler.config.appdef.ejb");
        WebArchive web = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addPackage("test.resthandler.config.appdef.web");
        ResourceAdapterArchive emb_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "EmbTestAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.config.adapter"));
        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(ejb)
                        .addAsModule(web)
                        .addAsModule(emb_rar);
        ShrinkHelper.exportToServer(server, "apps", app);
        server.addInstalledAppForValidation(APP_NAME);

        ResourceAdapterArchive tca_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "ConfigTestAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.config.adapter")
                                        .addClass("org.test.config.jmsadapter.JMSConnectionFactoryImpl")
                                        .addClass("org.test.config.jmsadapter.JMSDestinationImpl")
                                        .addClass("org.test.config.jmsadapter.JMSTopicConnectionFactoryImpl")
                                        .addClass("org.test.config.jmsadapter.JMSTopicConnectionImpl")
                                        .addClass("org.test.config.jmsadapter.ManagedJMSTopicConnectionFactoryImpl")
                                        .addClass("org.test.config.jmsadapter.NoOpSessionImpl"));
        ShrinkHelper.exportToServer(server, "connectors", tca_rar);

        FATSuite.setupServerSideAnnotations(server);

        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        messages.add("J2CA7001I: .* ConfigTestAdapter"); // J2CA7001I: Resource adapter ConfigTestAdapter installed in # seconds.

        server.waitForStringsInLogUsingMark(messages);

        // Invoke the servlet to ensure that the web module containing the app-defined data source is processed.
        FATServletClient.runTest(server, "AppDefResourcesApp/AppDefinedResourcesServlet", "doSomething");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an application-defined administered object.
     */
    @Test
    public void testAppDefinedAdminObject() throws Exception {
        JsonObject cspec = new HttpsRequest(server, "/ibm/api/config/adminObject/adminObject%5Bjava:global%2Fenv%2Feis%2FconSpec1%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + cspec;

        assertEquals(err, "adminObject", cspec.getString("configElementName"));
        assertEquals(err, "adminObject[java:global/env/eis/conSpec1]", cspec.getString("uid"));
        assertEquals(err, "adminObject[java:global/env/eis/conSpec1]", cspec.getString("id"));
        assertEquals(err, "java:global/env/eis/conSpec1", cspec.getString("jndiName"));

        assertNull(err, cspec.get("application"));
        assertNull(err, cspec.get("module"));
        assertNull(err, cspec.get("component"));

        JsonObject props;
        assertNotNull(err, props = cspec.getJsonObject("properties.ConfigTestAdapter.ConnectionSpec"));
        assertEquals(err, 4, props.size());
        assertEquals(err, 10203l, props.getJsonNumber("connectionTimeout").longValue());
        assertFalse(err, props.getBoolean("readOnly"));
        assertEquals(err, "aouser1", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for application-defined administered objects,
     * querying by component.
     */
    @Test
    public void testAppDefinedAdminObjectsQueryByComponent() throws Exception {
        JsonArray ispecs = new HttpsRequest(server, "/ibm/api/config/adminObject?component=AppDefinedResourcesBean")
                        .run(JsonArray.class);
        String err = "unexpected response: " + ispecs;
        assertEquals(err, 1, ispecs.size());

        JsonObject ispec;
        assertNotNull(err, ispec = ispecs.getJsonObject(0));
        assertEquals(err, "adminObject", ispec.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/adminObject[java:comp/env/eis/iSpec1]",
                     ispec.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/adminObject[java:comp/env/eis/iSpec1]",
                     ispec.getString("id"));
        assertEquals(err, "java:comp/env/eis/iSpec1", ispec.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", ispec.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", ispec.getString("module"));
        assertEquals(err, "AppDefinedResourcesBean", ispec.getString("component"));

        JsonObject props;
        assertNotNull(err, props = ispec.getJsonObject("properties.ConfigTestAdapter.InteractionSpec"));
        assertEquals(err, 1, props.size());
        assertEquals(err, "doSomethingUseful", props.getString("functionName"));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for app-defined connection factories with the
     * jndiName that is supplied as a query parameter.
     */
    @Test
    public void testAppDefinedConnectionFactoriesByJndiName() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/connectionFactory?jndiName=java:module%2Fenv%2Feis%2Fcf1")
                        .run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(2, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));

        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]", cf.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]", cf.getString("id"));
        assertEquals(err, "java:module/env/eis/cf1", cf.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", cf.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", cf.getString("module"));
        assertNull(err, cf.get("component"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 101, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 61, cm.getJsonNumber("reapTime").longValue());

        // support for containerAuthDataRef/recoveryAuthDataRef was never added to app-defined connection factories

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.ConfigTestAdapter.ConnectionFactory"));
        assertTrue(err, props.getBoolean("enableBetaContent"));
        assertEquals(err, "`", props.getString("escapeChar"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, 1515, props.getInt("portNumber"));
        assertEquals(err, "cfuser1", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));

        JsonArray api;
        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/connectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FconnectionFactory%5Bjava%3Amodule%2Fenv%2Feis%2Fcf1%5D",
                     api.getString(0));

        assertNotNull(err, cf = cfs.getJsonObject(1));

        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]", cf.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]", cf.getString("id"));
        assertEquals(err, "java:module/env/eis/cf1", cf.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", cf.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", cf.getString("module"));
        assertNull(err, cf.get("component"));

        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 50, cm.getInt("maxPoolSize"));
        assertEquals(err, "FailingConnectionOnly", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        assertNotNull(err, props = cf.getJsonObject("properties.ConfigTestAdapter.DataSource"));
        assertEquals(err, 1, props.size());
        assertEquals(err, "localhost", props.getString("hostName"));

        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/connectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesEJB.jar%5D%2FconnectionFactory%5Bjava%3Amodule%2Fenv%2Feis%2Fcf1%5D",
                     api.getString(0));

        // TODO should transactionSupport really show as an attribute of connectionFactory?
    }

    /**
     * Use the /ibm/api/config rest endpoint to obtain configuration for an app-defined connection factory from an embedded resource adapter
     */
    @Test
    public void testAppDefinedConnectionFactoryFromEmbeddedResourceAdapter() throws Exception {
        JsonArray array = new HttpsRequest(server, "/ibm/api/config/connectionFactory?component=AppDefinedResourcesBean")
                        .run(JsonArray.class);
        String err = "unexpected response: " + array;
        assertEquals(err, 1, array.size());

        JsonObject ds = array.getJsonObject(0);
        assertEquals(err, "connectionFactory", ds.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]",
                     ds.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]",
                     ds.getString("id"));
        assertEquals(err, "java:comp/env/eis/cf2", ds.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", ds.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", ds.getString("module"));
        assertEquals(err, "AppDefinedResourcesBean", ds.getString("component"));

        JsonObject cm;
        assertNotNull(err, cm = ds.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err,
                     "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]/connectionManager",
                     cm.getString("uid"));
        assertEquals(err,
                     "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]/connectionManager",
                     cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 2, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        JsonObject props;
        assertNotNull(err, props = ds.getJsonObject("properties.AppDefResourcesApp.EmbTestAdapter.DataSource"));
        assertEquals(err, 4, props.size());
        assertEquals(err, "^", props.getString("escapeChar"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, "******", props.getString("password"));
        assertEquals(err, "euser2", props.getString("userName"));

        JsonArray api;
        assertNotNull(err, api = ds.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added for connectionFactory
        assertEquals(err,
                     "/ibm/api/validation/connectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesEJB.jar%5D%2Fcomponent%5BAppDefinedResourcesBean%5D%2FconnectionFactory%5Bjava%3Acomp%2Fenv%2Feis%2Fcf2%5D",
                     api.getString(0));
    }

    /**
     * Use the /ibm/api/config rest endpoint to obtain configuration for the connection manager of an app-defined data source.
     */
    @AllowedFFDC("java.lang.IllegalArgumentException") // java:app/env/ds1's connectionManager has duration value that is not valid
    @Test
    public void testAppDefinedConnectionManager() throws Exception {
        JsonObject cm = new HttpsRequest(server, "/ibm/api/config/connectionManager/application%5BAppDefResourcesApp%5D%2FdataSource%5Bjava:app%2Fenv%2Fjdbc%2Fds1%5D%2FconnectionManager")
                        .run(JsonObject.class);
        String err = "unexpected response: " + cm;

        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/dataSource[java:app/env/jdbc/ds1]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/dataSource[java:app/env/jdbc/ds1]/connectionManager", cm.getString("id"));
        assertNull(err, cm.get("jndiName"));

        assertEquals(err, "1:05:30", cm.getString("agedTimeout")); // configured duration value is invalid and cannot be parsed to a number
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 50, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());
    }

    /**
     * Use the /ibm/api/config rest endpoint to obtain configuration for an app-defined data source.
     */
    @Test
    public void testAppDefinedDataSource() throws Exception {
        JsonObject ds = new HttpsRequest(server, "/ibm/api/config/dataSource/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FdataSource%5Bjava:module%2Fenv%2Fjdbc%2Fds2%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + ds;

        assertEquals(err, "dataSource", ds.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]", ds.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]", ds.getString("id"));
        assertEquals(err, "java:module/env/jdbc/ds2", ds.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", ds.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", ds.getString("module"));
        assertNull(err, ds.get("component"));

        assertTrue(err, ds.getBoolean("beginTranForResultSetScrollingAPIs"));
        assertTrue(err, ds.getBoolean("beginTranForVendorAPIs"));
        assertEquals(err, "MatchOriginalRequest", ds.getString("connectionSharing"));

        JsonObject cm;
        assertNotNull(err, cm = ds.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 0, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 2, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 2200, cm.getJsonNumber("reapTime").longValue());

        JsonObject authData;
        assertNotNull(err, authData = ds.getJsonObject("containerAuthDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "derbyAuth1", authData.getString("uid"));
        assertEquals(err, "derbyAuth1", authData.getString("id"));
        assertEquals(err, "dbuser1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertFalse(err, ds.getBoolean("enableConnectionCasting"));
        assertEquals(err, Connection.TRANSACTION_READ_COMMITTED, ds.getInt("isolationLevel"));

        JsonObject driver;
        assertNotNull(err, driver = ds.getJsonObject("jdbcDriverRef"));
        assertEquals(err, "jdbcDriver", driver.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]/jdbcDriver", driver.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]/jdbcDriver", driver.getString("id"));
        assertEquals(err, "org.apache.derby.jdbc.EmbeddedXADataSource", driver.getString("javax.sql.XADataSource"));

        JsonObject library;
        assertNotNull(err, library = driver.getJsonObject("libraryRef"));
        assertEquals(err, "library", library.getString("configElementName"));
        assertEquals(err, "Derby", library.getString("uid"));
        assertEquals(err, "Derby", library.getString("id"));
        assertEquals(err, "spec,ibm-api,api,stable", library.getString("apiTypeVisibility"));

        JsonArray files;
        JsonObject file;
        assertNotNull(err, files = library.getJsonArray("fileRef"));
        assertNotNull(err, file = files.getJsonObject(0));
        assertEquals(err, "file", file.getString("configElementName"));
        assertEquals(err, "library[Derby]/file[default-0]", file.getString("uid"));
        assertNull(err, file.get("id"));
        assertTrue(err, file.getString("name").endsWith("derby.jar"));

        JsonArray onConnect;
        assertNotNull(err, onConnect = ds.getJsonArray("onConnect"));
        assertEquals(err, 1, onConnect.size());
        assertEquals(err, "DECLARE GLOBAL TEMPORARY TABLE TEMP2 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED", onConnect.getString(0));

        JsonObject props;
        assertNotNull(err, props = ds.getJsonObject("properties"));
        assertEquals(err, 3, props.size());
        assertEquals(err, "create", props.getString("createDatabase"));
        String databaseName;
        assertNotNull(err, databaseName = props.getString("databaseName"));
        assertTrue(err, databaseName.endsWith("configRHTestDB"));
        assertTrue(err, databaseName.contains("resources")); // must expand ${shared.resource.dir}
        assertEquals(err, 220, props.getInt("loginTimeout"));

        assertEquals(err, 82, ds.getInt("queryTimeout"));

        assertNotNull(err, authData = ds.getJsonObject("recoveryAuthDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "derbyAuth2", authData.getString("uid"));
        assertEquals(err, "derbyAuth2", authData.getString("id"));
        assertEquals(err, "dbuser2", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertEquals(err, 22, ds.getInt("statementCacheSize")); // = maxStatements / maxPoolSize
        assertTrue(err, ds.getBoolean("syncQueryTimeoutWithTransactionTimeout"));
        assertTrue(err, ds.getBoolean("transactional"));
        assertEquals(err, "javax.sql.XADataSource", ds.getString("type"));

        JsonArray api;
        assertNotNull(err, api = ds.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added for data source
        assertEquals(err,
                     "/ibm/api/validation/dataSource/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FdataSource%5Bjava%3Amodule%2Fenv%2Fjdbc%2Fds2%5D",
                     api.getString(0));
    }

    /**
     * Use the /ibm/api/config rest endpoint to obtain configuration for an app-defined data source that
     * is defined in the java:global namespace. Use the API for validation that is provided in the config REST endpoint
     * output to test a connection.
     */
    @Test
    public void testAppDefinedDataSourceInJavaGlobalAndTestConnection() throws Exception {
        JsonObject ds = new HttpsRequest(server, "/ibm/api/config/dataSource/dataSource%5Bjava:global%2Fenv%2Fjdbc%2Fds4%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + ds;

        assertEquals(err, "dataSource", ds.getString("configElementName"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", ds.getString("uid"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", ds.getString("id"));
        assertEquals(err, "java:global/env/jdbc/ds4", ds.getString("jndiName"));

        assertNull(err, ds.get("application"));
        assertNull(err, ds.get("module"));
        assertNull(err, ds.get("component"));

        assertTrue(err, ds.getBoolean("beginTranForResultSetScrollingAPIs"));
        assertTrue(err, ds.getBoolean("beginTranForVendorAPIs"));
        assertEquals(err, "MatchOriginalRequest", ds.getString("connectionSharing"));

        JsonObject cm;
        assertNotNull(err, cm = ds.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]/connectionManager", cm.getString("uid"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 50, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        assertFalse(err, ds.getBoolean("enableConnectionCasting"));

        JsonObject driver;
        assertNotNull(err, driver = ds.getJsonObject("jdbcDriverRef"));
        assertEquals(err, "jdbcDriver", driver.getString("configElementName"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]/jdbcDriver", driver.getString("uid"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]/jdbcDriver", driver.getString("id"));
        assertTrue(err, driver.getString("javax.sql.XADataSource").startsWith("org.apache.derby.jdbc."));

        JsonObject library;
        assertNotNull(err, library = driver.getJsonObject("libraryRef"));
        assertEquals(err, "library", library.getString("configElementName"));
        assertEquals(err, "Derby", library.getString("uid"));
        assertEquals(err, "Derby", library.getString("id"));
        assertEquals(err, "spec,ibm-api,api,stable", library.getString("apiTypeVisibility"));

        JsonArray files;
        JsonObject file;
        assertNotNull(err, files = library.getJsonArray("fileRef"));
        assertNotNull(err, file = files.getJsonObject(0));
        assertEquals(err, "file", file.getString("configElementName"));
        assertEquals(err, "library[Derby]/file[default-0]", file.getString("uid"));
        assertNull(err, file.get("id"));
        assertTrue(err, file.getString("name").endsWith("derby.jar"));

        JsonObject props;
        assertNotNull(err, props = ds.getJsonObject("properties"));
        assertEquals(err, 4, props.size());
        assertEquals(err, "create", props.getString("createDatabase"));
        assertEquals(err, "memory:fourthdb", props.getString("databaseName"));
        assertEquals(err, "dbuser4", props.getString("user"));
        assertEquals(err, "******", props.getString("password"));

        assertEquals(err, 10, ds.getInt("statementCacheSize"));
        assertFalse(err, ds.getBoolean("syncQueryTimeoutWithTransactionTimeout"));
        assertTrue(err, ds.getBoolean("transactional"));
        assertEquals(err, "javax.sql.XADataSource", ds.getString("type"));

        JsonArray api;
        assertNotNull(err, api = ds.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added for data source
        assertEquals(err,
                     "/ibm/api/validation/dataSource/dataSource%5Bjava%3Aglobal%2Fenv%2Fjdbc%2Fds4%5D",
                     api.getString(0));

        // Use validation API
        JsonObject json = new HttpsRequest(server, api.getString(0)).run(JsonObject.class);
        err = "unexpected response: " + json;

        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", json.getString("uid"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", json.getString("id"));
        assertEquals(err, "java:global/env/jdbc/ds4", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertNotNull(err, json.getString("databaseProductVersion"));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertNotNull(err, json.getString("jdbcDriverVersion"));
        assertEquals(err, "DBUSER4", json.getString("schema"));
        assertEquals(err, "dbuser4", json.getString("user"));
    }

    /**
     * Verify that application-defined data sources are included in the output of the rest endpoint that
     * returns the configuration of all data sources.
     */
    @AllowedFFDC("java.lang.IllegalArgumentException") // java:app/env/ds1's connectionManager has duration value that is not valid
    @Test
    public void testAppDefinedDataSourcesAreIncluded() throws Exception {
        JsonArray dataSources = new HttpsRequest(server, "/ibm/api/config/dataSource").run(JsonArray.class);
        String err = "unexpected response: " + dataSources;
        assertEquals(err, 6, dataSources.size());
    }

    /**
     * Verify that /ibm/api/config/ REST endpoint copes with two different app-defined data sources that
     * have the same JNDI name, but are valid because they are in different scopes.
     */
    @Test
    public void testAppDefinedDataSourcesWithSameJndiName() throws Exception {
        JsonArray dataSources = new HttpsRequest(server, "/ibm/api/config/dataSource?jndiName=java:comp%2Fenv%2Fjdbc%2Fds3").run(JsonArray.class);
        String err = "unexpected response: " + dataSources;
        assertEquals(err, 2, dataSources.size());

        // config elements are ordered by config.displayId
        JsonObject web_ds, ejb_ds;
        assertNotNull(err, web_ds = dataSources.getJsonObject(0));
        assertNotNull(err, ejb_ds = dataSources.getJsonObject(1));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]", web_ds.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]", web_ds.getString("id"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]",
                     ejb_ds.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]",
                     ejb_ds.getString("id"));

        assertEquals(err, "dataSource", web_ds.getString("configElementName"));
        assertEquals(err, "dataSource", ejb_ds.getString("configElementName"));

        assertEquals(err, "java:comp/env/jdbc/ds3", web_ds.getString("jndiName"));
        assertEquals(err, "java:comp/env/jdbc/ds3", ejb_ds.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", web_ds.getString("application"));
        assertEquals(err, "AppDefResourcesApp", ejb_ds.getString("application"));

        assertEquals(err, "AppDefResourcesApp.war", web_ds.getString("module"));
        assertEquals(err, "AppDefResourcesEJB.jar", ejb_ds.getString("module"));

        assertNull(err, web_ds.get("component")); // per spec, app-defined resources in web container are scoped to the module, even if in java:comp
        assertEquals(err, "AppDefinedResourcesBean", ejb_ds.getString("component"));

        assertTrue(err, web_ds.getBoolean("beginTranForResultSetScrollingAPIs"));
        assertTrue(err, ejb_ds.getBoolean("beginTranForResultSetScrollingAPIs"));

        assertTrue(err, web_ds.getBoolean("beginTranForVendorAPIs"));
        assertTrue(err, ejb_ds.getBoolean("beginTranForVendorAPIs"));

        assertEquals(err, "MatchOriginalRequest", web_ds.getString("connectionSharing"));
        assertEquals(err, "MatchOriginalRequest", ejb_ds.getString("connectionSharing"));

        assertNotNull(err, web_ds.getJsonObject("connectionManagerRef"));
        assertNotNull(err, ejb_ds.getJsonObject("connectionManagerRef"));

        assertFalse(err, web_ds.getBoolean("enableConnectionCasting"));
        assertFalse(err, ejb_ds.getBoolean("enableConnectionCasting"));

        JsonObject web_ds_driver, ejb_ds_driver;
        assertNotNull(err, web_ds_driver = web_ds.getJsonObject("jdbcDriverRef"));
        assertNotNull(err, ejb_ds_driver = ejb_ds.getJsonObject("jdbcDriverRef"));

        assertEquals(err, "jdbcDriver", web_ds_driver.getString("configElementName"));
        assertEquals(err, "jdbcDriver", ejb_ds_driver.getString("configElementName"));

        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver", web_ds_driver.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver", web_ds_driver.getString("id"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver",
                     ejb_ds_driver.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver",
                     ejb_ds_driver.getString("id"));

        assertNull(err, web_ds_driver.get("javax.sql.ConnectionPoolDataSource"));
        assertEquals(err, "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource", ejb_ds_driver.getString("javax.sql.ConnectionPoolDataSource"));

        assertEquals(err, "org.apache.derby.jdbc.EmbeddedDataSource", web_ds_driver.getString("javax.sql.DataSource"));
        assertNull(err, ejb_ds_driver.get("javax.sql.DataSource"));

        assertNotNull(err, web_ds_driver.getJsonObject("libraryRef"));
        assertNotNull(err, ejb_ds_driver.getJsonObject("libraryRef"));

        JsonObject web_ds_props, ejb_ds_props;
        assertNotNull(err, web_ds_props = web_ds.getJsonObject("properties"));
        assertNotNull(err, ejb_ds_props = ejb_ds.getJsonObject("properties"));

        assertEquals(err, 1, web_ds_props.size());
        assertEquals(err, 2, ejb_ds_props.size());

        assertNull(err, web_ds_props.get("createDatabase"));
        assertEquals(err, "create", ejb_ds_props.getString("createDatabase"));

        assertEquals(err, "memory:thirddb;create=true", web_ds_props.getString("databaseName"));
        assertEquals(err, "memory:ejbdb", ejb_ds_props.getString("databaseName"));

        assertEquals(err, 10, web_ds.getInt("statementCacheSize"));
        assertEquals(err, 10, ejb_ds.getInt("statementCacheSize"));

        assertFalse(err, web_ds.getBoolean("syncQueryTimeoutWithTransactionTimeout"));
        assertFalse(err, ejb_ds.getBoolean("syncQueryTimeoutWithTransactionTimeout"));

        assertTrue(err, web_ds.getBoolean("transactional"));
        assertTrue(err, ejb_ds.getBoolean("transactional"));

        assertEquals(err, "javax.sql.DataSource", web_ds.getString("type"));
        assertEquals(err, "javax.sql.ConnectionPoolDataSource", ejb_ds.getString("type"));

        JsonArray web_ds_api, ejb_ds_api;
        assertNotNull(err, web_ds_api = web_ds.getJsonArray("api"));
        assertNotNull(err, ejb_ds_api = ejb_ds.getJsonArray("api"));

        assertEquals(err, 1, ejb_ds_api.size()); // increase if more REST API is added for data source
        assertEquals(err, 1, web_ds_api.size()); // increase if more REST API is added for data source

        assertEquals(err,
                     "/ibm/api/validation/dataSource/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FdataSource%5Bjava%3Acomp%2Fenv%2Fjdbc%2Fds3%5D",
                     web_ds_api.getString(0));
        assertEquals(err,
                     "/ibm/api/validation/dataSource/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesEJB.jar%5D%2Fcomponent%5BAppDefinedResourcesBean%5D%2FdataSource%5Bjava%3Acomp%2Fenv%2Fjdbc%2Fds3%5D",
                     ejb_ds_api.getString(0));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an application-defined JMS destination.
     */
    @Test
    public void testAppDefinedDestination() throws Exception {
        JsonObject q = new HttpsRequest(server, "/ibm/api/config/jmsDestination/jmsDestination%5Bjava:global%2Fenv%2Fjms%2Fdest1%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + q;

        assertEquals(err, "jmsDestination", q.getString("configElementName"));
        assertEquals(err, "jmsDestination[java:global/env/jms/dest1]", q.getString("uid"));
        assertEquals(err, "jmsDestination[java:global/env/jms/dest1]", q.getString("id"));
        assertEquals(err, "java:global/env/jms/dest1", q.getString("jndiName"));

        assertNull(err, q.get("application"));
        assertNull(err, q.get("module"));
        assertNull(err, q.get("component"));

        JsonObject props;
        assertNotNull(err, props = q.getJsonObject("properties.ConfigTestAdapter"));
        assertEquals(err, 1, props.size());
        assertEquals(err, "3605 Hwy 52N, Rochester, MN 55901", props.getString("destinationName"));
    }

    /**
     * Use the /ibm/api/config rest endpoint to obtain configuration for the jdbcDriver of an app-defined data source.
     */
    @Test
    public void testAppDefinedJDBCDriver() throws Exception {
        JsonObject driver = new HttpsRequest(server, "/ibm/api/config/jdbcDriver/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FdataSource%5Bjava:comp%2Fenv%2Fjdbc%2Fds3%5D%2FjdbcDriver")
                        .run(JsonObject.class);
        String err = "unexpected response: " + driver;

        assertEquals(err, "jdbcDriver", driver.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver",
                     driver.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]/jdbcDriver",
                     driver.getString("id"));
        assertNull(err, driver.get("jndiName"));
        assertEquals(err, "org.apache.derby.jdbc.EmbeddedDataSource", driver.getString("javax.sql.DataSource"));

        JsonObject library;
        assertNotNull(err, library = driver.getJsonObject("libraryRef"));
        assertEquals(err, "library", library.getString("configElementName"));
        assertEquals(err, "Derby", library.getString("uid"));
        assertEquals(err, "Derby", library.getString("id"));
        assertEquals(err, "spec,ibm-api,api,stable", library.getString("apiTypeVisibility"));

        JsonArray files;
        JsonObject file;
        assertNotNull(err, files = library.getJsonArray("fileRef"));
        assertNotNull(err, file = files.getJsonObject(0));
        assertEquals(err, "file", file.getString("configElementName"));
        assertEquals(err, "library[Derby]/file[default-0]", file.getString("uid"));
        assertNull(err, file.get("id"));
        assertTrue(err, file.getString("name").endsWith("derby.jar"));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an app-defined JMS connection factory.
     */
    @Test
    public void testAppDefinedJMSConnectionFactory() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/jmsConnectionFactory?application=AppDefResourcesApp&module=AppDefResourcesApp.war")
                        .run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(1, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));

        assertEquals(err, "jmsConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]", cf.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]", cf.getString("id"));
        assertEquals(err, "java:comp/env/jms/cf", cf.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", cf.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", cf.getString("module"));
        assertNull(err, cf.get("component"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 6, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        // support for containerAuthDataRef/recoveryAuthDataRef was never added to app-defined connection factories

        assertNull(err, cf.get("creates.objectClass"));
        assertNull(err, cf.get("jndiName.unique"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.wasJms"));
        assertEquals(err, "cfBus", props.getString("busName"));
        // TODO JMSConnectionFactoryResourceBuilder doesn't consider clientId (from annotation) and clientID (defined by resource adapter) to have the same meaning
        // assertEquals(err, "JMSClientID6", props.getString("clientID"));
        assertEquals(err, "defaultME", props.getString("durableSubscriptionHome"));
        assertEquals(err, "ExpressNonPersistent", props.getString("nonPersistentMapping"));
        assertEquals(err, "ReliablePersistent", props.getString("persistentMapping"));
        assertEquals(err, "AlwaysOff", props.getString("readAhead"));
        assertEquals(err, "NeverShared", props.getString("shareDurableSubscription"));
        assertEquals(err, "cfq", props.getString("temporaryQueueNamePrefix"));
        assertEquals(err, "temp", props.getString("temporaryTopicNamePrefix"));
        assertEquals(err, "jmsuser", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));

        JsonArray api;
        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/jmsConnectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FjmsConnectionFactory%5Bjava%3Acomp%2Fenv%2Fjms%2Fcf%5D",
                     api.getString(0));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an app-defined JMS QueueConnectionFactory.
     */
    @Test
    public void testAppDefinedJMSQueueConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsQueueConnectionFactory/application[AppDefResourcesApp]%2Fmodule[AppDefResourcesApp.war]%2FjmsQueueConnectionFactory[java:module%2Fenv%2Fjms%2Fqcf]")
                        .run(JsonObject.class);
        String err = "unexpected response: " + cf;

        assertEquals(err, "jmsQueueConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]", cf.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]", cf.getString("id"));
        assertEquals(err, "java:module/env/jms/qcf", cf.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", cf.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", cf.getString("module"));
        assertNull(err, cf.get("component"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]/connectionManager",
                     cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]/connectionManager",
                     cm.getString("id"));
        assertEquals(err, 25790, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 70, cm.getJsonNumber("connectionTimeout").longValue());
        assertFalse(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 450, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 7, cm.getInt("maxPoolSize"));
        assertEquals(err, 3, cm.getInt("minPoolSize"));
        assertEquals(err, "ValidateAllConnections", cm.getString("purgePolicy"));
        assertEquals(err, 72, cm.getJsonNumber("reapTime").longValue());

        // support for containerAuthDataRef/recoveryAuthDataRef was never added to app-defined connection factories

        assertNull(err, cf.get("creates.objectClass"));
        assertNull(err, cf.get("jndiName.unique"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.wasJms"));
        assertEquals(err, "qcfBus", props.getString("busName"));
        assertEquals(err, "ExpressNonPersistent", props.getString("nonPersistentMapping"));
        assertEquals(err, "ReliablePersistent", props.getString("persistentMapping"));
        assertEquals(err, "Default", props.getString("readAhead"));
        assertEquals(err, "tempq", props.getString("temporaryQueueNamePrefix"));
        assertNull(err, props.get("temporaryTopicNamePrefix"));
        assertNull(err, props.get("userName"));
        assertNull(err, props.get("password"));

        JsonArray api;
        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/jmsQueueConnectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FjmsQueueConnectionFactory%5Bjava%3Amodule%2Fenv%2Fjms%2Fqcf%5D",
                     api.getString(0));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an app-defined JMS TopicConnectionFactory.
     */
    @Test
    public void testAppDefinedJMSTopicConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsTopicConnectionFactory/application[AppDefResourcesApp]%2FjmsTopicConnectionFactory[java:app%2Fenv%2Fjms%2Ftcf]")
                        .run(JsonObject.class);
        String err = "unexpected response: " + cf;

        assertEquals(err, "jmsTopicConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]", cf.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]", cf.getString("id"));
        assertEquals(err, "java:app/env/jms/tcf", cf.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", cf.getString("application"));
        assertNull(err, cf.get("module"));
        assertNull(err, cf.get("component"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]/connectionManager", cm.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]/connectionManager", cm.getString("id"));
        assertEquals(err, -1, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 8, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        // support for containerAuthDataRef/recoveryAuthDataRef was never added to app-defined connection factories

        assertNull(err, cf.get("creates.objectClass"));
        assertNull(err, cf.get("jndiName.unique"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.ConfigTestAdapter"));
        assertTrue(err, props.getBoolean("enableBetaContent"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, 8765, props.getInt("portNumber"));

        JsonArray api;
        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/jmsTopicConnectionFactory/application%5BAppDefResourcesApp%5D%2FjmsTopicConnectionFactory%5Bjava%3Aapp%2Fenv%2Fjms%2Ftcf%5D",
                     api.getString(0));
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for an application-defined JMS queue.
     */
    @Test
    public void testAppDefinedQueue() throws Exception {
        JsonObject q = new HttpsRequest(server, "/ibm/api/config/jmsQueue/application%5BAppDefResourcesApp%5D%2FjmsQueue%5Bjava:app%2Fenv%2Fjms%2Fqueue1%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + q;

        assertEquals(err, "jmsQueue", q.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsQueue[java:app/env/jms/queue1]", q.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsQueue[java:app/env/jms/queue1]", q.getString("id"));
        assertEquals(err, "java:app/env/jms/queue1", q.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", q.getString("application"));
        assertNull(err, q.get("module"));
        assertNull(err, q.get("component"));

        JsonObject props;
        assertNotNull(err, props = q.getJsonObject("properties.wasJms"));
        assertEquals(err, 4, props.size());
        assertEquals(err, "Application", props.getString("deliveryMode"));
        assertEquals(err, "MyQueue", props.getString("queueName"));
        assertEquals(err, "AlwaysOff", props.getString("readAhead"));
        assertEquals(err, 0l, props.getJsonNumber("timeToLive").longValue());
    }

    /**
     * Use the /ibm/api/config REST endpoint to obtain configuration for application-defined administered objects,
     * querying by module and component.
     */
    @Test
    public void testAppDefinedTopicsQueryByModuleAndComponent() throws Exception {
        JsonArray topics = new HttpsRequest(server, "/ibm/api/config/jmsTopic?module=AppDefResourcesEJB.jar&component=AppDefinedResourcesBean")
                        .run(JsonArray.class);
        String err = "unexpected response: " + topics;
        assertEquals(err, 1, topics.size());

        JsonObject topic;
        assertNotNull(err, topic = topics.getJsonObject(0));
        assertEquals(err, "jmsTopic", topic.getString("configElementName"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/jmsTopic[java:comp/env/jms/topic1]",
                     topic.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/jmsTopic[java:comp/env/jms/topic1]",
                     topic.getString("id"));
        assertEquals(err, "java:comp/env/jms/topic1", topic.getString("jndiName"));

        assertEquals(err, "AppDefResourcesApp", topic.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", topic.getString("module"));
        assertEquals(err, "AppDefinedResourcesBean", topic.getString("component"));

        JsonObject props;
        assertNotNull(err, props = topic.getJsonObject("properties.wasJms"));
        assertEquals(err, 6, props.size());
        assertEquals(err, "Application", props.getString("deliveryMode"));
        assertEquals(err, 8, props.getInt("priority"));
        assertEquals(err, "AsConnection", props.getString("readAhead"));
        assertEquals(err, 246l, props.getJsonNumber("timeToLive").longValue());
        assertEquals(err, "MyTopic", props.getString("topicName"));
        assertEquals(err, "Default.Topic.Space", props.getString("topicSpace"));
    }

    /**
     * Query the /config/ REST endpoint for a server-config-defined connection factory
     * for a resource adapter that is embedded in the application and verify that it
     * returns the correct output.
     */
    @Test
    public void testConnectionFactoryFromEmbeddedResourceAdapter() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/connectionFactory?jndiName=eis/cf3")
                        .run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(err, 1, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));

        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "connectionFactory[default-0]", cf.getString("uid"));
        assertNull(err, cf.get("id"));
        assertEquals(err, "eis/cf3", cf.getString("jndiName"));

        assertNull(err, cf.get("application"));
        assertNull(err, cf.get("module"));
        assertNull(err, cf.get("component"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "connectionFactory[default-0]/connectionManager[default-0]", cm.getString("uid"));
        assertNull(err, cm.get("id"));
        assertEquals(err, 12783, cm.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 30, cm.getJsonNumber("connectionTimeout").longValue());
        assertFalse(err, cm.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, cm.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 3, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));
        assertEquals(err, 180, cm.getJsonNumber("reapTime").longValue());

        JsonObject auth;
        assertNotNull(err, auth = cf.getJsonObject("containerAuthDataRef"));
        assertEquals(err, "cfauth1", auth.getString("uid"));
        assertEquals(err, "cfauth1", auth.getString("id"));
        assertEquals(err, "cfuser1", auth.getString("user"));
        assertEquals(err, "******", auth.getString("password"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.AppDefResourcesApp.EmbTestAdapter.ConnectionFactory"));
        assertTrue(err, props.getBoolean("enableBetaContent"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, 3456, props.getInt("portNumber"));

        JsonArray api;
        assertNotNull(err, api = cf.getJsonArray("api"));
        assertEquals(err, 1, api.size()); // increase if more REST API is added
        assertEquals(err,
                     "/ibm/api/validation/connectionFactory/connectionFactory%5Bdefault-0%5D",
                     api.getString(0));
    }

    /*
     * Test that a data source nested under a transaction with an atypical case can be accessed
     * by calling the config endpoint matching with the case matching server config.
     */
    @Test
    public void testNestedDataSourceCase() throws Exception {
        JsonArray json = new HttpsRequest(server, "/ibm/api/config/DATASOURCE").run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 1, json.size());
        JsonObject j = json.getJsonObject(0);
        JsonObject jj;

        assertEquals(err, "DATASOURCE", j.getString("configElementName"));
        assertEquals(err, "transaction/DATASOURCE[default-0]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertNull(err, j.get("jndiName"));
        assertEquals(err, true, j.getBoolean("beginTranForResultSetScrollingAPIs"));
        assertEquals(err, true, j.getBoolean("beginTranForVendorAPIs"));
        assertNotNull(err, jj = j.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", jj.getString("configElementName"));
        assertEquals(err, "transaction/DATASOURCE[default-0]/connectionManager[default-0]", jj.getString("uid"));
        assertNull(err, jj.get("id"));
        assertEquals(err, -1, jj.getJsonNumber("agedTimeout").longValue());
        assertEquals(err, 0, jj.getJsonNumber("connectionTimeout").longValue());
        assertTrue(err, jj.getBoolean("enableSharingForDirectLookups"));
        assertEquals(err, 1800, jj.getJsonNumber("maxIdleTime").longValue());
        assertEquals(err, 5, jj.getJsonNumber("maxPoolSize").longValue());
        assertEquals(err, "EntirePool", jj.getString("purgePolicy"));
        assertEquals(err, 180, jj.getJsonNumber("reapTime").longValue());
        assertEquals(err, "MatchOriginalRequest", j.getString("connectionSharing"));
        assertEquals(err, false, j.getBoolean("enableConnectionCasting"));
        assertNotNull(json = j.getJsonArray("api"));
        boolean found = false;
        for (JsonValue jv : json)
            if ("/ibm/api/validation/DATASOURCE/transaction%2FDATASOURCE%5Bdefault-0%5D".equals(((JsonString) jv).getString()))
                if (found)
                    fail("Duplicate value in api list");
                else
                    found = true;
        assertTrue(err, found);
        assertNotNull(err, jj = j.getJsonObject("jdbcDriverRef"));
        assertEquals(err, "jdbcDriver", jj.getString("configElementName"));
        assertEquals(err, "transaction/DATASOURCE[default-0]/jdbcDriver[default-0]", jj.getString("uid"));
        assertNull(err, jj.get("id"));
        assertNotNull(err, jj = jj.getJsonObject("libraryRef"));
        assertEquals(err, "library", jj.getString("configElementName"));
        assertEquals(err, "Derby", jj.getString("uid"));
        assertEquals(err, "Derby", jj.getString("id"));
        assertEquals(err, "spec,ibm-api,api,stable", jj.getString("apiTypeVisibility"));
        assertNotNull(err, json = jj.getJsonArray("fileRef"));
        assertEquals(err, 1, json.size());
        assertNotNull(err, jj = json.getJsonObject(0));
        assertEquals(err, "file", jj.getString("configElementName"));
        assertEquals(err, "library[Derby]/file[default-0]", jj.getString("uid"));
        assertTrue(err, jj.getString("name").endsWith("derby.jar"));
        assertEquals(err, 10, j.getInt("statementCacheSize"));
        assertEquals(err, false, j.getBoolean("syncQueryTimeoutWithTransactionTimeout"));
        assertEquals(err, false, j.getBoolean("transactional"));
        assertNotNull(err, j = j.getJsonObject("properties.derby.embedded"));
        assertEquals(err, "memory:recoverydb", j.getString("databaseName"));
    }

    /**
     * Use the /ibm/api/validator REST endpoint to validate application-defined connection factories,
     * and also a server-defined connection factory for resource adapter that is embedded in the application.
     */
    @Test
    public void testValidateAppDefinedConnectionFactories() throws Exception {
        JsonArray array = new HttpsRequest(server, "/ibm/api/validation/connectionFactory")
                        .run(JsonArray.class);
        String err = "unexpected response: " + array;
        assertEquals(err, 4, array.size());

        JsonObject j;
        assertNotNull(err, j = array.getJsonObject(0)); // a javax.resource.cci.ConnectionFactory
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]", j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/connectionFactory[java:module/env/eis/cf1]", j.getString("id"));
        assertEquals(err, "java:module/env/eis/cf1", j.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", j.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", j.getString("module"));
        assertNull(err, j.get("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestConfig Data Store, Enterprise Edition", j.getString("eisProductName"));
        assertEquals(err, "48.55.72", j.getString("eisProductVersion"));
        assertEquals(err, "TestConfigAdapter", j.getString("resourceAdapterName"));
        assertEquals(err, "60.91.109", j.getString("resourceAdapterVersion"));
        assertEquals(err, "OpenLiberty", j.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", j.getString("resourceAdapterDescription"));
        assertEquals(err, "1.7", j.getString("connectorSpecVersion"));
        assertEquals(err, "cfuser1", j.getString("user"));

        assertNotNull(err, j = array.getJsonObject(1)); // javax.sql.DataSource from embedded RAR
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]",
                     j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/connectionFactory[java:comp/env/eis/cf2]",
                     j.getString("id"));
        assertEquals(err, "java:comp/env/eis/cf2", j.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", j.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", j.getString("module"));
        assertEquals(err, "AppDefinedResourcesBean", j.getString("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestConfig Data Store, Enterprise Edition", j.getString("databaseProductName"));
        assertEquals(err, "48.55.72", j.getString("databaseProductVersion"));
        assertEquals(err, "TestConfigJDBCAdapter", j.getString("driverName"));
        assertEquals(err, "65.72.97", j.getString("driverVersion"));
        assertEquals(err, "TestConfigDB", j.getString("catalog"));
        assertEquals(err, "EUSER2", j.getString("schema"));
        assertEquals(err, "euser2", j.getString("user"));

        assertNotNull(err, j = array.getJsonObject(2)); // a javax.sql.DataSource
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]", j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/connectionFactory[java:module/env/eis/cf1]", j.getString("id"));
        assertEquals(err, "java:module/env/eis/cf1", j.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", j.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", j.getString("module"));
        assertNull(err, j.get("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestConfig Data Store, Enterprise Edition", j.getString("databaseProductName"));
        assertEquals(err, "48.55.72", j.getString("databaseProductVersion"));
        assertEquals(err, "TestConfigJDBCAdapter", j.getString("driverName"));
        assertEquals(err, "65.72.97", j.getString("driverVersion"));
        assertEquals(err, "TestConfigDB", j.getString("catalog"));
        assertNull(err, j.get("schema"));
        assertNull(err, j.get("user"));

        assertNotNull(err, j = array.getJsonObject(3)); // javax.resource.cci.ConnectionFactory from embedded RAR (configured in server.xml)
        assertEquals(err, "connectionFactory[default-0]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertEquals(err, "eis/cf3", j.getString("jndiName"));
        assertNull(err, j.get("application"));
        assertNull(err, j.get("module"));
        assertNull(err, j.get("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestConfig Data Store, Enterprise Edition", j.getString("eisProductName"));
        assertEquals(err, "48.55.72", j.getString("eisProductVersion"));
        assertEquals(err, "TestConfigAdapter", j.getString("resourceAdapterName"));
        assertEquals(err, "60.91.109", j.getString("resourceAdapterVersion"));
        assertEquals(err, "OpenLiberty", j.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", j.getString("resourceAdapterDescription"));
        assertEquals(err, "1.7", j.getString("connectorSpecVersion"));
        assertNull(err, j.get("user"));
    }

    /**
     * Use the /ibm/api/validator REST endpoint to validate application-defined data sources
     */
    @AllowedFFDC({ "java.lang.IllegalArgumentException", // expected: Could not parse configuration value as a duration: 1:05:30
                   "java.security.PrivilegedActionException", // expected: Value 1:05:30 is not supported for agedTimeout
                   "javax.resource.ResourceException", // expected: Value 1:05:30 is not supported for agedTimeout
                   "jakarta.resource.ResourceException" // expected: Value 1:05:30 is not supported for agedTimeout
    })
    @Test
    public void testValidateAppDefinedDataSources() throws Exception {
        JsonArray array = new HttpsRequest(server, "/ibm/api/validation/dataSource?auth=container&authAlias=derbyAuth3")
                        .run(JsonArray.class);
        String err = array.toString();
        assertEquals(err, 6, array.size());

        JsonObject v, info, failure, cause;
        String message;
        JsonArray stack;

        assertNotNull(err, v = array.getJsonObject(0));
        assertEquals(err, "application[AppDefResourcesApp]/dataSource[java:app/env/jdbc/ds1]", v.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/dataSource[java:app/env/jdbc/ds1]", v.getString("id"));
        assertEquals(err, "java:app/env/jdbc/ds1", v.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", v.getString("application"));
        assertNull(err, v.get("module"));
        assertNull(err, v.get("component"));
        assertFalse(err, v.getBoolean("successful"));
        assertNull(err, v.get("info"));
        assertNotNull(err, failure = v.getJsonObject("failure"));
        assertEquals(err, "java.security.PrivilegedActionException", failure.getString("class"));
        assertNotNull(err, stack = failure.getJsonArray("stack"));
        assertTrue(err, stack.size() > 3);
        assertNotNull(stack.get(0));
        assertNotNull(stack.get(1));
        assertNotNull(stack.get(2));
        assertNotNull(err, cause = failure.getJsonObject("cause"));
        assertEquals(err, JakartaEE9Action.isActive() ? "jakarta.resource.ResourceException" : "javax.resource.ResourceException", cause.getString("class"));
        assertNotNull(err, message = cause.getString("message"));
        assertTrue(err, message.startsWith("J2CA8011E") && message.contains("1:05:30"));
        assertNotNull(err, stack = cause.getJsonArray("stack"));
        assertTrue(err, stack.size() > 3);
        assertNotNull(stack.get(0));
        assertNotNull(stack.get(1));
        assertNotNull(stack.get(2));
        assertNotNull(err, cause = cause.getJsonObject("cause"));
        assertEquals(err, "java.lang.IllegalArgumentException", cause.getString("class"));
        assertNotNull(err, message = cause.getString("message"));
        assertTrue(err, message.contains("1:05:30"));
        assertNotNull(err, stack = cause.getJsonArray("stack"));
        assertTrue(err, stack.size() > 3);
        assertNotNull(stack.get(0));
        assertNotNull(stack.get(1));
        assertNotNull(stack.get(2));
        assertNull(err, cause.getJsonObject("cause"));

        assertNotNull(err, v = array.getJsonObject(1));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]", v.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:comp/env/jdbc/ds3]", v.getString("id"));
        assertEquals(err, "java:comp/env/jdbc/ds3", v.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", v.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", v.getString("module"));
        assertNull(err, v.get("component"));
        assertTrue(err, v.getBoolean("successful"));
        assertNull(err, v.get("failure"));
        assertNotNull(err, info = v.getJsonObject("info"));
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").contains(DerbyVersion));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").contains(DerbyVersion));
        assertEquals(err, "DBUSER3", info.getString("schema"));
        assertEquals(err, "dbuser3", info.getString("user"));

        assertNotNull(err, v = array.getJsonObject(2));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]", v.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/dataSource[java:module/env/jdbc/ds2]", v.getString("id"));
        assertEquals(err, "java:module/env/jdbc/ds2", v.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", v.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", v.getString("module"));
        assertNull(err, v.get("component"));
        assertTrue(err, v.getBoolean("successful"));
        assertNull(err, v.get("failure"));
        assertNotNull(err, info = v.getJsonObject("info"));
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").contains(DerbyVersion));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").contains(DerbyVersion));
        assertEquals(err, "DBUSER3", info.getString("schema"));
        assertEquals(err, "dbuser3", info.getString("user"));

        assertNotNull(err, v = array.getJsonObject(3));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]",
                     v.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesEJB.jar]/component[AppDefinedResourcesBean]/dataSource[java:comp/env/jdbc/ds3]",
                     v.getString("id"));
        assertEquals(err, "java:comp/env/jdbc/ds3", v.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", v.getString("application"));
        assertEquals(err, "AppDefResourcesEJB.jar", v.getString("module"));
        assertEquals(err, "AppDefinedResourcesBean", v.getString("component"));
        assertTrue(err, v.getBoolean("successful"));
        assertNull(err, v.get("failure"));
        assertNotNull(err, info = v.getJsonObject("info"));
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").contains(DerbyVersion));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").contains(DerbyVersion));
        assertEquals(err, "DBUSER3", info.getString("schema"));
        assertEquals(err, "dbuser3", info.getString("user"));

        assertNotNull(err, v = array.getJsonObject(4));
        assertEquals(err, "DefaultDataSource", v.getString("uid"));
        assertEquals(err, "DefaultDataSource", v.getString("id"));
        assertNull(err, v.get("jndiName"));
        assertNull(err, v.get("application"));
        assertNull(err, v.get("module"));
        assertNull(err, v.get("component"));
        assertFalse(err, v.getBoolean("successful"));
        assertNull(err, v.get("info"));
        assertNotNull(err, failure = v.getJsonObject("failure"));
        assertNotNull(err, failure.getString("message"));

        assertNotNull(err, v = array.getJsonObject(5));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", v.getString("uid"));
        assertEquals(err, "dataSource[java:global/env/jdbc/ds4]", v.getString("id"));
        assertEquals(err, "java:global/env/jdbc/ds4", v.getString("jndiName"));
        assertNull(err, v.get("application"));
        assertNull(err, v.get("module"));
        assertNull(err, v.get("component"));
        assertTrue(err, v.getBoolean("successful"));
        assertNull(err, v.get("failure"));
        assertNotNull(err, info = v.getJsonObject("info"));
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").contains(DerbyVersion));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").contains(DerbyVersion));
        assertEquals(err, "DBUSER3", info.getString("schema"));
        assertEquals(err, "dbuser3", info.getString("user"));
    }

    /**
     * Use the /ibm/api/validator REST endpoint to validate an application-defined JMS connection factory
     */
    @Test
    public void testValidateAppDefinedJMSConnectionFactory() throws Exception {
        JsonObject j = new HttpsRequest(server, "/ibm/api/validation/jmsConnectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FjmsConnectionFactory%5Bjava%3Acomp%2Fenv%2Fjms%2Fcf%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + j;

        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]", j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsConnectionFactory[java:comp/env/jms/cf]", j.getString("id"));
        assertEquals(err, "java:comp/env/jms/cf", j.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", j.getString("application"));
        assertEquals(err, "AppDefResourcesApp.war", j.getString("module"));
        assertNull(err, j.get("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "IBM", j.getString("jmsProviderName"));
        assertEquals(err, "1.0", j.getString("jmsProviderVersion"));
        assertEquals(err, JakartaEE9Action.isActive() ? "3.0" : "2.0", j.getString("jmsProviderSpecVersion"));
        assertEquals(err, "clientID", j.getString("clientID"));
    }

    /**
     * Use the /ibm/api/validator REST endpoint to validate an application-defined JMS queue connection factory
     */
    @Test
    public void testValidateAppDefinedJMSQueueConnectionFactory() throws Exception {
        JsonObject j = new HttpsRequest(server, "/ibm/api/validation/jmsQueueConnectionFactory/application%5BAppDefResourcesApp%5D%2Fmodule%5BAppDefResourcesApp.war%5D%2FjmsQueueConnectionFactory%5Bjava%3Amodule%2Fenv%2Fjms%2Fqcf%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + j;

        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]", j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/module[AppDefResourcesApp.war]/jmsQueueConnectionFactory[java:module/env/jms/qcf]", j.getString("id"));
        assertEquals(err, "java:module/env/jms/qcf", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "IBM", j.getString("jmsProviderName"));
        assertEquals(err, "1.0", j.getString("jmsProviderVersion"));
        assertEquals(err, JakartaEE9Action.isActive() ? "3.0" : "2.0", j.getString("jmsProviderSpecVersion"));
    }

    /**
     * Use the /ibm/api/validator REST endpoint to validate an application-defined JMS topic connection factory
     */
    @Test
    public void testValidateAppDefinedJMSTopicConnectionFactory() throws Exception {
        JsonObject j = new HttpsRequest(server, "/ibm/api/validation/jmsTopicConnectionFactory/application%5BAppDefResourcesApp%5D%2FjmsTopicConnectionFactory%5Bjava%3Aapp%2Fenv%2Fjms%2Ftcf%5D")
                        .run(JsonObject.class);
        String err = "unexpected response: " + j;

        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]", j.getString("uid"));
        assertEquals(err, "application[AppDefResourcesApp]/jmsTopicConnectionFactory[java:app/env/jms/tcf]", j.getString("id"));
        assertEquals(err, "java:app/env/jms/tcf", j.getString("jndiName"));
        assertEquals(err, "AppDefResourcesApp", j.getString("application"));
        assertNull(err, j.get("module"));
        assertNull(err, j.get("component"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestConfig Messaging Provider", j.getString("jmsProviderName"));
        assertEquals(err, "88.105.137", j.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", j.getString("jmsProviderSpecVersion"));
        assertEquals(err, "AppDefinedClientId", j.getString("clientID"));
    }
}