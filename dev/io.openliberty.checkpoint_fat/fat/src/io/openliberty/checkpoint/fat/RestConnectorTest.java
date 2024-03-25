/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.BasicRegistry;
import com.ibm.websphere.simplicity.config.BasicRegistry.User;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpsRequest;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class RestConnectorTest extends FATServletClient {

    public static final String SERVER_NAME = "checkpointRestConnector";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(SERVER_NAME);

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }

    @Before
    public void setUp() throws Exception {
        server.saveServerConfiguration();
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    @Test
    public void testBasicRegistryConfigUpdate() throws Exception {
        server.checkpointRestore();
        assertNotNull(server.waitForStringInLog("CWWKS0008I")); // CWWKS0008I: The security service is ready.
        assertNotNull(server.waitForStringInLog("CWWKS4105I")); // CWWKS4105I: LTPA configuration is ready after # seconds.
        assertNotNull(server.waitForStringInLog("CWPKI0803A")); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        assertNotNull(server.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl")); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.

        JsonArray json = new HttpsRequest(server, "/ibm/api/config").run(JsonArray.class);
        int count = json.size();
        assertTrue("Unexpected response: " + json, count > 1);

        JsonArray json2 = new HttpsRequest(server, "/ibm/api/config/").run(JsonArray.class);
        int count2 = json2.size();
        assertEquals(count, count2);

        Map<String, JsonObject> allConfig = getAllConfig(json);

        JsonObject j = allConfig.get("basicRegistry");
        assertNotNull("Unexpected response: " + json, j);
        assertEquals("Incorrect number of users in basic registry", j.getJsonArray("user").size(), 3);
        server.stopServer(false, "");

        //Add another user
        addUser("user2", "user2pwd");
        server.checkpointRestore();
        json = new HttpsRequest(server, "/ibm/api/config").run(JsonArray.class);

        allConfig = getAllConfig(json);
        j = allConfig.get("basicRegistry");

        assertNotNull("Unexpected response: " + json, j);
        assertEquals("Incorrect number of users in basic registry", j.getJsonArray("user").size(), 4);
    }

    @Test
    public void testDataSourceConfigUpdate() throws Exception {
        server.checkpointRestore();
        assertNotNull(server.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl")); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.

        JsonArray json = new HttpsRequest(server, "/ibm/api/config/dataSource").run(JsonArray.class);
        assertEquals("unexpected response: " + json, 1, json.size());

        JsonArray json2 = new HttpsRequest(server, "/ibm/api/config/dataSource/").run(JsonArray.class);
        assertEquals(json, json2);

        Map<String, JsonObject> allConfig = getAllConfig(json);

        JsonObject j = allConfig.get("DefaultDataSource").getJsonObject("properties.derby.embedded");
        assertNotNull("Unexpected response: " + json, j);
        assertEquals("Wrong createDatabase value", "create", j.getString("createDatabase"));
        assertEquals("Wrong databaseName value", "defaultdb", j.getString("databaseName"));
        assertEquals("Wrong user value", "dbuser1", j.getString("user"));
        assertEquals("Wrong password value", "******", j.getString("password"));

        server.stopServer(false, "");
        ServerConfiguration config = server.getServerConfiguration();
        // update databse username
        config.getDataSources().getById("DefaultDataSource").getProperties_derby_embedded().getById("prop1").setUser("dbuser2");
        server.updateServerConfiguration(config);

        server.checkpointRestore();
        json = new HttpsRequest(server, "/ibm/api/config/dataSource").run(JsonArray.class);
        allConfig = getAllConfig(json);
        j = allConfig.get("DefaultDataSource").getJsonObject("properties.derby.embedded");
        assertNotNull("Unexpected response: " + json, j);
        assertEquals("Wrong user value", "dbuser2", j.getString("user"));

    }

    private Map<String, JsonObject> getAllConfig(JsonArray json) {
        Map<String, JsonObject> allConfig = new HashMap<String, JsonObject>();
        for (JsonValue jv : json) {
            JsonObject jo = (JsonObject) jv;
            String key = jo.getString(jo.containsKey("uid") ? "uid" : "configElementName");
            allConfig.put(key, jo);
        }
        return allConfig;
    }

    private void addUser(String name, String password) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        BasicRegistry registry = config.getBasicRegistries().getById("basicRegistry");
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        registry.getUsers().add(user);
        server.updateServerConfiguration(config);
    }

    @After
    public void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.restoreServerConfiguration();
        }
    }

}
