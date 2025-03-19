/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Utility class for DDL Generation
 */
public class DDLGenScript {

    // Known locations of scripts
    private final String script;
    private final String debugScript;

    // The server
    private final LibertyServer server;

    // Generated processes
    private final Deque<ProgramOutput> outputs;

    // Configuration fields
    private boolean isDebug = false;

    ///// Builder /////

    /**
     * Start to build an execution of the DDLGen Script for the provided server.
     *
     * @param  server The server on which the script will run.
     * @return        this
     */
    public static DDLGenScript build(LibertyServer server) {
        Objects.requireNonNull(server);

        if (!server.isStarted()) {
            throw new RuntimeException("Server must be started prior to DDLGenScript.build()");
        }

        return new DDLGenScript(server);
    }

    ///// Constructor /////

    private DDLGenScript(LibertyServer server) {
        this.server = server;

        Machine m = server.getMachine();

        OperatingSystem os;
        try {
            os = m.getOperatingSystem();
        } catch (Exception e) {
            throw new RuntimeException("Unknown operation system", e);
        }

        if (os == OperatingSystem.WINDOWS) {
            script = server.getInstallRoot() + File.separator + "bin" + File.separator + "ddlGen.bat";
            debugScript = null;
        } else {
            script = server.getInstallRoot() + File.separator + "bin" + File.separator + "ddlGen";
            debugScript = server.getInstallRoot() + File.separator + "bin" + File.separator + "ddlGenDebug";
        }

        outputs = new LinkedList<>();
    }

    ///// Actions /////

    /**
     * Will run the DDLGen debug script if it is available for the OS.
     * Will also collect additional debug information from the OS prior to executing the DDLGen [debug] script.
     *
     * @return this
     */
    public DDLGenScript withDebug() {
        if (!Objects.nonNull(debugScript)) {
            Log.info(DDLGenScript.class, "withDebug", "Operating system does not support debug script, will run with traditional script.");
            isDebug = true;
        }

        Properties env = new Properties();
        try {
            outputs.add(server.getMachine().execute("uname", new String[] {}, server.getInstallRoot(), env));
            outputs.add(server.getMachine().execute("dirname", new String[] { "`pwd`" }, server.getInstallRoot(), env));
            outputs.add(server.getMachine().execute("env", new String[] {}, server.getInstallRoot(), env));
            outputs.add(server.getMachine().execute("cat", new String[] { script }, server.getInstallRoot(), env));
        } catch (Exception e) {
            throw new RuntimeException("Unable to execute debug scripts: " + e);
        }

        return this;
    }

    ///// Termination /////

    public DDLGenScriptResult execute() {
        String[] opts = new String[] { "generate", server.getServerName() };
        Properties env = new Properties();

        try {
            if (isDebug) {
                outputs.addFirst(server.getMachine().execute(debugScript, opts, server.getInstallRoot(), env));
            } else {
                outputs.addFirst(server.getMachine().execute(script, opts, server.getInstallRoot(), env));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to execute ddlGen op ddlGenDebug script", e);
        }

        return DDLGenScriptResult.process(this);
    }

    ///// Getters /////

    public LibertyServer getServer() {
        return server;
    }

    public Deque<ProgramOutput> getOutputs() {
        return outputs;
    }

    ///// Result nested class ////
    public static class DDLGenScriptResult {

        private static final String SUCCESS_MESSAGE = "CWWKD0107I";

        // Obtained from https://www.ibm.com/docs/en/was-liberty/nd?topic=line-generating-data-definition-language#tasktwlp_ddlgen__result__1
        private static final Map<Integer, String> RESULT_CODE_MAP = new HashMap<>();
        static {
            RESULT_CODE_MAP.put(0, "Success");
            RESULT_CODE_MAP.put(20, "The action provided is not valid");
            RESULT_CODE_MAP.put(21, "The server was not found.");
            RESULT_CODE_MAP.put(22, "The localConnector feature is not present in the server configuration"
                                    + " or the server was not started.");
            RESULT_CODE_MAP.put(23, "The MBean that generates DDL was not found.");
            RESULT_CODE_MAP.put(24, "The MBean that generates DDL reported an error.");
            RESULT_CODE_MAP.put(25, "The server output directory was not found.");
            RESULT_CODE_MAP.put(255, "An unexpected error occurred.");
        }

        private final DDLGenScript script;

        private final List<String> sysout;

        private final File ddlDir;

        ///// Builder /////

        protected static DDLGenScriptResult process(DDLGenScript script) {
            Objects.requireNonNull(script);

            return new DDLGenScriptResult(script);
        }

        ///// Constructor /////

        private DDLGenScriptResult(DDLGenScript script) {
            this.script = script;

            sysout = new ArrayList<String>();

            final Iterator<ProgramOutput> i = Objects.requireNonNull(script.getOutputs()).iterator();
            while (i.hasNext()) {
                ProgramOutput output = i.next();

                sysout.addAll(Arrays.asList(output.getStdout().split(System.lineSeparator())));

                Log.info(DDLGenScriptResult.class, "<init>", "Executed command:" + output.getCommand());
                Log.info(DDLGenScriptResult.class, "<init>", "stdout:" + output.getStdout());
                Log.info(DDLGenScriptResult.class, "<init>", "stderr:" + output.getStderr());
                Log.info(DDLGenScriptResult.class, "<init>", "rc:" + output.getReturnCode());
                Log.info(DDLGenScriptResult.class, "<init>", "serverRoot:" + script.getServer().getServerRoot());
            }

            String userDir = script.getServer().getUserDir();
            String serverName = script.getServer().getServerName();

            String _ddlDir;
            if (userDir.length() >= 1 && userDir.endsWith(File.separator)) {
                _ddlDir = userDir + "servers" + File.separator + serverName + File.separator + "ddl";
            } else {
                _ddlDir = userDir + File.separator + "servers" + File.separator + serverName + File.separator + "ddl";
            }

            ddlDir = new File(_ddlDir);

        }

        ///// Assertions /////

        /**
         * Asserts that the script(s) all ran successfully, that the expected success message was output,
         * and that the directory where the DDL files should have been placed exists.
         *
         * @return this
         */
        public DDLGenScriptResult assertSuccessful() {
            Deque<ProgramOutput> outputs = Objects.requireNonNull(script.getOutputs());

            assertEquals("DDLGen script failed with result: " + RESULT_CODE_MAP.get(outputs.getFirst().getReturnCode()), 0, outputs.getFirst().getReturnCode());

            Iterator<ProgramOutput> i = outputs.iterator();
            while (i.hasNext()) {
                ProgramOutput output = i.next();
                assertEquals("Debug script failed with result: " + output.getReturnCode(), 0, output.getReturnCode());
            }

            sysout.stream()
                            .filter(line -> line.contains(SUCCESS_MESSAGE))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("Output did not contain " + SUCCESS_MESSAGE));

            assertTrue("The output directory did not exist: " + ddlDir.getAbsolutePath(), ddlDir.exists());
            assertTrue("The output directory was not a directory: " + ddlDir.getAbsolutePath(), ddlDir.isDirectory());

            return this;
        }

        /**
         * Asserts that an expected DDL file exists in the DDL directory
         *
         * @param  expectedFileName the expected file name with .ddl extension
         * @return                  this
         */
        public DDLGenScriptResult assertDDLFile(String expectedFileName) {
            assertTrue("Expected file " + expectedFileName + " did not exist in " + ddlDir.getAbsolutePath(), getFileNames().contains(expectedFileName));
            return this;
        }

        /**
         * Asserts that all the expected DDL files exists in the DDL directory
         *
         * @param  expectedFileNames the list of expected file names with .ddl extensions
         * @return                   this
         */
        public DDLGenScriptResult assertDDLFiles(List<String> expectedFileNames) {
            List<String> observedFileNames = getFileNames();
            assertEquals("Expected number of ddl files did not match actual number of files " + observedFileNames,
                         expectedFileNames.size(),
                         observedFileNames.size());

            for (String expectedFile : expectedFileNames) {
                assertTrue("The expected file " + expectedFile + " was not in " + ddlDir.getAbsolutePath(), getFileNames().contains(expectedFile));
            }

            return this;
        }

        /**
         * Asserts that none of the generated DDL files in the DDL directory contains the provided substring
         *
         * @param  unexpectedSubstring a substring you do not expect to find in any DDL file names
         * @return                     this
         */
        public DDLGenScriptResult assertNoDDLFileLike(String unexpectedSubstring) {
            for (String observedFileName : getFileNames()) {
                assertFalse("The generated DDL file " + observedFileName + " contained the unexpected substring " + unexpectedSubstring,
                            observedFileName.contains(unexpectedSubstring));
            }

            return this;
        }

        ///// Terminations /////

        /**
         * Return a line from system.out that contains an expected substring for further analysis.
         *
         * @param  expected an expected substring to find
         * @return          the line from system.out or null if no line was found
         */
        public String getLineInSysoutContaining(String expected) {
            for (String line : sysout) {
                if (line.contains(expected)) {
                    return line;
                }
            }
            return null;
        }

        /**
         * Get all DDL file names from the DDL directory.
         *
         * @return all generated DDL file names
         */
        public List<String> getFileNames() {
            return Arrays.asList(ddlDir.list());
        }

        /**
         * Get all DDL file paths in the DDL directory.
         *
         * @return all generated DDL file paths
         */
        public List<String> getFileLocations() {
            return Stream.of(ddlDir.list()).map(fileName -> ddlDir.getAbsolutePath() + File.separator + fileName).collect(Collectors.toList());
        }

    }
}
