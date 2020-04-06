/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.ListResourceBundle;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ejs.ras.Untraceable;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Traceable;
import com.ibm.websphere.ras.TruncatableThrowable;
import com.ibm.ws.logging.internal.impl.LoggingConstants.TraceFormat;

import test.TestConstants;
import test.common.SharedOutputManager;

/**
 *
 */
public class BaseTraceFormatterTest {
    static SharedOutputManager outputMgr;
    static final Object idObj = new Object() {
    };
    static final Object idHash = DataFormatHelper.padHexString(System.identityHashCode(idObj), 8);

    static final String id = " id=        ";
    static final String source = "source";
    static final String class_ = "class";
    static final String method = "method";
    static final String message = "ta-da!";
    static final String longName = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static final String threadId = DataFormatHelper.getThreadId();
    static final String processedId = DataFormatHelper.padHexString(System.identityHashCode(id), 8);

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
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    static final BaseTraceFormatter advancedFormatter = new BaseTraceFormatter(TraceFormat.ADVANCED);
    static final BaseTraceFormatter basicFormatter = new BaseTraceFormatter(TraceFormat.BASIC);
    static final BaseTraceFormatter enhancedFormatter = new BaseTraceFormatter(TraceFormat.ENHANCED);

    enum StringResults {
        testNullAll("] " + threadId
                    + "               2   null", "] " + threadId
                                                 + " id=00000000                                                              2 null", "] " + threadId
                                                                                                                                       + "  2 UOW= source= thread=["
                                                                                                                                       + Thread.currentThread().getName() + "]"
                                                                                                                                       + LoggingConstants.nl
                                                                                                                                       + "          null"),

        testSource("] " + threadId
                   + " source        2   null", "] " + threadId
                                                + " id=00000000 source                                                       2 null", "] " + threadId
                                                                                                                                      + "  2 UOW= source=source thread=["
                                                                                                                                      + Thread.currentThread().getName() + "]"
                                                                                                                                      + LoggingConstants.nl
                                                                                                                                      + "          null"),

        testClass("] " + threadId
                  + " class         2 class  null", "] " + threadId
                                                    + " id=00000000 class                                                        2 null", "] " + threadId
                                                                                                                                          + "  2 UOW= source= class=class thread=["
                                                                                                                                          + Thread.currentThread().getName() + "]"
                                                                                                                                          + LoggingConstants.nl
                                                                                                                                          + "          null"),

        testSourceAndClass("] " + threadId
                           + " source        2 class  null", "] " + threadId
                                                             + " id=00000000 class                                                        2 null", "] "
                                                                                                                                                   + threadId
                                                                                                                                                   + "  2 UOW= source=source class=class thread=["
                                                                                                                                                   + Thread.currentThread().getName()
                                                                                                                                                   + "]"
                                                                                                                                                   + LoggingConstants.nl
                                                                                                                                                   + "          null"),

        testMethod("] " + threadId
                   + " source        2  method null", "] " + threadId
                                                      + " id=00000000 source                                                       2 method null", "] "
                                                                                                                                                   + threadId
                                                                                                                                                   + "  2 UOW= source=source method=method thread=["
                                                                                                                                                   + Thread.currentThread().getName()
                                                                                                                                                   + "]"
                                                                                                                                                   + LoggingConstants.nl
                                                                                                                                                   + "          null"),
        testMessage("] " + threadId
                    + " source        2  method ta-da!", "] " + threadId
                                                         + " id=00000000 source                                                       2 method ta-da!", "] "
                                                                                                                                                        + threadId
                                                                                                                                                        + "  2 UOW= source=source method=method thread=["
                                                                                                                                                        + Thread.currentThread().getName()
                                                                                                                                                        + "]"
                                                                                                                                                        + LoggingConstants.nl
                                                                                                                                                        + "          ta-da!"),
        testLongName("] " + threadId
                     + " abcdefghijklm 2  method ta-da!", "] " + threadId
                                                          + " id=00000000 cdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 2 method ta-da!", "] "
                                                                                                                                                         + threadId
                                                                                                                                                         + "  2 UOW= source=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 method=method thread=["
                                                                                                                                                         + Thread.currentThread().getName()
                                                                                                                                                         + "]"
                                                                                                                                                         + LoggingConstants.nl
                                                                                                                                                         + "          ta-da!"),
        testId("] " + threadId + " abcdefghijklm 2  method ta-da!", "] " + threadId + " id=" + idHash
                                                                    + " cdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 2 method ta-da!", "] "
                                                                                                                                                       + threadId
                                                                                                                                                       + "  2 UOW= source=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 method=method id="
                                                                                                                                                       + idHash
                                                                                                                                                       + " thread=["
                                                                                                                                                       + Thread.currentThread().getName()
                                                                                                                                                       + "]"
                                                                                                                                                       + LoggingConstants.nl
                                                                                                                                                       + "          ta-da!"),
        testMessageLevel("] " + threadId + " abcdefghijklm I  method ta-da!", "] " + threadId + " id=" + idHash
                                                                              + " cdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 I method ta-da!", "] "
                                                                                                                                                                 + threadId
                                                                                                                                                                 + "  I UOW= source=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 method=method id="
                                                                                                                                                                 + idHash
                                                                                                                                                                 + " thread=["
                                                                                                                                                                 + Thread.currentThread().getName()
                                                                                                                                                                 + "]"
                                                                                                                                                                 + LoggingConstants.nl
                                                                                                                                                                 + "          ta-da!"),
        testTrEntry("] " + threadId + " source        >  method "
                    + BaseTraceFormatter.ENTRY, "] " + threadId + " id=00000000 source                                                       > method "
                                                + BaseTraceFormatter.ENTRY, "] " + threadId + "  > UOW= source=source method=method thread=["
                                                                            + Thread.currentThread().getName() + "]"
                                                                            + LoggingConstants.nl + "          " + BaseTraceFormatter.ENTRY),
        testTrEntryWithExitParam("] " + threadId + " source        >  method " + BaseTraceFormatter.ENTRY + " " +
                                 BaseTraceFormatter.nlBasicPadding
                                 + "ExitCode", "] " + threadId + " id=00000000 source                                                       > method "
                                               + BaseTraceFormatter.ENTRY + " " +
                                               BaseTraceFormatter.nlEnhancedPadding
                                               + "ExitCode", "] " + threadId + "  > UOW= source=source method=method thread=["
                                                             + Thread.currentThread().getName() + "]"
                                                             + LoggingConstants.nl + "          " + BaseTraceFormatter.ENTRY + " "
                                                             + BaseTraceFormatter.nlAdvancedPadding + "ExitCode"),
        testLoggerEntering("] " + threadId
                           + " source        >  method ENTRY", "] " + threadId
                                                               + " id=00000000 source                                                       > method ENTRY", "] "
                                                                                                                                                             + threadId
                                                                                                                                                             + "  > UOW= source=source method=method thread=["
                                                                                                                                                             + Thread.currentThread().getName()
                                                                                                                                                             + "]"
                                                                                                                                                             + LoggingConstants.nl
                                                                                                                                                             + "          ENTRY"),
        testLoggerEnteringParam("] " + threadId
                                + " source        >  method ENTRY aaa", "] " + threadId
                                                                        + " id=00000000 source                                                       > method ENTRY aaa", "] "
                                                                                                                                                                          + threadId
                                                                                                                                                                          + "  > UOW= source=source method=method thread=["
                                                                                                                                                                          + Thread.currentThread().getName()
                                                                                                                                                                          + "]"
                                                                                                                                                                          + LoggingConstants.nl
                                                                                                                                                                          + "          ENTRY aaa"),
        testLoggerEnteringParams("] " + threadId
                                 + " source        >  method ENTRY aaa bbb ccc", "] " + threadId
                                                                                 + " id=00000000 source                                                       > method ENTRY aaa bbb ccc", "] "
                                                                                                                                                                                           + threadId
                                                                                                                                                                                           + "  > UOW= source=source method=method thread=["
                                                                                                                                                                                           + Thread.currentThread().getName()
                                                                                                                                                                                           + "]"
                                                                                                                                                                                           + LoggingConstants.nl
                                                                                                                                                                                           + "          ENTRY aaa bbb ccc"),
        testTrExit("] " + threadId + " source        <  method "
                   + BaseTraceFormatter.EXIT, "] " + threadId + " id=00000000 source                                                       < method "
                                              + BaseTraceFormatter.EXIT, "] " + threadId + "  < UOW= source=source method=method thread=["
                                                                         + Thread.currentThread().getName() + "]"
                                                                         + LoggingConstants.nl + "          " + BaseTraceFormatter.EXIT),
        testTrExitWithEntryParam("] " + threadId + " source        <  method " + BaseTraceFormatter.EXIT + " " +
                                 BaseTraceFormatter.nlBasicPadding
                                 + "MapEntry", "] " + threadId + " id=00000000 source                                                       < method "
                                               + BaseTraceFormatter.EXIT + " " +
                                               BaseTraceFormatter.nlEnhancedPadding + "MapEntry", "] " + threadId
                                                                                                  + "  < UOW= source=source method=method thread=["
                                                                                                  + Thread.currentThread().getName() + "]"
                                                                                                  + LoggingConstants.nl + "          " + BaseTraceFormatter.EXIT + " "
                                                                                                  + BaseTraceFormatter.nlAdvancedPadding + "MapEntry"),
        testLoggerExiting("] " + threadId
                          + " source        <  method RETURN", "] " + threadId
                                                               + " id=00000000 source                                                       < method RETURN", "] "
                                                                                                                                                              + threadId
                                                                                                                                                              + "  < UOW= source=source method=method thread=["
                                                                                                                                                              + Thread.currentThread().getName()
                                                                                                                                                              + "]"
                                                                                                                                                              + LoggingConstants.nl
                                                                                                                                                              + "          RETURN"),
        testLoggerExitingParam("] " + threadId
                               + " source        <  method RETURN aaa", "] " + threadId
                                                                        + " id=00000000 source                                                       < method RETURN aaa", "] "
                                                                                                                                                                           + threadId
                                                                                                                                                                           + "  < UOW= source=source method=method thread=["
                                                                                                                                                                           + Thread.currentThread().getName()
                                                                                                                                                                           + "]"
                                                                                                                                                                           + LoggingConstants.nl
                                                                                                                                                                           + "          RETURN aaa");

        final String basic;
        final String enhanced;
        final String advanced;

        StringResults(String b, String e, String a) {
            basic = b;
            enhanced = e;
            advanced = a;
        }

        public void test(String b, String e, String a) {
            int pos = b.indexOf("]");
            System.out.println("expect:" + basic);
            System.out.println("actual:" + b.substring(pos));
            assertEquals(this.name() + " BASIC", basic, b.substring(pos));

            pos = e.indexOf("]");
            System.out.println("expect:" + enhanced);
            System.out.println("actual:" + e.substring(pos));
            assertEquals(this.name() + " ENHANCED", enhanced, e.substring(pos));

            pos = a.indexOf("]");
            System.out.println("expect:" + advanced);
            System.out.println("actual:" + a.substring(pos));
            assertEquals(this.name() + " ADVANCED", advanced, a.substring(pos));
        }
    }

    @Test
    public void testNullAll() {
        final String m = "testNullAll";

        try {
            LogRecord rec = new LogRecord(Level.FINER, null);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testNullAll.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testSource() {
        final String m = "testSource";

        try {
            LogRecord rec = new LogRecord(Level.FINER, null);
            rec.setLoggerName(source);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testSource.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testClass() {
        final String m = "testClass";

        try {
            LogRecord rec = new LogRecord(Level.FINER, null);
            rec.setSourceClassName(class_);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testClass.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testSourceAndClass() {
        final String m = "testSourceAndClass";

        try {
            LogRecord rec = new LogRecord(Level.FINER, null);
            rec.setLoggerName(source);
            rec.setSourceClassName(class_);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testSourceAndClass.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMethod() {
        final String m = "testMethod";

        try {
            LogRecord rec = new LogRecord(Level.FINER, null);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testMethod.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMessage() {
        final String m = "testMessage";

        try {
            LogRecord rec = new LogRecord(Level.FINER, message);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testMessage.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLongName() {
        final String m = "testLongName";

        try {
            LogRecord rec = new LogRecord(Level.FINER, message);
            rec.setLoggerName(longName);
            rec.setSourceMethodName(method);

            String b = basicFormatter.format(rec);
            String e = enhancedFormatter.format(rec);
            String a = advancedFormatter.format(rec);

            StringResults.testLongName.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMessageLevel() {
        final String m = "testMessageLevel";

        try {
            LogRecord rec = new LogRecord(Level.INFO, message);
            rec.setLoggerName(longName);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, idObj, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, idObj, null, null);
            String a = advancedFormatter.traceLogFormat(rec, idObj, null, null);

            StringResults.testMessageLevel.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrEntry() {
        final String m = "testTrEntry";

        try {
            LogRecord rec = new LogRecord(Level.FINER, BaseTraceFormatter.ENTRY);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testTrEntry.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrEntryWithExitParam() {
        final String m = "testTrEntryWithExitParam";

        try {
            LogRecord rec = new LogRecord(Level.FINER, BaseTraceFormatter.ENTRY);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);
            rec.setParameters(new Object[] { "ExitCode" });

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testTrEntryWithExitParam.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerEntering() {
        final String m = "testLoggerEntering";

        try {
            LogRecord rec = new LogRecord(Level.FINER, "ENTRY");
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testLoggerEntering.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerEnteringParam() {
        final String m = "testLoggerEnteringParam";

        try {
            LogRecord rec = new LogRecord(Level.FINER, "ENTRY {0}");
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);
            rec.setParameters(new Object[] { "aaa" });

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testLoggerEnteringParam.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerEnteringParams() {
        final String m = "testLoggerEnteringParams";

        try {
            LogRecord rec = new LogRecord(Level.FINER, "ENTRY {0} {1} {2}");
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);
            rec.setParameters(new Object[] { "aaa", "bbb", "ccc" });

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testLoggerEnteringParams.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrExit() {
        final String m = "testTrExit";

        try {
            LogRecord rec = new LogRecord(Level.FINER, BaseTraceFormatter.EXIT);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testTrExit.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTrExitWithEntryParam() {
        final String m = "testTrExitWithEntryParam";

        try {
            LogRecord rec = new LogRecord(Level.FINER, BaseTraceFormatter.EXIT);
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);
            rec.setParameters(new Object[] { "MapEntry" });

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testTrExitWithEntryParam.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerExiting() {
        final String m = "testLoggerExiting";

        try {
            LogRecord rec = new LogRecord(Level.FINER, "RETURN");
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testLoggerExiting.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoggerExitingParam() {
        final String m = "testLoggerExitingParam";

        try {
            LogRecord rec = new LogRecord(Level.FINER, "RETURN {0}");
            rec.setLoggerName(source);
            rec.setSourceMethodName(method);
            rec.setParameters(new Object[] { "aaa" });

            String b = basicFormatter.traceLogFormat(rec, null, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, null, null, null);
            String a = advancedFormatter.traceLogFormat(rec, null, null, null);

            StringResults.testLoggerExitingParam.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testId() {
        final String m = "testId";

        try {
            LogRecord rec = new LogRecord(Level.FINER, message);
            rec.setLoggerName(longName);
            rec.setSourceMethodName(method);

            String b = basicFormatter.traceLogFormat(rec, idObj, null, null);
            String e = enhancedFormatter.traceLogFormat(rec, idObj, null, null);
            String a = advancedFormatter.traceLogFormat(rec, idObj, null, null);

            StringResults.testId.test(b, e, a);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFormatThrowable() {
        final String m = "testFormatThrowable";

        try {
            String s = DataFormatHelper.throwableToString(null);
            assertTrue(m + "-1: null throwable should return none w/ line end", s.equals("none" + LoggingConstants.nl));

            Throwable t = new Throwable();
            s = DataFormatHelper.throwableToString(t);
            assertThat(m + "-2: real throwable should return the stack trace", s, containsString(BaseTraceFormatterTest.class.getName() + ".testFormatThrowable"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFormatObject() {
        final String m = "testFormatObject";

        try {
            String s;
            StringBuffer sb = new StringBuffer();

            BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.ENHANCED);

            s = formatter.formatObj(new TraceableClass());
            assertThat(m + "-1: traceable object w/o exception", s, containsString("LookAtMeIMTraceable"));
            sb.append(s);

            s = formatter.formatObj(new TraceableClass(true));
            assertThat(m + "-2: traceable object w/ exception should contain exception", s, containsString("caught while calling toTraceString() on object"));
            sb.append(s);

            s = formatter.formatObj(new UntraceableClass());
            assertThat(m + "-3: untraceable should just use classname", s, containsString(UntraceableClass.class.getName()));
            sb.append(s);

            s = formatter.formatObj(new BadToString());
            assertThat(m + "-4: bad toString should be caught", s, containsString("caught while calling toString() on object"));
            sb.append(s);

            // Format byte array
            Random r = new Random();
            byte ba[] = new byte[100];
            r.nextBytes(ba);

            s = formatter.formatObj(ba);
            s = s.trim();
            assertTrue(m + "-5: String starts with ", s.startsWith(ba.toString() + ",len=" + ba.length));

            // Format byte array that is "too long"
            ba = new byte[1024 * 16 + 2];
            r.nextBytes(ba);

            s = formatter.formatObj(ba);
            s = s.trim();
            assertTrue(m + "-6: String starts with ", s.startsWith(ba.toString() + ",len=" + ba.length));
            assertTrue(m + "-6a: String should end with ...", s.endsWith("..."));

            // Make a REALLY LONG string
            s = formatter.formatObj(new Object[] { "dummy string", ba, sb });
            s = s.trim();
            assertTrue(m + "-7: String should end with ...", s.endsWith("..."));

            // All exception should get trimmed stack traces in the non-verbose case ...
            s = formatter.formatObj(new Throwable());
            assertTrue(m + "-8: String should contain \"\\tat \": " + s, s.contains("\tat "));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFormatThrown() {
        final String m = "testFormatThrown";

        try {
            BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.ENHANCED);

            LogRecord logRecord = new LogRecord(Level.INFO, "msg");
            logRecord.setThrown(new Throwable());

            String msg = formatter.consoleLogFormat(logRecord, "msg");
            assertTrue("consoleLogFormat result should contain java.lang.Throwable: " + msg, msg.contains("java.lang.Throwable"));
            assertFalse("consoleLogFormat result should not contain stack trace: " + msg, msg.contains("\tat "));

            msg = formatter.messageLogFormat(logRecord, "msg");
            assertTrue("messageLogFormat result should contain java.lang.Throwable: " + msg, msg.contains("java.lang.Throwable"));
            assertTrue("messageLogFormat result should contain stack trace: " + msg, msg.contains("\tat "));

            msg = formatter.traceLogFormat(logRecord, null, null, "msg");
            assertTrue("traceLogFormat result should contain java.lang.Throwable: " + msg, msg.contains("java.lang.Throwable"));
            assertTrue("traceLogFormat result should contain stack trace: " + msg, msg.contains("\tat "));

            msg = formatter.format(logRecord);
            assertTrue("format result should contain java.lang.Throwable: " + msg, msg.contains("java.lang.Throwable"));
            assertTrue("format result should contain stack trace: " + msg, msg.contains("\tat "));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFormatMillis() {
        final String m = "testFormatMillis";

        try {
            BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.ENHANCED);

            LogRecord logRecord = new LogRecord(Level.INFO, "msg");
            logRecord.setMillis(0);

            String msg = formatter.format(logRecord);
            // Hour/minute will vary based on timezone, but sec/ms should all be 0.
            assertThat(msg, Matchers.containsString(":00:000 "));

            msg = formatter.messageLogFormat(logRecord, formatter.formatMessage(logRecord));
            // Hour/minute will vary based on timezone, but sec/ms should all be 0.
            assertThat(msg, Matchers.containsString(":00:000 "));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFormatVerbose() {
        final String m = "testFormatVerbose";

        try {
            BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.ENHANCED);

            LogRecord logRecord = new LogRecord(Level.INFO, "msg");
            logRecord.setResourceBundle(new ListResourceBundle() {
                @Override
                protected Object[][] getContents() {
                    return new Object[][] { new Object[] { "msg", "pre {0} post" } };
                }
            });
            logRecord.setParameters(new Object[] { new Throwable() });

            // Check for the stack trace in the formatted record, using [\\s\\S] matches all characters, including newlines
            String msg = formatter.formatMessage(logRecord);
            assertTrue("The message didn't look right: " + msg, msg.matches("(?s)pre java.lang.Throwable\\s*\tat [\\s\\S]*post"));

            String vmsg = formatter.formatVerboseMessage(logRecord, null);
            assertTrue("String should contain stack trace: " + vmsg, vmsg.matches("(?s)pre java\\.lang\\.Throwable.*\tat .* post"));

            vmsg = formatter.formatVerboseMessage(logRecord, msg);
            assertTrue("String should contain stack trace: " + vmsg, vmsg.matches("(?s)pre java\\.lang\\.Throwable.*\tat .* post"));

            // If the exception was truncatable, it shouldn't be truncated in the verbose log
            logRecord.setParameters(new Object[] { new TruncatableThrowable(new Throwable()) });
            vmsg = formatter.formatVerboseMessage(logRecord, msg);
            assertTrue("String should contain stack trace: " + vmsg, vmsg.matches("(?s)pre java\\.lang\\.Throwable.*\tat .* post"));
            assertFalse("String should not have truncated stack trace: " + vmsg, vmsg.matches("internal classes"));

            // Well, no matter what the exception was, it shouldn't be truncated in the verbose log
            logRecord.setParameters(new Object[] { new Throwable() });
            vmsg = formatter.formatVerboseMessage(logRecord, msg);
            assertTrue("String should contain stack trace: " + vmsg, vmsg.matches("(?s)pre java\\.lang\\.Throwable.*\tat .* post"));
            assertFalse("String should not have truncated stack trace: " + vmsg, vmsg.matches("internal classes"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testProxyObject() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                              new Class<?>[] { MyProxyInterface.class },
                                              new ProxyHandler(123));
        String formatted = formatter.formatObj(proxy);
        assertTrue(formatted.trim().startsWith("Proxy for " + proxy.getClass().getName() + "@"));
    }

    @Test
    public void testProxyObjectThatImplementsTraceable() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                              new Class<?>[] { MyProxyInterface2.class },
                                              new ProxyHandler(456));

        String formatted = formatter.formatObj(proxy);
        assertEquals("ProxiedTraceable.toTraceString()", formatted.trim());
    }

    @Test
    public void testListIncludingProxies() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Object proxy1 = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                               new Class<?>[] { MyProxyInterface.class },
                                               new ProxyHandler(789));
        Object proxy2 = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                               new Class<?>[] { MyProxyInterface2.class },
                                               new ProxyHandler(987));
        Object nonProxy = new String("foo");
        String formatted = formatter.formatObj(Arrays.asList(proxy1, proxy2, nonProxy)).trim();
        formatted = formatted.replace('\n', ',');

        assertEquals("Unexpected output: " + formatted,
                     "[" + proxyName(proxy1) + ", ProxiedTraceable.toTraceString(), foo]",
                     formatted.trim());
    }

    private String proxyName(Object proxyObj) {
        return "Proxy for " + proxyObj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxyObj));
    }

    @Test
    public void testSetIncludingProxies() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Set<Object> set = new HashSet<>();
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                              new Class<?>[] { MyProxyInterface.class },
                                              new ProxyHandler(234));
        set.add(proxy);
        set.add(Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                       new Class<?>[] { MyProxyInterface2.class },
                                       new ProxyHandler(567)));
        set.add(new String("foo"));

        String formatted = formatter.formatObj(set).trim();
        formatted = formatted.replace('\n', ',');
        assertTrue("Unexpected output: " + formatted, formatted.startsWith("["));
        assertTrue("Unexpected output: " + formatted, formatted.endsWith("]"));
        assertTrue("Unexpected output: " + formatted, formatted.contains("Proxy for " + proxy.getClass().getName()));
        assertTrue("Unexpected output: " + formatted, formatted.contains("ProxiedTraceable.toTraceString()"));
        assertTrue("Unexpected output: " + formatted, formatted.contains("foo"));
    }

    @Test
    public void testCollectionThatThrowsException() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                              new Class<?>[] { Collection.class },
                                              new ProxyHandler(3827));
        String formatted = formatter.formatObj(proxy).trim();
        assertTrue("Unexpected output: " + formatted, formatted.startsWith("["));
        assertTrue("Unexpected output: " + formatted, formatted.endsWith("]"));
        assertTrue("Unexpected output: " + formatted, formatted.contains("UnsupportedOperationException"));
        assertTrue("Unexpected output: " + formatted, formatted.contains("Caught"));
        assertTrue("Unexpected output: " + formatted, formatted.contains("while logging collection type"));
    }

    @Test
    public void testNullObject() {
        BaseTraceFormatter formatter = new BaseTraceFormatter(TraceFormat.BASIC);
        Object o = null;
        String formatted = formatter.formatObj(o);
        assertEquals("null", formatted.trim());
    }

    // ----------------------------------------------------------------------------------
    // supporting classes
    // ----------------------------------------------------------------------------------

    public static class BadToString {
        @Override
        public String toString() {
            throw new RuntimeException("EEEEEEEEWWWWWW: this class has a bad toString!");
        }
    }

    public static class UntraceableClass implements Untraceable {
        @Override
        public String toString() {
            return "MUST NOT SEE THIS TEXT";
        }
    }

    public static class TraceableClass implements Traceable {
        private final boolean throwsException;

        public TraceableClass() {
            throwsException = false;
        }

        public TraceableClass(boolean throwException) {
            this.throwsException = throwException;
        }

        @Override
        public String toTraceString() {
            if (throwsException)
                throw new ArrayIndexOutOfBoundsException();
            else
                return "LookAtMeIMTraceable";
        }
    }

    public static interface MyProxyInterface {
        int doSomething();
    }

    public static interface MyProxyInterface2 extends Traceable {
        int doSomethingElse();
    }

    public static class ProxyHandler implements InvocationHandler {

        final int hashCode;

        ProxyHandler(int hashCode) {
            this.hashCode = hashCode;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName())) {
                return hashCode;
            }
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }
            if ("toArray".contentEquals(method.getName())) {
                throw new UnsupportedOperationException("expected");
            }
            assertFalse("toString method invoked on proxy object", method.getName().equals("toString"));
            assertTrue(String.class.equals(method.getReturnType()) && method.getName().equals("toTraceString"));
            return "ProxiedTraceable.toTraceString()";
        }

    }
}
