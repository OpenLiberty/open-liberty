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
/**
 *
 */
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.SharedTraceComponent;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;

/**
 *
 */
@Ignore
public class TrToLoggerTestDisabled {
    static final Class<?> myClass = TrToLoggerTestDisabled.class;
    static final String myName = TrToLoggerTestDisabled.class.getName();

    static SharedOutputManager outputMgr;

    /** Common trace component */
    static TraceComponent tc;
    static BaseTraceService trService;
    static SharedLogHandler handler;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Configure Tr to use JSR47; registers a custom log handler,
        // push console output to stderr so we can see each separately
        // (don't rely on whether or not the default console handler is present, as
        // that seems to vary)
        System.setProperty("com.ibm.ws.logging.trace.file.name", "java.util.logging");

        // make stdout/stderr "quiet" & initialize Tr -- no output will 
        // show up for test unless one of the copy methods is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        tc = new SharedTraceComponent(myClass);
        TrConfigurator.registerTraceComponent(tc);

        trService = (BaseTraceService) SharedTr.getDelegate();
        handler = new SharedLogHandler();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
        System.clearProperty("com.ibm.ws.logging.trace.file.name");
    }

    @Before
    public void setUp() {
        Logger logger = tc.getLogger();
        if (logger != null) {
            logger.setLevel(Level.ALL);
            handler.addHandlerToLogger(logger);
        }

        System.out.println("logger=" + logger);
        System.out.println("in test code, tc is: " + tc);
        System.out.println("in test code, debug enabled is: " + tc.isDebugEnabled());
    }

    @After
    public void tearDown() {
        // Leave trace off for this tc
        LoggingTestUtils.setTraceSpec("*=info=enabled");
        outputMgr.resetStreams();
        handler.traceRecords.clear();
    }

    @Test
    public void testUseLogger() {
        final String m = "testUseLogger";

        try {
            assertNull("Using logger should clear the WsHandler singleton", LoggerHandlerManager.getSingleton());

            Field f = TraceComponent.class.getDeclaredField("logger");
            f.setAccessible(true);

            TraceComponent mine = new SharedTraceComponent(test.NoComClass.class);
            assertNull("testUseLogger: TraceComponent should not have a logger set by default", f.get(mine));

            // registering TraceComponent should add a logger
            trService.register(mine);
            assertNotNull("testUseLogger: Registration should have created a Logger", mine.getLogger());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testDebug() {
        final String m = "testDebug";

        try {
            trService.debug(tc, m);

            assertFalse("no console message for debug", outputMgr.checkForStandardErr(m));
            assertEquals("One record created by logger", 1, handler.traceRecords.size());

            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains(m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testDump() {
        final String m = "testDump";

        try {
            trService.dump(tc, m);

            assertFalse("no console message for debug", outputMgr.checkForStandardErr(m));
            assertEquals("One record created by logger", 1, handler.traceRecords.size());

            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains(m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testEntryExit() {
        final String m = "testEntryExit";

        try {
            trService.entry(tc, m);
            trService.exit(tc, m);

            assertFalse("no console message for debug", outputMgr.checkForStandardErr(m));
            assertEquals("Two log records should have been created", 2, handler.traceRecords.size());

            assertTrue("entry message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("method=testEntryExit Entry"));
            assertTrue("exit message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(1).getMessage().contains("method=testEntryExit Exit"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testEvent() {
        final String m = "testEvent";

        try {
            trService.event(tc, m);

            assertFalse("no console message for debug", outputMgr.checkForStandardErr(m));
            assertEquals("One record created by logger", 1, handler.traceRecords.size());

            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains(m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testAudit() {
        final String m = "testAudit";

        try {
            trService.audit(tc, m);

            assertTrue("console to standard err via tr", outputMgr.checkForStandardErr("key=" + m));

            assertEquals("One record created by logger", 1, handler.traceRecords.size());
            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("key=" + m));
        } catch (Throwable t) {
            LoggingTestUtils.dumpLogger(tc.getLogger());
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testError() {
        final String m = "testError";

        try {
            trService.error(tc, m);

            assertTrue("console to standard err via tr", outputMgr.checkForStandardErr("key=" + m));

            assertEquals("One record created by logger", 1, handler.traceRecords.size());
            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("key=" + m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFatal() {
        final String m = "testFatal";

        try {
            trService.fatal(tc, m);

            assertTrue("console to standard err via tr", outputMgr.checkForStandardErr("key=" + m));

            assertEquals("One record created by logger", 1, handler.traceRecords.size());
            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("key=" + m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testInfo() {
        final String m = "testInfo";

        try {
            trService.info(tc, m);

            assertTrue("console to standard err via tr", outputMgr.checkForStandardErr("key=" + m));

            assertEquals("One record created by logger", 1, handler.traceRecords.size());
            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("key=" + m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testWarning() {
        final String m = "testWarning";

        try {
            trService.warning(tc, m);

            assertTrue("console to standard err via tr", outputMgr.checkForStandardErr("key=" + m));

            assertEquals("One record created by logger", 1, handler.traceRecords.size());
            assertTrue(m + " message to logger: " + handler.traceRecords.get(0).getMessage(),
                       handler.traceRecords.get(0).getMessage().contains("key=" + m));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
