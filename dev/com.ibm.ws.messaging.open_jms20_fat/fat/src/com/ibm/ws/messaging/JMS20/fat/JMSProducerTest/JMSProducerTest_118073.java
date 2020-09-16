/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat.JMSProducerTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TestRule;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
public class JMSProducerTest_118073 {

    private static LibertyServer server = LibertyServerFactory
                    .getLibertyServer("TestServer");

    private static LibertyServer server1 = LibertyServerFactory
                    .getLibertyServer("TestServer1");

    private static final int PORT = server.getHttpDefaultPort();
    private static final String HOST = server.getHostname();

    private static boolean testResult = false;

    private boolean runInServlet(String test) throws IOException {
        boolean result = false;

        URL url = new URL("http://" + HOST + ":" + PORT + "/JMSProducer?test="
                          + test);
        System.out.println("The Servlet URL is : " + url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String sep = System.lineSeparator();
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                org.junit.Assert.fail("Missing success message in output. "
                                      + lines);
                result = false;
            }

            else
                result = true;

            return result;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void testConfigFileChange() throws Exception {

        server.copyFileToLibertyInstallRoot("lib/features",
                                            "features/testjmsinternals-1.0.mf");
        server1.copyFileToLibertyInstallRoot("lib/features",
                                             "features/testjmsinternals-1.0.mf");

        server.setServerConfigurationFile("JMSContext.xml");
        server1.setServerConfigurationFile("TestServer1.xml");
        server.startServer("JMSProducerTestClient_118073.log");
        server1.startServer("JMSProducerTestServer_118073.log");

    }

    // 118073_1_1 Clears any message properties set on this JMSProducer
    // Bindings and Security Off
    ////@Test
    public void testClearProperties_B_SecOff() throws Exception {

        testResult = runInServlet("testClearProperties_B_SecOff");

        assertTrue("Test testClearProperties_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    ////@Test
    public void testClearProperties_TCP_SecOff() throws Exception {

        testResult = runInServlet("testClearProperties_TCP_SecOff");

        assertTrue("Test testClearProperties_TCP_SecOff failed", testResult);

    }

    // 118073_1_2 Test invoking clearProperties() when there are no properties
    // set
    // 118073_1_3 Test invoking clearProperties() soon after clearProperties()
    // have been invoked

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testClearProperties_Notset_B_SecOff() throws Exception {

        testResult = runInServlet("testClearProperties_Notset_B_SecOff");

        assertTrue("Test testClearProperties_Notset_B_SecOff failed", testResult);

    }

    // TCP/IP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testClearProperties_Notset_TCP_SecOff() throws Exception {

        testResult = runInServlet("testClearProperties_Notset_TCP_SecOff");

        assertTrue("Test testClearProperties_Notset_TCP_SecOff failed", testResult);

    }

    // 118073_2_2 Test by passing name as empty string

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testPropertyExists_emptyString_B_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_emptyString_B_SecOff");

        assertTrue("Test testPropertyExists_emptyString_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testPropertyExists_emptyString_TCP_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_emptyString_TCP_SecOff");

        assertTrue("Test testPropertyExists_emptyString_TCP_SecOff failed", testResult);
    }

    // 118073_2_3 Test by passing name as null
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testPropertyExists_null_B_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_null_B_SecOff");

        assertTrue("Test testPropertyExists_null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testPropertyExists_null_TCP_SecOff() throws Exception {
        testResult = runInServlet("testPropertyExists_null_TCP_SecOff");

        assertTrue("Test testPropertyExists_null_TCP_SecOff failed", testResult);

    }

    // 118073_7 JMSProducer setDeliveryMode(int deliveryMode)
    // 118073_7_1 Specifies the delivery mode of messages that are sent using
    // this JMSProducer

    // Bindings and Security off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDeliveryMode_B_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;

        runInServlet("testSetDeliveryMode_persistent_B_SecOff");

        System.out.println("Restarting the servers for checking the persistence of messages");
        server.stopServer();

        server.startServer();

        val1 = runInServlet("testBrowseDeliveryMode_persistent_B_SecOff");

        runInServlet("testSetDeliveryMode_nonpersistent_B_SecOff");

        System.out.println("Restarting the servers for checking the persistence of messages");
        server.stopServer();

        server.startServer();

        val2 = runInServlet("testBrowseDeliveryMode_nonpersistent_B_SecOff");

        if (val1 == true && val2 == true)
            testResult = true;
        assertTrue("Test testSetDeliveryMode_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDeliveryMode_TCP_SecOff() throws Exception {

        boolean val1 = false;
        boolean val2 = false;

        runInServlet("testSetDeliveryMode_persistent_TCP_SecOff");

        server.stopServer();
        server1.stopServer();

        server1.startServer("JMSProducerTestServer_118073.log");
        String changedMessageFromLog = server1.waitForStringInLog(
                                                                  "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);
        server.startServer("JMSProducerTestClient_118073.log");

        val1 = runInServlet("testBrowseDeliveryMode_persistent_TCP_SecOff");

        runInServlet("testSetDeliveryMode_nonpersistent_TCP_SecOff");

        server.stopServer();
        server1.stopServer();

        server1.startServer("JMSProducerTestServer_118073.log");
        changedMessageFromLog = server1.waitForStringInLog(
                                                           "CWWKF0011I.*", server1.getMatchingLogFile("trace.log"));
        assertNotNull("Could not find the upload message in the new file",
                      changedMessageFromLog);
        server.startServer("JMSProducerTestClient_118073.log");

        val2 = runInServlet("testBrowseDeliveryMode_nonpersistent_TCP_SecOff");

        if (val1 == true && val2 == true)
            testResult = true;
        assertTrue("Test testSetDeliveryMode_B_SecOff failed", testResult);

    }

    // 118073_7_3 Test with deliveryMode as -1
    // 118073_7_4 Test with deliveryMode with the largest number possible for
    // int range
    // 118073_7_5 Test with deliveryMode as 0

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testDeliveryMode_Invalid_B_SecOff() throws Exception {
        testResult = runInServlet("testDeliveryMode_Invalid_B_SecOff");

        assertTrue("Test testDeliveryMode_Invalid_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testDeliveryMode_Invalid_TCP_SecOff() throws Exception {
        testResult = runInServlet("testDeliveryMode_Invalid_TCP_SecOff");

        assertTrue("Test testDeliveryMode_Invalid_TCP_SecOff failed", testResult);

    }

    // 118073_9_2 Priority is set to 4 by default.
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetPriority_default_B_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_default_B_SecOff");

        assertTrue("Test testSetPriority_default_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetPriority_default_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_default_TCP_SecOff");

        assertTrue("Test testSetPriority_default_TCP_SecOff failed", testResult);

    }

    // 118073_9_3 Test setPriority with -1
    // 118073_9_4 test setPriority with boundary values set for int
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetPriority_variation_B_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_variation_B_SecOff");

        assertTrue("Test testSetPriority_variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetPriority_variation_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_variation_TCP_SecOff");

        assertTrue("Test testSetPriority_variation_TCP_SecOff failed", testResult);

    }

    // 118073_11_1 Specifies the time to live of messages that are sent using
    // this JMSProducer. This is used to determine the expiration time of a
    // message.
    // 118073_11_2 Clients should not receive messages that have expired;
    // however, JMS does not guarantee that this will not happen.
    // 118073_11_3 Time to live is set to zero by default, which means a message
    // never expires.
    // 118073_12_1 the message time to live in milliseconds; a value of zero
    // means that a message never expires.
    // Bindings and Security Off
    ////@Test
    public void testSetTimeToLive_B_SecOff() throws Exception {
        testResult = runInServlet("testSetTimeToLive_B_SecOff");

        assertTrue("Test testSetTimeToLive_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    ////@Test
    public void testSetTimeToLive_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetTimeToLive_TCP_SecOff");

        assertTrue("Test testSetTimeToLive_TCP_SecOff failed", testResult);

    }

    // 118073_11_4 Test with timeToLive as -1
    // 118073_11_5 Test with timeToLive as out of range for long
    // 118073_11_6 Test with timeToLive set to boundary values for long

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetTimeToLive_Variation_B_SecOff() throws Exception {
        testResult = runInServlet("testSetTimeToLive_Variation_B_SecOff");

        assertTrue("Test testSetTimeToLive_Variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetTimeToLive_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetTimeToLive_Variation_TCP_SecOff");

        assertTrue("Test testSetTimeToLive_Variation_TCP_SecOff failed", testResult);

    }

    // 118073_14_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.
    // 118073_14_3 Test "name' set to empty string
    // 118073_14_4 Test "name" set to null

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetBooleanProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testGetBooleanProperty_MFRE_B_SecOff");

        assertTrue("Test testGetBooleanProperty_MFRE_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetBooleanProperty_MFRE_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetBooleanProperty_MFRE_TCP_SecOff");

        assertTrue("Test testGetBooleanProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_15_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetByteProperty_variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetByteProperty_variation_B_SecOff");

        assertTrue("Test testSetByteProperty_variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetByteProperty_variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetByteProperty_variation_TCP_SecOff");

        assertTrue("Test testSetByteProperty_variation_TCP_SecOff failed", testResult);

    }

    // 118073_16_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetByteProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testGetByteProperty_MFRE_B_SecOff");

        assertTrue("Test testGetByteProperty_MFRE_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetByteProperty_MFRE_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetByteProperty_MFRE_TCP_SecOff");

        assertTrue("Test testGetByteProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_17_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetShortProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_Null_B_SecOff");

        assertTrue("Test testSetShortProperty_Null_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetShortProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_Null_TCP_SecOff");

        assertTrue("Test testSetShortProperty_Null_TCP_SecOff failed", testResult);
    }

    // 118073_17_4 Test with "value" set as 0
    // 118073_17_5 Test with "value" set as -1
    // 118073_17_6 Test with "value" set to boundary values for short

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetShortProperty_Variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_Variation_B_SecOff");

        assertTrue("Test testSetShortProperty_Variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetShortProperty_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_Variation_TCP_SecOff");

        assertTrue("Test testSetShortProperty_Variation_TCP_SecOff failed", testResult);
    }

    // 118073_18_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetShortProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testGetShortProperty_MFRE_B_SecOff");

        assertTrue("Test testGetShortProperty_MFRE_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetShortProperty_MFRE_TCP_SecOff() throws Exception {
        testResult = runInServlet("testGetShortProperty_MFRE_TCP_SecOff");

        assertTrue("Test testGetShortProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_19_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetIntProperty_Null_B_SecOff");

        assertTrue("Test testSetIntProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_Null_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetIntProperty_Null_TCP_SecOff");

        assertTrue("Test testSetIntProperty_Null_TCP_SecOff failed", testResult);

    }

    // 118073_19_4 Test with "value" set as 0
    // 118073_19_5 Test with "value" set as -1
    // 118073_19_6 Test with "value" set to boundary values for short

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_Variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetIntProperty_Variation_B_SecOff");

        assertTrue("Test testSetIntProperty_Variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetIntProperty_Variation_TCP_SecOff");

        assertTrue("Test testSetIntProperty_Variation_TCP_SecOff failed", testResult);

    }

    // 118073_22_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testSetIntProperty_MFRE_B_SecOff");

        assertTrue("Test testSetIntProperty_MFRE_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetIntProperty_MFRE_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetIntProperty_MFRE_TCP_SecOff");

        assertTrue("Test testSetIntProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_23_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    // Bindings and security off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_Null_B_SecOff");

        assertTrue("Test testSetLongProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_Null_TCP_SecOff");

        assertTrue("Test testSetLongProperty_Null_TCP_SecOff failed", testResult);

    }

    // 118073_23_4 Test when "value" is set as 0
    // 118073_23_5 Test when 'value" is set as -1
    // 118073_23_6 Test when "value" is set to boundary values allowed for long

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_Variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_Variation_B_SecOff");

        assertTrue("Test testSetLongProperty_Variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_Variation_TCP_SecOff");

        assertTrue("Test testSetLongProperty_Variation_TCP_SecOff failed", testResult);

    }

    // 118073_24_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_MFRE_B_SecOff");

        assertTrue("Test testSetLongProperty_MFRE_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetLongProperty_MFRE_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetLongProperty_MFRE_TCP_SecOff");

        assertTrue("Test testSetLongProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_25_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetFloatProperty_Null_B_SecOff");

        assertTrue("Test testSetFloatProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetFloatProperty_Null_TCP_SecOff");

        assertTrue("Test testSetFloatProperty_Null_TCP_SecOff failed", testResult);

    }

    // 118073_25_4 Test with "value" set to 0
    // 118073_25_5 Test with "value" set to -1
    // 118073_25_6 Test with "value" set to boundary values for float.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_Variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetFloatProperty_Variation_B_SecOff");

        assertTrue("Test testSetFloatProperty_Variation_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetFloatProperty_Variation_TCP_SecOff");

        assertTrue("Test testSetFloatProperty_Variation_TCP_SecOff failed", testResult);

    }

    // 118073_26_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_MFRE_B_SecOff() throws Exception {

        testResult = runInServlet("testSetFloatProperty_MFRE_B_SecOff");

        assertTrue("Test testSetFloatProperty_MFRE_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetFloatProperty_MFRE_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetFloatProperty_MFRE_TCP_SecOff");

        assertTrue("Test testSetFloatProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_27_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetDoubleProperty_Null_B_SecOff");

        assertTrue("Test testSetDoubleProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetDoubleProperty_Null_TCP_SecOff");

        assertTrue("Test testSetDoubleProperty_Null_TCP_SecOff failed", testResult);

    }

    // 118073_27_4 Test with value set to 0
    // 118073_27_5 Test with value set to -1
    // 118073_27_6 Test with value set to boundary values allowed for double

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_Variation_B_SecOff() throws Exception {

        testResult = runInServlet("testSetDoubleProperty_Variation_B_SecOff");

        assertTrue("Test testSetDoubleProperty_Variation_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_Variation_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetDoubleProperty_Variation_TCP_SecOff");

        assertTrue("Test testSetDoubleProperty_Variation_TCP_SecOff failed", testResult);

    }

    // 118073_28_2 MessageFormatRuntimeException - if this type conversion is
    // invalid.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_MFRE_B_SecOff() throws Exception {
        testResult = runInServlet("testSetDoubleProperty_MFRE_B_SecOff");

        assertTrue("Test testSetDoubleProperty_MFRE_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetDoubleProperty_MFRE_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetDoubleProperty_MFRE_TCP_SecOff");

        assertTrue("Test testSetDoubleProperty_MFRE_TCP_SecOff failed", testResult);

    }

    // 118073_29_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetStringProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetStringProperty_Null_B_SecOff");

        assertTrue("Test testSetStringProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetStringProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetStringProperty_Null_TCP_SecOff");

        assertTrue("Test testSetStringProperty_Null_TCP_SecOff failed", testResult);

    }

    // No MessageFormatException should be thrown when property set as Boolean
    // is read as a String.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetStringProperty_B_SecOff() throws Exception {

        testResult = runInServlet("testGetStringProperty_B_SecOff");

        assertTrue("Test testGetStringProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetStringProperty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testGetStringProperty_TCP_SecOff");

        assertTrue("Test testGetStringProperty_TCP_SecOff failed", testResult);

    }

    // 118073_32_4 IllegalArgumentException - if the name is null or if the name
    // is an empty string.
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_Null_B_SecOff() throws Exception {

        testResult = runInServlet("testSetObjectProperty_Null_B_SecOff");

        assertTrue("Test testSetObjectProperty_Null_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_Null_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetObjectProperty_Null_TCP_SecOff");

        assertTrue("Test testSetObjectProperty_Null_TCP_SecOff failed", testResult);

    }

    // 118073_32_5 

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_NullObject_B_SecOff() throws Exception {

        testResult = runInServlet("testSetObjectProperty_NullObject_B_SecOff");

        assertTrue("Test testSetObjectProperty_NullObject_B_SecOff failed", testResult);
    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_NullObject_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetObjectProperty_NullObject_TCP_SecOff");

        assertTrue("Test testSetObjectProperty_NullObject_TCP_SecOff failed", testResult);

    }

    // 118073_33_2 if there is no property by this name, a null value is
    // returned
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_NullValue_B_SecOff() throws Exception {
        testResult = runInServlet("testSetObjectProperty_NullValue_B_SecOff");

        assertTrue("Test testSetObjectProperty_NullValue_B_SecOff failed", testResult);

    }

    // TCP and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetObjectProperty_NullValue_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetObjectProperty_NullValue_TCP_SecOff");

        assertTrue("Test testSetObjectProperty_NullValue_TCP_SecOff failed", testResult);

    }

    // 118073_31_3 java.lang.UnsupportedOperationException results when attempts
    // are made to modify the returned collection
    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testGetPropertyNames_Exception_B_SecOff() throws Exception {
        testResult = runInServlet("testGetPropertyNames_Exception_B_SecOff");

        assertTrue("Test testGetPropertyNames_Exception_B_SecOff failed", testResult);

    }

    // TCP and Security OFf
    @Mode(TestMode.FULL)
    @Test
    public void testGetPropertyNames_Exception_TCP_SecOff() throws Exception {
        testResult = runInServlet("testGetPropertyNames_Exception_TCP_SecOff");

        assertTrue("Test testGetPropertyNames_Exception_TCP_SecOff failed", testResult);

    }

    // 118073_34_2 Test what JMSCorrelationID can hold as its value

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetJMSCorrelationID_Value_B_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSCorrelationID_Value_B_SecOff");

        assertTrue("Test testSetJMSCorrelationID_Value_B_SecOff failed", testResult);

    }

    // TCP and Security OFf
    @Mode(TestMode.FULL)
    @Test
    public void testSetJMSCorrelationID_Value_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSCorrelationID_Value_TCP_SecOff");

        assertTrue("Test testSetJMSCorrelationID_Value_TCP_SecOff failed", testResult);

    }

    // 118073_36 JMSProducer setJMSType(String type)
    // 118073_36_1 Returns the JMSType header value that has been set on this
    // JMSProducer.
    // 118073_37 String getJMSType()
    // 118073_37_1 Returns the JMSType header value that has been set on this
    // JMSProducer.

    // Bindings and Security Off
    @Mode(TestMode.FULL)
    @Test
    public void testSetJMSType_B_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSType_B_SecOff");

        assertTrue("Test testSetJMSType_B_SecOff failed", testResult);

    }

    // TCP and Security OFf
    @Mode(TestMode.FULL)
    @Test
    public void testSetJMSType_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetJMSType_TCP_SecOff");

        assertTrue("Test testSetJMSType_TCP_SecOff failed", testResult);
    }

    // 118073_3 JMSProducer setDisableMessageID(boolean value)
    // Bindings and Security Off

    @Test
    public void testSetDisableMessageID_B_SecOff() throws Exception {
        testResult = runInServlet("testSetDisableMessageID_B_SecOff");

        assertTrue("Test testSetDisableMessageID_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetDisableMessageID_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetDisableMessageID_TCP_SecOff");

        assertTrue("Test testSetDisableMessageID_TCP_SecOff failed", testResult);

    }

    // 118073_5 JMSProducer setDisableMessageTimestamp(boolean value)
    // Bindings and Security Off

    @Test
    public void testSetDisableMessageTimestamp_B_SecOff() throws Exception {
        testResult = runInServlet("testSetDisableMessageTimestamp_B_SecOff");

        assertTrue("Test testSetDisableMessageTimestamp_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetDisableMessageTimestamp_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetDisableMessageTimestamp_TCP_SecOff");

        assertTrue("Test testSetDisableMessageTimestamp_TCP_SecOff failed", testResult);

    }

    // 118073_9 JMSProducer setPriority(int priority)
    // Bindings and Security Off

    @Test
    public void testPriority_B_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_B_SecOff");

        assertTrue("Test testPriority_B_SecOff failed", testResult);
    }

    // TCP and Security Off

    @Test
    public void testPriority_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetPriority_TCP_SecOff");

        assertTrue("Test testPriority_TCP_SecOff failed", testResult);

    }

    // 118073_13 JMSProducer setProperty(String name, boolean value)
    // 118073_13_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified boolean value.
    // 118073_13_2 Verify when this method is invoked when , it will replace any
    // property of the same name that is already set on the message being sent.
    // 118073_13_3 IllegalArgumentException - if the name is null or if the name
    // is an empty string.

    // Bindings and Security Off

    @Test
    public void testSetBooleanProperty_B_SecOff() throws Exception {

        testResult = runInServlet("testSetBooleanProperty_B_SecOff");

        assertTrue("Test testSetBooleanProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetBooleanProperty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetBooleanProperty_TCP_SecOff");

        assertTrue("Test testSetBooleanProperty_TCP_SecOff failed", testResult);

    }

    // 118073_15 JMSProducer setProperty(String name, byte value)
    // 118073_15_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified byte value.
    // 118073_15_2 Test when this invoked it will replace any property of the
    // same name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetByteProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetByteProperty_B_SecOff");

        assertTrue("Test testSetByteProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetByteProperty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetByteProperty_TCP_SecOff");

        assertTrue("Test testSetByteProperty_TCP_SecOff failed", testResult);

    }

    // 118073_17 JMSProducer setProperty(String name, short value)
    // 118073_17_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified short value.
    // 118073_17_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetShortProperty_B_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_B_SecOff");

        assertTrue("Test testSetShortProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetShortProperty_TCP_SecOff() throws Exception {

        testResult = runInServlet("testSetShortProperty_TCP_SecOff");

        assertTrue("Test testSetShortProperty_TCP_SecOff failed", testResult);

    }

    // 118073_19 JMSProducer setProperty(String name, int value)
    // 118073_19_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified int value.
    // 118073_19_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetIntProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetIntProperty_B_SecOff");

        assertTrue("Test testSetIntProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetIntProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetIntProperty_TCP_SecOff");

        assertTrue("Test testSetIntProperty_TCP_SecOff failed", testResult);

    }

    // 118073_23 JMSProducer setProperty(String name, long value)
    // 118073_23_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified long value.
    // 118073_23_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetLongProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetLongProperty_B_SecOff");

        assertTrue("Test testSetLongProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetLongProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetLongProperty_TCP_SecOff");

        assertTrue("Test testSetLongProperty_TCP_SecOff failed", testResult);

    }

    // 118073_25 JMSProducer setProperty(String name, float value)
    // 118073_25_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified float value.
    // 118073_25_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetFloatProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetFloatProperty_B_SecOff");

        assertTrue("Test testSetFloatProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetFloatProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetFloatProperty_TCP_SecOff");

        assertTrue("Test testSetFloatProperty_TCP_SecOff failed", testResult);

    }

    // 118073_27 JMSProducer setProperty(String name, double value)
    // 118073_27_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified double value.
    // 118073_27_2 Test when this method is invoked will replace any property of
    // the same name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetDoubleProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetDoubleProperty_B_SecOff");

        assertTrue("Test testSetDoubleProperty_B_SecOff failed", testResult);
    }

    // TCP and Security Off

    @Test
    public void testSetDoubleProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetDoubleProperty_TCP_SecOff");

        assertTrue("Test testSetDoubleProperty_TCP_SecOff failed", testResult);

    }

    // 118073_29 JMSProducer setProperty(String name, String value)
    // 118073_29_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified String value.
    // 118073_29_2 Invoking this method will replace any property of the same
    // name that is already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetStringProperty_B_SecOff() throws Exception {
        testResult = runInServlet("testSetStringProperty_B_SecOff");

        assertTrue("Test testSetStringProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetStringProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetStringProperty_TCP_SecOff");

        assertTrue("Test testSetStringProperty_TCP_SecOff failed", testResult);

    }

    // 118073_31 JMSProducer setProperty(String name,Object value)
    // 118073_32_1 Specifies that messages sent using this JMSProducer will have
    // the specified property set to the specified Java object value.
    // 118073_32_2 Verify that this method works only for the objectified
    // primitive object types (Integer, Double, Long ...) and String objects.
    // 118073_32_3 Test this will replace any property of the same name that is
    // already set on the message being sent.

    // Bindings and Security Off

    @Test
    public void testSetObjectProperty_B_SecOff() throws Exception {

        testResult = runInServlet("testSetObjectProperty_B_SecOff");

        assertTrue("Test testSetObjectProperty_B_SecOff failed", testResult);

    }

    // TCP and Security Off

    @Test
    public void testSetObjectProperty_TCP_SecOff() throws Exception {
        testResult = runInServlet("testSetObjectProperty_TCP_SecOff");

        assertTrue("Test testSetObjectProperty_TCP_SecOff failed", testResult);

    }

    @org.junit.AfterClass
    public static void tearDown() {
        try {
            System.out.println("Stopping server");
            server.stopServer();
            server1.stopServer();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
