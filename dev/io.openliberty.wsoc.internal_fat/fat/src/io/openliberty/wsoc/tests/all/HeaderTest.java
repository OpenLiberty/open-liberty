package com.ibm.ws.fat.wsoc.tests.all;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.wsoc.WsocTest;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class HeaderTest {

    private WsocTest wsocTest = null;

    private SharedServer SS = null;

    private File logDir = null;

    public HeaderTest(WsocTest test, SharedServer ss, File loggingDir) {
        this.wsocTest = test;
        this.SS = ss;
        this.logDir = loggingDir;
    }

    public void testInvalidVersionHeaders() throws Exception {

        //Verify no headers returns 404. This is not a Websocket request and Web container tries to handle this request as 
        //a normal servlet. However normal servlet for this URI doesn't exist and hence 404 - FileNotFound status is returned 
        //by web container.
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", 404);

        //For all the below tests.
        //First WebContainer asks WebSocket if this is a websocket request. This is a websocket request since 'websocket' 'upgrade'
        //connection is present. Next webcontainer hands off the request to websocket which verifies all the headers. For e.g for the 
        //first test below version '12' not supported version and hence error 400 - BAD REQUEST is returned.
        Map<String, String> headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Version", "12");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

        //version '13.5' not supported version, hence error 400 - BAD REQUEST 
        headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Version", "13.5");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

        //invalid Sec-WebSocket-Version, hence error 400 - BAD REQUEST
        headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Version", "STUPIDVERSION");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

        //Sec-WebSocket-Version is not present, hence error 400 - BAD REQUEST
        headers = getDefaultWsocHeaders();
        headers.remove("Sec-WebSocket-Version");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

    }

    public void testInvalidUpgradeHeaders() throws Exception {

        //First WebContainer asks WebSocket if this is a websocket request. This is a websocket request since 'websocket' 'upgrade'
        //is present. Next webcontainer hands off the request to websocket which verifies all the headers. For e.g for the 
        //first test 'Connection' should be 'upgrade' not 'NotUpgraded' and hence error 400 - BAD REQUEST is returned.
        Map<String, String> headers = getDefaultWsocHeaders();
        headers.put("Connection", "NotUpgraded");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

        //this is not a Websocket request since 'upgrade' should be 'websocket' not 'wsoc'. Web container tries to handle this request as 
        //a normal servlet. However normal servlet for this URI doesn't exist and hence 404 - FileNotFound status is returned 
        //by web container.
        headers = getDefaultWsocHeaders();
        headers.put("Upgrade", "wsoc");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 404);

        //this is a websocket request. However, 'connection' header is removed. Hence 400- BAD REQUEST
        headers = getDefaultWsocHeaders();
        headers.remove("Connection");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 400);

        //this is not a Websocket request since 'upgrade' should be present. Hence Web container tries to handle this request as 
        //a normal servlet. However normal servlet for this URI doesn't exist and hence 404 - FileNotFound status is returned 
        //by web container.
        headers = getDefaultWsocHeaders();
        headers.remove("Upgrade");
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedByteBuffer", headers, 404);

    }

    public void testInvalidAcceptKey() throws Exception {
        testInvalidAcceptKey("/basic/annotatedByteBuffer");
    }

    public void testInvalidAcceptKey(String path) throws Exception {

        Map<String, String> headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Key", "");
        SS.verifyStatusCode(createWebBrowserForTestCase(), path, headers, 400);

        // Not base64 encoded since not divisbile by 4
        headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Key", "s3pPLMBiTxaQ9kYGzzhZRbK+xO");
        SS.verifyStatusCode(createWebBrowserForTestCase(), path, headers, 400);

        // Decoded length != 16 bytes
        headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Key", "s3pPLMBiTxaQ9kYGzzhZRbK");
        SS.verifyStatusCode(createWebBrowserForTestCase(), path, headers, 400);

        headers = getDefaultWsocHeaders();
        headers.put("Sec-WebSocket-Key", "NOWAYTHISONE WORKS");
        SS.verifyStatusCode(createWebBrowserForTestCase(), path, headers, 400);

        headers = getDefaultWsocHeaders();
        headers.remove("Sec-WebSocket-Key");
        SS.verifyStatusCode(createWebBrowserForTestCase(), path, headers, 400);

    }

    public void testOriginReturns403() throws Exception {

        Map<String, String> headers = getDefaultWsocHeaders();
        SS.verifyStatusCode(createWebBrowserForTestCase(), "/basic/annotatedCheckOrigin", headers, 403);

    }

    private static Map<String, String> getDefaultWsocHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<String, String>(10);
        requestHeaders.put("Connection", "Upgrade");
        requestHeaders.put("Upgrade", "websocket");
        requestHeaders.put("Sec-WebSocket-Version", "13");

        requestHeaders.put("Sec-WebSocket-Key", "3ODLuv2NMJugtQtbQpT1AA==");

        return requestHeaders;
    }

    protected WebBrowser createWebBrowserForTestCase() {
        return WebBrowserFactory.getInstance().createWebBrowser(logDir);
    }

}