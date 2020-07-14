/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class PackageRunnableTest {
    private static String serverName = "runnableTestServer";
    private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
    private static final File runnableJar = new File("publish/" + serverName + ".jar");
    private static final File extractDirectory1 = new File("publish" + File.separator + "wlpExtract1");
    private static final File extractDirectory2 = new File("publish" + File.separator + "wlpExtract2");
    private static final File extractAndRunDir = new File("publish" + File.separator + "wlpExtractAndRun");
    private static final File extractDirectory3 = new File("publish" + File.separator + "wlpExtract3");
    private static File extractLocation = null;

    /*
     * return env as array and add WLP_JAR_EXTRACT_DIR=extractDirectory
     */
    private static String[] runEnv(String extractDirectory, boolean useDummyUserDir) {

        Map<String, String> envmap = System.getenv();
        Iterator<String> iKeys = envmap.keySet().iterator();
        List<String> envArrayList = new ArrayList<String>();

        while (iKeys.hasNext()) {
            String key = iKeys.next();
            String val = envmap.get(key);
            // Pass along except for this one special case
            if (key != "WLP_USER_DIR" || !useDummyUserDir) {
                envArrayList.add(key + "=" + val);
            }
        }
        if (extractDirectory != null) {
            String extDirVar = "WLP_JAR_EXTRACT_DIR=" + extractDirectory;
            envArrayList.add(extDirVar);
        }
        if (useDummyUserDir) {
            String dummyUserDir = extractDirectory + File.separator + "a1a" + File.separator + "b2b" + File.separator + "c3c";
            envArrayList.add("WLP_USER_DIR" + "=" + dummyUserDir);
        }
        return envArrayList.toArray(new String[0]);

    }

    @BeforeClass
    public static void setupClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {
        deleteDir(extractAndRunDir);
        deleteDir(extractDirectory1);
        deleteDir(extractDirectory2);
        deleteDir(extractDirectory3);
        if (extractLocation != null)
            deleteDir(extractLocation);
    }

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Test
    public void testRunnableJar() throws Exception {

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            System.out.println("Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        executeTheJar(extractDirectory1, false, true, false);
        checkDirStructure(extractDirectory1, true);
        extractAndExecuteMain();

    }

    /**
     * Set a bogus WLP_USER_DIR env var value and confirm that we can still find the server to launch.
     *
     * No need to attempt to execute against the extacted main as in "testRunnableJar". If we can launch the JAR
     * we'll count this as a success.
     *
     * @throws Exception
     */
    @Test
    public void testRunnableJarLaunchOnlyWithUserDirSet() throws Exception {

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            System.out.println("Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        executeTheJar(extractDirectory2, true, true, false);
        checkDirStructure(extractDirectory2, true);
    }

    /**
     * When WLP_JAR_EXTRACT_DIR is not set, the server should be completely deleted minus the /logs folder via the Shutdown Hook.
     *
     * @throws Exception
     */
    @Test
    public void testRunnableDeleteServerMinusLogsFolder() throws Exception {

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            System.out.println("Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        extractLocation = new File(executeTheJar(extractDirectory3, true, false, true));

        // wait 30s to give the shutdown script time to run in the shutdown hook
        Thread.sleep(30000);

        checkDirStructure(extractLocation, false);
    }

    /**
     * When WLP_JAR_EXTRACT_DIR is set, the server should be left as-is by the Shutdown Hook.
     *
     * @throws Exception
     */
    @Test
    public void testRunnableDoNotDeleteServerFolder() throws Exception {

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            System.out.println("Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        String extLoc = executeTheJar(extractDirectory3, false, true, true);

        assertTrue("The extract location does not match the WLP_JAR_EXTRACT_DIR. ExtractDir = " + extractDirectory3.getAbsolutePath() + " vs " + extractLocation,
                   extLoc.startsWith(extractDirectory3.getAbsolutePath()));

        assertTrue("root folder at " + extractDirectory3.getAbsolutePath() + " does not exist, but should.", extractDirectory3.exists());
    }

    /**
     * Checks that the /logs and optionally the /bin folder exist within the /wlp folder structure.
     *
     * @param extractDir
     * @param shouldBinFolderExist
     *
     */
    private void checkDirStructure(File extractDir, boolean shouldBinFolderExist) {

        StringBuffer sb = new StringBuffer();
        File[] files = extractDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            sb.append(files[i] + "\n");
        }

        File logDir = null;
        File binDir = null;

        if (extractDir.getAbsolutePath().endsWith("wlp")) {
            logDir = new File(extractDir.getAbsolutePath() + File.separator + "usr" + File.separator + "servers" + File.separator + serverName
                              + File.separator + "logs");
            binDir = new File(extractDir.getAbsolutePath() + File.separator + "bin");
        } else {
            logDir = new File(extractDir.getAbsolutePath() + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName
                              + File.separator + "logs");
            binDir = new File(extractDir.getAbsolutePath() + File.separator + "wlp" + File.separator + "bin");
        }

        assertTrue(File.separator + "logs folder at " + logDir.getAbsolutePath() + " does not exist, but should. " + sb.toString(), logDir.exists());

        if (shouldBinFolderExist) {
            assertTrue(File.separator + "bin folder at " + binDir.getAbsolutePath() + " does not exist, but should. Contents =" + sb.toString(), binDir.exists());
        } else {
            assertTrue(File.separator + "bin folder at " + binDir.getAbsolutePath() + " exists, but should not. Contents = " + sb.toString(), !binDir.exists());
        }
    }

    /**
     * @param useDummyUserDir If 'true', will execute JAR after setting the WLP_USR_DIR env variable with a bogus value (to test that we can ignore it),
     *            if 'false' we will not set a WLP_USER_DIR value on the java -jar execution
     * @throws IOException
     * @throws InterruptedException
     */
    private String executeTheJar(File extractDirectory, boolean useDummyUserDir, boolean useRunEnv, boolean useNormalStop) throws Exception, InterruptedException {

        if (!extractDirectory.exists()) {
            extractDirectory.mkdirs();
        }

        OutputStream os = new FileOutputStream(server.getLogsRoot() + File.separator + "executeTheJar.log");

        assertTrue("Extract directory " + extractDirectory.getAbsolutePath() + " does not exist.", extractDirectory.exists());

        String cmd = "java -jar " + runnableJar.getAbsolutePath();
        Process proc = null;
        if (useRunEnv == true) {
            proc = Runtime.getRuntime().exec(cmd, runEnv(extractDirectory.getAbsolutePath(), useDummyUserDir), null); // run server
        } else {
            proc = Runtime.getRuntime().exec(cmd);
        }

        // setup and start reader threads for error and output streams
//        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR", null);
//        errorReader.start();
        StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT", "CWWKF0011I", os);
        outputReader.start();

        int count = 0;

        // wait up to 90 seconds to find watch for string

        String extractLoc = null;
        boolean found = outputReader.foundWatchFor();
        while (!found && count <= 90) {

            synchronized (proc) {
                proc.wait(1000); // wait 1 second
                System.out.println("Waiting for server to complete initialization - " + count + " seconds elapsed.");
            }
            found = outputReader.foundWatchFor();
            extractLoc = outputReader.extractLoc();
            count++;
        }

        assertTrue("Server did not start successfully in time.", found);

        outputReader.setIs(null);
        if (useNormalStop != true) {
            // ensure no process left behind
            proc.destroy();
        } else {
            // stop cleanly so shutdown hook is called
            stopServer(extractLoc);
        }

        if (os != null) {
            os.close();
        }

        System.out.println("Waiting 30 seconds...to make sure all Liberty thread exiting.");
        Thread.sleep(30000); // wait 30 second

        return extractLoc;
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     *
     */
    private void extractAndExecuteMain() throws IOException, InterruptedException {
        if (!extractAndRunDir.exists()) {
            extractAndRunDir.mkdirs();
        }

        OutputStream os = new FileOutputStream(server.getLogsRoot() + File.separator + "extractAndExecuteMain.log");

        Path extractAndRunPath = extractAndRunDir.toPath();
        assertTrue("Extract and run directory " + extractAndRunDir.getAbsolutePath() + " does not exist.", extractAndRunDir.exists());
        try (JarFile jar = new JarFile(runnableJar)) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Path toPath = extractAndRunPath.resolve(entry.getName());
                    toPath.toFile().getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, toPath);
                    }
                }
            }
        }

        String cmd = "java -cp " + extractAndRunDir.getAbsolutePath() + " wlp.lib.extract.SelfExtractRun";
        Process proc = Runtime.getRuntime().exec(cmd, runEnv(null, false), null); // run server

        // setup and start reader threads for error and output streams
//        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR", null);
//        errorReader.start();
        StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT", "CWWKF0011I", os);
        outputReader.start();

        int count = 0;

        // wait up to 90 seconds to find watch for string

        boolean found = outputReader.foundWatchFor();
        while (!found && count <= 90) {

            synchronized (proc) {
                proc.wait(1000); // wait 1 second
                System.out.println("Waiting for server to complete initialization - " + count + " seconds elapsed.");
            }
            found = outputReader.foundWatchFor();
            count++;
        }

        assertTrue("Server did not start successfully in time.", found);

        outputReader.setIs(null);
        proc.destroy(); // ensure no process left behind
        System.out.println("Removing WLP installation directory: " + extractAndRunDir.getAbsolutePath());
        if (extractAndRunDir.exists()) {
            deleteDir(extractAndRunDir);
            System.out.println("WLP installation directory was removed.");
        }

        if (os != null) {
            os.close();
        }

        System.out.println("Waiting 30 seconds...to make sure all Liberty thread exiting.");
        Thread.sleep(30000); // wait 30 second
    }

    class StreamReader extends Thread {
        InputStream is;
        String type;
        OutputStream os;
        String watchFor;
        boolean foundWatchFor = false;
        String extractLoc = null;

        StreamReader(InputStream is, String type, String watchFor) {
            this(is, type, watchFor, null);
        }

        StreamReader(OutputStream os, String type, String watchFor) {
            this.os = os;
            this.type = type;
            this.watchFor = watchFor;

        }

        StreamReader(InputStream is, String type, String watchFor, OutputStream redirect) {
            this.is = is;
            this.type = type;
            this.os = redirect;
            this.watchFor = watchFor;
        }

        public void setIs(InputStream is) {
            this.is = is;
        }

        public boolean foundWatchFor() {
            return foundWatchFor;
        }

        public String extractLoc() {
            return extractLoc;
        }

        @Override
        public void run() {
            try {
                // stdin, process stream is output
                if (type.equals("INPUT")) {
                    runOutputStream();
                }
                // else stdout, stderr, process stream is input
                else {
                    runInputStream();
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        public void runInputStream() throws IOException {

            System.out.println("runInputStream() - in.");
            if (is == null) {
                System.out.println("runInputStream() - inputStream is null: skip.");
                return;
            }
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);

            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            String extract = "Extracting files to ";
            while (!foundWatchFor && is != null && (line = br.readLine()) != null) {
                if (pw != null)
                    pw.println("runInputStream() - readLine(): " + line);
                System.out.println(line);

                // Save off the extract location
                if (line.contains(extract)) {
                    extractLoc = line.substring(extract.length());
                    pw.println("extractLoc = " + line);
                }

                // if watchFor string supplied - search for it
                if (watchFor != null) {
                    if (line.indexOf(watchFor) > -1) {
                        foundWatchFor = true;
                        break;
                    }
                }
            }
            br.close();
            isr.close();

            System.out.println("runInputStream() - exit.");
            if (pw != null)
                pw.flush();
        }

        public void runOutputStream() throws IOException {
            OutputStreamWriter osr = new OutputStreamWriter(os, "UTF-8");
            BufferedWriter br = new BufferedWriter(osr);
            br.write("Y");
        }
    }

    /**
     * Stops the server at the specified location
     *
     * @param extractLocation
     * @throws Exception
     */
    private void stopServer(String extractLocation) throws Exception {
        // build the stop command for Unix platforms
        String cmd = extractLocation + File.separator + "bin" + File.separator + "server stop " + serverName + " --force";

        // modify cmd if windows based
        if (System.getProperty("os.name").startsWith("Win")) {
            if (System.getenv("WLP_JAR_CYGWIN") != null) {
                cmd = "bash -c  " + '"' + cmd.replace('\\', '/') + '"';
            } else {
                cmd = "cmd /k " + cmd;
            }
        }

        Runtime.getRuntime().exec(cmd); // stop server
    }
}
