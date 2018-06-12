/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class ACEScanner {
    // Variables that you might want to configure
    public String INPUT_FILE = "files/console.log";
    public String OUTPUT_FILE = "unique_j2sec_issues.log";
    public int WS_STACK_DEPTH = 0; // how many lines of stack to print after the first "com.ibm.ws"

    // Other variables
    public static final Pattern JAVA_2_SEC_NORETHROW = Pattern.compile("^\\[WARNING \\] CWWKE09(21W|12W|13E|14W|15W|16W):.*");
    public static final String JAVA_2_SEC_RETHROW = "ERROR: java.security.AccessControlException:";
    public static final String JAVA_2_SEC_SYSERR = "[err] java.security.AccessControlException:";
    // Matches: com.ibm.ws.*, com.ibm.wsspi.*, com.ibm.jbatch.*
    private static final Pattern IBM_STACK = Pattern.compile("^(\\s*at )?(com\\.ibm\\.ws|com\\.ibm\\.jbatch\\.).*");
    private static final Class<?> c = ACEScanner.class;
    private static final boolean DEBUG = false;
    private final Map<String, String> exceptionMap = new HashMap<String, String>();
    private int totalACECount = 0;

    public static void main(String[] args) throws Exception {
        new ACEScanner().run();
    }

    public ACEScanner() {}

    public ACEScanner(LibertyServer server) throws Exception {
        this.INPUT_FILE = server.getConsoleLogFile() == null ? null : server.getConsoleLogFile().getAbsolutePath();
    }

    public ACEScanner inputFile(String inputFile) {
        this.INPUT_FILE = inputFile;
        return this;
    }

    public ACEScanner stackDepth(int stackDepth) {
        this.WS_STACK_DEPTH = stackDepth;
        return this;
    }

    public void run() {
        try {
            Log.info(c, "run", "Processing log file: " + INPUT_FILE);

            // Find all the unique ACEs and store them in a map
            BufferedReader infile = new BufferedReader(new FileReader(INPUT_FILE));
            String curLine;
            while ((curLine = infile.readLine()) != null) {
                // short-cut check for possible line beginnings for ACE's
                if (!curLine.startsWith("[WARNING") &&
                    !curLine.startsWith("ERROR") &&
                    !curLine.startsWith("[err]"))
                    continue;

                if (curLine.startsWith(JAVA_2_SEC_RETHROW) ||
                    curLine.startsWith(JAVA_2_SEC_SYSERR) ||
                    JAVA_2_SEC_NORETHROW.matcher(curLine).matches()) {
                    if (DEBUG)
                        Log.info(c, "run", "starting to process: " + curLine);
                    processACEStack(infile);
                }
            }
            infile.close();

            if (exceptionMap.size() == 0) {
                Log.info(c, "run", "No AccessControlExceptions found in logs, so no report will be generated.");
                return;
            } else {
                Log.info(c, "run", "Found " + exceptionMap.size() + " unique AccessControlExceptions in logs");
            }

            // Write the ACE report
            String tstamp = new SimpleDateFormat("HH-mm-ss-SSS").format(new Date(System.currentTimeMillis()));
            PrintWriter outfile = new PrintWriter(new FileOutputStream("ACE-report-" + tstamp + ".log"));

            outfile.write("Begin AccessControlException report for for log file:\n");
            outfile.write(INPUT_FILE + "\n\n");
            outfile.write("============================================================\n\n");
            outfile.write("Found " + totalACECount + " total AccessControlExceptions in logs.\n");
            outfile.write("Found " + exceptionMap.size() + " unique occurrances.\n\n");
            outfile.write("============================================================\n\n\n");

            for (String stack : exceptionMap.values()) {
                outfile.write(stack);
                outfile.write("\n");
            }

            outfile.write("\n\nEnd of AccessControlException report.");
            outfile.close();
        } catch (IOException e) {
            Log.error(c, "run", e);
        }
    }

    /**
     * Get a string of the relevant stack trace
     */
    private void processACEStack(BufferedReader reader) throws IOException {
        /*
         * Example text to scan
         * [begin] ("java.lang.RuntimePermission" "getClassLoader")
         * Stack:
         * java.security.AccessControlException: Access denied ("java.lang.RuntimePermission" "getClassLoader")java.security.AccessController.throwACE(AccessController.java:125)
         * java.security.AccessController.checkPermission(AccessController.java:198)
         * java.lang.SecurityManager.checkPermission(SecurityManager.java:563)
         * com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager.checkPermission(MissingDoPrivDetectionSecurityManager.java:41)
         * java.lang.Class.getClassLoader(Class.java:393)
         * [end] com.ibm.wsspi.persistence.internal.PUInfoImpl.<init>(PUInfoImpl.java:74)
         * com.ibm.wsspi.persistence.internal.PersistenceServiceUnitImpl.<init>(PersistenceServiceUnitImpl.java:65)
         * com.ibm.wsspi.persistence.internal.PersistenceServiceImpl.createPersistenceServiceUnit(PersistenceServiceImpl.java:60)
         * com.ibm.wsspi.persistence.internal.DatabaseStoreImpl.createPersistenceServiceUnit(DatabaseStoreImpl.java:290)
         * com.ibm.ws.concurrent.persistent.db.DatabaseTaskStore.getPersistenceServiceUnit(DatabaseTaskStore.java:718)
         * com.ibm.ws.concurrent.persistent.db.DatabaseTaskStore.findOrCreate(DatabaseTaskStore.java:465)
         * com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl.getPartitionId(PersistentExecutorImpl.java:774)
         * com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl.newTask(PersistentExecutorImpl.java:1064)
         * com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl.schedule(PersistentExecutorImpl.java:1419)
         * web.PersistentDemoServlet.doGet(PersistentDemoServlet.java:63)
         * javax.servlet.http.HttpServlet.service(HttpServlet.java:687)
         * javax.servlet.http.HttpServlet.service(HttpServlet.java:790)
         */
        // We want to capture from the [begin] to [end], and use the [end] line as the unique key

        final String m = "processACEStack";

        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            sb.append(line).append('\n');
            if (DEBUG)
                Log.info(c, m, "Process line:" + line);
            if (IBM_STACK.matcher(line).matches() && !line.startsWith("com.ibm.ws.kernel.launch.internal.MissingDoPrivDetectionSecurityManager")) {
                if (DEBUG)
                    Log.info(c, m, "Found WebSphere stack: " + line);
                // found the last relevant line in the stack
                if (!exceptionMap.containsKey(line)) {
                    if (DEBUG)
                        Log.info(c, m, "Found new exception: " + line);
                    // we have not seen this exception before, so add it to the map
                    String exceptionKey = line;
                    for (int i = 0; i < WS_STACK_DEPTH && (line = reader.readLine()) != null; i++)
                        sb.append(line).append('\n');
                    exceptionMap.put(exceptionKey, sb.toString());
                }
                totalACECount++;
                return;
            }
        }
        if (DEBUG)
            Log.info(c, m, "exit");
    }
}
