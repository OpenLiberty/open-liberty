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
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.ws.logging.internal.SharedTraceNLSResolver;
import com.ibm.ws.logging.internal.TraceNLSResolverTest;

/**
 * Test instance methods of TraceNLS.
 * 
 * For static wrappers (which defer to the TraceNLSResolver), see
 * TraceNLSResolverTest
 * 
 * @see TraceNLSResolverTest
 */
public class TraceNLSTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static final Class<?> myClass = TraceNLSTest.class;

    static final String TEST_NLS = "test.resources.Messages";
    static final Object[] objs = new Object[] { "p1", "p2", "p3", "p4" };

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
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @Before
    public void setUp() throws Exception {
        TraceNLS.finder = null;
        TraceNLS.resolver = null;
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /**
     * A) Create NLS object with a class argument and a valid resource bundle
     */
    @Test
    public void testGetTraceNLS() {
        final String m = "testGetTraceNLS";
        try {
            TraceNLS nls;
            String msg;

            // A-1) Use _this_ class
            nls = TraceNLS.getTraceNLS(myClass, TEST_NLS);
            assertEquals("A-1: caller should match passed in class", nls.caller, myClass);
            assertEquals("A-1: bundle should match passed in vlaue", nls.ivBundleName, TEST_NLS);

            // A-2) Use some other class
            nls = TraceNLS.getTraceNLS(test.NoComClass.class, TEST_NLS);
            assertEquals("A-2: caller should match passed in class", nls.caller, test.NoComClass.class);
            assertEquals("A-2: bundle should match passed in vlaue", nls.ivBundleName, TEST_NLS);

            // A-3) getString
            msg = nls.getString("BUILDLEVELS_NOT_SAME_CWSJE0002E");
            assertTrue("A-3-1: Result of getString() should start with CWSJE0002E, but was: " + msg, msg.startsWith("CWSJE0002E"));

            // A-3-2) getString: missing key
            msg = nls.getString("nonExistKey");
            assertEquals("A-3-2: Result of getString() should be key", msg, "nonExistKey");

            // A-3-3) getString: missing key, default string
            msg = nls.getString("nonExistKey", "default");
            assertEquals("A-3-2: Result of getString() should be default string", msg, "default");

            // A-4) getFormattedMessage with object arguments
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, "");
            assertTrue("A-4: Result of getFormattedMessage() should start with CWSJE0001E, but was: " + msg, msg.startsWith("CWSJE0001E"));
            assertTrue("A-4: Result of getFormattedMessage() should contain object p1, but was: " + msg, msg.contains("p1"));
            assertTrue("A-4: Result of getFormattedMessage() should contain object p2, but was: " + msg, msg.contains("p2"));
            assertTrue("A-4: Result of getFormattedMessage() should contain object p3, but was: " + msg, msg.contains("p3"));
            assertTrue("A-4: Result of getFormattedMessage() contain object p4, but was: " + msg, msg.contains("p4"));

            // A-5) getFormattedMessage with missing object arguments
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", null, "");
            assertTrue("A-5: Result of getFormattedMessage() should start with CWSJE0001E, but was: " + msg, msg.startsWith("CWSJE0001E"));
            assertTrue("A-5: Result of getFormattedMessage() should contain object p1, but was: " + msg, msg.contains("{0}"));
            assertTrue("A-5: Result of getFormattedMessage() should contain object p2, but was: " + msg, msg.contains("{1}"));
            assertTrue("A-5: Result of getFormattedMessage() should contain object p3, but was: " + msg, msg.contains("{2}"));
            assertTrue("A-5: Result of getFormattedMessage() contain object p4, but was: " + msg, msg.contains("{3}"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * B) Create NLS object with a class argument and an invalid resource bundle
     */
    @Test
    public void testGetTraceNLSInvalidBundle() {
        final String m = "";
        try {
            TraceNLS nls;
            String msg;

            // B-1) Create NLS object with an invalid resource bundle
            nls = TraceNLS.getTraceNLS(myClass, "dummy");
            assertEquals("B-1: caller should match passed in class", nls.caller, myClass);
            assertEquals("B-1: bundle should match passed in value", nls.ivBundleName, "dummy");

            // B-2) getString: with invalid resource bundle, message == key
            msg = nls.getString("BUILDLEVELS_NOT_SAME_CWSJE0002E");
            assertEquals("B-2: Result of getString() should be the key... ", msg, "BUILDLEVELS_NOT_SAME_CWSJE0002E");

            // B-3) getFormattedMessage: with invalid resource bundle and no
            // default string, message == key
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, null);
            assertEquals("B-3: Result of getFormattedMessage() should be the key... ", msg, "BUILDLEVELS_NOT_SAME_CWSJE0001E");

            // B-4) getFormattedMessage: with invalid resource bundle and a
            // default string, message == default string
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, "");
            assertEquals("B-4: Result of getFormattedMessage() should match default string", msg, "");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * C) Deprecated constructor: Create NLS object with a valid resource bundle
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedGetTraceNLS() {
        final String m = "";
        try {
            TraceNLS nls;
            String msg;
            String msg2;

            // C-1) create with good bundle
            nls = TraceNLS.getTraceNLS(TEST_NLS);
            assertEquals("C-1: caller should match passed in class", nls.caller, myClass);
            assertEquals("C-1: bundle should match passed in value", nls.ivBundleName, TEST_NLS);

            // C-2) getString
            msg = nls.getString("BUILDLEVELS_NOT_SAME_CWSJE0002E");
            assertTrue("C-2: Result of getString() should start with CWSJE0002E, but was: " + msg, msg.startsWith("CWSJE0002E"));

            // C-3-1) getFormattedMessage with object arguments
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, "");
            assertTrue("C-3-1: Result of getFormattedMessage() should start with CWSJE0001E, but was: " + msg, msg.startsWith("CWSJE0001E"));
            assertTrue("C-3-1: Result of getFormattedMessage() should contain object p1, but was: " + msg, msg.contains("p1"));
            assertTrue("C-3-1: Result of getFormattedMessage() should contain object p2, but was: " + msg, msg.contains("p2"));
            assertTrue("C-3-1: Result of getFormattedMessage() should contain object p3, but was: " + msg, msg.contains("p3"));
            assertTrue("C-3-1: Result of getFormattedMessage() contain object p4, but was: " + msg, msg.contains("p4"));

            // C-3-2) same method call with quiet = true
            msg2 = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, "", true);
            assertEquals("C-3-2: msg2 and msg should match", msg2, msg);

            // C-4-1) getFormattedMessage with missing object arguments
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", null, "");
            assertTrue("C-4-1: Result of getFormattedMessage() should start with CWSJE0001E, but was: " + msg, msg.startsWith("CWSJE0001E"));
            assertTrue("C-4-1: Result of getFormattedMessage() should contain object p1, but was: " + msg, msg.contains("{0}"));
            assertTrue("C-4-1: Result of getFormattedMessage() should contain object p2, but was: " + msg, msg.contains("{1}"));
            assertTrue("C-4-1: Result of getFormattedMessage() should contain object p3, but was: " + msg, msg.contains("{2}"));
            assertTrue("C-4-1: Result of getFormattedMessage() contain object p4, but was: " + msg, msg.contains("{3}"));

            // C-4-2) same method call with quiet = true
            msg2 = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", null, "", true);
            assertEquals("C-4-2: msg2 and msg should match", msg2, msg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * D) Deprecated constructor: Create NLS object with an invalid resource
     * bundle
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedGetTraceNLSInvalidBundle() {
        final String m = "";
        try {
            TraceNLS nls;
            String msg;

            // D-1) Create NLS object with an invalid resource bundle
            nls = TraceNLS.getTraceNLS("dummy");
            assertEquals("D-1: caller should match passed in class", nls.caller, myClass);
            assertEquals("D-1: bundle should match passed in value", nls.ivBundleName, "dummy");

            // D-2) getString: with invalid resource bundle, message == key
            msg = nls.getString("BUILDLEVELS_NOT_SAME_CWSJE0002E");
            assertEquals("D-2: Result of getString() should be the key... ", msg, "BUILDLEVELS_NOT_SAME_CWSJE0002E");

            // D-3) getFormattedMessage: with invalid resource bundle and no
            // default string, message == key
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, null);
            assertEquals("D-3: Result of getFormattedMessage() should be the key... ", msg, "BUILDLEVELS_NOT_SAME_CWSJE0001E");

            // D-4) getFormattedMessage: with invalid resource bundle and a
            // default string, message == default string
            msg = nls.getFormattedMessage("BUILDLEVELS_NOT_SAME_CWSJE0001E", objs, "");
            assertEquals("D-4: Result of getFormattedMessage() should match default string", msg, "");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * TraceNLS.getFormattedMessage: Static wrapper around resolver.getMessage.
     * 
     * All possible variants of TraceNLSResolver.getMessage parameters are
     * tested in TraceNLSResolverTest. Don't bother repeating here, just make
     * sure that the methods from TraceNLS map to TraceNLSResolver with the
     * right parameters filled in.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testStaticGetFormattedMessage() {
        final String m = "";
        try {
            SharedTraceNLSResolver.setInstance(true);

            Class<?> caller = myClass;
            Object[] args = new Object[] {};
            Locale locale = Locale.getDefault();
            String bundleName = "bundleName", key = "key", defaultString = "string";
            boolean quiet = true;

            String msg;
            // E-1) TraceNLS.getFormattedMessage(bundleName, key, args,
            // defaultString);
            msg = TraceNLS.getFormattedMessage(bundleName, key, args, defaultString);
            assertEquals("E-1", msg, "null, null, bundleName, key, args, defaultString, format, null, false");

            // E-2) TraceNLS.getFormattedMessage(bundleName, key, args,
            // defaultString, quiet);
            msg = TraceNLS.getFormattedMessage(bundleName, key, args, defaultString, quiet);
            assertEquals("E-2", msg, "null, null, bundleName, key, args, defaultString, format, null, quiet");

            // E-3) TraceNLS.getFormattedMessage(bundleName, key, locale, args,
            // defaultString);
            msg = TraceNLS.getFormattedMessage(bundleName, key, locale, args, defaultString);
            assertEquals("E-3", msg, "null, null, bundleName, key, args, defaultString, format, locale, false");

            // E-4) TraceNLS.getFormattedMessage(bundleName, key, locale, args,
            // defaultString, quiet);
            msg = TraceNLS.getFormattedMessage(bundleName, key, locale, args, defaultString, quiet);
            assertEquals("E-4", msg, "null, null, bundleName, key, args, defaultString, format, locale, quiet");

            // E-5) TraceNLS.getFormattedMessage(caller, bundleName, key, args,
            // defaultString);
            msg = TraceNLS.getFormattedMessage(caller, bundleName, key, args, defaultString);
            assertEquals("E-5", msg, "class, null, bundleName, key, args, defaultString, format, null, false");

            // E-6) TraceNLS.getFormattedMessage(caller, bundleName, key, args,
            // defaultString, quiet);
            msg = TraceNLS.getFormattedMessage(caller, bundleName, key, args, defaultString, quiet);
            assertEquals("E-6", msg, "class, null, bundleName, key, args, defaultString, format, null, quiet");

            // E-7) TraceNLS.getFormattedMessage(caller, bundleName, key,
            // locale, args, defaultString);
            msg = TraceNLS.getFormattedMessage(caller, bundleName, key, locale, args, defaultString);
            assertEquals("E-7", msg, "class, null, bundleName, key, args, defaultString, format, locale, false");

            // E-8) TraceNLS.getFormattedMessage(caller, bundleName, key,
            // locale, args, defaultString, quiet);
            msg = TraceNLS.getFormattedMessage(caller, bundleName, key, locale, args, defaultString, quiet);
            assertEquals("E-8", msg, "class, null, bundleName, key, args, defaultString, format, locale, quiet");

            SharedTraceNLSResolver.setInstance(false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * TraceNLS.getStringFromBundle: Static wrapper around resolver.getMessage.
     * 
     * All possible variants of TraceNLSResolver.getMessage parameters are
     * tested in TraceNLSResolverTest. Don't bother repeating here, just make
     * sure that the methods from TraceNLS map to TraceNLSResolver with the
     * right parameters filled in.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testStaticGetStringFromBundle() {
        final String m = "";
        try {
            SharedTraceNLSResolver.setInstance(true);

            Class<?> caller = myClass;
            ResourceBundle bundle = ResourceBundle.getBundle(TEST_NLS);
            Locale locale = Locale.getDefault();
            String bundleName = "bundleName", key = "key", defaultString = "string";

            String msg;

            // F-1) TraceNLS.getStringFromBundle(bundleName, key);
            msg = TraceNLS.getStringFromBundle(bundleName, key);
            assertEquals("F-1", msg, "null, null, bundleName, key, null, null, false, null, false");

            // F-2) TraceNLS.getStringFromBundle(bundleName, key,
            // defaultString);
            msg = TraceNLS.getStringFromBundle(bundleName, key, defaultString);
            assertEquals("F-2", msg, "null, null, bundleName, key, null, defaultString, false, null, false");

            // F-3) TraceNLS.getStringFromBundle(bundleName, key, locale);
            msg = TraceNLS.getStringFromBundle(bundleName, key, locale);
            assertEquals("F-3", msg, "null, null, bundleName, key, null, null, false, locale, false");

            // F-4) TraceNLS.getStringFromBundle(bundleName, key, locale,
            // defaultString);
            msg = TraceNLS.getStringFromBundle(bundleName, key, locale, defaultString);
            assertEquals("F-4", msg, "null, null, bundleName, key, null, defaultString, false, locale, false");

            // F-5) TraceNLS.getStringFromBundle(bundle, bundleName, key,
            // locale);
            msg = TraceNLS.getStringFromBundle(bundle, bundleName, key, locale);
            assertEquals("F-5", msg, "null, bundle, bundleName, key, null, null, false, locale, false");

            // F-6) TraceNLS.getStringFromBundle(bundle, bundleName, key,
            // locale, defaultString);
            msg = TraceNLS.getStringFromBundle(bundle, bundleName, key, locale, defaultString);
            assertEquals("F-6", msg, "null, bundle, bundleName, key, null, defaultString, false, locale, false");

            // F-7) TraceNLS.getStringFromBundle(caller, bundleName, key)
            msg = TraceNLS.getStringFromBundle(caller, bundleName, key);
            assertEquals("F-7", msg, "class, null, bundleName, key, null, null, false, null, false");

            // F-8) TraceNLS.getStringFromBundle(caller, bundleName, key,
            // defaultString);
            msg = TraceNLS.getStringFromBundle(caller, bundleName, key, defaultString);
            assertEquals("F-8", msg, "class, null, bundleName, key, null, defaultString, false, null, false");

            // F-9) TraceNLS.getStringFromBundle(caller, bundleName, key,
            // locale);
            msg = TraceNLS.getStringFromBundle(caller, bundleName, key, locale);
            assertEquals("F-9", msg, "class, null, bundleName, key, null, null, false, locale, false");

            // F-10) TraceNLS.getStringFromBundle(caller, bundleName, key,
            // locale, defaultString);
            msg = TraceNLS.getStringFromBundle(caller, bundleName, key, locale, defaultString);
            assertEquals("F-10", msg, "class, null, bundleName, key, null, defaultString, false, locale, false");

            // F-11) TraceNLS.getStringFromBundle(caller, bundle, bundleName,
            // key, locale);
            msg = TraceNLS.getStringFromBundle(caller, bundle, bundleName, key, locale);
            assertEquals("F-11", msg, "class, bundle, bundleName, key, null, null, false, locale, false");

            // F-12) TraceNLS.getStringFromBundle(caller, bundle, bundleName,
            // key, locale, defaultString);
            msg = TraceNLS.getStringFromBundle(caller, bundle, bundleName, key, locale, defaultString);
            assertEquals("F-12", msg, "class, bundle, bundleName, key, null, defaultString, false, locale, false");

            SharedTraceNLSResolver.setInstance(false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Variations of TraceNLSResolver.getResourceBundle are tested ad nauseum in
     * TraceNLSResolverTest, so don't bother here, just make sure the methods
     * get to the resolver instance ok.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testStaticGetResourceBundle() {
        final String m = "";
        try {
            Class<?> caller = myClass;
            ResourceBundle bundle;
            ResourceBundle compare = ResourceBundle.getBundle(TEST_NLS);
            Locale locale = Locale.getDefault();
            String bundleName = TEST_NLS;

            // G-1) TraceNLS.getResourceBundle(bundleName, locale)
            bundle = TraceNLS.getResourceBundle(bundleName, locale);
            assertEquals("G-1", bundle, compare);

            // G-2) TraceNLS.getResourceBundle(caller, bundleName, locale)
            bundle = TraceNLS.getResourceBundle(caller, bundleName, locale);
            assertEquals("G-2", bundle, compare);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * TraceNLS.getFormattedMessageFromLocalizedMessage: Static wrapper around
     * resolver.getFormattedMessage.
     * 
     * All possible variants of TraceNLSResolver.getFormattedMessage parameters
     * are tested in TraceNLSResolverTest. Don't bother repeating here, just
     * make sure that the methods from TraceNLS map to TraceNLSResolver with the
     * right parameters filled in.
     */
    @Test
    public void getFormattedMessageFromLocalizedMessage() {
        final String m = "";
        try {
            SharedTraceNLSResolver.setInstance(true);

            Object[] args = new Object[] {};

            @SuppressWarnings("unused")
            Locale locale = Locale.getDefault();

            String message = "messsage";
            String msg;

            // F-1) TraceNLS.getStringFromBundle(bundleName, key);
            msg = TraceNLS.getFormattedMessageFromLocalizedMessage(message, args, true);
            assertEquals("H-1", msg, "message, args");

            SharedTraceNLSResolver.setInstance(false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

}
