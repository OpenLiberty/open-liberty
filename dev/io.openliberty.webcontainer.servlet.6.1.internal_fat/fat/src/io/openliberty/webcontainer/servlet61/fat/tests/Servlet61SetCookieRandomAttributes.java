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

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test ServletResponse addHeader and setHeader to add "Set-Cookie" with arbitrary attributes.
 * Verify that the response Set-Cookie at client is not split up into multiple Set-Cookie headers
 * Verify that attribute name with EMPTY value will show up with just the attribute name (there is no = trailing)
 * Verify that attribute name with NULL value will : (1) remove the existing attribute with same name. (2) remove itself
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61SetCookieRandomAttributes {
    private static final Logger LOG = Logger.getLogger(Servlet61SetCookieRandomAttributes.class.getName());
    private static final String TEST_APP_NAME = "SetCookieViaResponseHeader";

    private StringBuilder headerSetCookie = new StringBuilder();
    private int foundSetCookieHeaders = 0;
    private String setCookieHeadersString = null;

    private int EXPECTED_COOKIE_HEADERS = 3;

    //These attributes in all tests
    final String COOKIE_EXPECTED_ATT = "cookie_att_name=cookie_att_value";
    final String SETHEADER_EXPECTED_ATT = "set_att_name=set_att_value";
    final String ADDHEADER_EXPECTED_ATT = "add_att_name=add_att_value";

    @Server("servlet61_SetCookieAttributes")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_SetCookieAttributes.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "setcookie.servlets");

        ArrayList<String> expectedErrors = new ArrayList<String>();
        expectedErrors.add("CWWKT0047E:.*"); //Session Cookie attribute name has illegal character.
        server.addIgnoredErrors(expectedErrors);

        server.startServer(Servlet61SetCookieRandomAttributes.class.getSimpleName() + ".log");
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
     * Set-Cookie using Cookie() API - test : arbitrary attribute name and value - Expecting no split Set-Cookie headers
     *
     * Expected response cookies:
     * [set-cookie: cookie_att_test=cookie_value; httponly; cookie_att_name=cookie_att_value]
     * [set-cookie: set_cookie_att_test=set_cookie_value; secure; httponly; samesite=none; set_att_name=set_att_value]
     * [set-cookie: add_cookie_att_test=add_cookie_value; secure; httponly; samesite=strict; add_att_name=add_att_value]
     */
    @Test
    public void test_SetCookie_Attributes() throws Exception {
        LOG.info("====== <test_SetCookie_Attributes> ======");

        runTest("test_SetCookie_Attributes");

        setCookieHeadersString = headerSetCookie.toString().toLowerCase();
        LOG.info("\n" + setCookieHeadersString);

        assertCommonAttributes();
    }

    /*
     * Test attribute name with NULL attribute value :
     *  NULL attribute value removes the existing attribute
     *  NULL attribute value removes itself
     *
     * Expected response cookies:
     * [set-cookie: cookie_null_test=cookie_value; httponly; cookie_att_name=cookie_att_value]
     * [set-cookie: set_cookie_null_test=set_cookie_value; secure; samesite=none; set_att_name=set_att_value]
     * [set-cookie: add_cookie_null_test=add_cookie_value; secure; samesite=strict; add_att_name=add_att_value]
     */
    @Test
    public void test_SetCookie_Attributes_NULL() throws Exception {
        LOG.info("====== <test_SetCookie_Attributes_NULL> ======");

        //removed the previous attributes with the new attributes which has NULL value
        final String COOKIE_NOT_EXPECTED_REMOVED = "cookie_be_removed";
        final String SET_NOT_EXPECTED_REMOVED = "set_be_removed";
        final String ADD_NOT_EXPECTED_REMOVED= "add_be_removed";

        final String COOKIE_NOT_EXPECTED_NULL_VALUE = "cookie_null_value";
        final String SET_NOT_EXPECTED_NULL_VALUE = "set_null_value";
        final String ADD_NOT_EXPECTED_NULL_VALUE = "add_null_value";

        runTest("test_SetCookie_Attributes_NULL");

        setCookieHeadersString = headerSetCookie.toString().toLowerCase(); //make it easier to search in the log
        LOG.info("\n" + setCookieHeadersString);

        assertCommonAttributes();

        assertTrue("Cookie NULL Test - NOT Expected attribute [" + COOKIE_NOT_EXPECTED_REMOVED + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(COOKIE_NOT_EXPECTED_REMOVED)));
        assertTrue("Set Header NULL Test - NOT Expected attribute [" + SET_NOT_EXPECTED_REMOVED + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(SET_NOT_EXPECTED_REMOVED)));
        assertTrue("Add Header NULL Test - NOT Expected attribute [" + ADD_NOT_EXPECTED_REMOVED + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(ADD_NOT_EXPECTED_REMOVED)));

        assertTrue("Cookie NULL Test - NOT Expected attribute [" + COOKIE_NOT_EXPECTED_NULL_VALUE + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(COOKIE_NOT_EXPECTED_NULL_VALUE)));
        assertTrue("Set Header NULL Test - NOT Expected attribute [" + SET_NOT_EXPECTED_NULL_VALUE + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(SET_NOT_EXPECTED_NULL_VALUE)));
        assertTrue("Add Header NULL Test - NOT Expected attribute [" + ADD_NOT_EXPECTED_NULL_VALUE + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(ADD_NOT_EXPECTED_NULL_VALUE)));
    }


    /*
     * Test Empty attribute value : only attribute name showing without any = sign
     *
     * Expected response cookies:
     * [set-cookie: cookie_empty_test=cookie_value; httponly; cookie_att_name=cookie_att_value; cookie_name_only_sign; cookie_name_only]
     * [set-cookie: set_cookie_empty_test=set_cookie_value; secure; samesite=none; set_name_only_sign; set_name_only; set_att_name=set_att_value]
     * [set-cookie: add_cookie_empty_test=add_cookie_value; secure; samesite=strict; add_att_name=add_att_value; add_name_only_sign; add_name_only]
     */
    @Test
    public void test_SetCookie_Attributes_EMPTY() throws Exception {
        LOG.info("====== <test_SetCookie_Attributes_EMPTY> ======");

        final String COOKIE_EXPECTED_NAME_ONLY = "cookie_name_only";
        final String SET_EXPECTED_NAME_ONLY = "set_name_only";
        final String ADD_EXPECTED_NAME_ONLY = "add_name_only";

        //The cookies were set with trailing = sign (i.e ; cookie_name_only_sign=); The = is NOT expected in the response(i.e ; cookie_name_only_sign)
        final String COOKIE_NOT_EXPECTED_NAME_ONLY_SIGN = "cookie_name_only_sign=";
        final String SET_NOT_EXPECTED_NAME_ONLY_SIGN = "set_name_only_sign=";
        final String ADD_NOT_EXPECTED_NAME_ONLY_SIGN = "add_name_only_sign=";

        runTest("test_SetCookie_Attributes_EMPTY");

        setCookieHeadersString = headerSetCookie.toString().toLowerCase();
        LOG.info("\n" + setCookieHeadersString);

        assertCommonAttributes();

        assertTrue("Cookie EMPTY Test - Expected attribute [" + COOKIE_EXPECTED_NAME_ONLY + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(COOKIE_EXPECTED_NAME_ONLY)));
        assertTrue("Set Header EMPTY Test - Expected attribute [" + SET_EXPECTED_NAME_ONLY + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(SET_EXPECTED_NAME_ONLY)));
        assertTrue("Add Header EMPTY Test - Expected attribute [" + ADD_EXPECTED_NAME_ONLY + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(ADD_EXPECTED_NAME_ONLY)));

        //Not expected trailing = sign
        assertTrue("Cookie EMPTY Test - NOT Expected attribute [" + COOKIE_NOT_EXPECTED_NAME_ONLY_SIGN + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(COOKIE_NOT_EXPECTED_NAME_ONLY_SIGN)));
        assertTrue("Set Header EMPTY Test - NOT Expected attribute [" + SET_NOT_EXPECTED_NAME_ONLY_SIGN + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(SET_NOT_EXPECTED_NAME_ONLY_SIGN)));
        assertTrue("Add Header EMPTY Test - NOT Expected attribute [" + ADD_NOT_EXPECTED_NAME_ONLY_SIGN + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", !(setCookieHeadersString.contains(ADD_NOT_EXPECTED_NAME_ONLY_SIGN)));

    }

    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void test_SetCookie_Illegal_Attribute_Name() throws Exception {
        LOG.info("====== <test_SetCookie_Illegal_Attribute_Name> ======");

        EXPECTED_COOKIE_HEADERS = 1;    //temporary set it to 1 for the Cookie Set-Cookie.
        runTest("test_SetCookie_Illegal_Attribute_Name");

        EXPECTED_COOKIE_HEADERS = 3;   //reset...just in case this test run first.

        //nothing to assert explicitly here
        // expectedErrors.add("CWWKT0047E:.*") in setup will validate and error out the test if CWWKT0047E is not there
        // ExpectedFFDC above will complaint if it is not found
    }

    //Common attribute names and values that should be found in all tests
    private void assertCommonAttributes() {
        LOG.info("====== <assertCommonAttributes> ======");

        assertTrue("Cookie - Expected attribute [" + COOKIE_EXPECTED_ATT + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(COOKIE_EXPECTED_ATT)));
        assertTrue("Set Header - Expected attribute [" + SETHEADER_EXPECTED_ATT + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(SETHEADER_EXPECTED_ATT)));
        assertTrue("Add Header - Expected attribute [" + ADDHEADER_EXPECTED_ATT + "] not found from Set-Cookie headers [" + setCookieHeadersString + "]", (setCookieHeadersString.contains(ADDHEADER_EXPECTED_ATT)));
    }

    /**
     * @param testToRun - name of the test/method for the servlet to execute
     */
    private void runTest(String testToRun) throws Exception {
        LOG.info("====== <runTest> [" + testToRun + "] ENTRY ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestSetCookieAttributesViaResponseHeader";
        HttpGet httpMethod = new HttpGet(url);
        httpMethod.addHeader("runTest", testToRun);

        LOG.info("Sending [" + url + "]");

        foundSetCookieHeaders = 0;
        headerSetCookie.setLength(0);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(httpMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");
                Header[] headers = response.getHeaders();

                for (Header header : headers) {
                    LOG.info("\n" + "Header: " + header);
                    if (header.getName().equals("Set-Cookie")) {
                        foundSetCookieHeaders++;
                        headerSetCookie.append("[" +header + "] \n");
                    }
                }
            }
        }

        //quick fail if not find 3 set-cookie headers
        assertTrue("Expected " + EXPECTED_COOKIE_HEADERS + " Set-Cookie headers but found [" + foundSetCookieHeaders + "]", foundSetCookieHeaders == EXPECTED_COOKIE_HEADERS);

        LOG.info("====== <runTest> [" + testToRun + "] RETURN ======");
    }
}
