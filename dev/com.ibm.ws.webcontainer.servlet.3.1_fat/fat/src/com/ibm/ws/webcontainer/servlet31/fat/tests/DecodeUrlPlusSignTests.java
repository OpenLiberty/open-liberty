/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.WebContainerElement;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for the WebContainer decodeUrlPlusSign property.
 */

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DecodeUrlPlusSignTests {
    private static final Logger LOG = Logger.getLogger(DecodeUrlPlusSignTests.class.getName());

    private static final String DECODE_URL_PLUS_SIGN_APP_NAME = "DecodeUrlPlusSign";

    @Server("servlet31_decodeUrlPlusSignServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, DECODE_URL_PLUS_SIGN_APP_NAME + ".war");

        server.startServer(DecodeUrlPlusSignTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Servlet 5.0: decodeUrlPlusSign = "false" by default.
     * Only run in EE 9 (i.e skip 3.1 NO_MODIFICATION, and 4.0 EE8_FEATURES)
     */
    @Test
    @SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES })
    public void test_DecodeUrlPlusSignDefault_Servlet5() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/DecodeUrlPlusSign", "/plus+sign.html", new String[] { "This file has a plus sign in the name" });
    }

    /**
     * This test case verifies a plus in a URL is decoded to a space when default
     * decodeUrlPlusSign = "true" is in effect.
     */
    @Test
    @SkipForRepeat(EE9_OR_LATER_FEATURES)
    public void test_DecodeUrlPlusSignDefault() throws Exception {
        verifyStringsInResponse(new HttpClient(), "/DecodeUrlPlusSign", "/noplus+sign.html", new String[] { "This file has a space in the name" });
    }

    /**
     * This test case verifies that WC property decodeUrlPlusSign="false" leaves "+" undecoded.
     * For servlet-3.1 and servlet-4.0, the default for decodeUrlPlusSign has been true,
     * which decodes "+" to blank.
     *
     * For servlet-5.0, default decodeUrlPlusSign="false". This test explicitly set it to "true"
     * which decode "+" to a space/blank
     */
    @Test
    public void test_DecodeUrlPlusSign() throws Exception {
        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        // Set the decodeUrlPlusSign property to false.
        WebContainerElement webContainer = configuration.getWebContainer();

        if (JakartaEEAction.isEE9OrLaterActive()) {
            webContainer.setDecodeurlplussign(true);
            LOG.info("Setting decodeUrlPlusSign to true");
        } else {
            webContainer.setDecodeurlplussign(false);
            LOG.info("Setting decodeUrlPlusSign to false");
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(DECODE_URL_PLUS_SIGN_APP_NAME), false);

        LOG.info("Server configuration updated to: " + configuration);

        try {
            if (JakartaEEAction.isEE9OrLaterActive())
                verifyStringsInResponse(new HttpClient(), "/DecodeUrlPlusSign", "/noplus+sign.html", new String[] { "This file has a space in the name" });
            else
                verifyStringsInResponse(new HttpClient(), "/DecodeUrlPlusSign", "/plus+sign.html", new String[] { "This file has a plus sign in the name" });
        } finally {
            // Reset the server.xml.
            server.setMarkToEndOfLog();
            server.restoreServerConfiguration();
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(DECODE_URL_PLUS_SIGN_APP_NAME), false);
        }
    }

    private void verifyStringsInResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        getResponse(client, contextRoot, path, expectedResponseStrings);
    }

    private GetMethod getResponse(HttpClient client, String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        GetMethod get = new GetMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + contextRoot + path);
        int responseCode = client.executeMethod(get);
        String responseBody = get.getResponseBodyAsString();
        LOG.info("Response : " + responseBody);

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, responseCode);

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseBody.contains(expectedResponse));
        }

        return get;
    }
}
