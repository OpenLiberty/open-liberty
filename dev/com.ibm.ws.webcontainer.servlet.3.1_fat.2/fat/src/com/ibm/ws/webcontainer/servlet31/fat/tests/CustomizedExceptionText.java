/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.WebContainerElement;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils; 

@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class CustomizedExceptionText {

    private static final Logger LOGGER = Logger.getLogger(CustomizedExceptionText.class.getName());
    private static final String CUSTOM_ERROR_APP_NAME = "CustomErrorTestApp";

    @Server("servlet31_customizedExceptionText")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive customErrorApp = ShrinkHelper.buildDefaultApp(CUSTOM_ERROR_APP_NAME + ".war","com.ibm.ws.webcontainer.servlet_31_fat.customerrortest.war.test.servlets");
        customErrorApp = (WebArchive) ShrinkHelper.addDirectory(customErrorApp,"test-applications/" + CUSTOM_ERROR_APP_NAME + ".war/resources");
        ShrinkHelper.exportDropinAppToServer(server, customErrorApp);

        if (!server.isStarted()) {
            server.startServer();
            server.waitForStringInLogUsingMark("CWWKT0016I.*" + CUSTOM_ERROR_APP_NAME + ".*");
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void test403CustomMessage() throws Throwable {
        LOGGER.info("Starting test403CustomMessage");
        updateCustomizedExceptionText("Custom 403 Forbidden error message");

        URL requestURL = HttpUtils.createURL(server, "/" + CUSTOM_ERROR_APP_NAME + "/forbidden");
        String result = checkRequest(requestURL.toString(), 403, "Custom 403 Forbidden error message", "SRVE0218E");

        assertTrue("Custom 403 text not found in response:\n" + result, result.contains("PASS"));
    }

    @Test
    public void test500CustomMessage() throws Throwable {
        LOGGER.info("Starting test500CustomMessage");
        updateCustomizedExceptionText("Custom 500 internal server error message");

        URL requestURL = HttpUtils.createURL(server, "/" + CUSTOM_ERROR_APP_NAME + "/error");
        String result = checkRequest(requestURL.toString(), 500, "Custom 500 internal server error message", "SRVE0232E");

        assertTrue("Custom 500 text not found in response:\n" + result, result.contains("PASS"));
    }

    private String checkRequest(String URL, int responseCode, String expectedResText, String notExpectedResText)throws Throwable {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebResponse resp = wc.getResponse(URL);

        int code = resp.getResponseCode();
        String body = resp.getText();

        StringBuilder result = new StringBuilder();
        result.append("Checking Response Code: Expected [" + responseCode + "], actual [" + code + "]. ");
        result.append(code == responseCode ? "PASS\n" : "FAIL\n");

        if (!expectedResText.equals("SKIP")) {
            result.append("Checking Expected Response text: ");
            result.append(body.contains(expectedResText) ? "PASS\n" : "FAIL\n");
        }

        if (!notExpectedResText.equals("SKIP")) {
            result.append("Checking NOT Expected Response text: ");
            result.append(body.contains(notExpectedResText) ? "FAIL\n" : "PASS\n");
        }

        LOGGER.log(Level.INFO, "checkRequest Body:\n" + body);
        return result.toString();
    }

    /**
     * Method to update <webContainer displayCustomizedExceptionText="..." dynamically
     */
    private void updateCustomizedExceptionText(String customMessage) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        WebContainerElement webContainer = config.getWebContainer();

        boolean serverXMLChanged = false;
        String currentValue = webContainer.getDisplayCustomizedExceptionText();

        if (currentValue == null || !currentValue.equals(customMessage)) {
            LOGGER.info("Updating displayCustomizedExceptionText from [" + currentValue + "] to [" + customMessage + "]");
            webContainer.setDisplayCustomizedExceptionText(customMessage);
            serverXMLChanged = true;
        } else {
            LOGGER.info("No update required value already set to [" + currentValue + "]");
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);

        // Wait for a config update if we actually changed something
        if (serverXMLChanged) {
            try {
                server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(),false,"CWWKT0016I:.*CustomErrorTestApp.*");
                LOGGER.info("Config update message detected after applying customized exception text.");
            } 
            catch (Exception e) {
                LOGGER.warning("No explicit config update detected");
            }
        }
    }
}
