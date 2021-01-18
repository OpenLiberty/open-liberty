/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.servers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.TestServer;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ServerFileUtils;

public class ServerBootstrapUtils {
    private final Class<?> thisClass = ServerBootstrapUtils.class;
    ServerFileUtils serverFileUtils = new ServerFileUtils();
    CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();
    CommonMessageTools msgUtils = new CommonMessageTools();
    boolean log = true;
    boolean doNotLog = false;

    /**
     * Writes the specified bootstrap property and value to the provided server's bootstrap.properties file.
     */
    public void writeBootstrapProperty(TestServer server, String propKey, String propValue) throws Exception {
        writeBootstrapProperty(server.getServer(), propKey, propValue);
    }
    
    /**
     * Writes the specified bootstrap property and value to the provided server's bootstrap.properties file.
     */
    public void writeBootstrapProperty(LibertyServer server, String propKey, String propValue) throws Exception {
        String bootProps = getBootstrapPropertiesFilePath(server);
        appendBootstrapPropertyToFile(bootProps, propKey, propValue, doNotLog);
    }

    /**
     * Writes each of the specified bootstrap properties and values to the provided server's bootstrap.properties file.
     */
    public void writeBootstrapProperties(LibertyServer server, Map<String, String> miscParms) throws Exception {
        String thisMethod = "writeBootstrapProperties";
        loggingUtils.printMethodName(thisMethod);

        if (miscParms == null) {
            return;
        }
        String bootPropFilePath = getBootstrapPropertiesFilePath(server);
        for (Map.Entry<String, String> entry : miscParms.entrySet()) {
            appendBootstrapPropertyToFile(bootPropFilePath, entry.getKey(), entry.getValue(), doNotLog);
        }
    }
    

    /**
     * Writes each of the specified bootstrap properties and values to the provided server's bootstrap.properties file.
     */
    public void writeBootstrapProperties(TestServer server, Map<String, String> miscParms) throws Exception {
        writeBootstrapProperties(server.getServer(), miscParms);
    }
    
    /**
     * Writes each of the specified bootstrap properties and values to the provided server's bootstrap.properties file.
     * Please use this method when including passwords and or user information.
     */
    public void writeSecureBootstrapProperties(TestServer server, Map<String, String> miscParms) throws Exception {
        String thisMethod = "addBootstrapParms";
        msgUtils.printMethodName(thisMethod);

        if (miscParms == null) {
            return;
        }
        String bootPropFilePath = server.getBootstrapPropertiesFilePath();
        for (Map.Entry<String, String> entry : miscParms.entrySet()) {
            appendBootstrapPropertyToFile(bootPropFilePath, entry.getKey(), entry.getValue(), log);
        }
    }

    private void appendBootstrapPropertyToFile(String propertyFilePath, String propKey, String propValue, boolean log) throws IOException {
        String thisMethod = "appendBootstrapPropertyToFile";
        String newPropAndValue = createBootstrapPropertyString(propKey, propValue);
        Log.info(thisClass, thisMethod, "Adding " + newPropAndValue + " to bootstrap.properties");

        FileWriter writer = new FileWriter(propertyFilePath, true);
        writer.append(System.getProperty("line.separator"));
        writer.append(createBootstrapPropertyString(propKey, propValue));
        writer.append(System.getProperty("line.separator"));
        writer.close();
    }

    public String createBootstrapPropertyString(String key, String value) {
        return key + "=" + value;
    }

    public String getBootstrapPropertiesFilePath(LibertyServer server) throws Exception {
        String thisMethod = "getBootstrapPropertiesFilePath";
        String bootProps = serverFileUtils.getServerFileLoc(server) + "/bootstrap.properties";
        Log.info(thisClass, thisMethod, "Bootstrap property file path: " + bootProps);
        return bootProps;
    }
}
