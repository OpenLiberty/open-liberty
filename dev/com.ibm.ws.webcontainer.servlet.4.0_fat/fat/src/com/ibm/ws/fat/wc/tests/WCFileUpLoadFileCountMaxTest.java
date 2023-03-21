/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test FileUpload 1.5 with default value of fileCountMax
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCFileUpLoadFileCountMaxTest {

    private static final Logger LOG = Logger.getLogger(WCFileUpLoadFileCountMaxTest.class.getName());
    private static final String APP_NAME = "FileUploadFileCountMaxTest";

    @Server("servlet40_FileUploadFileCountMax")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : " + APP_NAME);
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "filecountmax");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCFileUpLoadFileCountMaxTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test default setting by set 5000 fields/files in the upload.
     *
     */
    @Test
    public void testFileUpload_default5000Files() throws Exception {
        int totalFiles = 5000;
        String expectedResponse = "Test Complete. Received parts size [5000]";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/GetPartName";

        MultipartEntityBuilder builder = createMultiPartData(totalFiles);
        HttpPost postRequest = new HttpPost(url);
        postRequest.setEntity(builder.build());

        LOG.info("\nSending [" + totalFiles + "] multipart Post Request to [" + url + "");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(postRequest)) {
                LOG.info("Request result: " + response.getReasonPhrase());
                LOG.info("Status code: " + response.getCode());
                LOG.info("Expected response contains: [" + expectedResponse + "]");

                String content = EntityUtils.toString(response.getEntity());
                LOG.info("Actual response: \n" + content);
                EntityUtils.consume(response.getEntity());

                assertTrue("Response did not contain expected response: [" + expectedResponse + "]", content.contains(expectedResponse));
            }
        }
    }

    /**
     * Test Exception by sending 5001 over max (5k) file count in the upload.
     * The servlet will catch and report back the Exception in its response.
     *
     */
    @Test
    public void testFileUpload_overMaxFileCount() throws Exception {
        int totalFiles = 5001;
        String expectedResponse = "CWWWC0006E";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/GetPartName";

        MultipartEntityBuilder builder = createMultiPartData(totalFiles);
        HttpPost postRequest = new HttpPost(url);
        postRequest.setEntity(builder.build());

        LOG.info("\nSending [" + totalFiles + "] multipart Post Request to [" + url + "");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(postRequest)) {
                LOG.info("Request result: " + response.getReasonPhrase());
                LOG.info("Status code: " + response.getCode());
                LOG.info("Expected response contains: [" + expectedResponse + "]");

                String content = EntityUtils.toString(response.getEntity());
                LOG.info("Actual response: \n" + content);
                EntityUtils.consume(response.getEntity());

                assertTrue("Response did not contain expected response: [" + expectedResponse + "]", content.contains(expectedResponse));
            }
        }
    }

    private MultipartEntityBuilder createMultiPartData(int parameterNum) {
        LOG.info("====== createMultiPartData creating [" + parameterNum + "] parts ======");
        MultipartEntityBuilder eBuilder = MultipartEntityBuilder.create();
        String name = "Name_";
        String value = "Value_";

        for (int i = 0; i < parameterNum; i++) {
            eBuilder.addTextBody(name + i, value + i);
        }
        return eBuilder;
    }
}
