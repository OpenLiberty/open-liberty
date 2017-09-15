/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.hpel.tests;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import com.ibm.websphere.simplicity.ApplicationServer;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.VerboseTestCase;
import com.ibm.ws.fat.hpel.setup.HpelSetup;
import com.ibm.ws.fat.ras.util.CommonTasks;

/**
 *
 */
public class BinaryLogRecordContextTest extends VerboseTestCase {
    private ApplicationServer appServ = null;
    RemoteFile rProfRootDir = null;
    RemoteFile rProfBinFile = null;
    String localLogsRepositoryPath = null;
    String extension1 = "userName=[hpel]";
    String extension2 = "productId=[liberty]";

    /**
     * @param name
     */
    public BinaryLogRecordContextTest(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setUp() throws Exception {
        // Call super.SetUp() cause we still want it's setup as well
        super.setUp();
        appServ = HpelSetup.getServerUnderTest();
        // Confirm HPEL is enabled
        if (!CommonTasks.isHpelEnabled(appServ)) {
            // HPEL is not enabled.
            this.logStep("HPEL is not enabled on " + appServ.getName() + ", attempting to enable.");
            CommonTasks.setHpelEnabled(appServ, true);
            // Restart now to complete switching to HPEL
            appServ.restart();
            this.logStepCompleted();
        }

        // Liberty profile root is the install root.
        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getServerUnderTest().getBackend().getInstallRoot());
//        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), rProfRootDir, "bin");
        // Setting the bootstrap with trace specification to get the trace logs.
        CommonTasks.addBootstrapProperty(appServ, "com.ibm.ws.test", "*=all=enabled");
        appServ.stop();
        appServ.start();

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

    public void testBinaryLogRecordExtension() throws Exception {
        System.out.println("----------testLogRecordServer starts 2-------------");
        String arg1 = "view";

        //Access the application
        URL url = new URL("http://" + appServ.getBackend().getHostname() + ":" + appServ.getBackend().getHttpDefaultPort() + "/LogFat?userName=hpel&productId=liberty");
        System.out.println("appServ.getBackend().getHttpDefaultPort() :" + appServ.getBackend().getHttpDefaultPort());
        HttpURLConnection con = getHttpConnection(url);
        System.out.println("URL :" + url);
        System.out.println("Response Code :" + con.getResponseCode());
        Thread.sleep(TWENTY_SECONDS);
        ProgramOutput lvPrgmOut = exeBinaryLog(new String[] { arg1, appServ.getBackend().getServerName(), "--format=advanced", "--includeInstance=latest" });
        System.out.println("appServ.getBackend().getServerName() :" + appServ.getBackend().getServerName());
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
        if (HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            exeExt = ".bat";
//                      } else {
//                              exeExt = ".sh";
        }
        rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getServerUnderTest().getBackend().getInstallRoot());
//      rProfRootDir = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), HpelSetup.getNodeUnderTest().getProfileDir());
        rProfBinFile = new RemoteFile(HpelSetup.getNodeUnderTest().getMachine(), rProfRootDir, "bin");
        StringBuilder cmd = new StringBuilder(100);
        cmd.append(rProfBinFile.getAbsolutePath()).append(HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator());
        cmd.append(BINARY_LOG).append(exeExt).append(" ");

        for (String cmdOption : cmdLineOptions) {
            if (!cmdOption.isEmpty()) {
                cmd.append("\"" + cmdOption + "\" ");
            }
        }

        System.out.println("executing: " + cmd.toString());
//        logMsg("executing: " + rProfBinFile.getAbsolutePath() + HpelSetup.getNodeUnderTest().getMachine().getOperatingSystem().getFileSeparator() + cmd.toString());
//        return HpelSetup.getNodeUnderTest().getMachine().execute(LOG_VIEWER + exeExt, cmdLineOptions, rProfBinFile.getAbsolutePath());
        return HpelSetup.getNodeUnderTest().getMachine().execute(cmd.toString(), rProfBinFile.getAbsolutePath());

//              }
//              //iSeries LogViewer needs to be executed in the shell qsh
//              else{                                           
//                      return HpelSetup.getNodeUnderTest().getMachine().executeQSH(LOG_VIEWER, cmdLineOptions, rProfBinFile.getAbsolutePath(), null);
//              }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}