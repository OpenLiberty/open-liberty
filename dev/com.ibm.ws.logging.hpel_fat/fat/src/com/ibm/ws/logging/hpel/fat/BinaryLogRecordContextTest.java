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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class BinaryLogRecordContextTest {
    @Server("HpelServer")
    public static LibertyServer server;
    static RemoteFile rProfRootDir = null;
    static RemoteFile rProfBinFile = null;
    String localLogsRepositoryPath = null;
    String extension1 = "userName=[hpel]";
    String extension2 = "productId=[liberty]";
    public static final long TWENTY_SECONDS = 20 * 1000;

    @BeforeClass
    public static void setUp() throws Exception {

        // Confirm HPEL is enabled
        ShrinkHelper.defaultDropinApp(server, "LogFat", "com.ibm.ws.logging.hpel");
        ShrinkHelper.defaultDropinApp(server, "HpelFat", "com.ibm.ws.logging.hpel.servlet");
        // Liberty profile root is the install root.
        rProfRootDir = new RemoteFile(server.getMachine(), server.getInstallRoot());
//        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(server.getMachine(), rProfRootDir, "bin");
        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(server, "com.ibm.ws.test", "*=all=enabled");
        server.stopServer();
        server.startServer();

    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    @Test
    public void testBinaryLogRecordExtension() throws Exception {
        System.out.println("----------testLogRecordServer starts 2-------------");
        String arg1 = "view";

        //Access the application
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/LogFat?userName=hpel&productId=liberty");
        System.out.println("server.getHttpDefaultPort() :" + server.getHttpDefaultPort());
        HttpURLConnection con = getHttpConnection(url);
        System.out.println("URL :" + url);
        System.out.println("Response Code :" + con.getResponseCode());
        Thread.sleep(TWENTY_SECONDS);
        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { arg1, server.getServerName(), "--format=advanced", "--includeInstance=latest" });
        System.out.println("server.getServerName() :" + server.getServerName());
        String out = lvPrgmOut.getStdout();
        System.out.println("Log view server --format=advanced :" + out);
        assertTrue("Extensions are added to LogRecordContext", out.contains(extension1) && out.contains(extension2));
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

        //if non-iSeries
//              if (!HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.ISERIES)){
        if (server.getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
//                      } else {
//                              exeExt = ".sh";
        }
        rProfRootDir = new RemoteFile(server.getMachine(), server.getInstallRoot());
//      rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(server.getMachine(), rProfRootDir, "bin");
        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(server.getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(BINARY_LOG).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }

        System.out.println("executing: " + cmd.toString());
//        logMsg("executing: " + rProfBinFile.getAbsolutePath() + HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator() + cmd.toString());
//        return HpelSetup.getNodeUnderTest().getMachine().execute(LOG_VIEWER + exeExt, cmdLineOptions, rProfBinFile.getAbsolutePath());
        return server.getMachine().execute(cmd.toString(), rProfBinFile.getAbsolutePath());

//              }
//              //iSeries LogViewer needs to be executed in the shell qsh
//              else{
//                      return HpelSetup.getNodeUnderTest().getMachine().executeQSH(LOG_VIEWER, cmdLineOptions, rProfBinFile.getAbsolutePath(), null);
//              }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}