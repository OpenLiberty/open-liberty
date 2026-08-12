/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs2x.cxfClientProps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotSame;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CxfClientPropsTestServlet")
public class CxfClientPropsTestServlet extends FATServlet {
   
    private final static Logger _log = Logger.getLogger(CxfClientPropsTestServlet.class.getName());
    private static final long defaultMargin = 30000;
    private final static String proxyPort = "8888";
    private final static String proxyHost = "127.0.0.1";
    private final static String myHost = "1.1.1.1";
    private static final long slowHardwareMargin = 61000;    
 
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    private static final boolean isAIX = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("aix");


    /**
     * Not actually testing CXF client properties, but rather testing socket timeouts,
     * which are a prereq for CXF client connection timeouts.
     */
    @Test
    public void testUrlConnectTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testUrlConnectTimeout";
        long SOCKET_TIMEOUT = 5000;
        long MARGIN = 2000;
        URL url = new URL("http://localhost:23/blah");
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(5000); // 5 seconds
        long startTime = System.currentTimeMillis();
        try {
            conn.connect();
            _log.info(m + " aborting test... we actually connected to the remote telnet port...");
        } catch (SocketTimeoutException expected) {
            
        } catch (IOException ex) {
            _log.info(m + " unexpected exception (expected SocketTimeoutException)");
            ex.printStackTrace();
        }
        long elapsed = System.currentTimeMillis() - startTime;
        _log.info(m + " Request finished in " + elapsed + "ms");
        if (elapsed > SOCKET_TIMEOUT + MARGIN) {
            fail("Did not timeout within the CXF-specific connection timeout, waited " + elapsed + "ms");
        }
    }

    @Test
    public void testCXFConnectTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testCXFConnectTimeout";
        String target = null;
        long CXF_TIMEOUT = 5000;
        long MARGIN = defaultMargin;
        if (isAIX || isWindows) {
            MARGIN = slowHardwareMargin;
        }        
        
        Client client = ClientBuilder.newBuilder()
                                     .property("client.ConnectionTimeout", CXF_TIMEOUT)
                                     .build();
        
        // https://stackoverflow.com/a/904609/6575578
        target = "http://10.255.255.1/blah";
     
        long startTime = System.currentTimeMillis();
        try {
            client.target(target).request().get();
            _log.info(m + " aborting test... we actually connected to the remote port...");
            return; // we accidentally connected ... abort the test here
        } catch (ProcessingException expected) {
        }

        long elapsed = System.currentTimeMillis() - startTime;
        _log.info(m + " Request finished in " + elapsed + "ms");
        if (elapsed > CXF_TIMEOUT + MARGIN) {
            fail("Did not timeout within the CXF-specific connection timeout, waited " + elapsed + "ms");
        }
    }

    @Test
    public void testCXFReadTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testCXFReadTimeout";
        long CXF_TIMEOUT = 5000;
        long MARGIN = defaultMargin;
        if (isAIX || isWindows) {
            MARGIN = slowHardwareMargin;
        }    
        
        Client client = ClientBuilder.newBuilder()
                                     .property("client.ReceiveTimeout", CXF_TIMEOUT)
                                     .build();
        
        long startTime = System.currentTimeMillis();
        Response r = null;
        try {
            r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/20000").request().get();
            _log.info(m + " Received " + r.getStatus() + " " + r.readEntity(String.class));
            fail("Did not time out as expected...");
        } catch (ProcessingException expected) {
        }

        assertNull(r);
        long elapsed = System.currentTimeMillis() - startTime;
        _log.info(m + " Request finished in " + elapsed + "ms");
        if (elapsed > CXF_TIMEOUT + MARGIN) {
            fail("Did not timeout within the CXF-specific read timeout, waited " + elapsed + "ms");
        }
    }

    @Test
    public void testIBMConnectTimeoutOverridesCXFConnectTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testIBMConnectTimeoutOverridesCXFConnectTimeout";
        String target = null;
        long IBM_TIMEOUT = 5000;
        long MARGIN = defaultMargin;
        long CXF_TIMEOUT = 35000;
        if (isAIX || isWindows) {
            MARGIN = slowHardwareMargin;
            CXF_TIMEOUT = 66000;
        }    
        
        Client client = ClientBuilder.newBuilder()
                                     .property("com.ibm.ws.jaxrs.client.connection.timeout", IBM_TIMEOUT)
                                     .property("client.ConnectionTimeout", CXF_TIMEOUT)
                                     .build();
        
        // https://stackoverflow.com/a/904609/6575578
        target = "http://10.255.255.1/blah";
        
        long startTime = System.currentTimeMillis();
        try {
            client.target(target).request().get();
            _log.info(m + " aborting test... we actually connected to the remote port...");
            return; // we accidentally connected ... abort the test here
        } catch (ProcessingException expected) {
        }

        long elapsed = System.currentTimeMillis() - startTime;
        _log.info(m + " Request finished in " + elapsed + "ms");
        if (elapsed > IBM_TIMEOUT + MARGIN) {
            fail("Did not timeout within the IBM-specific connection timeout, waited " + elapsed + "ms");
        }
    }

    @Test
    public void testIBMReadTimeoutOverridesCXFReadTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testIBMReadTimeoutOverridesCXFReadTimeout";
        long IBM_TIMEOUT = 5000;
        long MARGIN = defaultMargin;
        long CXF_TIMEOUT = 35000;
        if (isAIX || isWindows) {
            MARGIN = slowHardwareMargin;
            CXF_TIMEOUT = 66000;
        }    
        
        Client client = ClientBuilder.newBuilder()
                                     .property("com.ibm.ws.jaxrs.client.receive.timeout", IBM_TIMEOUT)
                                     .property("client.ReceiveTimeout", CXF_TIMEOUT)
                                     .build();

        long startTime = System.currentTimeMillis();
        Response r = null;
        try {
            r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/30000").request().get();
            _log.info(m + " Received " + r.getStatus() + " " + r.readEntity(String.class));
            fail("Did not time out as expected...");
        } catch (ProcessingException expected) {
        }

        assertNull(r);
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println(m + " Request finished in " + elapsed + "ms");
        if (elapsed > IBM_TIMEOUT + MARGIN) {
            fail("Did not timeout within the IBM-specific read timeout, waited " + elapsed + "ms");
        }
    }

    @Test
    public void testKeepAlive(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder()
                        .property("client.Connection", "CLOSE")
                        .build();
        Response r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/header")
                           .queryParam("h", "Connection")
                           .request()
                           .get();
        String connectionHeaderValue = r.readEntity(String.class);
        assertEquals("close", connectionHeaderValue.toLowerCase());

        client = client.property("client.Connection", "KEEP_ALIVE");
        r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/header")
                  .queryParam("h", "Connection")
                  .request()
                  .get();
        connectionHeaderValue = r.readEntity(String.class);
        assertEquals("keep-alive", connectionHeaderValue.toLowerCase());
    }

    @Test
    public void testChunkingThreshold(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<10000; i++) {
            sb.append("abc");
        }
        // entity is 30000 characters long
        Client client = ClientBuilder.newBuilder()
                                     .property("client.ChunkingThreshold", "10000")
                                     .build();
        String result = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                              .request()
                              .post(Entity.text(sb.toString()))
                              .readEntity(String.class);
        assertEquals("CHUNKING", result);

        client = ClientBuilder.newBuilder()
                              .property("client.ChunkingThreshold", "40000")
                              .build();
        result = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                       .request()
                       .post(Entity.text(sb.toString()))
                       .readEntity(String.class);
        assertEquals("30000:30000", result);
        
/* This testcase is commented out since it verifies incorrect behavior by the JDK (that they will not fix).   The JAXRS client should never receive a 100 response, however in certain circumstances it does.   
 * Customers may continue to hit this issue
 *         
        // Repeating the tests but adding the "Expect", "100-continue" header.  In this case a 100 will
        // be sent prior to the 200 containing the output.  The JDK will catch and handle this 100 and 
        // JAXRS will only get the 200 response when in streaming mode (which for now is only chunking).  
        // If not in chunking then JAXRS will receive the 100 response which will contain no returned data.
        client = ClientBuilder.newBuilder()
                        .property("client.ChunkingThreshold", "10000")
                        .build();
   
        Response response = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                              .request().header("Expect", "100-continue")
                              .post(Entity.text(sb.toString()));
        int status = response.getStatus();
        result = response.readEntity(String.class);
        
        assertEquals(200,status);
        assertEquals("CHUNKING", result);

        client = ClientBuilder.newBuilder()
                        .property("client.ChunkingThreshold", "40000")
                        .build();
        response = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                       .request().header("Expect", "100-continue")
                       .post(Entity.text(sb.toString()));
        status = response.getStatus();
        result = response.readEntity(String.class);

        // If a 100 response is received then no data will be sent.
        assertEquals(100,status);
        assertEquals("", result);
*/
    }

    @Test
    public void testAllowChunkingFalse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<10000; i++) {
            sb.append("abc");
        }
        // entity is 30000 characters long

        // Default is to enable chunking if the entity is greater than 4000 characters
        Client client = ClientBuilder.newBuilder()
                                     .build();
        String result = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                              .request()
                              .post(Entity.text(sb.toString()))
                              .readEntity(String.class);
        assertEquals("CHUNKING", result);


        client = ClientBuilder.newBuilder()
                              .property("client.AllowChunking", "false")
                              .build();
        result = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/chunking")
                       .request()
                       .post(Entity.text(sb.toString()))
                       .readEntity(String.class);
        assertEquals("30000:30000", result);
    }    
    
    @Test
    public void testProxyServer(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testProxyServer";
        Client client = ClientBuilder.newBuilder()
                        .property("client.ProxyServer", proxyHost)
                        .property("client.ProxyServerPort", proxyPort)
                        .property("client.ProxyServerType", "HTTP")
                        .property("client.AllowChunking", "false")
                        .build();
       
        Response r = client.target("http://" + myHost + ":" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                        .path("echo")
                        .path("Hello")
                        .request()
                        .get();
        
        String echoValue = r.readEntity(String.class);        
        assertEquals("hello", echoValue.toLowerCase());        
        
        client = ClientBuilder.newBuilder()
                        .property("client.ProxyServer", proxyHost)
                        .property("client.ProxyServerPort", proxyPort)
                        .property("client.ProxyServerType", "HTTP")
                        .property("client.NonProxyHosts", myHost)
                        .property("client.AllowChunking", "false")
                        .build();
        
        r = null;
        try {
            r = client.target("http://" + myHost + ":" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                            .path("echo")
                            .path("Hello")
                            .request()
                            .get();
            
            _log.info(m + " Received " + r.getStatus() + " " + r.readEntity(String.class));
            fail("Did not fail as expected...");
        } catch (ProcessingException expected) {
        }
        assertNull(r);   
    }
    
    @Test
    public void testDecoupledEndpoint(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int decoupledEndpointPort = req.getServerPort() + 1;
        Client client1 = ClientBuilder.newBuilder()                        
                        .build();        
        
        Response r1 = client1.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                        .path("echo")
                        .path("Hello")
                        .request()
                        .get();
        
        String echoValue1 = r1.readEntity(String.class);        
        assertEquals("hello", echoValue1.toLowerCase());
        
        // DecoupledEndpoint should have no effect on the response
        
        Client client2 = ClientBuilder.newBuilder()
                        .property("client.DecoupledEndpoint", "http://localhost:" + decoupledEndpointPort + "/decoupled_endpoint")
                        .build();        
        
        Response r2 = client2.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                        .path("echo")
                        .path("Hello")
                        .request()
                        .get();
        
        String echoValue2 = r2.readEntity(String.class);        
        assertEquals("hello", echoValue2.toLowerCase());
       
        assertEquals(r1.getHeaderString("Host"), r2.getHeaderString("Host"));        
    }
    
    @Test
    public void testAutoRedirect(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testAutoRedirect";
        List<String> statuses = new ArrayList<String>(Arrays.asList("301", "303", "307"));
        String status = null;
        
        Iterator<String> iterator = statuses.iterator();
        while(iterator.hasNext()) {
            status = iterator.next();
            _log.info(m + " Test status code= " + status);
           
            Client client1 = ClientBuilder.newBuilder()
                            .property("client.Connection", "KEEP_ALIVE")
                            .property("client.AutoRedirect", "true")                        
                            .property("client.AllowChunking", "false") 
                            .build();
            
            Response r1 = client1.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                            .path("redirect")
                            .path("Hello")
                            .path(status)
                            .request()
                            .get();
            
            _log.info("    " + m + " Received r1.getStatus() " + r1.getStatus());        
            String echoValue1 = r1.readEntity(String.class);        
            assertEquals("hello", echoValue1.toLowerCase());        
            
            Client client2 = ClientBuilder.newBuilder()                        
                            .property("client.AutoRedirect", "false")                        
                            .property("client.AllowChunking", "true") 
                            .build();        
            
            Response r2 = client2.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                                .path("redirect")
                                .path("Hello")
                                .path(status)                            
                                .request()
                                .get();
                
            _log.info("    " + m + " Received r2.getStatus() " + r2.getStatus());
            String echoValue2 = r2.readEntity(String.class);        
            assertNotSame("hello", echoValue2.toLowerCase());
        }        
    }
    
    @Test
    public void testAutoRedirectMultipleHops(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testAutoRedirectMultipleHops";
        List<String> statuses = new ArrayList<String>(Arrays.asList("301", "303", "307"));
        String status = null;
        
        Iterator<String> iterator = statuses.iterator();
        while(iterator.hasNext()) {
            status = iterator.next();
            _log.info(m + " Test status code= " + status);
           
            Client client1 = ClientBuilder.newBuilder()
                            .property("client.Connection", "KEEP_ALIVE")
                            .property("client.AutoRedirect", "true")                        
                            .property("client.AllowChunking", "false") 
                            .build();
            
            Response r1 = client1.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                            .path("redirecthop1")
                            .path("Hello")
                            .path(status)
                            .request()
                            .get();
            
            _log.info("    " + m + " Received r1.getStatus() " + r1.getStatus());        
            String echoValue1 = r1.readEntity(String.class);        
            assertEquals("hello", echoValue1.toLowerCase());        
            
            Client client2 = ClientBuilder.newBuilder()                        
                            .property("client.AutoRedirect", "false")                        
                            .property("client.AllowChunking", "true") 
                            .build();        
            
            Response r2 = client2.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                                .path("redirecthop1")
                                .path("Hello")
                                .path(status)                            
                                .request()
                                .get();
                
            _log.info("    " + m + " Received r2.getStatus() " + r2.getStatus());
            String echoValue2 = r2.readEntity(String.class);        
            assertNotSame("hello", echoValue2.toLowerCase());
        }        
    }
    
    @Test
    public void testMaxRetransmits(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testMaxRetransmits";
        String status = "301";        

        _log.info(m + " Test status code= " + status);
        
        Client client1 = ClientBuilder.newBuilder()
                        .property("client.Connection", "KEEP_ALIVE")
                        .property("client.AutoRedirect", "true")                        
                        .property("client.AllowChunking", "false")
                        .property("client.MaxRetransmits", -1)
                        .build();
        
        Response r1 = client1.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                        .path("redirecthop1")
                        .path("Hello")
                        .path(status)
                        .request()
                        .get();
        
        _log.info("    " + m + " Received r1.getStatus() " + r1.getStatus());        
        String echoValue1 = r1.readEntity(String.class);        
        assertEquals("hello", echoValue1.toLowerCase());        
        
        Client client2 = ClientBuilder.newBuilder()                        
                        .property("client.Connection", "KEEP_ALIVE")
                        .property("client.AutoRedirect", "true")                        
                        .property("client.AllowChunking", "false")
                        .property("client.MaxRetransmits", 1)
                        .build();        
        
        Response r2 = client2.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/")
                            .path("redirecthop1")
                            .path("Hello")
                            .path(status)                            
                            .request()
                            .get();
            
        _log.info("    " + m + " Received r2.getStatus() " + r2.getStatus());
        String echoValue2 = r2.readEntity(String.class);        
        assertNotSame("hello", echoValue2.toLowerCase());              
    }

    // -------------------------------------------------------------------------
    // Tests for set.content.type.for.empty.request (DT495859)
    // Verifies that the property works for DELETE, not just GET.
    // -------------------------------------------------------------------------

    /**
     * Baseline: GET with set.content.type.for.empty.request=false must NOT send Content-Type.
     * This already worked before the bug fix -- included as a regression guard.
     * Note: getHeaderString() returns null when absent; JAX-RS serialises null String return as "".
     */
    @Test
    public void testSetContentTypeForEmptyRequest_GET_propertyFalse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder()
                                     .property("set.content.type.for.empty.request", "false")
                                     .build();
        Response r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/header")
                           .queryParam("h", "Content-Type")
                           .request()
                           .get();
        String ct = r.readEntity(String.class);
        // null header → resource returns null → serialised as empty body ""
        assertEquals("GET with set.content.type.for.empty.request=false should NOT send Content-Type, but got: " + ct,
                     "", ct);
    }

    /**
     * Baseline: GET with set.content.type.for.empty.request=true MUST send Content-Type: *.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_GET_propertyTrue(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder()
                                     .property("set.content.type.for.empty.request", "true")
                                     .build();
        Response r = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/header")
                           .queryParam("h", "Content-Type")
                           .request()
                           .get();
        String ct = r.readEntity(String.class);
        assertEquals("GET with set.content.type.for.empty.request=true should send Content-Type: */*",
                     "*/*", ct);
    }

    /**
     * Bug reproducer (DT495859):
     * DELETE with set.content.type.for.empty.request=false must NOT send Content-Type.
     * Before the fix this fails -- Liberty ignores the property for DELETE and always
     * sends Content-Type: *.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyFalse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder()
                                     .property("set.content.type.for.empty.request", "false")
                                     .build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .delete(String.class);
        _log.info("testSetContentTypeForEmptyRequest_DELETE_propertyFalse: response body=[" + ct + "]");
        assertEquals("DELETE with set.content.type.for.empty.request=false should NOT send Content-Type",
                     "none", ct);
    }

    /**
     * DELETE with set.content.type.for.empty.request=true MUST send Content-Type: star/star.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyTrue(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder()
                                     .property("set.content.type.for.empty.request", "true")
                                     .build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .delete(String.class);
        assertEquals("DELETE with set.content.type.for.empty.request=true should send Content-Type: */*",
                     "*/*", ct);
    }

    /**
     * DELETE with no property set -- default behaviour sends Content-Type: star/star.
     * Only explicit property=false suppresses Content-Type.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyNotSet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .delete(String.class);
        assertEquals("DELETE with no property set should send Content-Type: star/star by default",
                     "*/*", ct);
    }

    /**
     * z/OS Connect reproducer (DT495859):
     * Property set on Invocation.Builder (not ClientBuilder) -- this is exactly what
     * z/OS Connect does via builder.property("set.content.type.for.empty.request", "FALSE").
     * Before the fix, the property is buried in jaxrs.filter.properties and never reaches
     * getContextualProperty(), so Content-Type: star/star is still sent.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyFalse_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .delete(String.class);
        assertEquals("DELETE with set.content.type.for.empty.request=false on InvocationBuilder should NOT send Content-Type",
                     "none", ct);
    }

    /**
     * z/OS Connect reproducer (DT495859) using method() with null entity --
     * exactly matching builder.method(httpMethod, null) call pattern used by z/OS Connect.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyFalse_onInvocationBuilder_methodWithNullEntity(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .method("DELETE", String.class);
        assertEquals("DELETE via method() with set.content.type.for.empty.request=false on InvocationBuilder should NOT send Content-Type",
                     "none", ct);
    }

    /**
     * Verify set.content.type.for.empty.request=true on InvocationBuilder still sends Content-Type.
     * This ensures the property IS reachable via getContextualProperty() when set on InvocationBuilder.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_DELETE_propertyTrue_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "true")
                          .delete(String.class);
        assertEquals("DELETE with set.content.type.for.empty.request=true on InvocationBuilder should send Content-Type: star/star",
                     "*/*", ct);
    }

    // -------------------------------------------------------------------------
    // Tests for POST, PUT, HEAD, OPTIONS with set.content.type.for.empty.request
    // These verify the InvocationBuilder dual-write fix covers all empty-body
    // HTTP methods, not just DELETE.
    // -------------------------------------------------------------------------

    /**
     * POST with no body and set.content.type.for.empty.request=false on InvocationBuilder.
     * Unlike DELETE, the JDK HttpURLConnection automatically sets Content-Type:
     * application/x-www-form-urlencoded for empty POST requests before CXF's transport
     * layer runs. This means CXF sees contentTypeSet=true and skips the dropContentType
     * logic entirely -- so the property cannot suppress Content-Type for POST with no body.
     * This test documents that known JDK behaviour and verifies the InvocationBuilder
     * dual-write fix does not break it.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_POST_noBody_propertyFalse_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .method("POST", String.class);
        // JDK HttpURLConnection sets application/x-www-form-urlencoded for empty POST
        // before CXF transport runs -- the property cannot suppress it for POST
        assertEquals("POST with no body: JDK HttpURLConnection sets Content-Type regardless of the property",
                     "application/x-www-form-urlencoded", ct);
    }

    /**
     * PUT with no body and set.content.type.for.empty.request=false on InvocationBuilder
     * must NOT send Content-Type.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_PUT_noBody_propertyFalse_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .method("PUT", String.class);
        assertEquals("PUT with no body and set.content.type.for.empty.request=false on InvocationBuilder should NOT send Content-Type",
                     "none", ct);
    }

    /**
     * HEAD with set.content.type.for.empty.request=false on InvocationBuilder must NOT send
     * Content-Type. HEAD responses have no body so the result is returned via the
     * X-Received-Content-Type response header set by the server endpoint.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_HEAD_propertyFalse_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        javax.ws.rs.core.Response response = client
                          .target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .head();
        String ct = response.getHeaderString("X-Received-Content-Type");
        assertEquals("HEAD with set.content.type.for.empty.request=false on InvocationBuilder should NOT send Content-Type",
                     "none", ct);
    }

    /**
     * OPTIONS with set.content.type.for.empty.request=false on InvocationBuilder must NOT send
     * Content-Type.
     */
    @Test
    public void testSetContentTypeForEmptyRequest_OPTIONS_propertyFalse_onInvocationBuilder(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        String ct = client.target("http://localhost:" + req.getServerPort() + "/cxfClientPropsApp/resource/contentTypeCheck")
                          .request(javax.ws.rs.core.MediaType.TEXT_PLAIN)
                          .property("set.content.type.for.empty.request", "false")
                          .method("OPTIONS", String.class);
        assertEquals("OPTIONS with set.content.type.for.empty.request=false on InvocationBuilder should NOT send Content-Type",
                     "none", ct);
    }
}