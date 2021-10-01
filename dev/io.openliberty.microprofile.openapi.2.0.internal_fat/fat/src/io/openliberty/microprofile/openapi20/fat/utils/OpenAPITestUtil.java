/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.utils;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;

/**
 * Utility methods used by OpenAPI Tests
 */
public class OpenAPITestUtil {

    private final static int TIMEOUT = 30000;

    /**
     * Change Liberty features (Mark is set first on log. Then wait for feature updated message using mark)
     *
     * @param server - Liberty server
     * @param features - Liberty features to enable
     * @throws Exception
     */
    public static void changeFeatures(LibertyServer server, String... features) throws Exception {
        List<String> featuresList = Arrays.asList(features);
        server.setMarkToEndOfLog();
        server.changeFeatures(featuresList);
        assertNotNull("Features weren't updated successfully", server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
    }

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to remove e.g. openapi-3.0
     */
    public static void removeFeature(LibertyServer server, String name) {
        try {
            server.setMarkToEndOfLog();
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (!features.contains(name))
                return;
            features.remove(name);
            server.updateServerConfiguration(config);
            assertNotNull("Config wasn't updated successfully",
                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
        } catch (Exception e) {
            Assert.fail("Unable to remove feature:" + name);
        }
    }

    /**
     * @param server - Liberty server
     * @param name - The name of a feature to add e.g. openapi-3.0
     */
    public static void addFeature(LibertyServer server, String name) {
        try {
            server.setMarkToEndOfLog();
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (features.contains(name))
                return;
            features.add(name);
            server.updateServerConfiguration(config);
            assertNotNull("Config wasn't updated successfully",
                          server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*"));
        } catch (Exception e) {
            Assert.fail("Unable to add feature:" + name);
        }
    }

    /**
     * Wait for the message stating the app has been processed by the application processor adding.
     *
     * @param server - Liberty server
     * @throws Exception
     */
    public static void waitForApplicationProcessorAddedEvent(LibertyServer server, String appName) {
        String s = server.waitForStringInTraceUsingMark("Application Processor: Adding application ended: appInfo=.*\\[" + appName + "\\]", TIMEOUT);
        assertNotNull("FAIL: Application processor didn't successfully finish adding the app " + appName, s);
    }

    /**
     * Wait for the message stating the app has been processed by the application processor.
     *
     * @param server - Liberty server
     * @throws Exception
     */
    public static void waitForApplicationProcessorProcessedEvent(LibertyServer server, String appName) {
        String s = server.waitForStringInTraceUsingMark("Application Processor: Processing application ended: appInfo=.*[" + appName + "]", TIMEOUT);
        assertNotNull("FAIL: Application processor didn't successfully finish adding the app " + appName, s);
    }

    /**
     * Wait for the message stating the app has been processed by the application processor removing.
     *
     * @param server - Liberty server
     * @throws Exception
     */
    public static void waitForApplicationProcessorRemovedEvent(LibertyServer server, String appName) {
        String s = server.waitForStringInTraceUsingMark("Application Processor: Removing application ended: appInfo=.*[" + appName + "]", TIMEOUT);
        assertNotNull("FAIL: Application processor didn't successfully finish removing the app " + appName, s);
    }

    public static void waitForApplicationAdded(LibertyServer server, String appName) {
        String s = server.waitForStringInTraceUsingMark("Processign application ended: appInfo=.*[" + appName + "]", TIMEOUT);
        assertNotNull("FAIL: Application processor didn't successfully process the app " + appName, s);
    }

    public static Application removeApplication(LibertyServer server, String appName) {
        Application webApp = null;
        try {
            ServerConfiguration config = server.getServerConfiguration();
            webApp = config.getApplications().removeById(appName);
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null);
            waitForApplicationProcessorRemovedEvent(server, appName);
            assertNotNull("FAIL: App didn't report is has been stopped.",
                          server.waitForStringInLogUsingMark("CWWKZ0009I.*" + appName));
        } catch (Exception e) {
            fail("FAIL: Could not remove the application " + appName);
        }
        return webApp;
    }

    /**
     * Adds an application to the current config, or updates an application with
     * a specific name if it already exists
     *
     * @param name the name of the application
     * @param path the fully qualified path to the application archive on the liberty machine
     * @param type the type of the application (ear/war/etc)
     * @param waitForUpdate boolean controlling if the method should wait for the configuration update event before returning
     * @return the deployed application
     */
    public static Application addApplication(LibertyServer server, String name, String path, String type, boolean waitForAppProcessor) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Application app = config.addApplication(name, path, type);
        server.updateServerConfiguration(config);
        if (waitForAppProcessor) {
            waitForApplicationProcessorAddedEvent(server, name);
        }
        server.validateAppLoaded(name);
        return app;
    }

    public static Application addApplication(LibertyServer server, String name, String path, String type) throws Exception {
        return addApplication(server, name, path, type, true);
    }

    /**
     * Adds an WAR application inside the '${server.config.dir}/apps/'
     * to the current config, or updates an application with a specific name
     * if it already exists. This method waits for the app to be processed by OpenAPI
     * Application Processor.
     *
     * @param name the name of the application
     * @return the deployed application
     */
    public static Application addApplication(LibertyServer server, String name) throws Exception {
        return addApplication(server, name, "${server.config.dir}/apps/" + name + ".war", "war", true);
    }

    public static Application addApplication(LibertyServer server, String name, boolean waitForAppProcessor) throws Exception {
        return addApplication(server, name, "${server.config.dir}/apps/" + name + ".war", "war", waitForAppProcessor);
    }

    public static JsonNode readYamlTree(String contents) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
        return new ObjectMapper().convertValue(yaml.load(contents), JsonNode.class);
    }

    /**
     * Removes all the applications from server.xml
     *
     * @param server
     * @throws Exception
     */
    public static void removeAllApplication(LibertyServer server) throws Exception {
        server.getServerConfiguration().getApplications().stream().forEach(app -> removeApplication(server, app.getName()));
    }

    public static void checkServer(JsonNode root, String... expectedUrls) {
        JsonNode serversNode = root.get("servers");
        assertNotNull(serversNode);
        assertTrue(serversNode.isArray());
        ArrayNode servers = (ArrayNode) serversNode;

        List<String> urls = Arrays.asList(expectedUrls);
        servers.findValues("url").forEach(url -> assertTrue("FAIL: Unexpected server URL " + url, urls.contains(url.asText())));
        assertEquals("FAIL: Found incorrect number of server objects.", urls.size(), servers.size());
    }

    public static void checkPaths(JsonNode root, int expectedCount, String... containedPaths) {
        JsonNode pathsNode = root.get("paths");
        assertNotNull(pathsNode);
        assertTrue(pathsNode.isObject());
        ObjectNode paths = (ObjectNode) pathsNode;
        
        List<String> pathNames = asList(paths.fieldNames());
        assertThat("Path names", pathNames, hasItems(containedPaths));
        assertThat("Path names", pathNames, hasSize(expectedCount));
    }

    public static void checkInfo(JsonNode root, String defaultTitle, String defaultVersion) {
        JsonNode infoNode = root.get("info");
        assertNotNull(infoNode);

        assertNotNull("Title is not specified to the default value", infoNode.get("title"));
        assertNotNull("Version is not specified to the default value", infoNode.get("version"));

        String title = infoNode.get("title").textValue();
        String version = infoNode.get("version").textValue();

        assertEquals("Incorrect default value for title", defaultTitle, title);
        assertEquals("Incorrect default value for version", defaultVersion, version);
    }

    public static void changeServerPorts(LibertyServer server, int httpPort, int httpsPort) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        HttpEndpoint http = config.getHttpEndpoints().getById("defaultHttpEndpoint");
        if (http == null) {
            http = new HttpEndpoint();
            http.setId("defaultHttpEndpoint");
            http.setHttpPort(Integer.toString(httpPort));
            http.setHttpsPort(Integer.toString(httpsPort));
            config.getHttpEndpoints().add(http);
        } else if (Integer.parseInt(http.getHttpPort()) == httpPort && Integer.parseInt(http.getHttpsPort()) == httpsPort) {
            return;
        }

        http.setHttpPort(Integer.toString(httpPort));
        http.setHttpsPort(Integer.toString(httpsPort));

        if (server.isStarted()) {
            // Set the mark to the current end of log
            setMarkToEndOfAllLogs(server);

            // Save the config and wait for message that was a result of the config change
            server.updateServerConfiguration(config);
            assertNotNull("FAIL: Didn't get expected config update log messages.", server.waitForConfigUpdateInLogUsingMark(null, false));
            String regex = "Updated server information.*"
                           + "httpPort=" + (httpPort == -1 ? 0 : httpPort) + ", httpsPort=" + (httpsPort == -1 ? 0 : httpsPort);
            server.waitForStringInTrace(regex, TIMEOUT);
        } else {
            server.updateServerConfiguration(config);
        }
    }
    
    public static String[] getServerURLs(LibertyServer server, int httpPort, int httpsPort) {
        return getServerURLs(server, httpPort, httpsPort, null);
    }
    
    public static String[] getServerURLs(LibertyServer server, int httpPort, int httpsPort, String contextRoot) {
        List<String> servers = new ArrayList<>();
        contextRoot = contextRoot == null ? "" : contextRoot.startsWith("/") ? contextRoot : "/" + contextRoot;
        if (httpPort != -1) {
            servers.add("http://" + server.getHostname() + ":" + httpPort + contextRoot);
        }
        if (httpsPort != -1) {
            servers.add("https://" + server.getHostname() + ":" + httpsPort + contextRoot);
        }
        return servers.toArray(new String[0]);
    }

    public static void setMarkToEndOfAllLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }
    
    private static <T> List<T> asList(Iterator<? extends T> i) {
        List<T> result = new ArrayList<>();
        while (i.hasNext()) {
            T item = i.next();
            result.add(item);
        }
        return result;
    }
}
