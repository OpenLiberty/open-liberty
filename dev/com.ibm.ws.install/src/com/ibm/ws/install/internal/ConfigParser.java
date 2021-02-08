/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class ConfigParser {

    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    private final Path serverXML;
    private final Map<String, String> varMap;
    private final String wlpDir = Utils.getInstallDir().toString();
    private final String wlpUsrDir = Utils.getUserDir().toString();
    private final String userExtensionDir = wlpUsrDir + "/extension";
    private final String sharedAppDir = wlpUsrDir + "/shared/apps";
    private final String sharedConfigDir = wlpUsrDir + "/shared/config";
    private final String sharedResourceDir = wlpUsrDir + "/shared/resources";
    private final String sharedStackGroupsDir = wlpUsrDir + "/shared/stackGroups";
    private final String serverOutputDir = Utils.getOutputDir().toString();

    public ConfigParser(Path serverXML, Map<String, String> varMap) throws IOException {
        this.serverXML = serverXML;
        this.varMap = populateMap(serverXML, varMap);
    }

    /**
     * parse through environment variables, bootstrap.properties, and make use of the server.xml defined variables
     * to create a singular map for all variables
     *
     * @param serverXML a server.xml under the wlp.user.dir
     * @param vMap      the map of variables we wish to populate
     * @return
     * @throws IOException
     */
    private Map<String, String> populateMap(Path serverXML, Map<String, String> vmap) throws IOException {
        Map<String, String> result = new HashMap<String, String>();
        String serverName = serverXML.getParent().getFileName().toString();
        //parse environment variables first
        result.putAll(System.getenv());
        //parse basic wlp based variables
        result.putAll(generateWlpBasedMap(serverName));
        //parse bootstrap.properties (we look at the bootstrap file under the server in serverXML's parent path)
        result.putAll(parseBootstrapProps(serverName));
        //add contents of vmap (which contains server.xml variables) to result
        result.putAll(vmap);

        return result;
    }

    /**
     * parse bootstrap.properties from under server name of the serverXML given
     *
     * @param serverName server name
     * @return
     * @throws IOException
     */
    private Map<String, String> parseBootstrapProps(String serverName) throws IOException {
        Map<String, String> resultMap = new HashMap<String, String>();
        String bootstrapProps = wlpUsrDir + "/servers/" + serverName + "/bootstrap.properties";
        File bootstrapPropsFile = new File(bootstrapProps);
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(bootstrapPropsFile)) {
            properties.load(input);
        } catch (FileNotFoundException e) {
            //bootstrap properties file was not created/found
            logger.log(Level.FINE, "file not found for bootstrap.properties under server: " + serverName);
        }
        Set<Object> keys = properties.keySet();
        for (Object k : keys) {
            String kstr = (String) k;
            resultMap.put(kstr, properties.getProperty(kstr));
        }
        return resultMap;
    }

    /**
     * Generate variables related to basic wlp based directories
     *
     * @param serverName server name
     * @return
     */
    private Map<String, String> generateWlpBasedMap(String serverName) {
        Map<String, String> resultMap = new HashMap<String, String>();
        resultMap.put("wlp.install.dir", wlpDir);
        resultMap.put("wlp.user.dir", wlpUsrDir);
        resultMap.put("usr.extension.dir", userExtensionDir);
        resultMap.put("shared.app.dir", sharedAppDir);
        resultMap.put("shared.config.dir", sharedConfigDir);
        resultMap.put("shared.resource.dir", sharedResourceDir);
        resultMap.put("shared.stackgroup.dir", sharedStackGroupsDir);
        resultMap.put("server.config.dir", wlpUsrDir + "/servers/" + serverName);
        resultMap.put("server.output.dir", serverOutputDir + "/" + serverName);
        return resultMap;
    }

    /**
     * Recursively resolve path until all variables are dealt with
     *
     * @param path path we wish to resolve
     * @return
     */
    public String resolvePath(String path) {
        String result = "/";
        if (InstallUtils.isWindows) {
            result = "";
        }
        String[] splitPath = path.split("/"); //\\$\\{.*\\}
        for (String s : splitPath) {
            String accumulator = "";
            if (s.contains("$")) {
                accumulator += resolveSubString(s) + "/";
            } else {
                if (s.contains(".xml")) {
                    accumulator += s;
                } else {
                    accumulator += s + "/";
                }
            }
            result += accumulator;
        }
        if (result.contains("${")) {
            result = resolvePath(result);
        }
        if (InstallUtils.isWindows) {
            result = result.replaceAll("\\/", "\\\\");
        }
        return result;
    }

    private String resolveSubString(String substr) {
        String pattern = "(.*)\\$\\{(.*)\\}(.*)";
        String result = substr;
        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(substr);

        if (m.find()) {
            String resolvedStr = m.group(2);
            if (resolvedStr.contains("env.")) {
                resolvedStr = System.getenv().get(resolvedStr.replace("env.", ""));
            } else {
                resolvedStr = this.varMap.get(resolvedStr);
            }
            result = m.group(1) + resolvedStr + m.group(3);
        }

        return result;
    }

}
