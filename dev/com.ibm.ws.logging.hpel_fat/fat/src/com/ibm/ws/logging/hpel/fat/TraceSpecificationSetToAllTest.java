/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This FAT test is written against RTC defect 137645 and 99171
 */
@RunWith(FATRunner.class)
public class TraceSpecificationSetToAllTest {

    @Server("HpelServer")
    public static LibertyServer server;
    private static final int CONN_TIMEOUT = 60;
    private final Class<?> c = TraceSpecificationSetToAllTest.class;

    RemoteFile rProfRootDir = null;
    RemoteFile rProfBinFile = null;

    private static final String traceSpecification = "com.ibm.ws.logging.*=all";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        if (!CommonTasks.isHpelEnabled(server)) {
            // HPEL is not enabled.
            CommonTasks.writeLogMsg(Level.INFO, "HPEL is not enabled on " + server.getServerName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(server, true);
            // if HPEL was not enabled, make sure trace spec is not valid to ensure restart below.

        }

        CommonTasks.setHpelTraceSpec(server, traceSpecification);

        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.logging.trace.specification", "com.ibm.ws.logging.*=all:com.ibm.ws.org.*=all=enabled");

        CommonTasks.writeLogMsg(Level.INFO, "Bouncing server for new spec to take effect. Stopping application server");
        server.stopServer();

        // Start Server
        CommonTasks.writeLogMsg(Level.INFO, "Starting the application server");
        server.startServer();

        CommonTasks.writeLogMsg(Level.INFO, "Checking the trace spec post app server restart: "
                                            + CommonTasks.getHpelTraceSpec(server));

        assertTrue("Failed assertion that HPEL trace specification is set to " + traceSpecification,
                   traceSpecification.contains(CommonTasks.getHpelTraceSpec(server)));
    }

    /**
     * This fat test ensures that if we set trace specification as "com.ibm.ws.logging.trace.specification=*=all"
     * and write the entry, exit level messages then servlet request goes well and
     * server does not throw StackOverflow exception.
     *
     * RTC WI : 137645
     *
     * @throws Exception
     */
    @Test
    public void testResourceInjectionWorkNotGivingStackOverflow() throws Exception {
        String arg1 = "view";
        String compareString;

        String txt = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/HpelFat/WritingCustomLogServlet";
        URL url = new URL(txt);
        Log.info(c, "testResourceInjectionWorkNotGivingStackOverflow", "Calling customLogger Application with URL=" + url.toString());

        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The response did not contain \'Servlet successfullly completed\' it's content was: " + line,
                   line.contains("Servlet successfullly completed"));

        compareString = "StackOverFlow";
        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { arg1, server.getServerName(), "--includeInstance=latest" });
        Log.info(c, "testResourceInjectionWorkNotGivingStackOverflow", "Server Name : " + server.getServerName());

        String out = lvPrgmOut.getStdout();
        assertFalse(compareString, out.contains(compareString));

        /**
         * This assert verify RTC WI : 99171
         * Defect says that HPEL logs does not matches with text log. And comparison string used was
         * "Event:org.osgi.framework.BundleEvent[source=com.ibm.ws.logging.osgi"
         * We have replicated the same scenario here.
         */
        compareString = "BundleEvent STARTING";
        assertTrue(compareString, out.contains(compareString));

        compareString = "Event:org.osgi.framework.BundleEvent[source=com.ibm.ws.logging.osgi";
        assertTrue(compareString, out.contains(compareString));
    }

    /**
     * A method to drive the execution of the binaryLog tool on the remote machine/server that is being tested.
     *
     * @throws Exception
     */
    private ProgramOutput exeBinaryLog(String[] cmdLineOptions) throws Exception {
        // make platform agnostic to handle .sh and .bat
        String exeExt = "";
        final String BINARY_LOG = "binaryLog";

        if (server.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
        }
        rProfRootDir = new RemoteFile(server.getMachine(), server.getInstallRoot());
        rProfBinFile = new RemoteFile(server.getMachine(), rProfRootDir, "bin");
        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(server.getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(BINARY_LOG).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }

        Log.info(c, "exeBinaryLog", "executing: " + cmd.toString());
        return server.getMachine().execute(cmd.toString(), rProfBinFile.getAbsolutePath());
    }
}
