/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.SharedTraceComponent;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCConfigurator;

/**
 *
 */
@RunWith(JMock.class)
public class TrLogServiceImplTest {
    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
        FFDCConfigurator.init(SharedTr.getDefaultConfig());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    public static final List<String> getTraceGroups(TraceComponent tc) {
        try {
            Field f = TraceComponent.class.getDeclaredField("groups");
            f.setAccessible(true);

            String[] groups = (String[]) f.get(tc);
            return Arrays.asList(groups);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find TraceComponent groups field", e);
        }
    }

    final String symName = "com.ibm.sym.name";
    final String version = "1.0.0";
    final String ffdc = symName + "-" + version;
    final String serviceRefTrace = "ServiceRef:[class1, class2](id=service.id, pid=service.pid)";
    final String bundleNameTrace = "Bundle:com.ibm.sym.name(id=2)";
    final String logTraceName = "LogService-2-com.ibm.sym.name";

    final Mockery context = new JUnit4Mockery();
    final Bundle mockBundle = context.mock(Bundle.class);

    final ServiceReference<?> mockReference = context.mock(ServiceReference.class);

    final TrLogImpl logImpl = new TrLogImpl();
    final Properties p = new Properties();

    @Before
    public void setUp() throws Exception {
        p.clear();
        Field f = TrLogImpl.class.getDeclaredField("myTc");
        f.setAccessible(true);
        TraceComponent tc = (TraceComponent) f.get(null);
        TrConfigurator.registerTraceComponent(tc);
    }

    @After
    public void tearDown() {
        SharedTr.clearComponents();
        LoggingTestUtils.setTraceSpec("*=info=enabled");
        outputMgr.resetStreams();
    }

    protected void setDefaultExpectations() {
        context.checking(new Expectations() {
            {
                allowing(mockBundle).getBundleId();
                will(returnValue(2L));

                atLeast(1).of(mockBundle).getVersion();
                will(returnValue(new Version(version)));

                atLeast(1).of(mockBundle).getSymbolicName();
                will(returnValue(symName));

                atLeast(1).of(mockBundle).getHeaders("");
                will(returnValue(p));
            }
        });
    }

    protected void setLogExpectations(boolean useServiceRef) {
        if (useServiceRef) {
            context.checking(new Expectations() {
                {
                    atLeast(1).of(mockReference).getProperty("objectClass");
                    will(returnValue(new String[] { "class1", "class2" }));

                    atLeast(1).of(mockReference).getProperty("service.id");
                    will(returnValue("service.id"));

                    atLeast(1).of(mockReference).getProperty("service.pid");
                    will(returnValue("service.pid"));
                }
            });
        }
    }

    private void assertMessages(String s, boolean sysout, boolean syserr, boolean message, boolean trace) {
        if (sysout) {
            assertTrue("System.out should contain " + s, outputMgr.checkForLiteralStandardOut(s));
        } else {
            assertFalse("System.out should not contain " + s, outputMgr.checkForLiteralStandardOut(s));
        }
        if (syserr) {
            assertTrue("System.err should contain " + s, outputMgr.checkForLiteralStandardErr(s));
        } else {
            assertFalse("System.err should not contain " + s, outputMgr.checkForLiteralStandardErr(s));
        }
        if (message) {
            assertTrue("Message should contain " + s, outputMgr.checkForLiteralMessages(s));
        } else {
            assertFalse("Message should not contain " + s, outputMgr.checkForLiteralMessages(s));
        }
        if (trace) {
            assertTrue("Trace should contain " + s, outputMgr.checkForLiteralTrace(s));
        } else {
            assertFalse("Trace should not contain " + s, outputMgr.checkForLiteralTrace(s));
        }
    }

    @Test
    public void testTrLogServiceCtorNameVersion() {
        final String m = "testTrLogServiceCtorNameVersion";
        try {
            setDefaultExpectations();
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            assertSame("mock bundle stored as bundle", mockBundle, impl.getBundle());
            assertEquals("ffdc string: " + ffdc, ffdc, impl.ffdcMe);
            assertEquals("Trace component class is the OSGiTraceComponent", TrLogServiceImpl.OSGiTraceComponent.class, impl.tc.getClass());
            assertEquals("Trace component name assembled log service string", logTraceName, impl.tc.getName());
            assertEquals("toString should be simple class name + ffdc", TrLogServiceImpl.class.getSimpleName() + "[" + ffdc + "]", impl.toString());

            List<String> list = getTraceGroups(impl.tc);
            assertEquals("Trace component has two groups", 2, list.size());
            assertTrue("Trace component has symName as group", list.contains(symName));
            assertTrue("Trace component has logservice as group", list.contains("logservice"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrLogServiceCtorNoName() {
        final String m = "testTrLogServiceCtorNoName";
        try {
            context.checking(new Expectations() {
                {
                    one(mockBundle).getSymbolicName();
                    will(returnValue(null));

                    one(mockBundle).getBundleId();
                    will(returnValue(2L));

                    one(mockBundle).getHeaders("");
                    will(returnValue(p));
                }
            });

            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            String fakeFFDC = "osgi-bundle-" + 2;

            assertSame("mock bundle stored as bundle", mockBundle, impl.getBundle());
            assertEquals("ffdc string: " + fakeFFDC, fakeFFDC, impl.ffdcMe);
            assertEquals("Trace component class is the OSGiTraceComponent", TrLogServiceImpl.OSGiTraceComponent.class, impl.tc.getClass());
            assertEquals("Trace component name is constructed log name", "LogService-2-osgi", impl.tc.getName());

            List<String> list = getTraceGroups(impl.tc);
            assertEquals("Trace component has two groups", 2, list.size());
            assertTrue("Trace component has osgi as group", list.contains("osgi"));
            assertTrue("Trace component has logservice as group", list.contains("logservice"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrLogServiceCtorGroupHeader() {
        final String m = "testTrLogServiceCtorGroupHeader";

        try {
            p.put("WS-TraceGroup", "dummy");

            context.checking(new Expectations() {
                {
                    one(mockBundle).getVersion();
                    will(returnValue(new Version(version)));

                    one(mockBundle).getBundleId();
                    will(returnValue(2L));

                    one(mockBundle).getSymbolicName();
                    will(returnValue(symName));

                    one(mockBundle).getHeaders("");
                    will(returnValue(p));
                }
            });

            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            assertSame("mock bundle stored as bundle", mockBundle, impl.getBundle());
            assertEquals("ffdc string: " + ffdc, ffdc, impl.ffdcMe);
            assertEquals("Trace component class is the OSGiTraceComponent", TrLogServiceImpl.OSGiTraceComponent.class, impl.tc.getClass());
            assertEquals("Trace component name is constructed trace name", logTraceName, impl.tc.getName());

            List<String> list = getTraceGroups(impl.tc);
            assertEquals("Trace component has three groups, the bundle name and the logservice", 3, list.size());
            assertTrue("Trace component has symName as group", list.contains(symName));
            assertTrue("Trace component has logservice as group", list.contains("logservice"));
            assertTrue("Trace component has symName as group", list.contains("dummy"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLog() {
        final String m = "testLog";
        try {
            setDefaultExpectations();
            setLogExpectations(false);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(LogService.LOG_INFO, m);
            assertMessages(m, false, false, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=all=enabled");

            String m1 = m + "-1";
            impl.log(LogService.LOG_INFO, m1);
            assertMessages(m1, false, false, true, true);
            assertTrue("INFO message contains id=", outputMgr.checkForLiteralMessages("id="));
            assertTrue("INFO message contains Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertFalse("INFO message must not contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogServiceRef() {
        final String m = "testLogServiceRef";
        try {
            setDefaultExpectations();
            setLogExpectations(true);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(mockReference, LogService.LOG_INFO, m);
            assertMessages(m, false, false, true, false);
            assertTrue("INFO message contains id=", outputMgr.checkForLiteralMessages("id="));
            assertTrue("INFO message contains Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertTrue("INFO message must contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogException() {
        final String m = "testLogException";
        try {
            setDefaultExpectations();
            setLogExpectations(false);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(LogService.LOG_WARNING, m, new Exception());

            assertMessages(m, true, false, true, false);
            assertTrue("WARNING message contains id=", outputMgr.checkForLiteralMessages("id="));
            assertTrue("WARNING message contains Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertFalse("WARNING message must not contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
            assertTrue("WARNING message must contain stack trace", outputMgr.checkForLiteralMessages("\tat"));

            assertFalse("Exception should not generate FFDC", outputMgr.checkForLiteralMessages("FFDC"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogBundleException() {
        final String m = "testLogBundleException";
        try {
            setDefaultExpectations();
            setLogExpectations(false);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(LogService.LOG_ERROR, m, new BundleException("message"));

            assertMessages(m, false, false, false, false);
            assertFalse("ERROR message must not contain id=", outputMgr.checkForLiteralMessages("id="));
            assertFalse("ERROR message must not contain Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertFalse("ERROR message must not contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
            assertFalse("ERROR message must not contain stack trace", outputMgr.checkForLiteralMessages("\tat"));

            assertFalse("Exception should not generate FFDC", outputMgr.checkForLiteralMessages("FFDC"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogBundleExceptionWithCause() {
        final String m = "testLogBundleExceptionWithCause";
        try {
            setDefaultExpectations();
            setLogExpectations(false);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(LogService.LOG_ERROR, m, new BundleException("message", new Exception("cause")));

            assertMessages(m, false, true, true, false);
            assertTrue("ERROR message contains id=", outputMgr.checkForLiteralMessages("id="));
            assertTrue("ERROR message contains Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertFalse("ERROR message must not contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
            assertTrue("ERROR message must contain stack trace", outputMgr.checkForLiteralMessages("\tat"));

            assertFalse("Exception should not generate FFDC", outputMgr.checkForLiteralMessages("FFDC"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogServiceRefException() {
        final String m = "testLogServiceRefException";
        try {
            setDefaultExpectations();
            setLogExpectations(true);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);
            LoggingTestUtils.setTraceSpec("*=info=enabled");

            impl.log(mockReference, LogService.LOG_ERROR, m, new Exception());

            assertMessages(m, false, true, true, false);

            assertTrue("ERROR message contains id=", outputMgr.checkForLiteralMessages("id="));
            assertTrue("ERROR message contains Bundle:symname", outputMgr.checkForLiteralMessages(bundleNameTrace));
            assertTrue("ERROR message must not contain ServiceRef", outputMgr.checkForLiteralMessages(serviceRefTrace));
            assertTrue("ERROR message must contain stack trace", outputMgr.checkForLiteralMessages("\tat"));

            assertFalse("Exception should not generate FFDC", outputMgr.checkForLiteralMessages("FFDC"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLogDebug() {
        final String m = "testLogDebug";
        try {
            setDefaultExpectations();
            setLogExpectations(true);
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            impl.log(mockReference, LogService.LOG_DEBUG, m, new Exception());
            assertMessages(m, false, false, false, false);
            assertFalse("publishLogEntry message to standard out", outputMgr.checkForLiteralStandardOut("publishLogEntry"));

            // now enable debug level
            LoggingTestUtils.setTraceSpec("*=all=enabled");
            impl.log(mockReference, LogService.LOG_DEBUG, m, new Exception());
            assertMessages(m, false, false, false, true);
            assertMessages("\tat", false, false, false, true);
            assertFalse("publishLogEntry message to standard out", outputMgr.checkForLiteralTrace("publishLogEntry"));
            assertTrue("DEBUG message contains id=", outputMgr.checkForLiteralTrace("id="));
            assertTrue("DEBUG message must contain log name", outputMgr.checkForLiteralTrace(logTraceName));
            assertTrue("DEBUG message must not contain ServiceRef", outputMgr.checkForLiteralTrace(serviceRefTrace));
            assertFalse("DEBUG message does not create FFDC", outputMgr.checkForLiteralTrace("FFDC"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            SharedTraceComponent.setAnyTracingEnabled(false);
        }
    }

    @Test
    public void testLogAllDebug() {
        final String m = "testLogAllDebug";
        try {
            setDefaultExpectations();
            setLogExpectations(true);

            TrLogServiceImpl.updatePublishEventConfig("ALL");
            TrLogServiceImpl impl = new TrLogServiceImpl(logImpl, mockBundle);

            LoggingTestUtils.setTraceSpec("*=all=enabled");

            impl.log(mockReference, LogService.LOG_DEBUG, m, new Exception());

            assertMessages(m, false, false, false, true);
            assertTrue("DEBUG message contains id=", outputMgr.checkForLiteralTrace("id="));
            assertTrue("DEBUG message must contain log name", outputMgr.checkForLiteralTrace(logTraceName));
            assertTrue("DEBUG message must not contain ServiceRef", outputMgr.checkForLiteralTrace(serviceRefTrace));
            assertFalse("DEBUG message does not create FFDC", outputMgr.checkForLiteralTrace("FFDC"));
            assertTrue("publishLogEntry message", outputMgr.checkForLiteralTrace("publishLogEntry"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            TrLogServiceImpl.updatePublishEventConfig("NONE");
        }
    }
}
