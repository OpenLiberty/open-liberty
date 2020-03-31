/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.fat.enterpriseApp;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EnterpriseAppTest extends FATServletClient {
    private final Class<? extends EnterpriseAppTest> c = this.getClass();
    private static final String appName = "enterpriseApp";
    private static final String warFile = "fvtweb";
    private static final String servletName = "JCAEnterpriseAppTestServlet";

    @Rule
    public TestName testName = new TestName();

    @Server("com.ibm.ws.jca.fat.enterpriseApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, warFile + ".war");
        war.addPackage("web");
        war.addPackage("web.mdb");
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml")); // includes login properties

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "enterpriseRA.rar");
        rar.as(JavaArchive.class).addPackage("com.ibm.test.jca.enterprisera");
        rar.addAsManifestResource(new File("test-resourceadapters/enterpriseAppRA/resources/META-INF/ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/enterpriseAppRA/resources/META-INF/wlp-ra.xml"));
        rar.addAsLibrary(new File("publish/shared/resources/derby/derby.jar"));

        ResourceAdapterArchive lmrar = ShrinkWrap.create(ResourceAdapterArchive.class, "loginModRA.rar");
        lmrar.as(JavaArchive.class).addPackage("com.ibm.test.jca.loginmodra");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsModule(lmrar);
        ShrinkHelper.addDirectory(ear, "lib/LibertyFATTestFiles/enterpriseApp");
        ShrinkHelper.exportToServer(server, "apps", ear);

        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }

    @Before
    public void beforeEach() throws Exception {
        System.out.println(">>> " + testName.getMethodName());
        Log.info(c, testName.getMethodName(), "Starting test: " + testName.getMethodName());
    }

    @After
    public void afterEach() throws Exception {
        System.out.println("<<< " + testName.getMethodName());
        Log.info(c, testName.getMethodName(), "Ending test: " + testName.getMethodName());
    }

    protected StringBuilder runInServlet(String test) throws Exception {
        // RepeatTests causes the test name to be appended with _EE8_FEATURES.  Strip it off so that the right
        // test name is sent to the servlet
        int index = test == null ? -1 : test.indexOf("_EE8_FEATURES");
        if (index != -1) {
            test = test.substring(0, index);
        }
        String host = server.getMachine().getHostname();
        StringBuilder lines = new StringBuilder();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/" + warFile + "/" + servletName + "?test=" + test);

        for (int numRetries = 2;; numRetries--) {
            Log.info(c, "runInJCAFATServlet", "URL is " + url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            try {
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setRequestMethod("GET");

                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");

                // Send output from servlet to console output
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                    Log.info(c, "runInServlet", line);
                }

                // Look for success message, otherwise fail test
                if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                    Log.info(c, "runInServlet", "failed to find completed successfully message");
                    fail("Missing success message in output. " + lines);
                }

                return lines;
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(getClass(), "runInJCAFATServlet", x + " occurred - will retry after 2 seconds");
                        Thread.sleep(2000); // down from 10s
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                Log.info(c, "runInJCAFATServlet", "disconnecting from servlet");
                con.disconnect();
            }
        }
    }

    @Test
    public void checkSetupTest() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testAdminObjectInjected() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testAdminObjectLookup() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testConnectionFactoryUsesLoginModule() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testDataSourceInjected() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testDataSourceLookup() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testDataSourceUsingLoginModule() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testSimpleMBeanCreation() throws Exception {
        runInServlet(testName.getMethodName());
    }

    @Test
    public void testPrizeWinner() throws Exception {
        runInServlet("testPrizeWinner&username=user1");

        runInServlet("testPrizeWinner&username=user2");

        if (runInServlet("testPrizeWinner&username=user1").indexOf("PRIZE!") > 0)
            throw new Exception("The 1st visitor should not have won a prize, but they did.");

        if (runInServlet("testPrizeWinner&username=user3").indexOf("PRIZE!") < 0)
            throw new Exception("The 3rd visitor should have won a prize, but they did not.");
    }

    @Test
    public void testCheckoutLine() throws Exception {
        runInServlet("testCheckoutLine&customer=cust0&function=ADD");
        runInServlet("testCheckoutLine&customer=cust1&function=ADD");
        if (runInServlet("testCheckoutLine&customer=cust2&function=ADD").indexOf("size=3") < 0)
            throw new Exception("Expected queue size to be 3 but it was not.");

        if (runInServlet("testCheckoutLine&function=REMOVE").indexOf("size=2") < 0)
            throw new Exception("Expected queue size to be 2 but it was not.");

        if (runInServlet("testCheckoutLine&customer=cust2&function=CONTAINS").indexOf("cust2 is in line") < 0)
            throw new Exception("Customer cust2 should be in line, but they were not.");
    }
}
