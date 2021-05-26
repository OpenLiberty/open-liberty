/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
 
@RunWith(FATRunner.class)
public class JAXRSClientLtpaTest extends AbstractTest {
    private static Class<?> c = JAXRSClientLtpaTest.class;

    @Server("jaxrs20.client.JAXRSLtpaServerTest")
    public static LibertyServer serverServer;
    private final static String serverTarget = "jaxrs20ltpa";

    @Server("jaxrs20.client.JAXRSLtpaClientTest")
    public static LibertyServer clientServer;
    private final static String clientTarget = "jaxrs20ltpaclient";

    private final static String loginTitle = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultApp(serverServer, serverTarget,
                                                       "com.ibm.ws.jaxrs20.client.LtpaClientTest.client",
                                                       "com.ibm.ws.jaxrs20.client.LtpaClientTest.service");
        WebArchive app2 = ShrinkHelper.defaultApp(clientServer, clientTarget, "com.ibm.ws.jaxrs20.client.LtpaClientTest.client");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            serverServer.addInstalledAppForValidation(serverTarget);
            serverServer.startServer(true);

            clientServer.addInstalledAppForValidation(clientTarget);
            clientServer.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        
        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on serverServer",
                      serverServer.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on serverServer",
                      serverServer.waitForStringInLog("CWWKS4105I.*"));
        
        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on clientServer",
                      clientServer.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on clientServer",
                      clientServer.waitForStringInLog("CWWKS4105I.*"));        
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (serverServer != null) {
            serverServer.dumpServer("jaxrs20.client.JAXRSLtpaServerTest");
            serverServer.stopServer();
        }
        if (clientServer != null) {
            clientServer.dumpServer("jaxrs20.client.JAXRSLtpaClientTest");
            clientServer.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = clientServer;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testClientLtpaHandler_ClientWithoutToken() throws Exception {
        String result = setCookie(false);
        Assert.assertTrue("Expect to report no ltpa authentication token exception", !result.equals("Hello LTPA Resource"));
    }

    @Test
    public void testClientLtpaHandler_ClientWithToken() throws Exception {
        String result = setCookie(true);
        Assert.assertTrue("Expect access resource with sso successfully: ", result.equals("Hello LTPA Resource"));
    }

    @Test
    public void testClientWrongLtpaHandler_ClientWithoutToken() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(clientTarget + "/ClientTestServlet", "testClientWrongLtpaHander_Client", p, loginTitle);
    }

    @Test
    public void testClientWrongValueLtpaHandler_ClientWithoutToken() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(clientTarget + "/ClientTestServlet", "testClientWrongValueLtpaHander_Client", p, loginTitle);
    }

    public String setCookie(boolean setCookie) throws Exception {
        String surl = "http://" + serverRef.getHostname() + ":" + serverRef.getHttpDefaultPort() + "/" + clientTarget + "/j_security_check";
        String query = "j_username=user1&j_password=pass1&login=Login";

        String urlStr = surl;

        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", "WASReqURL=http://" + serverRef.getHostname() + ":" + serverRef.getHttpDefaultPort() + "/" + clientTarget);
        connection.setInstanceFollowRedirects(false);
        connection.connect();
        OutputStreamWriter out = new OutputStreamWriter(
                        connection.getOutputStream(), "utf-8");
        out.write(query);
        out.flush();
        out.close();

        List<String> cookieVal = connection.getHeaderFields().get("Set-Cookie");
        Log.info(c, "setCookie", "cookieVal: " + cookieVal);

        //Only this method works
        urlStr = "http://" + serverRef.getHostname() + ":" + serverRef.getHttpDefaultPort() + "/" + clientTarget + "/ClientTestServlet?test=testClientLtpaHander_Client";

        url = new URL(urlStr);
        HttpURLConnection resumeConnection = (HttpURLConnection) url
                        .openConnection();
        if (cookieVal != null) {
            String newCookie = cookieVal.toString().substring(cookieVal.toString().indexOf("HttpOnly,"), cookieVal.toString().indexOf("]"));
            newCookie = newCookie.replace("Path=/;", "");
            Log.info(c, "setCookie", "newCookie: " + newCookie);

            if (setCookie) {
                resumeConnection.setRequestProperty("Cookie", newCookie);
            } else {
                Log.info(c, "setCookie", "Doesn't set cookie, will report error when sso");
            }
        }
        resumeConnection.connect();
        InputStream urlStream = resumeConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(urlStream));
        String ss = null;
        String total = "";
        while ((ss = bufferedReader.readLine()) != null) {
            Log.info(c, "setCookie", "LTPA Cookie Test Result with cookie(" + setCookie + "): " + ss);
            total += ss;
        }
        bufferedReader.close();

        Log.info(c, "setCookie", "returning " + total);
        return total;
    }
}
