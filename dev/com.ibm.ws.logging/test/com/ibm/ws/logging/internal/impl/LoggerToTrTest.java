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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.logging.LoggerHandlerManager;
import com.ibm.ws.logging.LoggerHelper;
import com.ibm.ws.logging.internal.WsLogger;

/**
 *
 */
public class LoggerToTrTest extends java.util.ListResourceBundle {
    static final Class<?> myClass = LoggerToTrTest.class;
    static final String myName = LoggerToTrTest.class.getName();

    static SharedOutputManager outputMgr;
    static Logger logger;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        logger = Logger.getLogger(myName, myName);
        assertTrue("logger is not an instance of WsLogger", logger instanceof WsLogger);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        //outputMgr.dumpStreams();

        // Always reset to the default
        LoggingTestUtils.setTraceSpec("*=info=enabled");

        outputMgr.resetStreams();
    }

    @Override
    public Object[][] getContents() {
        return resources;
    }

    private final static Object[][] resources = {
                                                 { "testLoggerInfoToTr", "testLoggerInfoToTr" },
                                                 { "testLoggerConfigToTr", "testLoggerConfigToTr" },
                                                 { "testLoggerConfigToTr-1", "testLoggerConfigToTr-1" },
                                                 { "testLoggerSevereToTr", "testLoggerSevereToTr" },
                                                 { "testLoggerSevereToTr-1", "testLoggerSevereToTr-1" },
                                                 { "testLoggerInsertSevereToTr", "testLoggerInsertSevereToTr param1 {0} param2 {1}" },
                                                 { "testWarning", "testWarning" },
                                                 { "testWarning-1", "testWarning-1" },
    };

    @Test
    public void testWSHandlerRegister() {
        final String m = "testWSHandlerRegister";

        try {
            // WSHandler is registered/added to logger during constructor
            // of the BaseTraceService. This testcase just makes sure that
            // the handler is there..
            boolean foundWsHandler = false;

            LogManager lm = LogManager.getLogManager();
            Enumeration<String> list = lm.getLoggerNames();
            Handler singleton = LoggerHandlerManager.getSingleton();

            System.out.println("singleton=" + singleton);
            System.out.println("list=" + list);
            System.out.println("logger=" + logger);

            while (list.hasMoreElements() && !foundWsHandler) {
                Logger log = lm.getLogger(list.nextElement());
                System.out.println("log=" + log);

                if (log != null) {
                    Handler handlers[] = log.getHandlers();
                    for (Handler h : handlers) {
                        System.out.println("\t" + h);
                        if (h == singleton) {
                            foundWsHandler = true;
                            break;
                        }
                    }
                }
            }

            if (!foundWsHandler)
                throw new Exception("BaseTraceServiceWSHandler not registered with Logger");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
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
    public void testLoggerEnteringToTr() {
        final String m = "testLoggerEnteringToTr";

        try {
            logger.entering(myName, m, "abcdef");
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=all=enabled");

            String m1 = m + "-1";
            logger.entering(myName, m1, "abcdef");
            assertMessages(m1, false, false, false, true);
            assertMessages("ENTRY", false, false, false, true);
            assertMessages("abcdef", false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerExitingToTr() {
        final String m = "testLoggerExitingToTr";

        try {
            logger.exiting(myName, m, "out");
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=all=enabled");

            String m1 = m + "-1";
            logger.exiting(myName, m1, "out");
            assertMessages(m1, false, false, false, true);
            assertMessages("RETURN", false, false, false, true);
            assertMessages("out", false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerInfoToTr() {
        final String m = "testLoggerInfoToTr";

        try {
            logger.info(m);
            assertMessages(m, false, false, true, false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerConfigToTr() {
        final String m = "testLoggerConfigToTr";

        try {
            logger.config(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=config=enabled");

            String m1 = m + "-1";
            logger.config(m1);
            assertMessages(m1, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerFineToTr() {
        final String m = "testLoggerFineToTr";

        try {
            logger.fine(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=fine=enabled");

            String m1 = m + "-1";
            logger.fine(m1);
            assertMessages(m1, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerFinerToTr() {
        final String m = "testLoggerFinerToTr";

        try {
            logger.finer(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=finer=enabled");

            String m1 = m + "-1";
            logger.finer(m1);
            assertMessages(m1, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerFinestToTr() {
        final String m = "testLoggerFinestToTr";

        try {
            logger.finest(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=finest=enabled");

            String m1 = m + "-1";
            logger.finest(m1);
            assertMessages(m1, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /*
     * Test the Logger.log function
     * Include test for message parameter substitution
     */
    @Test
    public void testLoggerLogToTr() {
        final String m = "testLoggerLogpToTr";
        final String mp1 = "{0}";
        final String testParam = "parameter0";

        try {
            // Level.FINEST should not be written as logging is disabled
            logger.finest(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=finest=enabled");

            // Level.FINEST enabled, message should only be in trace log
            String m1 = m + "-1";
            logger.log(Level.FINEST, m1);
            assertMessages(m1, false, false, false, true);

            // Test message parameter substitution
            String m2 = m + "-2 ";
            String m2txt = m2 + mp1;
            String m2Result = m2 + testParam;
            logger.log(Level.FINEST, m2txt, testParam);
            assertMessages(m2Result, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerSevereToTr() {
        final String m = "testLoggerSevereToTr";

        try {
            logger.severe(m);
            assertMessages(m, false, true, true, false);
            assertMessages(BaseTraceFormatter.levelToString(Level.SEVERE), false, true, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=finer=enabled");

            String m1 = m + "-1";
            logger.severe(m1);
            assertMessages(m1, false, true, true, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerLinebreakFineToTr() {
        final String m = "testLoggerLinebreakFineToTr";
        final String suffix = "\nafter the break";

        try {
            logger.logp(Level.FINE, "CLASS_NAME", "METHOD", m + suffix);
            assertMessages(m + suffix, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=fine=enabled");

            String m1 = m + "-1";
            logger.logp(Level.FINE, "CLASS_NAME", "METHOD", m1 + suffix);
            assertMessages(m1 + suffix, false, false, false, true);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerInsertSevereToTr() {
        final String m = "testLoggerInsertSevereToTr";
        final Object[] params = new Object[] { "first thing", "second thing" };
        final String compare = "testLoggerInsertSevereToTr param1 first thing param2 second thing";

        try {
            logger.logp(Level.SEVERE, "class.name", "methodName", m, params);
            assertMessages(compare, false, true, true, false);
            assertMessages(BaseTraceFormatter.levelToString(Level.SEVERE), false, true, false, false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerWarningToTr() {
        final String m = "testLoggerWarningToTr";

        try {
            logger.warning(m);
            assertMessages(m, true, false, true, false);
            assertMessages(BaseTraceFormatter.levelToString(Level.WARNING), true, false, false, false);

            logger.logp(Level.WARNING, "CLASS_NAME", "METHOD", "ugly message", new Throwable());
            assertMessages(m, true, false, true, false);
            assertMessages("ugly message", true, false, true, false);
            assertMessages("Throwable", true, false, true, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=fine=enabled");

            String m1 = m + "-1";
            logger.warning(m1);
            assertMessages(m1, true, false, true, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerThrowingToTr() {
        final String m = "testLoggerThrowingToTr";

        try {
            // Throwing is finer level trace
            Exception ex = new Exception("bang");
            logger.throwing(myName, m, ex);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("*=finer=enabled");

            String m1 = m + "-1";
            logger.throwing(myName, m1, ex);
            assertMessages(m1, false, false, false, true);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerHelperAddGroup() {
        final String m = "testLoggerHelperAddGroup";

        try {
            logger.fine(m);
            assertMessages(m, false, false, false, false);

            outputMgr.resetStreams();
            LoggingTestUtils.setTraceSpec("LoggerHelperGroup=fine=enabled");

            LoggerHelper.addLoggerToGroup(logger, "LoggerHelperGroup");

            logger.fine(m);
            assertMessages(m, false, false, false, true);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
