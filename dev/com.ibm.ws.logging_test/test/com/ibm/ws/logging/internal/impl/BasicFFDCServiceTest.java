/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.DiagnosticModule;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.ffdc.SharedFFDCConfigurator;
import com.ibm.wsspi.logprovider.LogProviderConfig;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;
import test.common.TestFile;

public class BasicFFDCServiceTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);
    static File ffdcDir;

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LogProviderConfig config = SharedTr.getDefaultConfig();

        FFDCConfigurator.init(config);

        File logDir = config.getLogDirectory();
        ffdcDir = new File(logDir, "ffdc");
    }

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    @Before
    public void setup() throws Exception {
        SharedFFDCConfigurator.clearFFDCIncidents();

        TestFile.recursiveClean(ffdcDir);
        assertTrue("Log directory exists: " + ffdcDir, ffdcDir.exists() || ffdcDir.mkdirs());
    }

    @Ignore
    // Test FFDC calls
    @Test
    public void test1() {
        final Exception e = new Exception("unittest exception - test1");
        FFDCFilter.processException(e, "sourceId", "probeId");
        assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
        outputMgr.resetStreams();
        FFDCFilter.processException(e, "sourceId", "probeId", this);
        assertFalse("FFDC info message for duplicate source/probe should not be in messages.log", outputMgr.checkForMessages("FFDC1015I"));
        FFDCFilter.processException(e, "sourceId", "probeId", objs);
        assertFalse("FFDC info message for duplicate source/probe should not be in messages.log", outputMgr.checkForMessages("FFDC1015I"));
        FFDCFilter.processException(e, "sourceId", "probeId", this, objs);
        assertFalse("FFDC info message for duplicate source/probe should not be in messages.log", outputMgr.checkForMessages("FFDC1015I"));
    }

    // Test base diagnostic module support
    @Test
    public void test2() {
        final Exception e = new Exception("unittest exception - test2");
        FFDC.registerDiagnosticModule(new DiagnosticModule(), "ras.lite");
        FFDCFilter.processException(e, "sourceId", "probeId");
        FFDC.deregisterDiagnosticModule("ras.lite");
    }

    private String getPackageName() {
        String className = getClass().getName();
        return className.substring(0, className.lastIndexOf('.'));
    }

    // Test our own diagnostic module support
    @Test
    public void testDiagnosticModule() {
        String[] packageNames = new String[] { getPackageName(), "dummy.verify.single.DiagnosticModule.initialize" };
        Object callerThis = this;
        Object[] objects = { 1, true };
        String sourceId = "sourceId";

        myDiagnosticModule dm = new myDiagnosticModule();
        for (String packageName : packageNames) {
            FFDC.registerDiagnosticModule(dm, packageName);
        }
        try {
            testDiagnosticModuleNested(callerThis, objects, sourceId);
        } finally {
            for (String packageName : packageNames) {
                FFDC.deregisterDiagnosticModule(packageName);
            }
        }

        String[] expectedMethodNames = { "ffdcDumpDefaultTest1", "ffdcDumpDefaultTest2", "ffdcDumpDefaultTest3" };
        Set<String> methodNames = new TreeSet<String>();

        for (myDiagnosticModule.Call call : dm.calls) {
            assertSame("callerThis should be what was passed", callerThis, call.callerThis);
            assertEquals("objects should be what was passed", Arrays.asList(objects), Arrays.asList(call.objects));
            assertEquals("sourceId should be what was passed", sourceId, call.sourceId);
            methodNames.add(call.method);
        }

        String callsToString = dm.calls.toString();
        assertEquals("Should only be called " + expectedMethodNames.length + " times: " + callsToString, 3, dm.calls.size());
        assertEquals("Expected methods should all be called: " + callsToString, new TreeSet<String>(Arrays.asList(expectedMethodNames)), methodNames);
    }

    // Add two stack frames from this class.
    private void testDiagnosticModuleNested(Object callerThis, Object[] objects, String sourceId) {
        final Exception e = new Exception("unittest exception - testDiagnosticModule");
        FFDCFilter.processException(e, sourceId, "probeId", callerThis, objects);
    }

    // Test that various object can be captured
    @Test
    public void test4() {
        final Exception e = new Exception("unittest exception - test4");

        FFDCFilter.processException(e, "sourceId", "probeId", new byte[] { 1, 2, 3, 4, 5, 76, 7, 8, 9, 9, 6, 5, 43, 5, 56, -34, 3, 5, 6 });
        FFDCFilter.processException(e, "sourceId", "probeId", this);
        FFDCFilter.processException(e, "sourceId", "probeId", new myDiagnosticModule());
    }

    // Test self introspection
    @Test
    public void test5() {
        final Exception e = new Exception("unittest exception - test5");

        FFDCFilter.processException(e, "sourceId", "probeId", this, new Object[] { new MySelfIntrospectable() });
    }

    /**
     * This test makes sure that there is a new FFDC file created for every unique exception (46715)
     */
    @Ignore
    @Test
    public void testNewFileForEachFfdc() {
        BaseFFDCService baseFFDC = (BaseFFDCService) SharedFFDCConfigurator.getDelegate();
        File[] originalFfdcLogs = getFfdcLogs();

        // Make sure a new file is created for each of the first 10 unique exception messages
        for (int i = 1; i <= 30; i++) {
            FFDCFilter.processException(new Exception("unittest exception testNewFileForEachFfdc " + i), "sourceId", "probeId");
            File[] postExceptionFiles = getFfdcLogs();
            if (i <= 10) {
                assertEquals("An FFDC file should be created for exception #" + i,
                             i, postExceptionFiles.length - originalFfdcLogs.length);

                assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
            } else {
                assertEquals("An FFDC file should not be created for exception #" + i,
                             10, postExceptionFiles.length - originalFfdcLogs.length);
                assertFalse("FFDC info message should not exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
            }
            outputMgr.resetStreams();
        }

        // Find the incident that should be the common denominator for all of these exceptions
        IncidentImpl incident = baseFFDC.getIncident("sourceId", "probeId", new Exception(), this, new Object[] {});
        assertEquals("Incident should have 10 associated files", 10, incident.getFiles().size());

        // Now "roll" the logs: should get new/replacement files created for the new exception messages
        // The number of files should remain constant (one added, one removed)
        baseFFDC.rollLogs();

        for (int i = 1; i <= 30; i++) {
            FFDCFilter.processException(new Exception("unittest exception testNewFileForEachFfdc-POST-ROLL " + i), "sourceId", "probeId");
            File[] postExceptionFiles = getFfdcLogs();
            assertEquals("The number of exception files should remain 10",
                         10, postExceptionFiles.length - originalFfdcLogs.length);
            if (i <= 10) {
                assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
            } else {
                assertFalse("FFDC info message should not exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
            }
            outputMgr.resetStreams();
        }

        // Now clean up the incident: should delete all of the files
        File[] postExceptionFiles = getFfdcLogs();
        incident.cleanup();
        File[] postCleanupFiles = getFfdcLogs();
        assertEquals("10 files should be missing from the previous number",
                     10, postExceptionFiles.length - postCleanupFiles.length);
    }

    /**
     * This test makes sure that there is a not new FFDC file created for duplicate exceptions (46715)
     */
    @Ignore
    @Test
    public void testNoDuplicateIncident() {
        final Exception e1 = new Exception("unittest exception testNoDuplicateIncident");

        // Make sure a new file is created for the exception
        File[] originalFfdcLogs = getFfdcLogs();
        FFDCFilter.processException(e1, "sourceId", "probeId");
        File[] postExceptionFiles = getFfdcLogs();
        assertEquals("A new FFDC file should be created for the first exception",
                     1, postExceptionFiles.length - originalFfdcLogs.length);
        assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
        outputMgr.resetStreams();

        // Make sure no new file is created for the exception
        FFDCFilter.processException(e1, "sourceId", "probeId");
        postExceptionFiles = getFfdcLogs();
        assertEquals("Additional FFDC file should not be created for the second (duplicate) exception",
                     1, postExceptionFiles.length - originalFfdcLogs.length);
        assertFalse("FFDC info message should not exist in messages.log for duplicate exception", outputMgr.checkForMessages("FFDC1015I"));
        outputMgr.resetStreams();

        // Now "roll" the logs: should get another file created for the exception
        BaseFFDCService baseFFDC = (BaseFFDCService) SharedFFDCConfigurator.getDelegate();
        baseFFDC.rollLogs();

        originalFfdcLogs = getFfdcLogs();
        FFDCFilter.processException(e1, "sourceId", "probeId");
        postExceptionFiles = getFfdcLogs();
        assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
        assertEquals("The new FFDC file should have replaced the old one", 0, postExceptionFiles.length - originalFfdcLogs.length);
        outputMgr.resetStreams();

        FFDCFilter.processException(e1, "sourceId", "probeId");
        postExceptionFiles = getFfdcLogs();
        assertEquals("Additional FFDC file should not be created for the second (duplicate) exception",
                     0, postExceptionFiles.length - originalFfdcLogs.length);
        assertFalse("FFDC info message should not exist in messages.log for duplicate exception", outputMgr.checkForMessages("FFDC1015I"));
        outputMgr.resetStreams();
    }

    @Ignore
    @Test
    public void testRollIncidents() throws Exception {
        BaseFFDCService baseFFDC = (BaseFFDCService) SharedFFDCConfigurator.getDelegate();
        File[] originalFfdcLogs = getFfdcLogs();

        for (int i = 0; i < 510; i++) {
            // 510 unique incidents
            FFDCFilter.processException(new Exception("unittest exception testRollIncidents"), "sourceId", Integer.toString(i));
            assertTrue("FFDC info message should exist in messages.log", outputMgr.checkForMessages("FFDC1015I"));
            outputMgr.resetStreams();
        }

        File[] postExceptionFiles = getFfdcLogs();
        assertEquals("There should be 510 exception files in the FFDC dir",
                     510, postExceptionFiles.length - originalFfdcLogs.length);

        // Now "roll" the logs: this should trim down to 250 files...
        baseFFDC.rollLogs();

        postExceptionFiles = getFfdcLogs();
        assertEquals("There should be 500 exception files in the FFDC dir",
                     500, postExceptionFiles.length - originalFfdcLogs.length);

    }

    /**
     * Little class with a couple of sensitive fields.
     */
    static class ClassWithSensitiveFields {
        @Sensitive
        String password = "password";

        @Sensitive
        Map<String, byte[]> credentials = new HashMap<String, byte[]>();

        @Sensitive
        String secret = null;

        String message = "This is simply a message.";
    }

    /**
     * Test that sensitive fields are handled appropriately.
     */
    @Test
    public void testSensitiveFields() throws Exception {
        Exception exception = new Exception("TestSensitive");
        ClassWithSensitiveFields target = new ClassWithSensitiveFields();

        // Get list of files as they exist before this FFDC
        List<File> originalFiles = Arrays.asList(getFfdcLogs());
        FFDCFilter.processException(exception, "TestSensitiveSource", "TestSensitiveProbe", target);

        // Get the list of files after the FFDC and remove all of the old ones
        List<File> newFiles = new ArrayList<File>(Arrays.asList(getFfdcLogs()));
        newFiles.removeAll(originalFiles);

        // Make sure only one was generated
        assertEquals(1, newFiles.size());

        // Get the contents of the generated file
        String fileContents = LoggingTestUtils.readFile(newFiles.get(0));

        // Ensure we got the expected format
        assertTrue(fileContents, fileContents.matches("(?s).*password = \"<sensitive java\\.lang\\.String@[a-f0-9]+>\".*"));
        assertFalse(fileContents, fileContents.contains("password = \"password\""));
        assertTrue(fileContents, fileContents.matches("(?s).*credentials = \"<sensitive java\\.util\\.HashMap@[a-f0-9]+>\".*"));
        assertTrue(fileContents, fileContents.contains("secret = null"));
        assertTrue(fileContents, fileContents.contains("message = \"This is simply a message.\""));
    }

    /**
     * Tiny class that's marked as sensitive. All fields in the
     * object hierarchy should be treated as sensitive.
     */
    @Sensitive
    static class SensitiveClass extends ClassWithSensitiveFields {
        String sensitiveClassField = "sensitiveClassField";
    }

    /**
     * Test to ensure types marked as sensitive have all fields
     * treated that way.
     */
    @Test
    public void testSensitiveClass() throws Exception {
        Exception exception = new Exception("TestSensitiveClass");
        ClassWithSensitiveFields target = new SensitiveClass();

        // Get list of files as they exist before this FFDC
        List<File> originalFiles = Arrays.asList(getFfdcLogs());
        FFDCFilter.processException(exception, "TestSensitiveClassSource", "TestSensitiveClassProbe", target);

        // Get the list of files after the FFDC and remove all of the old ones
        List<File> newFiles = new ArrayList<File>(Arrays.asList(getFfdcLogs()));
        newFiles.removeAll(originalFiles);

        // Make sure only one was generated
        assertEquals(1, newFiles.size());

        // Get the contents of the generated file
        String fileContents = LoggingTestUtils.readFile(newFiles.get(0));

        // Ensure we got the expected format
        assertTrue(fileContents, fileContents.matches("(?s).*password = \"<sensitive java\\.lang\\.String@[a-f0-9]+>\".*"));
        assertFalse(fileContents, fileContents.contains("password = \"password\""));
        assertTrue(fileContents, fileContents.matches("(?s).*credentials = \"<sensitive java\\.util\\.HashMap@[a-f0-9]+>\".*"));
        assertTrue(fileContents, fileContents.contains("secret = null"));
        assertTrue(fileContents, fileContents.matches("(?s).*message = \"<sensitive java\\.lang\\.String@[a-f0-9]+>\".*"));
        assertTrue(fileContents, fileContents.matches("(?s).*sensitiveClassField = \"<sensitive java\\.lang\\.String@[a-f0-9]+>\".*"));
    }

    @Test
    public void testFFDCSummary() throws Exception {
        BaseFFDCService ffdcService = new BaseFFDCService();

        // Non-destructive pre-config value:
        assertEquals("maxFiles should start out as 0", 0, ffdcService.summaryLogSet.getMaxFiles());

        // Apply initial configuration
        LogProviderConfigImpl config = new LogProviderConfigImpl(new HashMap<String, String>(), TestConstants.BUILD_TMP, SharedTr.fileStreamFactory);
        ffdcService.init(config);
        File targetDir = new File(config.getLogDirectory(), "ffdc");
        assertEquals("ffdc log directory should be set via config", targetDir.getCanonicalPath(), ffdcService.getFFDCLogLocation().getCanonicalPath());
        assertEquals("maxFiles should be set via config (0)", 0, ffdcService.summaryLogSet.getMaxFiles());

        // Mimic change from config admin
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("maxFiles", 10);
        map.put("logDirectory", new File(TestConstants.BUILD_TMP, "2"));
        config.update(map);
        ffdcService.update(config);

        assertEquals("ffdc log directory should be set via config", targetDir.getCanonicalPath(), ffdcService.getFFDCLogLocation().getCanonicalPath());
        assertEquals("maxFiles should be set via config (10)", 10, ffdcService.summaryLogSet.getMaxFiles());

        File[] files = new File[3];
        for (int i = 0; i < 3; i++) {
            files[i] = ffdcService.createNewSummaryFile();
            System.out.println("createNewFile: " + files[i]);
        }

        assertTrue("there should be at least 3 files in the configured log directory", getSummaryLogs().length >= 3);
        for (int i = 0; i < 3; i++) {
            assertTrue(files[i] + " should still exist", files[i].isFile());
        }

        // we have at least 3 files now (maybe more if something leftover from another run)
        // change the max files to 2, make sure we have at most 2 files when update returns
        map.put("maxFiles", 2);
        config.update(map);
        ffdcService.update(config);
        assertEquals("maxFiles should be set via config (2)", 2, ffdcService.summaryLogSet.getMaxFiles());
        assertEquals("There should be only 2 files", 2, getSummaryLogs().length);
        for (int i = 1; i < 3; i++) {
            assertTrue(files[i] + " should still exist", files[i].isFile());
        }

        //We should still be trimming down to maxFiles of two upon creation of extra files.
        for (int i = 0; i < 2; i++) {
            files[i] = ffdcService.createNewSummaryFile();
            System.out.println("createNewFile: " + files[i]);
        }

        assertEquals("maxFiles should be set via config (2)", 2, ffdcService.summaryLogSet.getMaxFiles());
        assertEquals("There should be only 2 files", 2, getSummaryLogs().length);
        for (int i = 0; i < 2; i++) {
            assertTrue(files[i] + " should still exist", files[i].isFile());
        }
    }

    static FilenameFilter ffdcLogFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("ffdc_") && name.endsWith(".log");
        }
    };

    static FilenameFilter summaryFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("exception_summary_") && name.endsWith(".log");
        }
    };

    /**
     * Gets all of the FFDC logs from the ffdc folder. Returns an empty array if there are no files in there.
     *
     * @return An array of the ffdc log files
     */
    private File[] getFfdcLogs() {
        File target = FFDCConfigurator.getFFDCLocation();
        if (target.exists()) {
            File[] ffdcFiles = target.listFiles(ffdcLogFilter);
            return ffdcFiles;
        } else {
            System.out.println("The ffdc directory did not exist: " + target.getAbsolutePath());
        }

        // If the folder didn't exist just return an empty array
        return new File[0];
    }

    private File[] getSummaryLogs() {
        File target = FFDCConfigurator.getFFDCLocation();
        if (target.exists()) {
            File[] ffdcFiles = target.listFiles(summaryFilter);
            System.out.println("Summary logs: " + Arrays.toString(ffdcFiles));
            return ffdcFiles;
        } else {
            System.out.println("The ffdc directory did not exist: " + target.getAbsolutePath());
        }

        // If the folder didn't exist just return an empty array
        return new File[0];
    }

    // Our own diagnostic module

    public class myDiagnosticModule extends DiagnosticModule {
        final List<Call> calls = new ArrayList<Call>();

        public void ffdcDumpDefaultTest1(Throwable t, IncidentStream is, Object o, Object[] objs, String s) {
            is.writeLine("ffdcDumpDefaultTest1 called with o=" + o + " objs=" + objs + " s=" + s, "");
            calls.add(new Call("ffdcDumpDefaultTest1", o, objs, s));
        }

        public void ffdcDumpDefaultTest2(Throwable t, IncidentStream is, Object o, Object[] objs, String s) {
            is.writeLine("ffdcDumpDefaultTest2 called with o=" + o + " objs=" + objs + " s=" + s, "");
            calls.add(new Call("ffdcDumpDefaultTest2", o, objs, s));
        }

        public void ffdcDumpDefaultTest3(Throwable t, IncidentStream is, Object o, Object[] objs, String s) {
            is.writeLine("ffdcDumpDefaultTest3 called with o=" + o + " objs=" + objs + " s=" + s, "");
            calls.add(new Call("ffdcDumpDefaultTest3", o, objs, s));
        }

        public class Call {
            final String method;
            final Object callerThis;
            final Object[] objects;
            final String sourceId;

            public Call(String method, Object callerThis, Object[] objects, String sourceId) {
                this.method = method;
                this.callerThis = callerThis;
                this.objects = objects;
                this.sourceId = sourceId;
            }

            @Override
            public String toString() {
                return super.toString() +
                       "[method=" + method +
                       ",callerThis=" + callerThis +
                       ",objects=" + Arrays.asList(objects) +
                       ",sourceId=" + sourceId +
                       ']';
            }
        }
    }

    private static class MySelfIntrospectable implements FFDCSelfIntrospectable {
        @SuppressWarnings("unused")
        private final String _password = "password"; // MUST not appear in the FFDC

        // report :-)

        @Override
        public String[] introspectSelf() {
            return new String[] { "field1 = value1", "field2 = value2" };
        }
    }

}
