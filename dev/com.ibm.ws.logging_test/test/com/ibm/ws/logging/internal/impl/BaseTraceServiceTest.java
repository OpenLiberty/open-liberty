/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static com.ibm.ws.logging.internal.InternalPackageChecker.checkIfPackageIsInSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.ras.SharedTraceComponent;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.logging.internal.PackageProcessor;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 */
public class BaseTraceServiceTest extends java.util.ListResourceBundle {
    static SharedOutputManager outputMgr;
    static SharedTraceComponent tc = SharedTraceComponent.createTcClassBundle(BaseTraceServiceTest.class, BaseTraceServiceTest.class.getName());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        TrConfigurator.registerTraceComponent(tc);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    static final String s = " nosubstitute";
    static final String e1 = " entry";
    static final String e2 = " exit";
    static final String sf = " maybe just one {0}";
    static final String sfe = " or more {0} {1";

    static final String sfp1 = " maybe just one p1";

    Object os[] = new Object[] { "p1" };

    @Override
    public Object[][] getContents() {
        return resources;
    }

    private final static Object[][] resources = {
                                                  { "testAuditService", "testAuditService" },
                                                  { "testAuditService-1", "testAuditService-1" + sf },
                                                  { "testError", "testError" + sf },
                                                  { "testError-1", "testError-1" + s },
                                                  { "testError-2", "testError-2" + sf },
                                                  { "testFatal", "testFatal" + s },
                                                  { "testFatal-1", "testFatal-1" + sf },
                                                  { "testInfo", "testInfo" + s },
                                                  { "testInfo-1", "testInfo-1" + sf },
                                                  { "testWarning", "testWarning" + s },
                                                  { "testWarning-1", "testWarning-1" + sf },
    };

    @After
    public void tearDown() {
        // Always reset to NO trace enabled
        LoggingTestUtils.setTraceSpec("");

        outputMgr.resetStreams();
    }

    private void assertMessages(String s, boolean sysout, boolean syserr, boolean message, boolean trace) {
        if (sysout) {
            assertTrue("System.out should contain " + s, outputMgr.checkForStandardOut(s));
        } else {
            assertFalse("System.out should not contain " + s, outputMgr.checkForStandardOut(s));
        }
        if (syserr) {
            assertTrue("System.err should contain " + s, outputMgr.checkForStandardErr(s));
        } else {
            assertFalse("System.err should not contain " + s, outputMgr.checkForStandardErr(s));
        }
        if (message) {
            assertTrue("Message should contain " + s, outputMgr.checkForMessages(s));
        } else {
            assertFalse("Message should not contain " + s, outputMgr.checkForMessages(s));
        }
        if (trace) {
            assertTrue("Trace should contain " + s, outputMgr.checkForTrace(s));
        } else {
            assertFalse("Trace should not contain " + s, outputMgr.checkForTrace(s));
        }
    }

    @Test
    public void testSystemOutSystemErr() {
        final String m = "testSystemOutSystemErr";

        Map<String, Object> map = new HashMap<String, Object>();
        try {
            // Turn off copy of System.out and System.err to system streams
            map.put("copySystemStreams", false);
            map.put("suppressSensitiveTrace", false);
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=all");

            System.out.println("No system.out");
            System.err.println("No system.err");
            assertMessages("No system.out", false, false, true, true);
            assertMessages("No system.err", false, false, true, true);

            // Turn on copy of System.out and System.err to system streams
            map.put("copySystemStreams", true);
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=all");

            System.out.println("Yes system.out");
            System.err.println("Yes system.err");
            assertMessages("Yes system.out", true, false, true, true);
            assertMessages("Yes system.err", false, true, true, true);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);

            map.put("copySystemStreams", true);
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=info");
        }
    }

    @Test
    public void testStackTraceTrimming() {

        // Set up some mock private packages
        JUnit4Mockery context = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final Set<String> internalPackages = new HashSet<String>();
        // Define some sensible default internalPackages
        internalPackages.clear();
        internalPackages.add("com.ibm.ws.kernel.boot");
        internalPackages.add("com.ibm.ws.kernel.otherboot");

        final PackageProcessor processor = context.mock(PackageProcessor.class);
        context.checking(new Expectations() {
            {
                allowing(processor).isIBMPackage(with(any(String.class)));
                will(checkIfPackageIsInSet(internalPackages));
            }
        });
        PackageProcessor.setProcessor(processor);
        String internalClassesMarker = "\tat [internal classes]";

        String line = "\tat com.ibm.user.package.Thinger.die(Thinger.java:42)";
        System.err.println(line);
        // Don't use the output manager checks, because we don't want a regex, since we use special characters
        String output = fixCR(outputMgr.getCapturedErr());
        assertEquals("A stack trace of a user package shouldn't be filtered. ", line + "\n", output);
        assertFalse("If the stack trace was not trimmed, there should not be something saying we trimmed it: " + output,
                    output.contains(internalClassesMarker));

        outputMgr.resetStreams();

        line = "\tat com.ibm.ws.kernel.boot.Launcher.configAndLaunchPlatform(Launcher.java:311)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("A first stack trace of a user package should be passed through.", line + "\n", output);
        // Any subsequent things in a private package should be suppressed, and have the internal package marker added instead
        outputMgr.resetStreams();
        line = "\tat com.ibm.ws.kernel.otherboot.AnotherLauncher.anotherMethod(AnotherLauncher.java:221)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("Any following private packages get the internal classes marker.", internalClassesMarker + "\n", output);
        // If we add another line, we shouldn't get any output at all
        outputMgr.resetStreams();
        line = "\tat com.ibm.ws.kernel.boot.AnotherLauncher.anotherMethod(AnotherLauncher.java:221)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("Any further private packages should be suppressed entirely.", "", output);

        // Even public packages should be suppressed, until we leave the stack trace
        outputMgr.resetStreams();
        line = "\tat some.dangling.jvm.method(AnotherLauncher.java:221)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("After we hit internal packages, third-party-API should not be suppressed.", line + "\n", output);

        // User packages should still be passed through, too
        outputMgr.resetStreams();
        line = "\tat com.ibm.user.package.TroubleCauser(TroubleCauser.java:63)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("A stack trace of a user package shouldn't be filtered, if we're not at the bottom of a stack containing internal packages. ", line + "\n", output);
        assertFalse("If the stack trace was not trimmed, there should not be something saying we trimmed it: " + output,
                    output.contains(internalClassesMarker));

        // Once we leave the stack trace, we better get output
        outputMgr.resetStreams();
        line = "And now we have some normal output";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("Normal output should be passed through", line + "\n", output);

        // User packages should now be passed through, too
        outputMgr.resetStreams();
        line = "\tat com.ibm.user.package.Thinger.die(Thinger.java:42)";
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("A stack trace of a user package shouldn't be filtered, if we're not at the bottom of a stack containing internal packages. ", line + "\n", output);
        assertFalse("If the stack trace was not trimmed, there should not be something saying we trimmed it: " + output,
                    output.contains(internalClassesMarker));

        // but we should resume filtering once we hit internal packages
        outputMgr.resetStreams();
        line = "\tat com.ibm.ws.kernel.otherboot.AnotherLauncher.anotherMethod(AnotherLauncher.java:221)";
        String expected = line + "\n" + internalClassesMarker + "\n";
        System.err.println(line);
        System.err.println(line);
        System.err.println(line);
        output = fixCR(outputMgr.getCapturedErr());
        assertEquals("After filtering and then outputting normal output subsequent internal packages should be filtered as before", expected, output);

    }

    @Ignore
    @Test
    public void testAuditService() {
        final String m = "testAuditService";

        try {
            Tr.audit(tc, m, os);
            assertMessages(m, true, false, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=audit=enabled");

            Tr.audit(tc, m + "-1", os);
            assertMessages(m + "-1" + sfp1, true, false, true, false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTraceFileNameStdout() {
        final String m = "testTraceFileNameStdout";

        Map<String, Object> map = new HashMap<String, Object>();
        try {
            // Turn off copy of System.out and System.err to system streams
            map.put("traceFileName", "stdout");
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=all");

            System.out.println("system.out, no trace");
            System.err.println("system.err, no trace");
            assertMessages("system.out", true, false, true, false);
            assertMessages("system.err", false, true, true, false);

            Tr.info(tc, "Tr.info: system.out, no trace", os);
            Tr.warning(tc, "Tr.warning: system.out, no trace", os);
            Tr.error(tc, "Tr.error: system.err, no trace", os);

            assertMessages("Tr.info", true, false, true, false);
            assertMessages("Tr.warning", true, false, true, false);
            assertMessages("Tr.error", false, true, true, false);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);

        } finally {
            map.put("traceFileName", "trace.log");
            TrConfigurator.update(map);
            LoggingTestUtils.setTraceSpec("*=info");
        }
    }

    @Test
    public void testDebug() {
        final String m = "testDebug";

        try {
            Tr.debug(tc, m, os);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=debug=enabled");

            String m1 = m + "-1";
            Tr.debug(tc, m1, os);
            assertMessages(m1, false, false, false, true);
            assertTrue("Trace output should contain parameter " + os[0], outputMgr.checkForTrace((String) os[0]));

            //test parameter substitution in a message
            String m2 = m + "-2 ";
            String m2txt = m2 + sf;
            String m2result = m2 + sfp1;
            Tr.debug(tc, m2txt, os);
            assertMessages(m2result, false, false, false, true);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testOff() {
        final String m = "testOff";

        try {
            Tr.debug(tc, m, os);
            assertMessages(m, false, false, false, false);

            String m1 = m + "-1";
            Tr.audit(tc, m1, os);
            assertMessages(m1, true, false, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=off");

            String m2 = m + "-2";
            Tr.debug(tc, m2, os);
            assertMessages(m2, false, false, false, false);

            // AUDIT message still goes to console.log and messages.log!
            String m3 = m + "-3";
            Tr.audit(tc, m3);
            assertMessages(m3, true, false, true, false);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testDump() {
        final String m = "testDump";

        try {
            Tr.dump(tc, m, os);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=dump=enabled");

            String m1 = m + "-1";
            Tr.dump(tc, m1, os);
            assertMessages(m1, false, false, false, true);
            assertTrue("Trace output should contain parameter " + os[0], outputMgr.checkForTrace((String) os[0]));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testEntryExit() {
        final String m = "testEntryExit";

        try {
            Tr.entry(tc, m + e1, os);
            Tr.exit(tc, m + e2);
            assertMessages(m + e1, false, false, false, false);
            assertMessages(m + e2, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=entryExit=enabled");

            String m1 = m + "-1";
            Tr.entry(tc, m1 + e1, os);
            Tr.exit(tc, m1 + e2);
            assertMessages(m1 + e1, false, false, false, true);
            assertMessages(m1 + e2, false, false, false, true);
            assertTrue("Trace output should contain output parameter p1", outputMgr.checkForTrace((String) os[0]));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testError() {
        final String m = "testError";

        try {
            Tr.error(tc, m, os);
            assertMessages(m + sfp1, false, true, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=error=enabled");

            String m1 = m + "-1";
            String m2 = m + "-2";
            Tr.error(tc, m1, os);
            Tr.error(tc, m2, os);
            assertMessages(m1 + s, false, true, true, false);
            assertMessages(m2 + sfp1, false, true, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=all=enabled");

            Tr.error(tc, m1, os);
            Tr.error(tc, m2, os);
            assertMessages(m1 + s, false, true, true, true);
            assertMessages(m2 + sfp1, false, true, true, true);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testEvent() {
        final String m = "testEvent";

        try {
            Tr.event(tc, m, os);
            assertMessages(m, false, false, false, false);

            LoggingTestUtils.setTraceSpec("*=event=enabled");

            String m1 = m + "-1";
            Tr.event(tc, m1 + s, os);
            assertMessages(m1 + s, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFatal() {
        final String m = "testFatal";

        try {
            Tr.fatal(tc, m, os);
            assertMessages(m + s, false, true, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=all=enabled");

            // With audit on, expect two trace records, and one message to stderr
            String m1 = m + "-1";
            Tr.fatal(tc, m1, os);
            assertMessages(m1 + sfp1, false, true, true, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testInfo() {
        final String m = "testInfo";

        try {
            Tr.info(tc, m, os);
            assertMessages(m + s, false, false, true, false);

            LoggingTestUtils.setTraceSpec("*=all=enabled");

            String m1 = m + "-1";
            Tr.info(tc, m1, os);
            assertMessages(m1 + sfp1, false, false, true, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testSquashSystemOut() {
        final String m = "testSquashSystemOut";

        try {
            System.out.println(m);
            assertMessages(m, true, false, true, false);

            LoggingTestUtils.setTraceSpec("SystemOut=warning");
            String m1 = m + "-1";
            System.out.println(m1);
            assertMessages(m1, true, false, true, false);

            LoggingTestUtils.setTraceSpec("SystemOut=off");
            String m2 = m + "-2";
            System.out.println(m2);
            assertMessages(m2, false, false, false, false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testSquashSystemErr() {
        final String m = "testSquashSystemErr";

        try {
            System.err.println(m);
            assertMessages(m, false, true, true, false);

            LoggingTestUtils.setTraceSpec("SystemErr=warning");
            String m1 = m + "-1";
            System.err.println(m1);
            assertMessages(m1, false, true, true, false);

            LoggingTestUtils.setTraceSpec("SystemErr=off");
            String m2 = m + "-2";
            System.err.println(m2);
            assertMessages(m2, false, false, false, false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Ignore
    @Test
    public void testWarning() {
        final String m = "testWarning";

        try {
            Tr.warning(tc, m, os);
            assertMessages(m + s, true, false, true, false);

            LoggingTestUtils.setTraceSpec("*=all=enabled");

            // With audit on, expect two trace records, and one console record
            String m1 = m + "-1";
            Tr.warning(tc, m1, os);
            assertMessages(m1 + sfp1, true, false, true, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private String fixCR(String input) {
        //remove \r chars
        return input.replace("\r", "");
    }

}
