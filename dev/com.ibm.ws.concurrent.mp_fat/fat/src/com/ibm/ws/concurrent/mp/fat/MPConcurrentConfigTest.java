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
package com.ibm.ws.concurrent.mp.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.test.context.location.CityContextProvider;
import org.test.context.location.StateContextProvider;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.mp.fat.config.web.MPConcurrentConfigTestServlet;

@RunWith(FATRunner.class)
public class MPConcurrentConfigTest extends FATServletClient {

    private static final String APP_NAME = "MPConcurrentConfigApp";

    @Server("MPConcurrentConfigTestServer")
    @TestServlet(servlet = MPConcurrentConfigTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public StringBuilder runTestAsUser(String user, String pwd, LibertyServer server, String path, String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + '/' + path + '?' + queryString);
        Log.info(getClass(), testName.getMethodName(), "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            if (user != null) {
                String encodedAuthData = Base64.getEncoder().encodeToString((user + ":" + pwd).getBytes("UTF-8"));
                con.setRequestProperty("Authorization", "Basic " + encodedAuthData);
            }

            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf(FATServletClient.SUCCESS) < 0) {
                Log.info(getClass(), testName.getMethodName(), "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.config.web");

        JavaArchive customContextProviders = ShrinkWrap.create(JavaArchive.class, "customContextProviders.jar")
                        .addPackage("org.test.context.location")
                        .addAsServiceProvider(ThreadContextProvider.class, CityContextProvider.class, StateContextProvider.class);
        ShrinkHelper.exportToServer(server, "lib", customContextProviders);

        server.startServer();

        // Await an extra message which indicates that security is ready
        assertNotNull(server.waitForStringInLog("CWWKS0008I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Verifies that a managed executor configured with cleared=ALL_REMAINING clears security context for async actions/tasks.
     */
    @Test
    public void testSecurityContextIsClearedWhenClearedIsAllRemaining() throws Exception {
        runTestAsUser("user1", "pwd1", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testSecurityContextClearedWhenAllRemaining");
    }

    /**
     * Verifies that a managed executor configured with cleared=SECURITY,... clears security context for async actions/tasks.
     */
    @Test
    public void testSecurityContextIsClearedWhenClearedIsSecurity() throws Exception {
        runTestAsUser("user1", "pwd1", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testSecurityContextCleared");
    }

    /**
     * Verifies that a managed executor configured with propagated=ALL_REMAINING propagates security context to async actions/tasks.
     */
    @Test
    public void testSecurityContextIsIncludedWhenPropagatedIsAllRemaining() throws Exception {
        runTestAsUser("user1", "pwd1", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testSecurityContextPropagatedWhenAllRemaining");
    }

    /**
     * Chain several dependent stages to an incomplete CompletionStage, where the dependent stages run as different users from each other,
     * and from a separate request that completes the incomplete stage, allowing the others to run. With Security context configured to propagate,
     * each action must run as its respective user, and the original security context must be restored on the thread afterward.
     */
    @Test
    public void testSecurityContextIsPropagatedWhenPropagatedIsSecurity() throws Exception {
        runTestAsUser(null, null, server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testCreateCompletionStageThatRequiresUserPrincipal");
        runTestAsUser("user3", "pwd3", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testCreateCompletionStageThatRequiresUserPrincipal");
        runTestAsUser("user2", "pwd2", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testCreateCompletionStageThatRequiresUserPrincipal");
        runTestAsUser("user1", "pwd1", server, APP_NAME + "/MPConcurrentConfigTestServlet", "testMethod=testCompletionStageAccessesUserPrincipal");
    }
}
