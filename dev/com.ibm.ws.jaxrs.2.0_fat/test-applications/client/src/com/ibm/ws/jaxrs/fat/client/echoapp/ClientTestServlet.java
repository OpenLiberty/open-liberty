/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.client.echoapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ibm.ws.jaxrs.fat.client.jaxb.Echo;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = -6383539037018936934L;
    private static final String CONTEXT_ROOT = "client";
    private static String serverIP;
    private static String serverPort;

    private static Client client;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            client = ClientBuilder.newClient();
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            serverIP = req.getLocalAddr();
            serverPort = String.valueOf(req.getLocalPort());
            m.put("serverIP", serverIP);
            m.put("serverPort", serverPort);

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        } finally {
            client.close();
        }
    }

    private static String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + CONTEXT_ROOT + "/" + path;
    }

    // convenience method for accessing resources on the server.
    private String runGetMethod(String requestURL) {

        StringBuilder lines = new StringBuilder();
        HttpURLConnection con = null;
        String sep = System.getProperty("line.separator");
        try {
            URL url = new URL(requestURL);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            int retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            lines.append("RETURN CODE FROM SERVER: " + retcode + sep);
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

        } catch (Exception ex) {
            lines.append(sep + "Exception occured in runGetMethod of testcase: " + ex + sep);
        } finally {
            con.disconnect();
        }
        return lines.toString();
    }

    private void checkExpected(String result, String expected) {
        if (!result.contains(expected)) {
            System.out.println("============= expected to find this: \n" + expected +
                               "\n =========== but it is not present in this output: \n " + result +
                               "\n====================================");
            fail("did not find expected output in result from servlet, see systemout for details");
        }
    }

    private void checkNotExpected(String result, String notExpected) {
        if (result.contains(notExpected)) {
            System.out.println("============= expected to NOT find this: \n" + notExpected +
                               "\n =========== but it IS  present in this output: \n " + result +
                               "\n====================================");
            fail("found unexpected output in result from servlet, see systemout for details");
        }
    }

    /*
     * test that client configuration inside a server app respects the webtarget declarations in server.xml
     * see publish\servers\com.ibm.ws.jaxrs.fat.client\server.xml for the declarations we're testing against.
     * Note that these tests don't actually invoke the client and test the functionality of the properties,
     * that is covered elsewhere. We only verify that the right properties make it out of server.xml
     * and onto the WebTarget instance.
     *
     */
    public void testClientConfigPropertyMerging(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=mergeme1 value=mergeme_resourceref*";
        checkExpected(result, expected);
        ret.append("OK");
    }

    /*
     * for the 3 sso property shortnames, check that the correct long name is set.
     */
    public void testClientConfigPropertiesSaml(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/saml";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.saml.sendToken value=true";
        checkExpected(result, expected);
        ret.append("OK");
    }

    public void testClientConfigPropertiesOauth(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/oauth";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.oauth.sendToken value=true";
        checkExpected(result, expected);
        ret.append("OK");
    }

    public void testClientConfigPropertiesLtpa(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/ltpa";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.ltpa.handler value=true";
        checkExpected(result, expected);
        ret.append("OK");
    }

    // in this case there should be no authntoken in the properties and we should have a warning message in the lgos
    public void testClientConfigPropertiesBogus(Map<String, String> param, StringBuilder ret) {

        /*
         * RemoteFile messages = null;
         * try {
         * messages = server.getMatchingLogFile("messages.log");
         * server.setMarkToEndOfLog(messages);
         *
         * } catch (Exception e) {
         * e.printStackTrace(System.out);
         * }
         */

        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/bogus";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        checkNotExpected(result, "authnToken");
        ret.append("OK");
        /*
         * this is hopelessly inefficient, out for now until I can find some way that doesn't take an eternity
         * //argh, until we go through translation have to check for raw key and later, the message number
         * //result = server.waitForStringInLogUsingMark("warn.invalid.authorization.token.type", 5000);
         * result = server.waitForStringInLog("warn.invalid.authorization.token.type", messages);
         * if (result == null)
         * result = server.waitForStringInLogUsingMark("CWWKW0061W", messages);
         * if (result == null)
         * fail("did not find warn.invalid.authorization.token.type or CWWKW0061W in messages.log");
         */
    }

    /*
     * test that proxy param shortnames are correctly translated to long name
     */
    public void testClientConfigProxyProps(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/proxy";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.proxy.host value=myProxyHost";
        checkExpected(result, expected);

        expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.proxy.port value=55555";
        checkExpected(result, expected);

        expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.proxy.type value=HTTP";
        checkExpected(result, expected);
        ret.append("OK");
    }

    /*
     * test that timeout shortnames are correctly translated and inserted.
     */
    public void testClientConfigPropertiesTimeouts(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/timeouts";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.connection.timeout value=5000";
        checkExpected(result, expected);
        expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.receive.timeout value=5000";
        checkExpected(result, expected);
        ret.append("OK");
    }

    public void testClientConfigPropertiesMisc(Map<String, String> param, StringBuilder ret) {
        String url = getAddress("ClientConfigTestServlet") + "?url=http://localhost:56789/misc";
        System.out.println("accessing url: " + url);
        String result = runGetMethod(url);
        String expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.ssl.config value=mysslRef";
        checkExpected(result, expected);
        expected = "url=resource/foo key=com.ibm.ws.jaxrs.client.disableCNCheck value=true";
        checkExpected(result, expected);
        ret.append("OK");
    }

    /**
     * Test that the system property for the read timeout can be overwritten
     * programatically
     */
    public void testOverrideReadTimeout(Map<String, String> param, StringBuilder ret) {
        Client temp = getReadTimeoutClient();
        assertEquals("20000", temp.getConfiguration().getProperty("http.receive.timeout"));
        temp.close();
        ret.append("OK");
    }

    /**
     * Test that the system property for the connect timeout can be overwritten
     * programmatically
     */
    public void testOverrideConnectTimeout(Map<String, String> param, StringBuilder ret) {
        Client temp = getConnectTimeoutClient();
        assertEquals("40000", temp.getConfiguration().getProperty("http.connection.timeout"));
        temp.close();
        ret.append("OK");
    }

    /**
     * Test that a request is processed if it takes less time than the timeout
     * value
     */
    public void testReadTimeoutNoTimeout(Map<String, String> param, StringBuilder ret) {
        WebTarget target = client.target(getAddress("client/timeout") + "?timeout=5000");
        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertEquals("request processed", response.readEntity(String.class));
        ret.append("OK");
    }

    /**
     * Test that the client times out if the request is not processed in less
     * than the readTimeout value
     */
    public void testReadTimeoutTimeout(Map<String, String> param, StringBuilder ret) {
        Client temp = getReadTimeoutClient(); // timeout is 20 seconds
        WebTarget target = temp.target(getAddress("client/timeout") + "?timeout=21000");
        try {
            target.request().get();
            fail("The client did not timeout after waiting more than 21000 milliseconds for the request.");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("SocketTimeoutException") != -1);
        } finally {
            temp.close();
        }
        ret.append("OK");
    }

    private Client getReadTimeoutClient() {
        Client client = ClientBuilder.newClient().property("http.receive.timeout", "20000");
        return client;
    }

    private Client getConnectTimeoutClient() {
        Client client = ClientBuilder.newClient().property("http.connection.timeout", "40000");
        return client;
    }

    /**
     * If the Accept header is already set, then the Accept Header handler
     * should not attempt to set it. This is particularly useful for types like
     * String which would do MediaType.WILDCARD.
     *
     * @throws JSONException
     * @throws JAXBException
     */
    // Test won't work because no serializer for class org.json.JSONObject
    public void testAcceptHeaderSet(Map<String, String> param, StringBuilder ret) throws Exception {

        String s = client.target(getAddress("echo/echoaccept")).request().accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
        JSONObject j = new JSONObject(new JSONTokener(s));
        assertEquals("echo: " + MediaType.APPLICATION_JSON, j.get("value"));

        s = client.target(getAddress("echo/echoaccept")).request().accept(MediaType.TEXT_XML).get(String.class);
        Echo e = (Echo) JAXBContext.newInstance(Echo.class).createUnmarshaller().unmarshal(new StringReader(s));
        assertEquals(e.getValue(), "echo: " + MediaType.TEXT_XML);

        /*
         * this is actually a trick to make sure that plain text is returned.
         * the server side piece does not produce APPLICATION_XML but will
         * produce TEXT_PLAIN for any non-produced type. it really should return
         * 415 status code.
         */
        s = client.target(getAddress("echo/echoaccept")).request().accept(MediaType.APPLICATION_XML).get(String.class);
        assertEquals("echo: " + MediaType.APPLICATION_XML, s);
        ret.append("OK");
    }

    /**
     * If the Accept header is not set, then let the AcceptHeaderHandler set it automatically.
     */
    public void testAcceptHeaderNotSetString(Map<String, String> param, StringBuilder ret) {
        String s = client.target(getAddress("echo/echoaccept")).request().get(String.class);
        assertEquals("echo: " + MediaType.WILDCARD, s);
        ret.append("OK");
    }

    /**
     * If no entity class is specified in the initial GET, then the AcceptHeaderHandler should not set anything. However, the
     * underlying client may set the header as a failsafe.
     */
    public void testAcceptHeaderNoEntity(Map<String, String> param, StringBuilder ret) {
        Response resp = client.target(getAddress("echo/echoaccept")).request().get();
        /*
         * in this case the underlying client set the WILDCARD header for
         * default HttpURLConnection based client.
         */
        assertEquals("echo: " + MediaType.WILDCARD, resp.readEntity(String.class));
        ret.append("OK");
    }

    /**
     * For JAXB objects, the AcceptHeaderHandler should automatically
     * take care of the Accept header.
     */
    public void testAcceptHeaderForJAXB(Map<String, String> param, StringBuilder ret) {
        client = ClientBuilder.newClient();//(config);
        Echo p = client.target(getAddress("echo/echoaccept")).request().accept(MediaType.TEXT_XML, MediaType.APPLICATION_XML).get(Echo.class);
        assertTrue(p.getValue().contains(MediaType.TEXT_XML) && p.getValue().contains(MediaType.APPLICATION_XML));
        ret.append("OK");
    }

    /**
     * For JSON objects, the AcceptHeaderHandler should automatically
     * take care of the Accept header.
     */
    // Test won't work because no serializer for class org.json.JSONObject
    public void testAcceptHeaderForJSON(Map<String, String> param, StringBuilder ret) throws JSONException {
        JSONObject j = client.target(getAddress("echo/echoaccept")).request().get(JSONObject.class);
        String v = j.getString("value");
        assertTrue(v.contains(MediaType.APPLICATION_JSON) && v.contains("application/javascript"));
        ret.append("OK");
    }

    /**
     * If the Accept header is not set, then let the client set a default to
     * send. In regular RestClient, it is set to {@link MediaType.WILDCARD}
     */
    public void testNoAcceptHeaderNotSetString(Map<String, String> param, StringBuilder ret) {
        String s = client.target(getAddress("echo/echoaccept")).request().get(String.class);
        assertEquals("echo: " + MediaType.WILDCARD, s);
        ret.append("OK");
    }

    /**
     * If no entity class is specified in the initial GET, then the AcceptHeaderHandler should not set anything. However, the
     * underlying client may set the header as a failsafe.
     *
     * @throws JSONException
     */
    public void testNoAcceptHeaderNoEntity(Map<String, String> param, StringBuilder ret) {
        Response resp = client.target(getAddress("echo/echoaccept")).request().get();
        /*
         * in this case the underlying client set the WILDCARD header for
         * default HttpURLConnection based client.
         */
        assertEquals("echo: " + MediaType.WILDCARD, resp.readEntity(String.class));
        ret.append("OK");
    }

    /**
     * For JAXB objects, there will be an error as the resource will return a
     * text/plain representation.
     */
    public void testNoAcceptHeaderForJAXB(Map<String, String> param, StringBuilder ret) throws Exception {
        String msg = "No message body reader has been found for class com.ibm.ws.jaxrs.fat.client.jaxb.Echo, ContentType: text/plain";
        try {
            client.target(getAddress("echo/echoaccept")).request().get(Echo.class);
            fail("The GET request returned successfully, when an exception indicating that a MessageBodyReader could not be found was expected");

        } catch (RuntimeException e) {
            assertTrue("Unexpected exception message text: " + e.getMessage(), e.getMessage().startsWith(msg));
        }
        ret.append("OK");
    }
}
