/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package wlp.lib.extract;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class PackageRunnableTest {
    protected static final Class<?> c = PackageRunnableTest.class;
    protected static final String CLASS_NAME = c.getName();

    private static final String serverName = "runnableTestServer";
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
    private static final File runnableJar = new File("publish/" + serverName + ".jar");
    private static final File extractDirectory1 = new File("publish" + File.separator + "wlpExtract1");
    private static final File extractDirectory2 = new File("publish" + File.separator + "wlpExtract2");
    private static final File extractAndRunDir = new File("publish" + File.separator + "wlpExtractAndRun");
    private static final File extractDirectory3 = new File("publish" + File.separator + "wlpExtract3");
    private static final File extractDirectory4 = new File("publish" + File.separator + "wlpExtract4");
    private static File extractLocation = null;
    private static File outputAutoFVTDirectory = null;
    private static final long DUMMY_MANIFEST_FILE_SIZE = 41;
    // Wait a few seconds longer than quiesce time
    private static final int STOP_RETRY_COUNT = 35;

    /*
     * return env as array and add WLP_JAR_EXTRACT_DIR=extractDirectory
     */
    private static void runEnv(Map<String, String> envmap, String extractDirectory, boolean useDummyUserDir) {
        String method = "runEnv";

        if (useDummyUserDir) {
            String dummyUserDir = extractDirectory + File.separator + "a1a" + File.separator + "b2b" + File.separator + "c3c";
            envmap.put("WLP_USER_DIR", dummyUserDir);
            Log.info(c, method, "Adding env variable WLP_USER_DIR = " + dummyUserDir);
        }
        if (extractDirectory != null) {
            envmap.put("WLP_JAR_EXTRACT_DIR", extractDirectory);
            Log.info(c, method, "Adding env variable WLP_JAR_EXTRACT_DIR = " + extractDirectory);
        }
    }

    @Before
    public void setup() throws Exception {
        String method = "setup";

        // Verify that /lib/extract exits, and the manifest.mf is valid
        validateWLPLibExtractAndManifest();

        /*
         * // Save off the output directory so we can save a copy of the .jar if its bad
         * outputAutoFVTDirectory = new File("output/servers/", serverName);
         * Log.info(c, method, "outputAutoFVTDirectory: " + outputAutoFVTDirectory.getAbsolutePath());
         */
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        final String METHOD_NAME = "setUpBeforeClass";

        // Save this off for the tearDown method to manually copy logs from /NonDefaultUser
        // folder to /autoFVT/output/servers/ folder.
        outputAutoFVTDirectory = new File("output/servers", serverName);
        Log.info(c, METHOD_NAME, "outputAutoFVTDirectory: " + outputAutoFVTDirectory.getAbsolutePath());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {

        deleteDir(extractAndRunDir);
        deleteDir(extractDirectory1);
        deleteDir(extractDirectory2);
        deleteDir(extractDirectory3);
        deleteDir(extractDirectory4);
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

        String method = "testRunnableJar()";

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String[] args = new String[] { "--archive=" + runnableJar.getAbsolutePath(), "--include=minify,runnable" };
        Log.info(c, method, "package command parameters = " + Arrays.toString(args));
        String stdout = server.executeServerScript("package", args).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            Log.warning(c, "Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        Log.info(c, method, "stdout for package cmd is: \n" + stdout);

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

        String method = "testRunnableJarLaunchOnlyWithUserDirSet";

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            Log.warning(c, "Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        Log.info(c, method, "stdout for package cmd is: \n" + stdout);

        executeTheJar(extractDirectory2, true, true, true);
        checkDirStructure(extractDirectory2, true);
    }

    /**
     * When WLP_JAR_EXTRACT_DIR is set, the server should be left as-is by the Shutdown Hook.
     *
     * @throws Exception
     */
    @Test
    public void testRunnableDoNotDeleteServerFolder() throws Exception {

        String method = "testRunnableDoNotDeleteServerFolder";

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            Log.warning(c, "Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        Log.info(c, method, "stdout for package cmd is: \n" + stdout);

        String extLoc = executeTheJar(extractDirectory3, false, true, true);

        assertTrue("The extract location does not match the WLP_JAR_EXTRACT_DIR. ExtractDir = " + extractDirectory3.getAbsolutePath() + " vs " + extLoc,
                   extLoc.startsWith(extractDirectory3.getAbsolutePath()));

        assertTrue("root folder at " + extractDirectory3.getAbsolutePath() + " does not exist, but should.", extractDirectory3.exists());
    }

    /**
     * When WLP_JAR_EXTRACT_DIR is not set, the server should be completely deleted minus the /logs folder via the Shutdown Hook.
     *
     * @throws Exception
     */
    @Test
    public void testRunnableDeleteServerMinusLogsFolder() throws Exception {

        String method = "testRunnableDeleteServerMinusLogsFolder";

        // Doesn't work on z/OS (because you can't package into a jar on z/OS)
        assumeTrue(!System.getProperty("os.name").equals("z/OS"));

        String stdout = server.executeServerScript("package",
                                                   new String[] { "--archive=" + runnableJar.getAbsolutePath(),
                                                                  "--include=minify,runnable" }).getStdout();

        String searchString = "Server " + serverName + " package complete";
        if (!stdout.contains(searchString)) {
            Log.warning(c, "Warning: test case " + PackageRunnableTest.class.getName() + " could not package server " + serverName);
            return; // get out
        }

        Log.info(c, method, "stdout for package cmd is: \n" + stdout);

        extractLocation = new File(executeTheJar(extractDirectory4, true, false, true));

        // Make sure the server is stopped
        assertNotNull("The server did not show that it had stopped after executing the jar.",
                      server.waitForStringInLog("CWWKE0036I:.*"));

        // wait 30s to give the shutdown script time to run in the shutdown hook
        Thread.sleep(30000);

        checkDirStructure(extractLocation, false);
    }

    /**
     * Checks that the /logs and optionally the /bin folder exist within the /wlp folder structure.
     *
     * @param extractDir
     * @param shouldBinFolderExist
     *
     */
    private void checkDirStructure(File extractDir, boolean shouldFolderExist) {

        String method = "checkDirStructure";
        StringBuffer sb = new StringBuffer();
        File[] files = extractDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            sb.append(files[i] + "\n");
        }

        File logDir = null;
        File templateDir = null;

        if (extractDir.getAbsolutePath().endsWith("wlp")) {
            logDir = new File(extractDir.getAbsolutePath() + File.separator + "usr" + File.separator + "servers" + File.separator + serverName
                              + File.separator + "logs");
            templateDir = new File(extractDir.getAbsolutePath() + File.separator + "templates");
        } else {
            logDir = new File(extractDir.getAbsolutePath() + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName
                              + File.separator + "logs");
            templateDir = new File(extractDir.getAbsolutePath() + File.separator + "wlp" + File.separator + "templates");
        }

        // Logs should always exist regardless
        assertTrue(File.separator + "logs folder at " + logDir.getAbsolutePath() + " does not exist, but should. " + sb.toString(), logDir.exists());

        if (shouldFolderExist) {
            assertTrue(File.separator + "templates folder at " + templateDir.getAbsolutePath() + " does not exist, but should. Contents =" + sb.toString(), templateDir.exists());
        } else {
            Log.info(c, method, "Contents at " + templateDir.getAbsolutePath() + " are : " + sb.toString());
            assumeTrue(!templateDir.exists());
        }
    }

    /**
     * @param useDummyUserDir If 'true', will execute JAR after setting the WLP_USR_DIR env variable with a bogus value (to test that we can ignore it),
     *            if 'false' we will not set a WLP_USER_DIR value on the java -jar execution
     * @throws IOException
     * @throws InterruptedException
     */
    private String executeTheJar(File extractDirectory, boolean useDummyUserDir, boolean useRunEnv, boolean useNormalStop) throws Exception, InterruptedException {
        String extractLoc = null;
        Process proc = null;
        String method = "executeTheJar";

        try {
            if (!extractDirectory.exists()) {
                extractDirectory.mkdirs();
            }

            OutputStream os = new FileOutputStream(server.getLogsRoot() + File.separator + "executeTheJar.log");

            assertTrue("Extract directory " + extractDirectory.getAbsolutePath() + " does not exist.", extractDirectory.exists());

            String[] cmd;

            if (System.getProperty("os.name").equalsIgnoreCase("os/400")) {
                // If this is running on IBM i, add the -XX:+EnableHCR option to not run in a separate JVM
                cmd = new String[] { "java", "-XX:+EnableHCR", "-jar", runnableJar.getAbsolutePath() };
            } else {
                cmd = new String[] { "java", "-jar", runnableJar.getAbsolutePath() };
            }

            Log.info(c, method, "Running command: " + Arrays.toString(cmd));
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            if (useRunEnv == true) {
                runEnv(processBuilder.environment(), extractDirectory.getAbsolutePath(), useDummyUserDir);
            }
            proc = processBuilder.start();

            // setup and start reader threads for error and output streams
//        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR", null);
//        errorReader.start();
            StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT", "CWWKF0011I", os);
            outputReader.start();

            int count = 0;

            // wait up to 90 seconds to find watch for string

            boolean found = outputReader.foundWatchFor();
            extractLoc = outputReader.extractLoc();
            while (!found && count <= 90) {

                synchronized (proc) {
                    proc.wait(1000); // wait 1 second
                    Log.info(c, method, "Waiting for server to complete initialization - " + count + " seconds elapsed.");
                }
                found = outputReader.foundWatchFor();
                extractLoc = outputReader.extractLoc();
                count++;
            }

            if (!found) {
                Log.info(c, method, "Process is alive: " + proc.isAlive());
                // capture the messages.log for debugging test
                File messagesLog = new File(server.getInstallRoot(), "/usr/servers/" + serverName + "/logs/messages.log").getAbsoluteFile();
                if (messagesLog.exists()) {
                    Files.lines(messagesLog.toPath()).forEach((l) -> {
                        Log.info(c, method, "MESSAGES LINE: " + l);
                    });
                } else {
                    Log.info(c, method, "No messages.log - " + messagesLog.getAbsolutePath());
                }

                // log the contents of the runnable jar's manifest.mf
                JarFile jarFile = new JarFile(runnableJar.getAbsolutePath());
                boolean manifestFound = false;

                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                    JarEntry je = e.nextElement();
                    Log.info(c, method, "entry name = " + je.getName() + " entry size = " + je.getSize());
                    if (je.getName().equals("META-INF/MANIFEST.MF")) {

                        Log.info(c, method, "=== Start dumping contents of manifest file ===");
                        Log.info(c, method, readJarEntryContent(jarFile, je));
                        manifestFound = true;
                        Log.info(c, method, "=== End dumping contents of manifest file ===");
                    }
                }

                if (jarFile != null) {
                    jarFile.close();
                }

                assertTrue("Runnable jar did not contain a META-INF/MANIFEST.MF file", manifestFound);

                // If we have an invalid package, save off the jar for troubleshooting.
                outputAutoFVTDirectory = new File("output/servers/", serverName);
                Log.info(c, method, "outputAutoFVTDirectory: " + outputAutoFVTDirectory.getAbsolutePath());

                outputAutoFVTDirectory.mkdirs();
                Log.info(c, method, "Copying directory from " +
                                    runnableJar.getAbsolutePath() + " to " +
                                    outputAutoFVTDirectory.getAbsolutePath() + "/" + serverName + ".jar");

                File srcDir = new File(runnableJar.getAbsolutePath());
                copyFile(srcDir, new File(outputAutoFVTDirectory.getAbsolutePath() + "/" + serverName + ".jar"));

            }

            assertTrue("Server did not start successfully in time.", found);

            outputReader.setIs(null);

            // Attempt to stop the server
            int retry = 0;

            if (useNormalStop == false) {
                // ensure no process left behind
                while (proc.isAlive() && retry < STOP_RETRY_COUNT) {
                    Log.info(c, method, "Server is stopping via the proc.destroy() method.  Retry = " + retry);
                    proc.destroy();
                    Thread.sleep(1000);
                    retry++;
                }
            } else {
                // stop normally so shutdown hook is called
                stopServer(extractLoc);
            }

            if (os != null) {
                os.close();
            }

            Log.info(c, method,
                     "Server with name = " + server.getServerName() + " server.isStarted() = "
                                + server.isStarted() + " proc.isAlive() = " + proc.isAlive() + " retry = " + retry);

        } finally {

            if (proc.isAlive()) {
                if (server.isStarted()) {
                    // Attempt to dump the server if its still alive
                    Log.info(c, method, "Dumping server as it has not stopped by request.");
                    server.dumpServer(extractLoc + File.separator + "usr" + File.separator + "servers" + File.separator + serverName);
                }
                Log.info(c, method, "Destroying the process as it was not stopped via the previous attempts.");
                proc.destroy();
            }
/*
 * // Manually copy the logs since the framework does not do this given the extract locations.
 * int loc = extractDirectory.getAbsolutePath().lastIndexOf(File.separator);
 * String folder = extractDirectory.getAbsolutePath().substring(loc);
 * File pathWithFolder = new File(outputAutoFVTDirectory.getAbsolutePath() + File.separator + folder);
 * Log.info(c, method, "Saving logs to " + pathWithFolder.getAbsolutePath());
 * pathWithFolder.mkdirs();
 * Log.info(c, method, "Copying directory from " +
 * extractLoc + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + " to " +
 * pathWithFolder.getAbsolutePath());
 *
 * File srcDir = new File(extractLoc + File.separator + "usr" + File.separator + "servers" + File.separator + serverName);
 * copyDirectory(srcDir, pathWithFolder.getAbsoluteFile());
 */
        }
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

        String[] cmd;

        if (System.getProperty("os.name").equalsIgnoreCase("os/400")) {
            // If this is running on IBM i, add the -XX:+EnableHCR option to not run in a separate JVM
            cmd = new String[] { "java", "-XX:+EnableHCR", "-cp", extractAndRunDir.getAbsolutePath(), "wlp.lib.extract.SelfExtractRun" };
        } else {
            cmd = new String[] { "java", "-cp", extractAndRunDir.getAbsolutePath(), "wlp.lib.extract.SelfExtractRun" };
        }

        Log.info(c, "executeAndExecuteMain", "Running command: " + Arrays.toString(cmd));
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        runEnv(processBuilder.environment(), null, false);
        Process proc = processBuilder.start();

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
                Log.info(c, "extractAndExecuteMain", "Waiting for server to complete initialization - " + count + " seconds elapsed.");
            }
            found = outputReader.foundWatchFor();
            count++;
        }

        // make sure we close out the OutputStream in case we timeout looking for the msg above! when
        // this happens the server framework cant delete the extractAndExecuteMain.log file.
        if (os != null) {
            os.close();
        }

        assertTrue("Server did not start successfully in time.", found);

        outputReader.setIs(null);
        proc.destroy(); // ensure no process left behind
        Log.info(c, "extractAndExecuteMain", "Removing WLP installation directory: " + extractAndRunDir.getAbsolutePath());
        if (extractAndRunDir.exists()) {
            deleteDir(extractAndRunDir);
            Log.info(c, "extractAndExecuteMain", "WLP installation directory was removed.");
        }

        Log.info(c, "extractAndExecuteMain", "Waiting 30 seconds...to make sure all Liberty threads exit.");
        Thread.sleep(30000); // wait 30 second
    }

    class StreamReader extends Thread {
        InputStream is;
        OutputStream os;
        String watchFor;
        boolean foundWatchFor = false;
        String extractLoc = null;

        StreamReader(InputStream is, String type, String watchFor, OutputStream redirect) {
            this.is = is;
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
                runInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        public void runInputStream() throws IOException {

            Log.info(c, "runInputStream", "runInputStream() - in.");
            if (is == null) {
                Log.info(c, "runInputStream", "runInputStream() - inputStream is null: skip.");
                return;
            }
            PrintWriter pw = new PrintWriter(os);

            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            String extract = "Extracting files to ";
            while (!foundWatchFor && is != null && (line = br.readLine()) != null) {
                Log.info(c, "runInputStream", "line=" + line);
                pw.println("runInputStream() - readLine(): " + line);

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

            Log.info(c, "runInputStream", "runInputStream() - exit.");
            if (pw != null)
                pw.flush();
        }
    }

    /**
     * Stops the server at the specified location via the server framework
     *
     * @param extractLocation
     * @throws Exception
     */
    private void stopServer(String extractLocation) throws Exception {
        String method = "stopServer";

        Log.info(c, method, "Getting existing liberty server");
        LibertyServer server = LibertyServerFactory.getExistingLibertyServer(extractLocation + File.separator + "usr" + File.separator + "servers" + File.separator
                                                                             + serverName);
        Log.info(c, method, "Attempting to stop server = " + server.getServerName() + " with status = " + server.isStarted());

        server.stopServer();
    }

    /**
     * Reads the contents line by line of the JarEntry from the JarFile
     *
     * @param jf
     * @param je
     */
    private static String readJarEntryContent(JarFile jf, JarEntry je) throws IOException {
        InputStream is = jf.getInputStream(je);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader r = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                sb.append(line + "\n");
            }
        } finally {
            r.close();
            isr.close();
            is.close();
        }

        return sb.toString();
    }

    /**
     * Copies a file from one location to another
     *
     * @param fromFile
     * @param toFile
     * @throws IOException
     */
    private static void copyFile(File fromFile, File toFile) throws IOException {
        String method = "copyFile";
        // Open the source file
        FileInputStream fis = new FileInputStream(fromFile);
        try {
            // Open the destination file
            File destDir = toFile.getParentFile();
            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new IOException("Failed to create path: " + destDir.getAbsolutePath());
            }

            Log.info(c, method, "Copying file from: " + fromFile.getAbsolutePath() + " to: " + toFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(toFile);

            // Perform the transfer using nio channels; this is simpler, and usually
            // faster, than copying the file a chunk at a time
            try {
                FileChannel inChan = fis.getChannel();
                FileChannel outChan = fos.getChannel();
                inChan.transferTo(0, inChan.size(), outChan);
            } finally {
                fos.close();
            }
        } finally {
            fis.close();
        }
    }

    /**
     * As of today, the FAT environment's installation of WLP does not include lib/extract directory.
     * The package command requires that the lib/extract directory exists, as this directory
     * contains a required manifest, self extractable classes, etc. Copy the wlp.lib.extract.jar
     * contents to wlp/lib/extract folder. Also, verifies that if the lib/extract directory does exist,
     * it contains an up to date meta-inf/manifest.mf file else one is built.
     *
     * @return false if /lib/extract exists, else true if it is created or if the manifest is re-written.
     *
     * @throws Exception
     */
    private static boolean validateWLPLibExtractAndManifest() throws Exception {
        String method = "createWLPLibExtract";

        try {
            // Check if /lib/extract exists
            server.getFileFromLibertyInstallRoot("lib/extract");

            // Make sure the manifest.mf file exists and is greater than 41
            // bytes: "Dummy manifest written during minify test".
            File manifestFile = new File(server.getInstallRoot(), "lib/extract/META-INF/MANIFEST.MF");

            // if the manifest does not exist build it
            if (!manifestFile.exists()) {
                Log.info(c, method, "File system manifest.mf did not exist.  Building the complete /lib/extract/ folder.");
                buildLibExtractFolder();
                return true;
            } else if (manifestFile.length() != getManifestSize()) {
                Log.info(c, method, "File system manifest.mf size did not match manifest.mf size in wlp.lib.extract.jar. MANIFEST.MF on file system size = " + manifestFile.length()
                                    + ". Deleting the manifest.mf file and rebuilding the complete /lib/extract/ folder.");
                manifestFile.delete();
                buildLibExtractFolder();
                return true;
            } else {
                Log.info(c, method, "/lib/extract/ existed, and the manifest.mf was greater than " + DUMMY_MANIFEST_FILE_SIZE + " bytes in size.  Actual size is "
                                    + manifestFile.length() + " bytes.");
                return false;
            }
        } catch (FileNotFoundException ex) {
            // expected - the directory does not exist - so proceed.
            Log.info(c, method, "The /lib/extract/ folder did not exist, creating it from /lib/LibertyFATTestFiles/wlp.lib.extract.jar.");
        }

        // The /lib/extract folder did not exist at all, so create it
        buildLibExtractFolder();
        return true;
    }

    /**
     * Gets the manifest.mf size from the wlp.lib.extract.jar file
     *
     * @return
     * @throws Exception
     */
    private static long getManifestSize() throws Exception {
        JarFile libExtractJar = new JarFile("lib/LibertyFATTestFiles/wlp.lib.extract.jar");
        long mfSize = 0;
        try {
            for (Enumeration<JarEntry> entries = libExtractJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                    return entry.getSize();
                }
            }
        } finally {
            libExtractJar.close();
        }
        return mfSize;
    }

    /**
     * Builds the /lib/extract folder from the wlp.lib.extract.jar
     *
     * @throws Exception
     */
    private static void buildLibExtractFolder() throws Exception {

        JarFile libExtractJar = new JarFile("lib/LibertyFATTestFiles/wlp.lib.extract.jar");

        try {
            // Build the /lib/extract structure since it didnt exist
            RemoteFile libExtractDir = LibertyFileManager.createRemoteFile(server.getMachine(), server.getInstallRoot() + "/lib/extract");
            libExtractDir.mkdirs();

            for (Enumeration<JarEntry> entries = libExtractJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if ("wlp/".equals(entryName) || "wlp/lib/".equals(entryName) || "wlp/lib/extract/".equals(entryName)) {
                    continue;
                }
                File libExtractFile = new File(libExtractDir.getAbsolutePath() + "/" + entryName);

                //Jar contains some contents in wlp/lib/extract folder. Copy those contents in libExtractDir directly.
                if (entryName.startsWith("wlp/lib/extract")) {
                    libExtractFile = new File(libExtractDir.getAbsolutePath() + "/" + entryName.substring(entryName.lastIndexOf("extract/") + 8));
                }

                if (entryName.endsWith("/")) {
                    libExtractFile.mkdirs();
                } else if (!entryName.endsWith("/")) {
                    writeFile(libExtractJar, entry, libExtractFile);
                }
            }
        } finally {
            libExtractJar.close();
        }

    }

    /**
     * Writes out a jar file
     *
     * @param jar
     * @param entry
     * @param file
     * @throws IOException
     * @throws FileNotFoundException
     */
    private static void writeFile(JarFile jar, JarEntry entry, File file) throws IOException, FileNotFoundException {
        try (InputStream is = jar.getInputStream(entry)) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int read = -1;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        }
    }

    /**
     * Writes a file out to the output log
     *
     * @param f
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void dumpFileToLog(File f) throws FileNotFoundException, IOException {
        BufferedReader reader = null;
        String method = "dumpFileToLog";
        try {
            reader = new BufferedReader(new FileReader(f));
            String s = null;
            Log.info(c, method, "\t Contents of " + f.getAbsolutePath() + " : ");
            while ((s = reader.readLine()) != null) {
                Log.info(c, method, "\t\t" + s);
            }
        } finally {
            reader.close();
        }
    }

    public static void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdir();
            }

            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(source, children[i]),
                              new File(target, children[i]));
            }
        } else {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(target);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

}
