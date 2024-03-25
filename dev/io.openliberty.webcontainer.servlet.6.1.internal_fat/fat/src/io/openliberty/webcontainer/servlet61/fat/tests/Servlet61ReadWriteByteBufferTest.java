/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
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
 * Test ServletInputStream.read(ByteBuffer) and ServletOutputStream.write(ByteBuffer)
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61ReadWriteByteBufferTest {
    private static final Logger LOG = Logger.getLogger(Servlet61ReadWriteByteBufferTest.class.getName());
    private static final String TEST_APP_NAME = "ReadWriteByteBuffer";
    private static final String POST_DATA = "ABCDEFGHIJKLMNOPQRSTUVWXZY_1234567890@0987654321";

    @Server("servlet61_ReadWriteByteBuffer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_ReadWriteByteBuffer.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "readwritebytebuffer.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWWC0010E:.*"); //Write Null ByteBuffer
        server.addIgnoredErrors(expectedErrors);

        server.startServer(Servlet61ReadWriteByteBufferTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Test ServletInputStream.read(ByteBuffer) from POST request
     * then response back the read data using ServletOutputStream.write(ByteBuffer)
     */
    @Test
    public void test_Stream_ReadWriteByteBuffer() throws Exception {
        LOG.info("====== <test_Stream_ReadWriteByteBuffer> ======");
        runTest("testReadWriteByteBuffer", POST_DATA);
    }

    /*
     * Test the ServletOutputStream.write(ByteBuffer) with Null ByteBuffer
     */

    @Test
    public void test_Stream_WriteNullByteBuffer() throws Exception {
        LOG.info("====== <test_Stream_WriteNullByteBuffer> ======");
        runTest("testWriteNullByteBuffer", null);
    }

    /*
     * Test the ServletInputStream.read(ByteBuffer) with Null ByteBuffer
     */

    @Test
    public void test_Stream_ReadNullByteBuffer() throws Exception {
        LOG.info("====== <test_Stream_ReadNullByteBuffer> ======");
        runTest("testReadNullByteBuffer", null);
    }
    /**
     * @param testToRun - name of the test/method for the servlet to execute
     * @param postData - request POST data; can also be null
     */
    private void runTest(String testToRun, String postData) throws Exception {
        LOG.info("====== <runTest> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestReadWriteByteBuffer";
        HttpPost httpMethod = new HttpPost(url);

        httpMethod.addHeader("runTest", testToRun);

        if (postData != null)
            httpMethod.setEntity(new StringEntity(postData));

        LOG.info("Sending [" + url + "]");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                switch (testToRun) {
                    case "testReadWriteByteBuffer" :
                        assertTrue("Expecting response data ["+ POST_DATA + "] but found response [" + responseText + "]", responseText.trim().equals(POST_DATA));
                        break;
                    case "testWriteNullByteBuffer" :
                        assertTrue("Expecting PASS response but found [" + responseText + "]", responseText.contains("PASS"));
                        break;
                    case "testReadNullByteBuffer" :
                        assertTrue("Expecting PASS response but found [" + responseText + "]", responseText.contains("PASS"));
                        break;
                }
            }
        }
    }
}
