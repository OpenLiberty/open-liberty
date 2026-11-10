/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.fat;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@Ignore("This is not a test class")
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class CommonTestHelper {

    private final static Class<?> thisClass = CommonTestHelper.class;

    private final String hostname;
    private final String port;

    public CommonTestHelper(String hostname, String port) {
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Construct an HttpURLConnection to the authentication servlet given the parameters
     * specified.
     *
     * @param userName
     * @param password
     * @param jaasEntryName
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    public String performAction(String action, String userName, String password, String jaasEntryName) throws Exception {
        String fullResponse = "";
        HttpURLConnection connection = null;

        try {
            URL url = createURL(action, userName, password, jaasEntryName);
            Log.info(thisClass, "performAction", "URL=" + url);
            connection = getHttpConnection(url);
            fullResponse = getFullResponse(connection);

            connection.disconnect();
            connection = null;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return fullResponse;
    }

    private URL createURL(String action, String userName, String password, String jaasEntryName) throws MalformedURLException {
        return new URL("http://" + hostname + ":" + port
                       + "/authenticationService?action=" + action
                       + "&user=" + userName
                       + "&password=" + password
                       + "&jaasEntryName=" + jaasEntryName);
    }

    /**
     * Construct the HttpURLConnection with normal default values.
     *
     * @param url
     * @return
     * @throws IOException
     * @throws ProtocolException
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(30000);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    private String getFullResponse(HttpURLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line).append(System.getProperty("line.separator"));
            line = br.readLine();
        }
        line = sb.toString();
        Log.info(thisClass, "getFullResponse", line);
        return line;
    }

    /**
     * This method is used to set the server.xml
     */
    public void setServerConfiguration(LibertyServer server, String serverXML) throws Exception {
        // Use server.xml with non-default config options defined
        Log.info(thisClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
        server.setServerConfigurationFile("/" + serverXML);
        Log.info(thisClass, "setServerConfiguration", "waitForStringInLog: CWWKG0017I: The server configuration was successfully updated");
        server.waitForStringInLogUsingMark("CWWKG0017I", 60000);
    }

    public void stopServerConfiguration(LibertyServer server, String serverXML) throws Exception {
        server.stopServer();

        // Use server.xml with non-default config options defined
        Log.info(thisClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
        server.setServerConfigurationFile("/" + serverXML);
    }

    public void verifyServerStarted(LibertyServer server) {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
    }

    public void addJVMOptionPropeties(LibertyServer server, HashMap<String, String> props) throws Exception {
        String thisMethod = "addJVMOptionPropeties";

        if (props == null) {
            Log.info(thisClass, thisMethod, "null properties map entered, Not adding to jvm.options");
            return;
        }

        //Get current jvm options, and add new properties
        Map<String, String> optionsMap = server.getJvmOptionsAsMap();
        optionsMap.putAll(props);
        server.setJvmOptions(optionsMap);
    }

    public void addJVMOptionProperty(LibertyServer server, String key, String value) throws Exception {
        String thisMethod = "addJVMOptionProperty";

        if (key == null) {
            Log.info(thisClass, thisMethod, "null key entered, Not adding to jvm.options");
            return;
        }

        if (value == null) {
            Log.info(thisClass, thisMethod, "null value entered, Not adding to jvm.options");
            return;
        }

        HashMap<String, String> props = new HashMap<String, String>();
        props.put(key, value);
        addJVMOptionPropeties(server, props);
    }

    public void removeJVMOptionProperties(LibertyServer server, String... keys) throws Exception {
        String thisMethod = "removeJVMOptionProperties";

        if (keys == null) {
            Log.info(thisClass, thisMethod, "null key(s) entered");
            return;
        }

        //Get current jvm options, and remove key if needed
        Map<String, String> optionsMap = server.getJvmOptionsAsMap();
        for (String key : keys) {
            Log.info(thisClass, thisMethod, "Removing: " + key + " from jvm.options");
            optionsMap.remove(key);
        }
        server.setJvmOptions(optionsMap);
    }

    public void removeJVMOptionProperty(LibertyServer server, String key) throws Exception {
        removeJVMOptionProperties(server, key);
    }
}
