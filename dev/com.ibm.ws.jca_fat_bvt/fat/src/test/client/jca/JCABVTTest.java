/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.client.jca;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Basic tests covering use of a generic resource adapter.
 */
public class JCABVTTest {
    private static SharedOutputManager outputMgr;

    private static final String PORT = System.getProperty("HTTP_default", "8000");

    /**
     * Utility method to run a test on JDBCBVTServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test) throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/bvtapp?test=" + test);
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
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        checkMessage();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        checkMessage();
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testActivationSpec() throws Exception {
        runInServlet("testActivationSpec");
    }

    @Test
    public void testAdminObjects() throws Exception {
        runInServlet("testAdminObjects");
    }

    @Test
    public void testContainerManagedAuth() throws Exception {
        runInServlet("testContainerManagedAuth");
    }

    @Test
    public void testConnectionFactory() throws Exception {
        runInServlet("testConnectionFactory");
    }

    @Test
    public void testDirectLookups() throws Exception {
        runInServlet("testDirectLookups");
    }

    @Test
    public void testSharing() throws Exception {
        runInServlet("testSharing");
    }

    @Test
    public void testTimer() throws Exception {
        runInServlet("testTimer");
    }

    @Test
    public void testWorkContext() throws Exception {
        runInServlet("testWorkContext");
    }

    @Test
    public void testWorkContextInflow() throws Exception {
        runInServlet("testWorkContextInflow");
    }

    @Test
    public void testXARecovery() throws Exception {
        runInServlet("testXARecovery");
    }

//    @Test  Apparently its not possible to look at the whole message.log in a bvt test?
    public void testService_ObjectClassMatching() throws Exception {
        assertTrue("Expected 'CFReference successfully bound resource factory' in log", checkMessage());
    }

    static boolean serviceObjectClassMessageFound;

    private static boolean checkMessage() {
        serviceObjectClassMessageFound |= outputMgr.checkForMessages("CFReference successfully bound resource factory");
        return serviceObjectClassMessageFound;
    }
}
