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
    private static final File extractDirectory = new File("publish" + File.separator + "wlpExtract");
    private static final File extractAndRunDir = new File("publish" + File.separator + "wlpExtractAndRun");

    /*
     * return env as array and add WLP_JAR_EXTRACT_DIR=extractDirectory
     */
    private static String[] runEnv(String extractDirectory) {

        Map<String, String> envmap = System.getenv();
        Iterator<String> iKeys = envmap.keySet().iterator();
        List<String> envArray = new ArrayList<String>();

        int i = 0;
        while (iKeys.hasNext()) {
            String key = iKeys.next();
            String val = envmap.get(key);
            envArray.add(key + "=" + val);
        }
        if (extractDirectory != null) {
            // add extract dir to end
            String extDirVar = "WLP_JAR_EXTRACT_DIR=" + extractDirectory;
            envArray.add(extDirVar);
        }
        return envArray.toArray(new String[0]);

    }

    @BeforeClass
    public static void setupClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    private void deleteDir(File file) {
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

        executeTheJar();

        extractAndExecuteMain();
    }

    private void executeTheJar() throws IOException, InterruptedException {
        if (!extractDirectory.exists()) {
            extractDirectory.mkdirs();
        }

        assertTrue("Extract directory " + extractDirectory.getAbsolutePath() + " does not exist.", extractDirectory.exists());

        String cmd = "java -jar " + runnableJar.getAbsolutePath();
        Process proc = Runtime.getRuntime().exec(cmd, runEnv(extractDirectory.getAbsolutePath()), null); // run server

        // setup and start reader threads for error and output streams
//        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR", null);
//        errorReader.start();
        StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT", "CWWKF0011I");
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
        System.out.println("Removing WLP installation directory: " + extractDirectory.getAbsolutePath());
        if (extractDirectory.exists()) {
            deleteDir(extractDirectory);
            System.out.println("WLP installation directory was removed.");
        }
        System.out.println("Waiting 30 seconds...to make sure all Liberty thread exiting.");
        Thread.sleep(30000); // wait 30 second
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
        Process proc = Runtime.getRuntime().exec(cmd, runEnv(null), null); // run server

        // setup and start reader threads for error and output streams
//        StreamReader errorReader = new StreamReader(proc.getErrorStream(), "ERROR", null);
//        errorReader.start();
        StreamReader outputReader = new StreamReader(proc.getInputStream(), "OUTPUT", "CWWKF0011I");
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
        System.out.println("Removing WLP installation directory: " + extractDirectory.getAbsolutePath());
        if (extractAndRunDir.exists()) {
            deleteDir(extractAndRunDir);
            System.out.println("WLP installation directory was removed.");
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
            while (!foundWatchFor && is != null && (line = br.readLine()) != null) {
                if (pw != null)
                    pw.println("runInputStream() - readLine(): " + line);
                System.out.println(line);
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

}
