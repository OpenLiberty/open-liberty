/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.RemoteFile;

import org.junit.Assert;

import componenttest.topology.impl.LibertyServer;
import io.grpc.ManagedChannel;

/**
 * Utilities for grpc_fat. Taken from com.ibm.ws.jsf23.fat.JSFUtils
 */
public class GrpcTestUtils {

    protected static final Class<?> c = GrpcTestUtils.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    private static final int CHANNEL_SHUTDOWN_TIMEOUT = 2 * 1000; // 2 seconds

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
     * This method is used to set the server.xml; validation is enabled
     */
    public static String setServerConfiguration(LibertyServer server,
                                                String originalServerXML,
                                                String serverXML,
                                                Set<String> appName,
                                                Logger logger) throws Exception {
        return setServerConfiguration(server, originalServerXML, serverXML, appName, logger, false);
    }

    /**
     * This method is used to set the server.xml
     */
    public static String setServerConfiguration(LibertyServer server,
                                                String originalServerXML,
                                                String serverXML,
                                                Set<String> appName,
                                                Logger logger,
                                                boolean skipValidation) throws Exception {
        logger.info("Entered set server config with xml " + serverXML);
        if (originalServerXML == null || !originalServerXML.equals(serverXML)) {
            server.setMarkToEndOfLog();
            // Update server.xml
            logger.info("setServerConfiguration setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            if (!skipValidation) {
                if (appName != null) {
                    server.waitForConfigUpdateInLogUsingMark(appName);
                } else {
                    server.waitForStringInLog("CWWKG001[7-8]I");
                }
            }
        }
        return serverXML;
    }

    /**
     * This method is used to set the server.xml
     */
    public static void setServerConfiguration(LibertyServer server,
                                              RemoteFile serverXML,
                                              Set<String> appName,
                                              Logger logger) throws Exception {
        logger.info("Entered set server config with xml " + serverXML);
        if (serverXML != null && serverXML.exists()) {
            server.setMarkToEndOfLog();
            // Update server.xml
            logger.info("setServerConfiguration setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.getServerConfigurationFile().copyFromSource(serverXML);
            if (appName != null) {
                server.waitForConfigUpdateInLogUsingMark(appName);
            } else {
                server.waitForStringInLog("CWWKG001[7-8]I");
            }
        }
    }

    /**
     * Terminate a channel and wait for it to stop; if the stop does not complete within 
     * CHANNEL_SHUTDOWN_TIMEOUT then an Assert failure will be thrown
     *
     * @param channel
     */
    public static void stopGrpcService(ManagedChannel channel) {
        channel.shutdownNow();
        boolean terminated = false;
        try {
            terminated = channel.awaitTermination(CHANNEL_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.info("stopGrpcService() : awaitTermination() for chanel " + channel + " was interrupted: " + e);
        }
        Assert.assertTrue("channel termination failed for " + channel, terminated);
        if (terminated) {
            LOG.info("stopGrpcService(): " + channel + " has been closed");
        }
    }
}
