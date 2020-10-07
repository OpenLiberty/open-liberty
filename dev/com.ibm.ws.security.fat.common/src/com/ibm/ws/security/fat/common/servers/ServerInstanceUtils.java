/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.servers;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.MessageConstants;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

public class ServerInstanceUtils {
    private final static Class<?> thisClass = ServerInstanceUtils.class;
    //    ServerFileUtils serverFileUtils = new ServerFileUtils();
    //    CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    public static void addHostNameAndAddrToBootstrap(LibertyServer server) {
        String thisMethod = "addHostNameAndAddrToBootstrap";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String serverHostName = addr.getHostName();
            String serverHostIp = addr.toString().split("/")[1];

            bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, serverHostName);
            bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, serverHostIp);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, thisMethod, "Setup failed to add host info to bootstrap.properties");
        }
    }

    // Special case SSL ready checker - for test classes that don't enable/use SSL from server
    // startup.  This method will check entire message log for the ssl inited msg (instead of checking
    // from the last mark)
    public static void waitForSSLMsg(LibertyServer server) {

        waitForSSLMsg(server, System.currentTimeMillis());
    }

    private static void waitForSSLMsg(LibertyServer server, long startTime) {

        String methodName = "waitForSSLMsg";
        try {

            // wait for up to 2 minutes
            if (System.currentTimeMillis() - startTime > (2 * 60 * 1000)) {
                Log.info(thisClass, methodName, "Timed out searching for SSL ready message - test will probably fail");
                return;
            }
            // if nothing is found, sleep and request another try
            List<String> wasFound = LibertyFileManager.findStringsInFile(".*" + MessageConstants.CWWKO0219I_SSL_CHANNEL_READY + ".*", server.getDefaultLogFile());
            if (wasFound == null || wasFound.isEmpty()) {
                Thread.sleep(5 * 1000);
                waitForSSLMsg(server, startTime);
            } else {
                for (String msg : wasFound) {
                    Log.info(thisClass, "waitForSSLMsg", "Found SSL MSG: " + msg);
                }
                Log.info(thisClass, methodName, "Found SSL ready message");
                return;
            }
        } catch (Exception e) {
            Log.info(thisClass, "waitForSSLMsg", "Something went wrong while checking for the SSL ready msg: " + e.getMessage());
        }
    }

    /**
     * Update/Set config variables for a server and push the updates to the server.
     * Method waits for server to update or indicate that no update in needed
     *
     * @param server
     *            - ref to server that will be updated
     * @param valuesToSet
     *            - a map of the variables and their values to set
     * @throws Exception
     */
    public static void updateServerSettings(LibertyServer server, Map<String, String> valuesToSet) throws Exception {

        String thisMethod = "updateServerSettings";
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> configVars = config.getVariables();

        for (Variable variableEntry : configVars) {
            Log.info(thisClass, thisMethod, "Already set configVar: " + variableEntry.getName() + " configVarValue: " + variableEntry.getValue());
        }

        for (Entry<String, String> variableEntry : valuesToSet.entrySet()) {
            addOrUpdateConfigVariable(configVars, variableEntry.getKey(), variableEntry.getValue());
        }

        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Update a servers variable map with the key/value passed in.
     *
     * @param vars
     *            - map of existing variables
     * @param name
     *            - the key to add/update
     * @param value
     *            - the value for the key specified
     */
    protected static void addOrUpdateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {

        Log.info(thisClass, "addOrUpdateConfigVariable", "Setting/resetting var: " + name + " value: " + value);
        Variable var = vars.getBy("name", name);
        if (var == null) {
            vars.add(new Variable(name, value));
        } else {
            var.setValue(value);
        }
    }

    // using this method to update multiple variables will result in multiple config updates
    // if you need to update multiple values, call updateServerSettings directly will all updates
    /**
     * Add/update one variable in a server config
     *
     * @param server
     *            - the server to update
     * @param key
     *            - The variable's key nane
     * @param value
     *            - the variable's value
     * @throws Exception
     */
    public static void setOneVar(LibertyServer server, String key, String value) throws Exception {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(key, value);
        updateServerSettings(server, vars);
    }
}
