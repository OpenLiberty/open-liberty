/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context;

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

import componenttest.annotation.ExpectedFFDC;

public class ContextServiceTest {
    private static SharedOutputManager outputMgr;

    private String getPort() {
        return System.getProperty("HTTP_default", "9080");
    }

    /**
     * Utility method to run a test on the servlet.
     * 
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test) throws IOException {
        URL url = new URL("http://localhost:" + getPort() + "/contextbvt?test=" + test);
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

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @Test
    public void testApplyDefaultContextForAllContextTypes() throws Exception {
        runInServlet("testApplyDefaultContextForAllContextTypes");
    }

    @Test
    public void testApplyDefaultContextForUnconfiguredContextTypes() throws Exception {
        runInServlet("testApplyDefaultContextForUnconfiguredContextTypes");
    }

    @Test
    public void testCaptureContextOfServiceComponent() throws Exception {
        runInServlet("testCaptureContextOfServiceComponent");
    }

    @Test
    public void testClassloaderContext() throws Exception {
        runInServlet("testClassloaderContext");
    }

    @Test
    public void testContextSnapshot() throws Exception {
        runInServlet("testContextSnapshot");
    }

    @Test
    public void testContextualizeAppDefinedClass() throws Exception {
        runInServlet("testContextualizeAppDefinedClass");
    }

    @Test
    public void testContextualMethods() throws Exception {
        runInServlet("testContextualMethods");
    }

    @Test
    public void testContextWithConfiguredAttribute() throws Exception {
        runInServlet("testContextWithConfiguredAttribute");
    }

    @Test
    public void testDefaultClassloaderContext() throws Exception {
        runInServlet("testDefaultClassloaderContext");
    }

    @Test
    public void testDefaultJEEMetadataContext() throws Exception {
        runInServlet("testDefaultJEEMetadataContext");
    }

    @Test
    public void testErrorsInConfig() throws Exception {
        runInServlet("testErrorsInConfig");
    }

    @Test
    public void testGetContext() throws Exception {
        runInServlet("testGetContext");
    }

    @Test
    public void testGetExecutionProperties() throws Exception {
        runInServlet("testGetExecutionProperties");
    }

    @Test
    public void testInfiniteBaseContext() throws Exception {
        runInServlet("testInfiniteBaseContext");
    }

    @Test
    public void testJEEMetadataContext() throws Exception {
        runInServlet("testJEEMetadataContext");
    }

    @Test
    public void testMultipleContextServices() throws Exception {
        runInServlet("testMultipleContextServices");
    }

    @Test
    public void testMultipleInterfaces() throws Exception {
        runInServlet("testMultipleInterfaces");
    }

    @Test
    public void testNoContext() throws Exception {
        runInServlet("testNoContext");
    }

    @Test
    public void testOrderOfContextPropagation() throws Exception {
        runInServlet("testOrderOfContextPropagation");
    }

    @Test
    public void testProgrammaticallyAddContextConfiguration() throws Exception {
        runInServlet("testProgrammaticallyAddContextConfiguration");
    }

    @Test
    @ExpectedFFDC(value = "java.util.concurrent.RejectedExecutionException")
    public void testRejectedExecutionException() throws Exception {
        runInServlet("testRejectedExecutionException");
    }

    @Test
    public void testRunOnThreadPool() throws Exception {
        runInServlet("testRunOnThreadPool");
    }

    @Test
    public void testSerialization() throws Exception {
        runInServlet("testSerialization");
    }

    @Test
    public void testServiceRanking() throws Exception {
        runInServlet("testServiceRanking");
    }

    @Test
    public void testSingleInterface() throws Exception {
        runInServlet("testSingleInterface");
    }

    @Test
    public void testThreadFactory() throws Exception {
        runInServlet("testThreadFactory");
    }

    @Test
    public void testWSIdentifiable() throws Exception {
        runInServlet("testWSIdentifiable");
    }
}
