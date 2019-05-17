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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ConfigRESTHandlerAppDefinedResourcesTest extends FATServletClient {
    private static final String APP_NAME = "AppDefResourcesApp";

    @Server("com.ibm.ws.rest.handler.config.appdef.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "test.resthandler.config.appdef.web");

        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/

        server.waitForStringsInLogUsingMark(messages);

        // Invoke the servlet to ensure that the web module containing the app-defined data source is processed.
        FATServletClient.runTest(server, "AppDefResourcesApp/AppDefinedResourcesServlet", "doSomething");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
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
        assertEquals(err, 5, dataSources.size());
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
}