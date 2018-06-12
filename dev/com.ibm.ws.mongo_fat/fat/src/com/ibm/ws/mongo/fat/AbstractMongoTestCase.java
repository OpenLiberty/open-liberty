/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.mongo.fat.shared.MongoServletAction;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.TestCase;

public abstract class AbstractMongoTestCase extends TestCase {

    public static final String APP_NAME = "mongo";

    protected static LibertyServer server = null;
    protected static ServerConfiguration config = null;
    private static String HOST_NAME;

    static {
        try {
            AbstractMongoTestCase.HOST_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            AbstractMongoTestCase.HOST_NAME = "localhost-" + System.nanoTime();
        }
    }

    enum MongoServlet {
        JNDI,
        INJECTED,
        RESOURCEENVREF,
        NO_SSLREF,
        NO_SSL_FEATURE,
        NESTED_SSL,
        SSLENABLED_FALSE,
        SSLENABLED_FALSE_SSLREF,
        DIFFERENT_SSLREF,
        INVALID_TRUSTSTORE,
        CERT_ALIAS_INV,
        CERT_ALIAS_VALID,
        CERT_ALIAS_NOT_IN_KEYSTORE,
        CERT_ALIAS_MISSING,
        CERT_ALIAS_NOT_REQD,
        CERT_TRUST_INV,
        CERT_PASSWORD_INV,
        CERT_USER_INV,
        CERT_SSLENABLED_NOT_SET,
        CERT_SSLENABLED_FALSE,
        CERT_INVALID_SCENARIOS,
        CERT_INVALID_DRIVER_LEVEL
    }

    protected BufferedReader doRequest(MongoServletAction action, MongoServlet servlet, Object... params) throws IOException {
        String req = (params != null && params.length > 0) ? "&" : "";

        for (int i = 0; (params != null && i < params.length); i += 2) {
            req += params[i] + "=" + params[i + 1] + "&";
        }
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                          + "/mongo/" + servlet + "?action=" + action + req);

        return getConnectionStream(getHttpConnection(url));
    }

    protected BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    protected HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    public static void before(String serverName) throws Exception {
        server = LibertyServerFactory.getLibertyServer(serverName, null, false);

        if (server.isStarted()) {
            throw new RuntimeException("Server shouldn't be running yet!");
        }

        config = server.getServerConfiguration();
        MongoServerSelector.assignMongoServers(server);
        config = server.getServerConfiguration();

        ShrinkHelper.defaultApp(server, APP_NAME,
                                "com.ibm.ws.mongo.servlet",
                                "com.ibm.ws.mongo.fat.shared");

        server.startServer();
    }

    public static void after(String... expectedFailures) throws Exception {
        if (server != null && server.isStarted())
            server.stopServer(expectedFailures);
    }

    protected void updateServerConfig(ServerConfiguration sc, boolean featureChange, boolean functionalChange, MongoServlet servlet,
                                      boolean callServerBefore, boolean callServerAfter) throws Exception {
        updateServerConfigWithParams(sc, featureChange, functionalChange, servlet, callServerBefore, callServerAfter);
    }

    protected void updateServerConfigWithParams(ServerConfiguration sc, boolean featureChange, boolean functionalChange, MongoServlet servlet,
                                                boolean callServerBefore, boolean callServerAfter, Object... params) throws Exception {
        Map<String, String> res = null;
        if (callServerBefore) {
            res = parseResult(doRequest(MongoServletAction.BEFORE_CONFIG_UPDATE, servlet, params).readLine());
            assertTrue("success".equals(res.get("result")));
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(sc);
        // CWWKG0016I: Starting server configuration update.
        if (server.waitForStringInLogUsingMark("CWWKG0016I") == null) {
            throw new Exception("Server configuration update did not start within the allotted interval");
        }
        if (functionalChange) {
            // CWWKG0017I: The server configuration was successfully updated in 0.8 seconds.
            if (server.waitForStringInLogUsingMark("CWWKG0017I") == null) {
                throw new Exception("Server configuration update did not complete within the allotted interval");
            }
        } else {
            // CWWKG0018I: The server configuration was not updated. No functional changes were detected.
            if (server.waitForStringInLogUsingMark("CWWKG0018I") == null) {
                throw new Exception("Server configuration update did not complete within the allotted interval");
            }
        }
        if (featureChange) {
            // CWWKF0008I: Feature update completed in 0.0 seconds.
            if (server.waitForStringInLogUsingMark("CWWKF0008I") == null) {
                throw new Exception("Server feature update did not complete within the allotted interval");
            }
        }
        if (callServerAfter) {
            // CWWKZ0001I: Application mongo started in 0.9 seconds.
            // CWWKZ0003I: The application mongo updated in 0.3 seconds.
            if (server.waitForStringInLogUsingMark("CWWKZ000[13]I.* mongo") == null) {
                throw new Exception("Server application restart did not complete within the allotted interval");
            }
            res = parseResult(doRequest(MongoServletAction.AFTER_CONFIG_UPDATE, servlet, params).readLine());
            assertTrue("success".equals(res.get("result")));
        }
    }

    protected Map<String, String> parseResult(String line) {
        Map<String, String> res = new HashMap<String, String>();
        String[] fulls = line.trim().split("[,]");
        for (String full : fulls) {
            String[] kv = full.trim().split("[=]");
            res.put(kv[0].trim(), kv[1].trim());
        }
        return res;
    }

    protected boolean find(String key, String expectedData, boolean authenticated, MongoServlet servlet) throws IOException {
        BufferedReader reader = doRequest(MongoServletAction.FIND, servlet, "key", key, "authenticated", authenticated);

        String line = reader.readLine();
        Map<String, String> res = parseResult(line);
        String result = res.get("result");
        if ("success".equals(result)) {
            String d = res.get("data");
            if (expectedData.equals(d)) {
                return true;
            }
        }
        fail("Find operation failed. expected data=" + expectedData + " Return : " + line);
        return false;
    }

    /**
     * Unauthenticated mongodb operations are not available at this time. See RTC 238129 and GIT6375
     */
    protected void insertFindScenario(boolean authenticated, MongoServlet servlet) throws IOException {
        final String key = AbstractMongoTestCase.generateKey();
        final String data = AbstractMongoTestCase.generateData();

        insert(key, data, authenticated, servlet);
        find(key, data, authenticated, servlet);
    }

    protected Map<String, String> getConfig(MongoServlet servlet) throws IOException {
        BufferedReader reader = doRequest(MongoServletAction.DUMP_CONFIG, servlet);
        return parseConfig(reader.readLine());
    }

    protected Map<String, String> parseConfig(String response) {
        Map<String, String> res = new HashMap<String, String>();
        String[] values = response.split("[;]");
        for (String value : values) {
            String[] kv = value.split("[=]");
            String k = kv[0];
            String v = kv[1];

            res.put(k, v);
        }

        return res;
    }

    /**
     * This test doesn't do anything more than call to a servlet that ensures
     * that a DB instance was injected.
     *
     * @throws IOException
     */
    protected void insert(String key, String data, boolean authenticated, MongoServlet servlet) throws IOException {
        BufferedReader reader = doRequest(MongoServletAction.INSERT, servlet, "key", key, "data", data, "authenticated", authenticated);

        String line = reader.readLine();
        Map<String, String> res = parseResult(line);
        String result = res.get("result");
        if ("success".equals(result)) {
            return;
        }
        fail("insert failed with " + line);
        return;
    }

    protected void modifyLib(ServerConfiguration sc, String libName) {
        // Update application
        updateApplication(sc, libName);

        //Update mongo elements
        for (MongoElement mon : sc.getMongos()) {
            if ("mongo-lib".equals(mon.getLibraryRef())) {
                mon.setLibraryRef(libName);
            }
        }
    }

    protected abstract void updateApplication(ServerConfiguration sc, String libName);

    static String generateKey() {
        return "key_" + System.nanoTime() + "-" + AbstractMongoTestCase.HOST_NAME;
    }

    static String generateData() {
        return "data_" + System.nanoTime();
    }

    protected boolean sslEnabledOnServer(ServerConfiguration config) {
        boolean sslEnabledOnAServer = false;
        for (MongoElement mongo : config.getMongos()) {
            if (mongo.getSslEnabled() != null && mongo.getSslEnabled())
                sslEnabledOnAServer = true;
        }
        return sslEnabledOnAServer;
    }
}
