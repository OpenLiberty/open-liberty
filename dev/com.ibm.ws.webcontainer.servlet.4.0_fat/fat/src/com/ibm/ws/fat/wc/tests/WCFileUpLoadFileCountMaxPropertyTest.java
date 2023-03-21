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
 * Test FileUpload 1.5's property fileCountMax=-1 which is unlimited
 * Send more than 5k params in body and verify working
 * Send more than 10k params which exceeding the maxparamperrequest's default limit of 10000 and cause exception.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCFileUpLoadFileCountMaxPropertyTest {

    private static final Logger LOG = Logger.getLogger(WCFileUpLoadFileCountMaxPropertyTest.class.getName());
    private static final String APP_NAME = "FileUploadFileCountMaxTest";

    @Server("servlet40_FileUploadFileCountMaxProperty")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : " + APP_NAME);
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "filecountmax");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCFileUpLoadFileCountMaxPropertyTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown : stop server");

        if (server != null && server.isStarted()) {
            //Expecting the Error during the param parsing
            server.stopServer("SRVE0133E");
        }
    }

    /**
     * Test webContainer maxFileCount=-1
     * Send in 5555 files/parts
     * Verify no Exception.
     */
    @Test
    public void testFileUpload_overMaxFileCountWithProperty() throws Exception {
        int totalFiles = 5555;
        String expectedResponse = "Test Complete. Received parts size [" + totalFiles + "]";
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

                assertTrue("Response did not contain expected response: [" + expectedResponse + "] ; Actual response [" + content + "]", content.contains(expectedResponse));
            }
        }
    }

    /**
     * Test webContainer maxFileCount=-1
     * Send in 10,001 files/parts. This will cause exception because it exceeds maxparamperrequest default limit of 10,000
     * Expecting SRVE0133E
     */
    @Test
    public void testFileUpload_over10KWithProperty() throws Exception {
        int totalFiles = 10001;
        String expectedResponse = "SRVE0325E: Exceeding maximum parameters allowed per request";
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

                assertTrue("Response did not contain expected response: [" + expectedResponse + "] ; Actual response [" + content + "]", content.contains(expectedResponse));
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
