/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXBToolsTest {

    // This server doesn't ever get started, we just have it here to get a hold of files from the Liberty install
    @Server("com.ibm.ws.jaxb.tools.TestServer")
    public static LibertyServer server;

    private static File xjcSourceDir;
    private static File xjcTargetDir;
    private static File schemagenSourceDir;
    private static File schemagenTargetDir;

    /**
     * True if running on Windows and the .bat file should be used.
     */
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    /**
     * Environment variable that can be set to test the UNIX script on Windows.
     */
    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    @BeforeClass
    public static void setup() throws Exception {
        String serverRoot = server.getServerRoot();
        xjcSourceDir = new File(serverRoot + "/temp/xjcSourceDir");
        xjcTargetDir = new File(serverRoot + "/temp/xjcTargetDir");
        schemagenSourceDir = new File(serverRoot + "/temp/schemagenSourceDir");
        schemagenTargetDir = new File(serverRoot + "/temp/schemagenTargetDir");

        xjcTargetDir.mkdirs();
        schemagenTargetDir.mkdirs();
    }

    @Test
    public void testXJCToolWithoutTarget() throws Exception {
        String xjcArgs = new StringBuilder().append("-p po")
                        .append(" -d ")
                        .append(xjcTargetDir.getAbsolutePath())
                        .append(" ")
                        .append(xjcSourceDir.getAbsolutePath())
                        .append(File.separator)
                        .append("purchaseOrder.xsd")
                        .toString();
        RemoteFile xjc = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc");
        RemoteFile xjcBat = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc.bat");

        assertTrue("The file bin/xjc does not exist.", xjc.exists());
        assertTrue("The file bin/xjc.bat does not exist.", xjcBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(xjcBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                commandBuilder.append("/bin/sh");
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(xjc);
        }
        commandBuilder.append(" ").append(xjcArgs);

        StringBuilder outputBuilder = new StringBuilder();

        for (String line : execute(commandBuilder.toString())) {
            outputBuilder.append(line);
        }

        assertTrue("The output should contain the error id 'CWWKW0700E', but the actual is not.", outputBuilder.toString().indexOf("CWWKW0700E") >= 0);
    }

    @Test
    public void testXJCTool() throws Exception {
        String xjcArgs = new StringBuilder().append("-p po")
                        .append(" -d ")
                        .append(xjcTargetDir.getAbsolutePath())
                        .append(" -target 2.2 ")
                        .append(xjcSourceDir.getAbsolutePath())
                        .append(File.separator)
                        .append("purchaseOrder.xsd")
                        .toString();
        RemoteFile xjc = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc");
        RemoteFile xjcBat = server.getFileFromLibertyInstallRoot("bin/jaxb/xjc.bat");

        assertTrue("The file bin/xjc does not exist.", xjc.exists());
        assertTrue("The file bin/xjc.bat does not exist.", xjcBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(xjcBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                commandBuilder.append("/bin/sh");
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(xjc);
        }
        commandBuilder.append(" ").append(xjcArgs);
        execute(commandBuilder.toString());

        RemoteFile itemsJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po/Items.java");
        RemoteFile purchaseOrderTypeJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po" + File.separator
                                                                                   + "PurchaseOrderType.java");
        RemoteFile shippingAddressJavaFile = server.getFileFromLibertyServerRoot("temp/xjcTargetDir/po" + File.separator
                                                                                 + "ShippingAddress.java");
        assertTrue("Items.java does not exist.", itemsJavaFile.exists());
        assertTrue("PurchaseOrderType.java does not exist.", purchaseOrderTypeJavaFile.exists());
        assertTrue("ShippingAddress.java does not exist.", shippingAddressJavaFile.exists());
    }

    @Test
    public void testSchemaGenTool() throws Exception {
        String schemagenArgs = new StringBuilder().append("-d ")
                        .append(schemagenTargetDir.getAbsolutePath())
                        .append(" ")
                        .append(schemagenSourceDir.getAbsolutePath())
                        .append(File.separator)
                        .append("Items.java")
                        .append(" ")
                        .append(schemagenSourceDir.getAbsolutePath())
                        .append(File.separator)
                        .append("PurchaseOrderType.java")
                        .append(" ")
                        .append(schemagenSourceDir.getAbsolutePath())
                        .append(File.separator)
                        .append("ShippingAddress.java")
                        .toString();
        RemoteFile schemagen = server.getFileFromLibertyInstallRoot("bin/jaxb/schemagen");
        RemoteFile schemagenBat = server.getFileFromLibertyInstallRoot("bin/jaxb/schemagen.bat");

        assertTrue("The file bin/schemagen does not exist.", schemagen.exists());
        assertTrue("The file bin/schemagen.bat does not exist.", schemagenBat.exists());

        StringBuilder commandBuilder = new StringBuilder();
        if (isWindows && WLP_CYGWIN_HOME == null) {
            commandBuilder.append(schemagenBat);
        } else {
            if (WLP_CYGWIN_HOME == null) {
                commandBuilder.append("/bin/sh");
            } else {
                commandBuilder.append(WLP_CYGWIN_HOME + "/bin/sh");
            }
            commandBuilder.append(" -x ");
            commandBuilder.append(schemagen);
        }
        commandBuilder.append(" ").append(schemagenArgs);
        execute(commandBuilder.toString());

        RemoteFile xsdFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/schema1.xsd");
        RemoteFile itemsClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/Items.class");
        RemoteFile purchaseOrderTypeClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/PurchaseOrderType.class");
        RemoteFile shippingAddressClassFile = server.getFileFromLibertyServerRoot("temp/schemagenTargetDir/po/ShippingAddress.class");
        assertTrue("schema1.xsd does not exist.", xsdFile.exists());
        assertTrue("Items.class does not exist.", itemsClassFile.exists());
        assertTrue("PurchaseOrderType.class does not exist.", purchaseOrderTypeClassFile.exists());
        assertTrue("ShippingAddress.class does not exist.", shippingAddressClassFile.exists());
    }

    private List<String> execute(String commandLine) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        for (String arg : commandLine.split(" ")) {
            command.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        builder.environment().put("JAVA_HOME", JavaInfo.forServer(server).javaHome());

        final Process p = builder.start();
        List<String> output = new ArrayList<String>();

        Thread stderrCopier = new Thread(new OutputStreamCopier(p.getErrorStream(), output));
        stderrCopier.start();
        new OutputStreamCopier(p.getInputStream(), output).run();

        stderrCopier.join();
        p.waitFor();

        int exitValue = p.exitValue();
        if (exitValue != 0) {
            throw new IOException(command.get(0) + " failed (" + exitValue + "): " + output);
        }

        return output;
    }

    private class OutputStreamCopier implements Runnable {
        private final InputStream in;
        private final List<String> output;

        OutputStreamCopier(InputStream in, List<String> lines) {
            this.in = in;
            this.output = lines;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                boolean inEval = false;
                int carryover = 0;

                for (String line; (line = reader.readLine()) != null;) {
                    // Filter empty lines and sh -x trace output.
                    if (inEval) {
                        System.out.println("(trace eval) " + line);
                        if (line.trim().equals("'")) {
                            inEval = false;
                        }
                    } else if (line.equals("+ eval '")) {
                        inEval = true;
                        System.out.println("(trace eval) " + line);
                    } else if (carryover > 0) {
                        carryover--;
                        System.out.println("(trace) " + line);
                    } else if (line.startsWith("+") || line.equals("'")) {
                        int index = 0;
                        index = line.indexOf("+", index + 1);
                        while (index != -1) {
                            index = line.indexOf("+", index + 1);
                            carryover++;
                        }
                        System.out.println("(trace) " + line);
                    } else if (!line.isEmpty()) {
                        synchronized (output) {
                            output.add(line);
                        }
                        System.out.println(line);
                    }
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }
}
