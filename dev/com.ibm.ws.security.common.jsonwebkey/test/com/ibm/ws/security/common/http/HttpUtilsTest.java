package com.ibm.ws.security.common.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class HttpUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);

    HttpUtils utils = new HttpUtils();

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
        utils = new HttpUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    /******************************************* createHttpPostMethod *******************************************/

    /**
     * Tests:
     * - URL: null
     * - Headers: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_createHttpPostMethod_nullUrl_nullHeaders() {
        try {
            String url = null;
            List<NameValuePair> commonHeaders = null;

            HttpPost result = utils.createHttpPostMethod(url, commonHeaders);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: null
     * - Headers: Empty
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_createHttpPostMethod_nullUrl_emptyHeaders() {
        try {
            String url = null;
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpPost result = utils.createHttpPostMethod(url, commonHeaders);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid URL
     * - Headers: null
     * Expects:
     * - Should get valid POST object with matching URL and empty header list
     */
    @Test
    public void test_createHttpPostMethod_validUrl_nullHeaders() {
        try {
            String url = "http://localhost";
            List<NameValuePair> commonHeaders = null;

            HttpPost postObj = utils.createHttpPostMethod(url, commonHeaders);
            verifyHttpPostObject(postObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Invalid URL
     * Expects:
     * - IllegalArgumentException should be thrown because of the invalid URL
     */
    @Test
    public void test_createHttpPostMethod_invalidUrl() {
        try {
            String url = "some \n\r non-URL";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            try {
                utils.createHttpPostMethod(url, commonHeaders);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Invalid URL using different protocol than HTTP or HTTPS
     * Expects:
     * - Should get valid POST object with matching URL and empty header list
     */
    @Test
    public void test_createHttpPostMethod_nonHttpUrl() {
        try {
            String url = "ftp://localhost";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpPost postObj = utils.createHttpPostMethod(url, commonHeaders);
            verifyHttpPostObject(postObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid, simple URL
     * - Headers: Empty
     * Expects:
     * - Should get valid POST object with matching URL and empty header list
     */
    @Test
    public void test_createHttpPostMethod_emptyHeaders() {
        try {
            String url = "http://localhost";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpPost postObj = utils.createHttpPostMethod(url, commonHeaders);
            verifyHttpPostObject(postObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid, complex URL
     * - Headers: Not empty
     * Expects:
     * - Should get valid POST object with matching URL and header list
     */
    @Test
    public void test_createHttpPostMethod_complexUrl_nonEmptyHeaders() {
        try {
            String url = "HtTpS://non-existent.subdomain.example.com:80/path/to/somewhere?with=query&other&params#and-fragment1";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();
            commonHeaders.add(new BasicNameValuePair("Header1", "value 1"));
            commonHeaders.add(new BasicNameValuePair("Another \n\r header ", "next value for other header"));

            HttpPost postObj = utils.createHttpPostMethod(url, commonHeaders);
            verifyHttpPostObject(postObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* createHttpGetMethod *******************************************/

    /**
     * Tests:
     * - URL: null
     * - Headers: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_createHttpGetMethod_nullUrl_nullHeaders() {
        try {
            String url = null;
            List<NameValuePair> commonHeaders = null;

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            assertNull("Result should have been null but was [" + getObj + "].", getObj);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: null
     * - Headers: Empty
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_createHttpGetMethod_nullUrl_emptyHeaders() {
        try {
            String url = null;
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            assertNull("Result should have been null but was [" + getObj + "].", getObj);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid URL
     * - Headers: null
     * Expects:
     * - Should get valid GET object with matching URL and empty header list
     */
    @Test
    public void test_createHttpGetMethod_validUrl_nullHeaders() {
        try {
            String url = "http://localhost";
            List<NameValuePair> commonHeaders = null;

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            verifyHttpGetObject(getObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Invalid URL
     * Expects:
     * - IllegalArgumentException should be thrown because of the invalid URL
     */
    @Test
    public void test_createHttpGetMethod_invalidUrl() {
        try {
            String url = "some \n\r non-URL";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            try {
                utils.createHttpGetMethod(url, commonHeaders);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Invalid URL using different protocol than HTTP or HTTPS
     * Expects:
     * - Should get valid GET object with matching URL and empty header list
     */
    @Test
    public void test_createHttpGetMethod_nonHttpUrl() {
        try {
            String url = "ftp://localhost";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            verifyHttpGetObject(getObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid, simple URL
     * - Headers: Empty
     * Expects:
     * - Should get valid GET object with matching URL and empty header list
     */
    @Test
    public void test_createHttpGetMethod_emptyHeaders() {
        try {
            String url = "http://localhost";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            verifyHttpGetObject(getObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid, complex URL
     * - Headers: Not empty
     * Expects:
     * - Should get valid GET object with matching URL and header list
     */
    @Test
    public void test_createHttpGetMethod_complexUrl_nonEmptyHeaders() {
        try {
            String url = "HtTpS://non-existent.subdomain.example.com:80/path/to/somewhere?with=query&other&params#and-fragment1";
            List<NameValuePair> commonHeaders = new ArrayList<NameValuePair>();
            commonHeaders.add(new BasicNameValuePair("Header1", "value 1"));
            commonHeaders.add(new BasicNameValuePair("Another \n\r header ", "next value for other header"));

            HttpGet getObj = utils.createHttpGetMethod(url, commonHeaders);
            verifyHttpGetObject(getObj, url, commonHeaders);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* createHTTPClient *******************************************/

    /**
     * Tests:
     * - SSL socket factory: null
     * - URL: null
     * - Verify hostname: false
     * Expects:
     * - IllegalArgumentException should be thrown because of the null SSL socket factory
     */
    @Test
    public void test_createHTTPClient_nullSslSocketFactory() {
        try {
            SSLSocketFactory sslSocketFactory = null;
            String url = null;

            try {
                utils.createHTTPClient(sslSocketFactory, url, false);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: null
     * - Verify hostname: false
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_nullUrl_doNotVerifyHostname() {
        try {
            String url = null;

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Empty
     * - Verify hostname: false
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_emptyUrl_doNotVerifyHostname() {
        try {
            String url = "";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Invalid
     * - Verify hostname: true
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_invalidUrl_verifyHostname() {
        try {
            String url = "some invalid URL";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid
     * - Verify hostname: true
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_validUrl_verifyHostname() {
        try {
            String url = "http://localhost";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid HTTP URL
     * - Username: null
     * - Password: null
     * Expects:
     * - IllegalArgumentException should be thrown because of the null username
     */
    @Test
    public void test_createHTTPClient_withCredentials_nullUsername_nullPassword() {
        try {
            String url = "http://localhost";
            String username = null;
            String password = null;

            try {
                utils.createHTTPClient(sslSocketFactory, url, false, username, password);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid HTTP URL
     * - Verify hostname: false
     * - Username: null
     * - Password: Empty
     * Expects:
     * - IllegalArgumentException should be thrown because of the null username
     */
    @Test
    public void test_createHTTPClient_withCredentials_nullUsername_emptyPassword() {
        try {
            String url = "http://localhost";
            String username = null;
            String password = "";

            try {
                utils.createHTTPClient(sslSocketFactory, url, false, username, password);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Valid HTTP URL
     * - Verify hostname: false
     * - Username: Empty
     * - Password: null
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_withCredentials_emptyUsername_nullPassword() {
        try {
            String url = "http://localhost";
            String username = "";
            String password = null;

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false, username, password);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - SSL socket factory: null
     * - URL: Valid HTTP URL
     * Expects:
     * - Client should not be null - a valid HTTP URL means the SSL socket factory shouldn't be used
     */
    @Test
    public void test_createHTTPClient_withCredentials_nullSslSocketFactory_validHttpUrl() {
        try {
            SSLSocketFactory sslSocketFactory = null;
            String url = "http://localhost";
            String username = "";
            String password = "";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false, username, password);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - SSL socket factory: null
     * - URL: Invalid
     * Expects:
     * - A URL that doesn't start with "http:" will attempt to use the SSL socket factory
     * - IllegalArgumentException should be thrown because of the null SSL socket factory
     */
    @Test
    public void test_createHTTPClient_withCredentials_nullSslSocketFactory_invalidUrl() {
        try {
            SSLSocketFactory sslSocketFactory = null;
            String url = "some invalid URL";
            String username = "my username";
            String password = "pass\n\r word";

            try {
                utils.createHTTPClient(sslSocketFactory, url, false, username, password);
                fail("Should have thrown an IllegalArgumentException but did not.");
            } catch (IllegalArgumentException e) {
                // Expected to be thrown because of the invalid URL
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - SSL socket factory: null
     * - URL: HTTP URL with mixed casing of the HTTP scheme
     * Expects:
     * - Client should not be null - a valid HTTP URL means the SSL socket factory shouldn't be used
     */
    @Test
    public void test_createHTTPClient_withCredentials_nullSslSocketFactory_httpUrlMixedCase() {
        try {
            SSLSocketFactory sslSocketFactory = null;
            String url = "hTtP://localhost";
            String username = "username";
            String password = "password";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false, username, password);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Non-HTTP URL
     * - Verify host name: true
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_withCredentials_verifyHostName() {
        try {
            String url = "https://localhost";
            String username = "username";
            String password = "password";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, true, username, password);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - URL: Non-HTTP URL
     * - Verify host name: false
     * Expects:
     * - Client should not be null
     */
    @Test
    public void test_createHTTPClient_withCredentials_doNotVerifyHostName() {
        try {
            String url = "ftp://example";
            String username = "username";
            String password = "password";

            HttpClient client = utils.createHTTPClient(sslSocketFactory, url, false, username, password);
            assertNotNull("HttpClient object should not have been null but was.", client);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* Helper methods *******************************************/

    private void verifyHttpPostObject(HttpPost postObj, String inputUrl, List<NameValuePair> inputHeaders) {
        assertNotNull("HttpPost object should not have been null but was.", postObj);
        assertEquals("Request method did not match expected value.", "POST", postObj.getMethod().toUpperCase());
        assertEquals("Request URI did not match input URL.", inputUrl, postObj.getURI().toString());

        verifyHttpObjectHeaders(postObj, inputHeaders);
    }

    private void verifyHttpGetObject(HttpGet getObj, String inputUrl, List<NameValuePair> inputHeaders) {
        assertNotNull("HttpGet object should not have been null but was.", getObj);
        assertEquals("Request method did not match expected value.", "GET", getObj.getMethod().toUpperCase());
        assertEquals("Request URI did not match input URL.", inputUrl, getObj.getURI().toString());

        verifyHttpObjectHeaders(getObj, inputHeaders);
    }

    private void verifyHttpObjectHeaders(HttpRequestBase httpRequestObj, List<NameValuePair> inputHeaders) {
        Header[] headers = httpRequestObj.getAllHeaders();
        assertNotNull("Header list should not have been null but was.", headers);
        if (inputHeaders == null) {
            assertTrue("Object headers should have been empty but were: " + Arrays.toString(headers), headers.length == 0);
        } else {
            verifyNonNullHeaders(inputHeaders, headers);
        }
    }

    private void verifyNonNullHeaders(List<NameValuePair> inputHeaders, Header[] headers) {
        assertEquals("Number of entries in header list did not match expected value. Input headers were: " + inputHeaders + ". Got headers: " + Arrays.toString(headers), inputHeaders.size(), headers.length);
        if (!inputHeaders.isEmpty()) {
            for (Header postObjHeader : headers) {
                NameValuePair compareHeaderValue = new BasicNameValuePair(postObjHeader.getName(), postObjHeader.getValue());
                assertTrue("Input headers did not originally contain obtained header: " + postObjHeader + ". Input headers were: " + inputHeaders + ". Got headers: " + Arrays.toString(headers), inputHeaders.contains(compareHeaderValue));
            }
        }
    }

}
