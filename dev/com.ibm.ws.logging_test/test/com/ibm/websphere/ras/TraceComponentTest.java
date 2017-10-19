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
package com.ibm.websphere.ras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.ejs.ras.TraceStateChangeListener;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;
import com.ibm.ws.logging.internal.SafeTraceLevelIndexFactory;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.TraceSpecification.TraceElement;
import com.ibm.ws.logging.internal.WsLogger;

import test.NoComClass;
import test.TestConstants;
import test.common.SharedOutputManager;
import test.common.SharedOutputManagerTest;

@RunWith(JMock.class)
public class TraceComponentTest {
    static final Class<?> myClass = TraceComponentTest.class;

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    Mockery context = new Mockery();
    final TraceStateChangeListener mockListener = context.mock(TraceStateChangeListener.class);

    @After
    public void tearDown() throws Exception {
        TrConfigurator.setTraceSpec("*=info=enabled"); // restore to default
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        final String m = "testConstructor";
        try {
            TraceComponent tc = new TraceComponent(myClass);
            assertEquals(tc.getName(), myClass.getName());

            tc = new TraceComponent(myClass.getName(), myClass, "group", "bundle");
            assertEquals(tc.getName(), myClass.getName());
            assertEquals(tc.getResourceBundleName(), "bundle");

            assertFalse(tc.isDebugEnabled()); // trace should be off by default
            tc.setTraceSpec("group=all=enabled"); // enable the group
            assertTrue(tc.isDebugEnabled()); // trace should be on
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testIntrospectSelf() {
        final String m = "testIntrospectSelf";
        try {
            TraceComponent tc = new TraceComponent(myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle

            assertEquals("TraceComponent[" + myClass.getName()
                         + "," + myClass
                         + ",[],null,null]", str[0]);

            tc.setResourceBundleName("new name");

            str = tc.introspectSelf();
            assertEquals("TraceComponent[" + myClass.getName()
                         + "," + myClass
                         + ",[],new name,null]", str[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testStateChangeListener() {
        final String m = "testStateChangeListener";
        try {

            TraceComponent tc = new TraceComponent(myClass);
            tc.setLoggerForCallback(mockListener);

            context.checking(new Expectations() {
                {
                    one(mockListener).traceStateChanged();
                }
            });

            tc.setTraceSpec("*=all=enabled");

            outputMgr.assertContextStatisfied(context);

            tc.setLoggerForCallback(null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    @Ignore
    public void testUseLogger() {
        final String m = "testUseLogger";
        try {
            TraceComponent tc = new TraceComponent(myClass);

            String[] out = tc.introspectSelf();
            for (String o : out) {
                System.out.println(o);
            }

            Logger logger = tc.getLogger();

            out = tc.introspectSelf();
            for (String o : out) {
                System.out.println(o);
            }

            System.out.println(LogManager.getLogManager());
            assertSame("TraceComponent should return same instance fore repeated calls to getLogger", logger, tc.getLogger());

            Field f = WsLogger.class.getDeclaredField("ivTC");
            f.setAccessible(true);
            Object o = f.get(logger);
            assertSame("Logger should link back to original trace component", o, tc);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test all variations of trace levels: *=tracelevel=enabled|disabled
     */
    @Test
    public void testTraceLevels() {
        final String m = "testTraceLevels";
        try {
            TraceComponent tc = new TraceComponent(myClass);

            Field f = TraceComponent.class.getDeclaredField("fineTraceEnabled");
            f.setAccessible(true);
            //Now the field fineTraceEnabled is of Object type
            f.set(null, null);

            assertAllFlagsDisabled(tc);

            int i = 0;
            int g = 0;

            System.out.println(m + ": enabled tr components: " + TraceComponent.isAnyTracingEnabled());

            // Make sure that as we turn on each individual level (the strings
            // from TrLevelConstants.traceLevels), the appropriate traceLevel value
            // is assigned, and the is*Enabled methods return the corresponding value
            for (String group[] : TrLevelConstants.traceLevels) {
                for (String strLevel : group) {
                    // enable given trace spec
                    tc.setTraceSpec("*=" + strLevel + "=enabled");

                    String msg = "level=" + strLevel + ", tc.getLoggerLevel=" + tc.getLoggerLevel() + " :: ";

                    // This is (unfortunately) hard-coded in TraceComponent
                    // based on the value of "found". The bese we can do is
                    // indicate here when we've mismatched the group
                    if (g <= 0)
                        assertTrue(msg + "dump should be enabled", tc.isDumpEnabled());
                    else
                        assertFalse(msg + "dump should be disabled", tc.isDumpEnabled());

                    if (g <= 1)
                        assertTrue(msg + "debug should be enabled", tc.isDebugEnabled());
                    else
                        assertFalse(msg + "debug should be disabled", tc.isDebugEnabled());

                    if (g <= 2)
                        assertTrue(msg + "entry/exit should be enabled", tc.isEntryEnabled());
                    else
                        assertFalse(msg + "entry/exit should be disabled", tc.isEntryEnabled());

                    if (g <= 3)
                        assertTrue(msg + "event should be enabled", tc.isEventEnabled());
                    else
                        assertFalse(msg + "event should be disabled", tc.isEventEnabled());

                    if (g <= 4)
                        assertTrue(msg + "detail should be enabled", tc.isDetailEnabled());
                    else
                        assertFalse(msg + "detail should be disabled", tc.isDetailEnabled());

                    if (g <= 5)
                        assertTrue(msg + "config should be enabled", tc.isConfigEnabled());
                    else
                        assertFalse(msg + "config should be disabled", tc.isConfigEnabled());

                    if (g <= 6)
                        assertTrue(msg + "info should be enabled", tc.isInfoEnabled());
                    else
                        assertFalse(msg + "info should be disabled", tc.isInfoEnabled());

                    if (g <= 7) {
                        assertTrue(msg + "audit should be enabled", tc.isAuditEnabled());
                        assertTrue(msg + "service should be enabled", tc.isServiceEnabled());
                    } else {
                        assertFalse(msg + "audit should be disabled", tc.isAuditEnabled());
                        assertFalse(msg + "service should be disabled", tc.isServiceEnabled());
                    }

                    if (g <= 8)
                        assertTrue(msg + "warning should be enabled", tc.isWarningEnabled());
                    else
                        assertFalse(msg + "warning should be disabled", tc.isWarningEnabled());

                    if (g <= 9)
                        assertTrue(msg + "error should be enabled", tc.isErrorEnabled());
                    else
                        assertFalse(msg + "error should be disabled", tc.isErrorEnabled());

                    if (g <= 10)
                        assertTrue(msg + "fatal should be enabled", tc.isFatalEnabled());
                    else
                        assertFalse(msg + "fatal should be disabled", tc.isFatalEnabled());

                    i++;
                }
                g++;
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test all variations of trace levels: *=tracelevel=enabled|disabled,
     * but test that more detailed levels than the minimum safe level for
     * sensitive packages are not set.
     * Safe level set to com.ibm.websphere.ras.TraceComponentTest=info.
     */
    @Test
    public void testTraceLevelsSensitive() {
        final String m = "testTraceLevelsSensitive";
        try {
            TraceComponent tc = new TraceComponent(myClass);

            Field f = TraceComponent.class.getDeclaredField("fineTraceEnabled");
            f.setAccessible(true);
            //Now the field fineTraceEnabled is of Object type
            f.set(null, null);

            assertAllFlagsDisabled(tc);

            System.out.println(m + ": enabled tr components: " + TraceComponent.isAnyTracingEnabled());

            PackageIndex<Integer> packageIndex = SafeTraceLevelIndexFactory.createPackageIndex(
                                                                                               "test/properties/test.ras.rawtracelist.properties");

            changeSpecLevelsAndAssertTraceComponentLevelsNotSensitive(tc, packageIndex);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test all variations of trace levels: *=tracelevel=enabled|disabled,
     * but test that more detailed levels than the minimum safe level for
     * sensitive packages are not set.
     * Safe level set to TraceComponentTestGroup=info.
     */
    @Test
    public void testTraceLevelsSensitiveWithGroups() {
        final String m = "testTraceLevelsSensitiveWithGroups";
        try {
            TraceComponent tc = new TraceComponent("TraceComponentTest", myClass, "TraceComponentTestGroup", "TraceComponentTestBundle");

            Field f = TraceComponent.class.getDeclaredField("fineTraceEnabled");
            f.setAccessible(true);
            //Now the field fineTraceEnabled is of Object type
            f.set(null, null);

            assertAllFlagsDisabled(tc);

            System.out.println(m + ": enabled tr components: " + TraceComponent.isAnyTracingEnabled());

            PackageIndex<Integer> packageIndex = SafeTraceLevelIndexFactory.createPackageIndex(
                                                                                               "test/properties/test.groups.ras.rawtracelist.properties");

            changeSpecLevelsAndAssertTraceComponentLevelsNotSensitive(tc, packageIndex);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private void changeSpecLevelsAndAssertTraceComponentLevelsNotSensitive(TraceComponent tc, PackageIndex<Integer> packageIndex) {
        int g = 0;
        for (String group[] : TrLevelConstants.traceLevels) {
            for (String strLevel : group) {
                // enable given trace spec
                TraceSpecification ts = new TraceSpecification("*=" + strLevel + "=enabled", packageIndex, true);
                tc.setTraceSpec(ts);

                String msg = "level=" + strLevel + ", tc.getLoggerLevel=" + tc.getLoggerLevel() + " :: ";

                if (g <= 0)
                    assertFalse(msg + "dump should be disabled", tc.isDumpEnabled());

                if (g <= 1)
                    assertFalse(msg + "debug should be disabled", tc.isDebugEnabled());

                if (g <= 2)
                    assertFalse(msg + "entry/exit should be disabled", tc.isEntryEnabled());

                if (g <= 3)
                    assertFalse(msg + "event should be disabled", tc.isEventEnabled());

                if (g <= 4)
                    assertFalse(msg + "detail should be disabled", tc.isDetailEnabled());

                if (g <= 5)
                    assertFalse(msg + "config should be disabled", tc.isConfigEnabled());

                if (g <= 6)
                    assertTrue(msg + "info should be enabled", tc.isInfoEnabled());
                else
                    assertFalse(msg + "info should be disabled", tc.isInfoEnabled());

                if (g <= 7) {
                    assertTrue(msg + "audit should be enabled", tc.isAuditEnabled());
                    assertTrue(msg + "service should be enabled", tc.isServiceEnabled());
                } else {
                    assertFalse(msg + "audit should be disabled", tc.isAuditEnabled());
                    assertFalse(msg + "service should be disabled", tc.isServiceEnabled());
                }

                if (g <= 8)
                    assertTrue(msg + "warning should be enabled", tc.isWarningEnabled());
                else
                    assertFalse(msg + "warning should be disabled", tc.isWarningEnabled());

                if (g <= 9)
                    assertTrue(msg + "error should be enabled", tc.isErrorEnabled());
                else
                    assertFalse(msg + "error should be disabled", tc.isErrorEnabled());

                if (g <= 10)
                    assertTrue(msg + "fatal should be enabled", tc.isFatalEnabled());
                else
                    assertFalse(msg + "fatal should be disabled", tc.isFatalEnabled());
            }
            g++;
        }
    }

    /**
     * Test that the sensitive flag can be set when using
     * the same trace specification string.
     */
    @Test
    public void testLevelWhenUsingSameSpecificationStringDifferentSensitiveFlag() {
        final String m = "testLevelWhenUsingSameSpecificationStringDifferentSensitiveFlag";
        try {
            // Remove the package index and set location to load the index from
            Field f = TrConfigurator.class.getDeclaredField("safeLevelsIndex");
            f.setAccessible(true);
            f.set(null, null);
            TrConfigurator.setSensitiveTraceListResourceName("test/properties/test.ras.rawtracelist.properties");

            // Set specification and register this test class
            TrConfigurator.setTraceSpec("com.ibm.websphere.ras.*=all");
            TraceComponent tc = Tr.register(myClass);

            // Turn off sensitive traces suppression and keep same trace specification
            final Map<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put("suppressSensitiveTrace", false);
            newConfig.put("traceSpecification", "com.ibm.websphere.ras.*=all");
            TrConfigurator.update(newConfig);

            assertAllFlagsEnabled(tc);

            // Turn on sensitive traces suppression and keep same trace specification
            newConfig.put("suppressSensitiveTrace", true);
            TrConfigurator.update(newConfig);

            assertFlagsMoreDetailedThanInfoAreDisabled(tc);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private void assertAllFlagsEnabled(TraceComponent tc) {
        assertTrue(tc.isAuditEnabled());
        assertTrue(tc.isConfigEnabled());
        assertTrue(tc.isDebugEnabled());
        assertTrue(tc.isDetailEnabled());
        assertTrue(tc.isDumpEnabled());
        assertTrue(tc.isEntryEnabled());
        assertTrue(tc.isErrorEnabled());
        assertTrue(tc.isEventEnabled());
        assertTrue(tc.isFatalEnabled());
        assertTrue(tc.isInfoEnabled());
        assertTrue(tc.isServiceEnabled());
        assertTrue(tc.isWarningEnabled());
    }

    private void assertAllFlagsDisabled(TraceComponent tc) {
        assertFalse(tc.isAuditEnabled());
        assertFalse(tc.isConfigEnabled());
        assertFalse(tc.isDebugEnabled());
        assertFalse(tc.isDetailEnabled());
        assertFalse(tc.isDumpEnabled());
        assertFalse(tc.isEntryEnabled());
        assertFalse(tc.isErrorEnabled());
        assertFalse(tc.isEventEnabled());
        assertFalse(tc.isFatalEnabled());
        assertFalse(tc.isInfoEnabled());
        assertFalse(tc.isServiceEnabled());
        assertFalse(tc.isWarningEnabled());
    }

    private void assertFlagsMoreDetailedThanInfoAreDisabled(TraceComponent tc) {
        assertFalse("dump should be disabled", tc.isDumpEnabled());
        assertFalse("debug should be disabled", tc.isDebugEnabled());
        assertFalse("entry/exit should be disabled", tc.isEntryEnabled());
        assertFalse("event should be disabled", tc.isEventEnabled());
        assertFalse("detail should be disabled", tc.isDetailEnabled());
        assertFalse("config should be disabled", tc.isConfigEnabled());
        assertTrue("info should be enabled", tc.isInfoEnabled());
        assertTrue("audit should be enabled", tc.isAuditEnabled());
        assertTrue("service should be enabled", tc.isServiceEnabled());
    }

    /**
     * Test variations of trace strings, including multiples
     */
    @Test
    public void testTraceStrings() {
        final String m = "testTraceStrings";
        try {
            TraceSpecification ts = null;
            TraceComponent ibm = new TraceComponent(myClass);
            TraceComponent test = new TraceComponent(NoComClass.class);

            // Single, com.*
            String spec = "com.*=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Single, com.ibm.ejs.ras.Tr=all=enabled (specific string, no
            // match)
            spec = "com.ibm.websphere.ras.Tr=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertFalse("debug for com.* should be disabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Single, com.ibm.ejs.ras.TraceComponentTest=all=enabled (specific
            // string, match)
            spec = "com.ibm.websphere.ras.TraceComponentTest=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Single, test.*
            spec = "test.*=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertFalse("debug for com.* should be disabled", ibm.isDebugEnabled());
            assertTrue("debug for test.* should be enabled", test.isDebugEnabled());

            // Double, com.*:test.*
            spec = "test.*=all=enabled:com.*=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertTrue("debug for test.* should be enabled", test.isDebugEnabled());

            // Double: all enabled, test disabled
            spec = "*=all=enabled:test.*=all=disabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            TraceComponent.setAnyTracingEnabled(ts.isAnyTraceEnabled());
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());
            assertTrue("isAnyTraceEnabled should be true", TraceComponent.isAnyTracingEnabled());

            // Double: all enabled, test disabled
            spec = "*=all=enabled:*=warning=disabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            TraceComponent.setAnyTracingEnabled(ts.isAnyTraceEnabled());
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("warning for com.* should be disabled", ibm.isWarningEnabled());
            assertTrue("isAnyTraceEnabled should be true", TraceComponent.isAnyTracingEnabled());

            // only warning enabled: isAnyTracingEnabled should return false
            spec = "*=all=disabled:*=warning=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            TraceComponent.setAnyTracingEnabled(ts.isAnyTraceEnabled());
            assertFalse("debug for com.* should be disabled", ibm.isDebugEnabled());
            assertTrue("warning for com.* should be enabled", ibm.isWarningEnabled());
            assertFalse("isAnyTraceEnabled should be false", TraceComponent.isAnyTracingEnabled());

            // package names without wildcards
            spec = "com.ibm.websphere.ras=all=enabled:test=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            TraceComponent.setAnyTracingEnabled(ts.isAnyTraceEnabled());
            assertTrue("debug for com.ibm.websphere.ras should be enabled", ibm.isDebugEnabled());
            assertTrue("debug for test should be enabled", test.isDebugEnabled());

            // parent package name
            spec = "*=all=disabled:com.ibm.websphere=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            TraceComponent.setAnyTracingEnabled(ts.isAnyTraceEnabled());
            assertTrue("debug for com.ibm.websphere should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test should be disabled", test.isDebugEnabled());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test variations of trace strings, including case tolerance
     */
    @Test
    public void testMoreTraceStrings() {
        final String m = "testMoreTraceStrings";

        try {
            TraceSpecification ts = null;
            TraceComponent ibm = new TraceComponent(myClass);
            TraceComponent test = new TraceComponent(NoComClass.class);

            // Upper case ALL
            String spec = "com.*=ALL=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Mixed case All
            spec = "com.*=All=enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Upper case ENABLED
            spec = "com.*=all=ENABLED";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());

            // Mixed case Enabled
            spec = "com.*=all=Enabled";
            ts = new TraceSpecification(spec, null, false);
            ibm.setTraceSpec(ts);
            test.setTraceSpec(ts);
            assertTrue("debug for com.* should be enabled", ibm.isDebugEnabled());
            assertFalse("debug for test.* should be disabled", test.isDebugEnabled());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTraceSpecOrdering() {
        final String m = "testTraceSpecOrdering";

        try {
            TraceComponent rasPkg = new TraceComponent(myClass);
            TraceComponent testCommonPkg = new TraceComponent(SharedOutputManagerTest.class);
            TraceComponent testPkg = new TraceComponent(NoComClass.class);

            String spec = "test.common.*=all=disabled:test.*=all=disabled:*=info=enabled";
            TraceSpecification ts = new TraceSpecification(spec, null, false);
            rasPkg.setTraceSpec(ts);
            testCommonPkg.setTraceSpec(ts);
            testPkg.setTraceSpec(ts);
            assertTrue("info for * should be enabled", rasPkg.isInfoEnabled());
            assertFalse("info for * should be disabled", rasPkg.isDumpEnabled());
            assertFalse("all for test.common.* should be disabled", testCommonPkg.isDumpEnabled());
            assertFalse("all for test.common.* should be disabled", testCommonPkg.isFatalEnabled());
            assertFalse("dump for test.* should be disabled", testPkg.isDumpEnabled());
            assertFalse("info for test.* should be disabled", testPkg.isInfoEnabled());

            spec = "a.bc*=all=enabled:a.b*=all=enabled:a.*=all=enabled:a*=all=enabled:a.b.*=all=enabled";
            ts = new TraceSpecification(spec, null, false);
            List<TraceElement> specs = ts.getSpecs();
            ArrayList<String> actual = new ArrayList<String>();
            for (TraceElement tsSpec : specs) {
                actual.add(tsSpec.groupName);
            }

            List<String> expected = Arrays.asList("*", "a*", "a.*", "a.b*", "a.b.*", "a.bc*");
            assertEquals("Wrong sort order", expected, actual);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUniquify() {
        class UniquifyTest {
            final String[] in;
            final String[] out;

            UniquifyTest(String[] in) {
                this(in, in);
            }

            UniquifyTest(String[] in, String[] out) {
                this.in = in;
                this.out = out;
            }
        }

        final UniquifyTest[] tests = {
                                       new UniquifyTest(new String[0], new String[0]),
                                       new UniquifyTest(new String[] { "group" }),
                                       new UniquifyTest(new String[] { null }, new String[0]),

                                       new UniquifyTest(new String[] { "group1", "group2" }),
                                       new UniquifyTest(new String[] { "group", null }, new String[] { "group" }),
                                       new UniquifyTest(new String[] { null, "group" }, new String[] { "group" }),
                                       new UniquifyTest(new String[] { "group", "group" }, new String[] { "group" }),
                                       new UniquifyTest(new String[] { null, null }, new String[0]),

                                       new UniquifyTest(new String[] { "group1", "group2", "group3" }),
                                       new UniquifyTest(new String[] { "group1", null, "group3" }, new String[] { "group1", "group3" }),
                                       new UniquifyTest(new String[] { "group", "group", "group" }, new String[] { "group" }),
                                       new UniquifyTest(new String[] { "group1", "group2", "group2" }, new String[] { "group1", "group2" }),
        };

        for (UniquifyTest test : tests) {
            String testMessage = "in=" + Arrays.toString(test.in);
            String[] in = test.in.clone();
            if (test.in == test.out) {
                Assert.assertArrayEquals(testMessage, test.in, TraceComponent.uniquify(in, true));
                Assert.assertSame(testMessage, in, TraceComponent.uniquify(in, false));
                Assert.assertArrayEquals(testMessage, test.in, TraceComponent.uniquify(in, true));
                Assert.assertNotSame(testMessage, test.in, TraceComponent.uniquify(in, true));
            } else {
                Assert.assertArrayEquals(testMessage, test.out, TraceComponent.uniquify(in, false));
                Assert.assertArrayEquals(testMessage, test.out, TraceComponent.uniquify(in, true));
            }
        }
    }
}
