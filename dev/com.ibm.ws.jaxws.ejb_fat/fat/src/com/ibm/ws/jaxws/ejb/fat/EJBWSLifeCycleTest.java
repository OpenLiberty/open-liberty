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
package com.ibm.ws.jaxws.ejb.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class EJBWSLifeCycleTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbwslifecycle")
    public static LibertyServer server;

    private static final String ejbwslifecyclejar = "EJBWSLifeCycle";
    private static final String ejbwslifecycleclientwar = "EJBWSLifeCycleClient";
    private static final String ejbwslifecycleear = "EJBWSLifeCycle";

    private static String SERVICE_ADDRESS = null;

    private static final String SERVLET_PATH = "/EJBWSLifeCycleClient/EJBWSLifeCycleTestServlet".toString();

    private static String[] expectedOutout = {
                                               "sessionContext is not null",
                                               "com.ibm.ws.jaxws.ejblifecycle.SimpleEcho: postConstruct is invoked",
                                               "com.ibm.ws.jaxws.ejblifecycle.SimpleEcho: echo is invoked",
                                               "com.ibm.ws.jaxws.ejblifecycle.SimpleEcho: PreDestroy is invoked"
    };

    @Before
    public void before() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwslifecyclejar + ".jar", "com.ibm.ws.jaxws.ejblifecycle.*");

        WebArchive war = ShrinkWrap.create(WebArchive.class, ejbwslifecycleclientwar + ".war").addPackages(true, "com.ibm.ws.jaxws.ejblifecycle.client");
        ShrinkHelper.addDirectory(war, "test-applications/EJBWSLifeCycleClient/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwslifecycleear + ".ear").addAsModule(jar).addAsModule(war);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        SERVICE_ADDRESS = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/EJBWSLifeCycle/SimpleEchoService").toString();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testLifeCycle() throws Exception {
        String encodedServiceAddress = URLEncoder.encode(SERVICE_ADDRESS, "utf-8");

        String servletUrl = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?url=").append(encodedServiceAddress).toString();

        String expectedValue = "Hello EJBWSLifeCycle";

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK, 10);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        assertTrue("The response should be '" + expectedValue + "', while the actual is '" + line + "'", expectedValue.equals(line));

        uninstallApplications();

        // Test invoke sequence
        assertStatesExistedFromMark(true, 5000, expectedOutout);
    }

    private void assertStatesExistedFromMark(boolean needReset, long timeout, String... states) {
        if (needReset) {
            server.resetLogOffsets();
        }

        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLogUsingMark(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }

    private void uninstallApplications() throws Exception {
        try {
            RemoteFile jarFile = server.getFileFromLibertyServerRoot("dropins/EJBWSLifeCycle.ear/EJBWSLifeCycle.jar");
            jarFile.delete();
        } catch (FileNotFoundException e) {
            Log.warning(this.getClass(), e.getMessage());
        }

        try {
            RemoteFile warFile = server.getFileFromLibertyServerRoot("dropins/EJBWSLifeCycle.ear/EJBWSLifeCycleClient.war");
            warFile.delete();
        } catch (FileNotFoundException e) {
            Log.warning(this.getClass(), e.getMessage());
        }

        try {
            RemoteFile appFile = server.getFileFromLibertyServerRoot("dropins/EJBWSLifeCycle.ear");
            appFile.delete();
        } catch (FileNotFoundException e) {
            Log.warning(this.getClass(), e.getMessage());
        }
        assertNotNull("Application EJBWSLifeCycle does not appear to have removed.", server.waitForStringInLog(" CWWKT0017I:.*EJBWSLifeCycle"));

    }
}
