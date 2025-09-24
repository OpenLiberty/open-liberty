/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
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
 * Test FileUpload 1.6's 
 * 
 * <webContainer maxPartHeaderSize="90"/>
 * 
 * Expect an exception in the response. Since this is consider request/data issue, not server issue, no WARNING/ERROR is logged for the exception.
 * If trace is enable, the original message can be seen:
 * 
 * "parseMultipart SizeLimitExceededException [org.apache.commons.fileupload.FileUploadBase$SizeLimitExceededException: Header section has more than 90 bytes (maybe it is not properly terminated)]
 * 
 * Default value test is not needed as other fileupload tests should be qualified for the default value case.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCFileUpLoadPartHeaderSizeMaxPropertyTest {

    private static final Logger LOG = Logger.getLogger(WCFileUpLoadPartHeaderSizeMaxPropertyTest.class.getName());
    private static final String APP_NAME = "FileUploadFileCountMaxTest";

    @Server("servlet40_FileUploadPartHeaderSizeMaxProperty")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : " + APP_NAME);
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "headersizemax");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCFileUpLoadPartHeaderSizeMaxPropertyTest.class.getSimpleName() + ".log");
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
     * Test <webContainer maxPartHeaderSize="90"/>
     * Send in 200 files/parts (this is not matter as it is the part body which is not a concern in this test
     * 
     * Verify: Response with message "SRVE8022E: The request is too large"
     */
    @Test
    public void test_FileUpload_partHeaderSizeWithProperty() throws Exception {
        int totalFiles = 200;
        String expectedResponse = "SRVE8022E: The request is too large";
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

        eBuilder.setCharset(Charset.forName("ISO-8859-1")); //it is part of the "part header"
        for (int i = 0; i < parameterNum; i++) {
            eBuilder.addTextBody(name + i, value + i);
        }
        return eBuilder;
    }
}
