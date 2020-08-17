package com.ibm.ws.security.common.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class AuthUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    AuthUtils utils = new AuthUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new AuthUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @Test
    public void test_getBearerTokenFromHeader_nullHeaderValue() {
        String rawHeaderValue = null;
        String scheme = "my scheme";
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertNull("Returned value should have been null but was [" + result + "].", result);
    }

    @Test
    public void test_getBearerTokenFromHeader_nullScheme() {
        String rawHeaderValue = " some header value ";
        String scheme = null;
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value should have matched raw value, but it didn't.", rawHeaderValue, result);
    }

    @Test
    public void test_getBearerTokenFromHeader_schemeNotPresent() {
        String rawHeaderValue = " some header value ";
        String scheme = "my scheme";
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value should have matched raw value, but it didn't.", rawHeaderValue, result);
    }

    @Test
    public void test_getBearerTokenFromHeader_schemeDifferentCase() {
        String rawHeaderValue = "scheme some header value ";
        String scheme = "Scheme";
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value should have matched raw value, but it didn't.", rawHeaderValue, result);
    }

    @Test
    public void test_getBearerTokenFromHeader_whitespaceBeforeScheme() {
        String rawHeaderValue = " Scheme some header value ";
        String scheme = "Scheme";
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value should have matched raw value, but it didn't.", rawHeaderValue, result);
    }

    @Test
    public void test_getBearerTokenFromHeader_whitespaceValid() {
        String expectedValue = "    some header value ";
        String scheme = "Scheme";
        String rawHeaderValue = scheme + expectedValue;
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value did not match the expected value", expectedValue, result);
    }

    @Test
    public void test_getBearerTokenFromHeader_goldenPath() {
        String expectedValue = "myToken";
        String scheme = "Bearer ";
        String rawHeaderValue = scheme + expectedValue;
        String result = utils.getBearerTokenFromHeader(rawHeaderValue, scheme);
        assertEquals("Returned value did not match the expected value", expectedValue, result);
    }

}
