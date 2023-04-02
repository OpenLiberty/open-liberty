/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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

package suite.r80.base.jca16.gwc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jca.fat.regr.util.JCAFATTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import suite.r80.base.jca16.TestSetupUtils;

/**
 * This test case class contains calls for tests that verify the proper
 * behaviors with Work Context and Work Management. <br/>
 */
@AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "javax.resource.spi.work.WorkRejectedException" })
@RunWith(FATRunner.class)
public class GenericWorkContextTest extends JCAFATTest {
    private final static String CLASSNAME = GenericWorkContextTest.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    @Server("suite.r80.base.jca16.gwc")
    public static LibertyServer server;

    private final static Class<?> c = GenericWorkContextTest.class;

    /**
     * Utility method to run a test on GWCTestServlet.
     * 
     * @param test
     *                 Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException
     *                         if an error occurs
     */

    private StringBuilder runInServlet(String test, String servlet,
                                       String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + "/" + webmodule + "/" + servlet
                          + "?test=" + test);
        Log.info(getClass(), "runInServlet", "URL is " + url);
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
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(c, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(c, "runInServlet",
                         "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } catch (IOException x) {
            throw x;
        } finally {
            con.disconnect();
        }

    }

    @BeforeClass
    public static void setUp() throws Exception {
        TestSetupUtils.setUpGwcApp(server);
        server.startServer("GenericWorkContextTest.log");
        server.waitForStringInLog("CWWKE0002I");

        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));

        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, "tearDown");
        }
        if (server.isStarted()) {
            server.stopServer("J2CA862[3-5]E: Resource adapter gwcapp.adapter_jca16_gwc_GenericWorkContextTestRAR");
        }
    }

    /**
     * This test verifies if TestWorkContext is supported <br/>
     * The expected outcome is that False
     */
    @Test
    public void testIsTestWorkContextSupported() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testIsHintsContextSupported() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testIsTransactionContextSupported() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    // @Test
    // The test work will when the InboundSecurity feature is implemented.
    // Hence commenting the test temporarily.
    public void testIsSecurityContextSupported() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testExecutionContextNotNulldoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testExecutionContextNotNullstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testExecutionContextNotNullscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testUnsupportedWorkContextdoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testUnsupportedWorkContextstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8625E"));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testUnsupportedWorkContextscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8625E"));
        server.setMarkToEndOfLog();

    }

    @Test
    public void testUnsupportedAndSupportedWorkContextdoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testUnsupportedAndSupportedWorkContextstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8625E"));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testUnsupportedAndSupportedWorkContextscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8625E"));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testDupWorkContextdoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testDupWorkContextstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8624E"));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testDupWorkContextscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8688E"));
        assertNotNull("Test Failed : " + getTestMethodSimpleName()
                      + "WorkCompletedException is not thrown as expected",
                      server.waitForStringInLogUsingMark("J2CA8624E"));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testDupWorkContextdoWorkSub() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testTxWorkContextdoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Test
    public void testHintsWorkContextdoWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTxWorkContextstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNull("Test Failed : " + getTestMethodSimpleName()
                   + "UnExpectedException is thrown",
                   server.waitForStringInLogUsingMark("J2CA.*E.*", 30 * 1000));
        server.setMarkToEndOfLog();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTxWorkContextscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNull("Test Failed : " + getTestMethodSimpleName()
                   + "UnExpectedException is thrown",
                   server.waitForStringInLogUsingMark("J2CA.*E.*", 30 * 1000));
        server.setMarkToEndOfLog();
    }

    @Test
    public void testTxWorkContextSubdoWork() throws Exception {
        final String servletName = "GWCTestServlet";;
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        server.setMarkToEndOfLog();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTxWorkContextSubstartWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNull("Test Failed : " + getTestMethodSimpleName()
                   + "UnExpectedException is thrown",
                   server.waitForStringInLogUsingMark("J2CA.*E.*", 30 * 1000));
        server.setMarkToEndOfLog();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTxWorkContextSubscheduleWork() throws Exception {
        final String servletName = "GWCTestServlet";
        runInServlet(getTestMethodSimpleName(), servletName, "gwcweb");
        assertNull("Test Failed : " + getTestMethodSimpleName()
                   + "UnExpectedException is thrown",
                   server.waitForStringInLogUsingMark("J2CA.*E.*", 30 * 1000));
        server.setMarkToEndOfLog();
    }
}
