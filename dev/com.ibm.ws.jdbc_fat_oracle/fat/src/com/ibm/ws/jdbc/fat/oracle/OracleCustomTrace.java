/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.oracle.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(FULL)
@RunWith(FATRunner.class)
public class OracleCustomTrace extends FATServletClient {

    public static final String JEE_APP = "oracletracefat";
    public static final String SERVLET_NAME = "OracleTraceTestServlet";
    private static final String SSL_PASSWORD = "{xor}Lz4sLCgwLTtubw==";

    /**
     * Oracle tracing properties that can be set
     */
    private static final String ORACLELOG_ENABLE_TRACE = "-Doracle.jdbc.Trace",
                    ORACLELOG_FILENAME = "-DoracleLogFileName",
                    ORACLELOG_PACKAGENAME = "-DoracleLogPackageName",
                    ORACLELOG_FILE_SIZE_LIMIT = "-DoracleLogFileSizeLimit",
                    ORACLELOG_FILE_COUNT = "-DoracleLogFileCount",
                    ORACLELOG_FORMAT = "-DoracleLogFormat",
                    ORACLELOG_TRACELEVEL = "-DoracleLogTraceLevel";

    /**
     * Set of logging property keys
     */
    private static final List<String> loggingPropKeys = Arrays.asList(ORACLELOG_FILENAME, ORACLELOG_PACKAGENAME,
                                                                      ORACLELOG_FILE_SIZE_LIMIT, ORACLELOG_FILE_COUNT,
                                                                      ORACLELOG_FORMAT, ORACLELOG_TRACELEVEL);

    private static final OracleContainer oracle = FATSuite.getSharedOracleContainer();
    private static final Map<String, String> TIME_ZONE_OPTION = Collections.singletonMap("-Doracle.jdbc.timezoneAsRegion", "false");
    private static final Map<String, String> TRACE_OPTION = Collections.singletonMap("-Doracle.jdbc.Trace", "true");

    private static Map<String, String> serverJvmOptClone;

    @Server("com.ibm.ws.jdbc.fat.oracle.trace")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        serverJvmOptClone = server.getJvmOptionsAsMap();
        ShrinkHelper.defaultApp(server, JEE_APP, "trace.web");
        //Do not start server, tests will do that
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("DSRA7043W");
        }
    }

    private void restartServerWithNewLogging(Map<String, String> jvmOpts) throws Exception {
        if (server.isStarted()) {
            server.stopServer("DSRA7043W");
        }

        server.addEnvVar("ORACLE_URL", oracle.getJdbcUrl());
        server.addEnvVar("ORACLE_USER", oracle.getUsername());
        server.addEnvVar("ORACLE_PASSWORD", oracle.getPassword());
        server.addEnvVar("SSL_PASSWORD", SSL_PASSWORD);

        server.setJvmOptions(Stream.of(serverJvmOptClone, jvmOpts, TIME_ZONE_OPTION, TRACE_OPTION)
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (existing, replacement) -> existing)));

        server.startServer(getTestMethodSimpleName() + ".log");
    }

    private Map<String, List<String>> findLoggingStringInTrace() throws Exception {
        Map<String, List<String>> results = new HashMap<>(6);
        //NOTE: using mark, so order is important here
        results.put(ORACLELOG_FILENAME, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_FILENAME")));
        results.put(ORACLELOG_PACKAGENAME, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_PACKAGENAME")));
        results.put(ORACLELOG_FILE_SIZE_LIMIT, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_FILE_SIZE_LIMIT")));
        results.put(ORACLELOG_FILE_COUNT, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_FILE_COUNT")));
        results.put(ORACLELOG_FORMAT, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_FORMAT")));
        results.put(ORACLELOG_TRACELEVEL, server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG_TRACELEVEL")));

        return results;
    }

    @Test
    public void testOracleCustomLogging() throws Exception {
        Map<String, String> expectedValues = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(ORACLELOG_ENABLE_TRACE, "true");
                put(ORACLELOG_FILENAME, "logs/oracle.log");
                put(ORACLELOG_PACKAGENAME, "oracle.jdbc");
                put(ORACLELOG_FILE_SIZE_LIMIT, "1000");
                put(ORACLELOG_FILE_COUNT, "2");
                put(ORACLELOG_FORMAT, "XMLFormatter");
                put(ORACLELOG_TRACELEVEL, "FINE");
            }
        };
        restartServerWithNewLogging(expectedValues);

        server.setTraceMarkToEndOfDefaultTrace();
        runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=1");

        Map<String, List<String>> actualValues = findLoggingStringInTrace();

        loggingPropKeys.stream().forEach(key -> {
            assertEquals("Failed to find " + key + " in logs and trace", 1, actualValues.get(key).size());
            assertTrue("Failed to verify " + key + " in logs and trace", actualValues.get(key).get(0).contains(expectedValues.get(key)));
        });

        //Ensure at least one file was created and named correctly
        File oracleLog1 = Paths.get(server.getLogsRoot() + "oracle.0.0.log").toFile();
        assertTrue("Expected to find oracle log file at: " + oracleLog1.getPath(), oracleLog1.isFile());

        //Ideally, we would want to make an assertion that this file contains logs.
        //However, we have no guarantee that the Oracle driver has removed it's lock on the log file.
        //Instead, assume if a log file was created, then the oracle driver is logging to it.

        //Use a second datasource and ensure we do not setup custom logging again
        server.setTraceMarkToEndOfDefaultTrace();
        runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLoggingAgain&iteration=2");
        List<String> unexpectedLines = server.findStringsInLogsAndTraceUsingMark(Pattern.quote("ORACLELOG"));
        assertEquals("Found lines in trace that show oracle custom logging was done more than once", 0, unexpectedLines.size());
    }

    @Test
    public void testOracleCustomLoggingParseErrors() throws Exception {
        Map<String, String> erronousConfig = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                //Valid required config
                put(ORACLELOG_ENABLE_TRACE, "true");
                put(ORACLELOG_FILENAME, "logs/oracle.log");
                put(ORACLELOG_PACKAGENAME, "oracle.jdbc");
                //Expected validation/parsing errors
                put(ORACLELOG_FILE_SIZE_LIMIT, "-1");
                put(ORACLELOG_FILE_COUNT, "0");
                put(ORACLELOG_FORMAT, "my.custom.format.Class");
                put(ORACLELOG_TRACELEVEL, "DEBUG");
            }
        };

        Map<String, String> expectedValues = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(ORACLELOG_ENABLE_TRACE, "true");
                put(ORACLELOG_FILENAME, "logs/oracle.log");
                put(ORACLELOG_PACKAGENAME, "oracle.jdbc");
                put(ORACLELOG_FILE_SIZE_LIMIT, "0");
                put(ORACLELOG_FILE_COUNT, "1");
                put(ORACLELOG_FORMAT, "SimpleFormatter");
                put(ORACLELOG_TRACELEVEL, "INFO");
            }
        };
        restartServerWithNewLogging(erronousConfig);

        server.setTraceMarkToEndOfDefaultTrace();
        runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=3");

        Map<String, List<String>> actualValues = findLoggingStringInTrace();

        loggingPropKeys.stream().forEach(key -> {
            assertEquals("Failed to find " + key + " in logs and trace", 1, actualValues.get(key).size());
            assertTrue("Failed to verify " + key + " in logs and trace", actualValues.get(key).get(0).contains(expectedValues.get(key)));
        });

        List<String> expectedErrorLines = server.findStringsInLogsAndTraceUsingMark("DSRA7043W");
        assertEquals("Expected number or warnings not found for invalid config", 8, expectedErrorLines.size());

        //Ensure file was created
        File oracleLog = Paths.get(server.getLogsRoot() + "oracle.0.0.log").toFile();
        assertTrue("Expected to find oracle log file at: " + oracleLog.getPath(), oracleLog.isFile());

        //Ideally, we would want to make an assertion that this file contains logs.
        //However, we have no guarantee that the Oracle driver has removed it's lock on the log file.
        //Instead, assume if a log file was created, then the oracle driver is logging to it.
    }

    @Test
    public void testOracleCustomLoggingInsuffcientConfig() throws Exception {
        Map<String, String> insufficentConfig = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put(ORACLELOG_ENABLE_TRACE, "true");
                put(ORACLELOG_FILENAME, "logs/oracle.log");
            }
        };

        restartServerWithNewLogging(insufficentConfig);

        server.setTraceMarkToEndOfDefaultTrace();
        runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=4");

        List<String> expectedInfoLine = server.findStringsInLogsAndTraceUsingMark("DSRA7044I");
        assertEquals("Expected to be informed of insufficient config", 2, expectedInfoLine.size());

        //Ensure file was NOT created
        File oracleLog = Paths.get(server.getLogsRoot() + "oracle.0.0.log").toFile();
        assertTrue("Did not expect to find oracle log file at: " + oracleLog.getPath(), !oracleLog.isFile());
    }
}
