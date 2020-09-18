/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import java.net.URL;
import java.util.Set;
import java.util.logging.Logger;

import componenttest.topology.impl.LibertyServer;

/**
 * Utilities for grpc_fat. Taken from com.ibm.ws.jsf23.fat.JSFUtils
 */
public class GrpcTestUtils {

    protected static final Class<?> c = GrpcTestUtils.class;

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server      - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path        - Additional path information for the request.
     * @return - A fully formed URL.
     * @throws Exception
     */
    public static URL createHttpUrl(LibertyServer server, String contextRoot, String path) throws Exception {
        return new URL(createHttpUrlString(server, contextRoot, path));
    }

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server      - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path        - Additional path information for the request.
     * @return - A fully formed URL string.
     * @throws Exception
     */
    public static String createHttpUrlString(LibertyServer server, String contextRoot, String path) {

        StringBuilder sb = new StringBuilder();
        sb.append("http://")
                        .append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append("/")
                        .append(contextRoot)
                        .append("/")
                        .append(path);

        return sb.toString();
    }

    /**
     * This method is used to set the server.xml
     */
    public static String setServerConfiguration(LibertyServer server,
                                                String originalServerXML,
                                                String serverXML,
                                                Set<String> appName,
                                                Logger logger) throws Exception {
        System.out.println("Entered set server config with xml " + serverXML);
        if (originalServerXML == null || !originalServerXML.equals(serverXML)) {
            server.setMarkToEndOfLog();
            // Update server.xml
            logger.info("setServerConfiguration setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            server.waitForStringInLog("CWWKG0017I");
            if (appName != null) {
                server.waitForConfigUpdateInLogUsingMark(appName);
            }
        }
        return serverXML;
    }
}
