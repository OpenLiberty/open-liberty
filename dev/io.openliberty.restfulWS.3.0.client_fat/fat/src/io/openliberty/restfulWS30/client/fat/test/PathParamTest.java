/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.client.fat.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
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
public class PathParamTest extends AbstractTest {

    @Server("PathParamTest")
    public static LibertyServer server;

    public static final String moduleName = "pathparam";
    private static final String stringVariable = "abc";
    private static final short shortVariable = (short) 123;
    private static final long longVariable =  -9223372036L;
    private static final double doubleVariable = 789.123;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, moduleName, "io.openliberty.restfulWS30.client.fat.pathparam");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private StringBuilder runGetMethod(String path, int exprc, String testOut, boolean check) throws IOException {
        
        String urlString = "http://localhost:" + getPort() + "/" + moduleName + path;
        URL url = new URL(urlString);        
        Log.info(this.getClass(), "runGetMethod", "Calling ClientTestApp with URL=", urlString);        
        
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET"); 

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (check) {
                if (lines.indexOf(testOut) < 0)
                    fail("Missing success message in output. " + lines);

                if (retcode != exprc)
                    fail("Bad return Code from Get. Expected " + exprc + "Got" + retcode);
            }

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @Test
    public void testStringResource() throws Exception {
        runGetMethod("/pathparam/resource/string/" + stringVariable, 200, "ok", true);
    }

    @Test
    public void testLongResource() throws Exception {
        runGetMethod("/pathparam/resource/biglong/" + longVariable, 200, "ok", true);
    }

    @Test
    public void testDoubleResource() throws Exception {
        runGetMethod("/pathparam/resource/bigdouble/" + doubleVariable, 200, "ok", true);
    }

    @Test
    public void testShortResource() throws Exception {
        runGetMethod("/pathparam/resource/smallshort/" + shortVariable, 200, "ok", true);
    }

    @Test
    public void testAllResources1() throws Exception {
        server.stopServer();
        server.startServer(true);
        runGetMethod("/pathparam/resource/smallshort/" + shortVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/bigdouble/" + doubleVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/biglong/" + longVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/string/" + stringVariable, 200, "ok", true);
    }

    @Test
    public void testAllResources2() throws Exception {
        server.stopServer();
        server.startServer(true);
        runGetMethod("/pathparam/resource/bigdouble/" + doubleVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/biglong/" + longVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/string/" + stringVariable, 200, "ok", true);
        runGetMethod("/pathparam/resource/smallshort/" + shortVariable, 200, "ok", true);
    }
}
