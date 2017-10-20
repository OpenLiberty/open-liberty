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
package com.ibm.ejs.ras;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.impl.LoggingFileUtils;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.LogProviderConfig;
import com.ibm.wsspi.logprovider.TrService;

import test.LoggingTestUtils;
import test.common.SharedOutputManager;

/**
 * Test TraceComponent registration methods
 */
@SuppressWarnings("deprecation")
@RunWith(JMock.class)
public class TrRegisterTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static final Class<?> myClass = TrRegisterTest.class;
    static final String myName = TrRegisterTest.class.getName();

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

    static TraceComponent tc = new com.ibm.ejs.ras.TraceComponent(TrService.class);

    Mockery context = new JUnit4Mockery();
    final LogProviderConfig mockConfig = context.mock(LogProviderConfig.class);
    final TrService mockService = context.mock(TrService.class);
    final TextFileOutputStreamFactory txtFactory = context.mock(TextFileOutputStreamFactory.class);
    OutputStream mFileStream;
    OutputStream tFileStream;

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        SharedTr.clearConfig();

        final String testBuildDir = System.getProperty("test.buildDir", "generated");
        File traceLogs = new File(testBuildDir + "/trace-logs");
        String logs = "logs";
        if (traceLogs.exists()) {
            logs = "trace-logs";
        }
        final File mFile = new File(testBuildDir + "/" + logs + "/messages.log").getAbsoluteFile();
        mFileStream = new FileOutputStream(mFile, true);
        final File tFile = new File(testBuildDir + "/" + logs + "/trace.log").getAbsoluteFile();
        tFileStream = new FileOutputStream(tFile, true);
        // Create one TraceComponent shared by tests below
        // (See TrRegisterTest for exercise of Tr.register)
        context.checking(new Expectations() {
            {
                allowing(mockConfig).getTrDelegate();
                will(returnValue(mockService));

                allowing(mockConfig).getTextFileOutputStreamFactory();
                will(returnValue(txtFactory));

                allowing(txtFactory).createOutputStream(with(mFile), with(true));
                will(returnValue(mFileStream));

                allowing(txtFactory).createOutputStream(with(tFile), with(true));
                will(returnValue(tFileStream));

                allowing(mockService).init(mockConfig);
                allowing(mockService).stop();

                allowing(mockConfig).getTraceString();
                will(returnValue("*=all=enabled"));

                one(mockService).register(with(any(TraceComponent.class)));
                allowing(mockService).info(with(TraceSpecification.getTc()), with("MSG_TRACE_STATE_CHANGED"), with(any(String.class)));
            }
        });
        TrConfigurator.init(mockConfig);
        tc = Tr.register(myClass, null);
    }

    @After
    public void tearDown() throws Exception {
        if (tFileStream != null)
            LoggingFileUtils.tryToClose(tFileStream);
        if (mFileStream != null)
            LoggingFileUtils.tryToClose(mFileStream);
        outputMgr.resetStreams();
        SharedTr.clearConfig();
        SharedTr.clearComponents();
    }

    @Test
    public void testRegisterClass() {
        final String m = "testRegisterClass";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myClass);
            assertEquals(tc.getTraceClass(), myClass);

            System.out.println(tc.toString());
            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            assertEquals("TraceComponent[" + myClass.getCanonicalName()
                         + "," + myClass
                         + ",[],null,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterClassGroup() {
        final String m = "testRegisterClassGroup";

        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myClass, "group");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            assertEquals("TraceComponent[" + myClass.getCanonicalName()
                         + "," + myClass
                         + ",[group],null,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterClassGroupBundle() {
        final String m = "testRegisterClassGroupBundle";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myClass, "group", "bundle");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            System.out.println(tc.toString());
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[group],bundle,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterName() {
        final String m = "testRegisterName";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register("x.logwriter");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            System.out.println(tc.toString());
            assertEquals("TraceComponent[x.logwriter"
                         + "," + myClass
                         + ",[x.logwriter],null,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /*
     * @Test public void testRegisterInvalidName() { final String m =
     * "testRegisterInvalidName"; try { context.checking(new Expectations() {{
     * one (mockService).register(with(any(TraceComponent.class))); }});
     *
     * TraceComponent tc = Tr.register("BadClassName"); // use an invalid class
     * name assertEquals(tc.getTraceClass(), myClass); // Should still end up
     * with this class!
     *
     * String str[] = tc.introspectSelf(); // returns name, group, and bundle
     * assertEquals(str[0], "name = " + myName); // Should still be the name of
     * this class! assertEquals(str[1], "groups = []"); assertEquals(str[2],
     * "bundle = null"); } catch(Throwable t) { outputMgr.failWithThrowable(m,
     * t); } }
     */

    @Test
    public void testRegisterNameGroup() {
        final String m = "testRegisterNameGroup";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myName, "groupName");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            System.out.println(tc.toString());
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[groupName],null,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRegisterNameGroupBundle() {
        final String m = "testRegisterNameGroupBundle";
        try {
            context.checking(new Expectations() {
                {
                    one(mockService).register(with(any(TraceComponent.class)));
                }
            });

            TraceComponent tc = Tr.register(myName, "groupName", "bundle");
            assertEquals(tc.getTraceClass(), myClass);

            String str[] = tc.introspectSelf(); // returns name, group, and
            // bundle
            System.out.println(tc.toString());
            assertEquals("TraceComponent[" + myName
                         + "," + myClass
                         + ",[groupName],bundle,null]", str[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /*
     * @Test public void testRegisterDumpable() { final String m =
     * "testRegisterDumpable"; try { context.checking(new Expectations() {{ one
     * (mockService).register(with(any(TraceComponent.class))); }});
     *
     * final TraceComponent tc = Tr.register(myClass); final Dumpable d =
     * context.mock(Dumpable.class);
     *
     * context.checking(new Expectations() {{ never
     * (mockService).registerDumpable(with(same(tc)), with(same(d))); }});
     *
     * Tr.registerDumpable(tc, d);
     *
     * throw new Exception(
     * "registerDumpable did not throw expected UnsupportedOperationException");
     * } catch(UnsupportedOperationException uex) { // Expected exception }
     * catch(Throwable t) { outputMgr.failWithThrowable(m, t); } }
     */
}
