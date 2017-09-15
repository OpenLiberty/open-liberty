/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.aries;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.AfterClass;

import com.ibm.componenttest.common.ComponentTest;
import com.ibm.componenttest.common.Record;
import componenttest.topology.impl.LibertyServer;

public abstract class AriesFatTestBase {
    protected static LibertyServer server;
    protected static AriesTopologyForLiberty topology;
    protected static Logger LOGGER;

    public static void setUp(Logger log, String serverName) throws Exception {
        setUp(log, serverName, null);
    }

    public static void setUp(Logger log, String serverName, Set<String> appNamesToValidate) throws Exception {
        preSetUp(log, serverName, appNamesToValidate);
        topology.startLibertyServer();
    }

    public static void preSetUp(Logger log, String serverName) throws Exception {
        preSetUp(log, serverName, null);
    }

    public static void preSetUp(Logger log, String serverName, Set<String> appNamesToValidate) throws Exception {
        topology = new TopologyImpl(log, serverName);
        server = topology.getServer();
        if (appNamesToValidate != null) {
            for (String appNameToValidate : appNamesToValidate) {
                server.addInstalledAppForValidation(appNameToValidate);
            }
        }
        server.pathToAutoFVTTestFiles = "lib";
        LOGGER = log;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    protected void assertTestPass(String bundleName, String testName) throws Exception {
        Record testResult = ComponentTest.getResult(bundleName, testName);
        LOGGER.info("assertTestPass : test name is " + testName + ", result message is " + testResult.getMsg());
        assertTrue(testResult.toString(), (testResult.getResult() == Record.Result.PASS));
    }

    protected static void readLog(String logFileName) {
        try {
            ComponentTest.readResultLog(new File(logFileName));
        } catch (Exception e2) {
            System.out.println("Failed to obtain test log results");
        }
    }

    /*
     * Bug 89173 improved diagnostics below (and the set-up of the calling
     * test). If errors due to the servlet not being available are still seen,
     * retry logic can be added to this method (in a similar manner to the
     * Topology.checkForComponentTestFinish method).
     */
    public String getWebContent(String testUri) throws Exception {
        // Defect 119101: we're still seeing timeouts here, typically because the JPA/OSGi
        // server is still initialising and while the context root is available, the servlets
        // aren't up behind it yet. 
        // If the first attempt fails, we'll wait 2, 4, 8, 16 and finally 32 seconds 
        // before retrying. 
        String result = null;
        int waitTime = 1;
        while (result == null) { // Forever loop prevented by 'throw x' once waitTime > 60
            try {
                result = doGetWebContent(testUri);
            } catch (Exception x) {
                waitTime *= 2;
                if (waitTime > 60) {
                    throw x;
                }
                try {
                    Thread.sleep(waitTime * 1000);
                } catch (InterruptedException ix) {
                }
            }
        }
        return result;
    }

    private String doGetWebContent(String testUri) throws Exception
    {
        HttpURLConnection con = null;
        try {
            String testServerAndUri = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUri;
            con = topology.getConnection(testServerAndUri);
            con.setRequestMethod("GET");
            InputStream is;
            try {
                is = con.getInputStream();
            } catch (IOException ioe) {
                LOGGER.severe("couldn't get input stream at uri : " + testServerAndUri);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ioe.printStackTrace(pw);
                pw.flush();
                LOGGER.severe("exception stack trace is : " + sw.toString());
                pw.close();
                int httpResponseCode = con.getResponseCode();
                LOGGER.severe("connection response code is : " + httpResponseCode);
                is = con.getErrorStream();
                LOGGER.info("error stream is : " + is);
            }
            String lineSeparator = System.getProperty("line.separator");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
                builder.append(lineSeparator);
            }
            return builder.toString();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
