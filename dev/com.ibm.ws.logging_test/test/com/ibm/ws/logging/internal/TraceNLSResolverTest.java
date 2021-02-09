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
package com.ibm.ws.logging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.websphere.ras.SharedTraceComponent;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 * Verify behavior of the TraceNLSResolver, and of static TraceNLS methods that
 * defer to it.
 */
public class TraceNLSResolverTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static final Class<?> myClass = TraceNLSResolverTest.class;
    static final String TEST_NLS = "test.resources.Messages";

    static SharedOutputManager outputMgr;
    static TraceComponent tc;

    static final Object[] emptyArray = new Object[0];

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        tc = new SharedTraceComponent(BundleManifestTest.class);
        TrConfigurator.registerTraceComponent(tc);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
        SharedTr.clearComponents();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        // Clear Tr's internal delegate
        outputMgr.resetStreams();
    }

    @Test
    public void testLogEvent() {
        final String m = "testLogEvent";
        try {
            SharedTraceNLSResolver.setInstance(true);
            TraceNLSResolver instance = TraceNLSResolver.getInstance();
            SharedTraceNLSResolver.beLoud();

            Field f = TraceNLSResolver.class.getDeclaredField("tc");
            f.setAccessible(true);
            TraceComponent tc = (TraceComponent) f.get(instance);
            TrConfigurator.registerTraceComponent(tc);

            final String s = "message {0}";
            final String sp1 = "message p1";

            LoggingTestUtils.setTraceSpec("*=all=enabled");
            TraceNLSResolver.logEvent("message {0}", null);
            TraceNLSResolver.logEvent("message {0}", new Object[] { "p1" });
            assertTrue("untranslated string should be in logged event", outputMgr.checkForTrace(s));
            assertTrue("translated string should be in logged event", outputMgr.checkForTrace(sp1));

            LoggingTestUtils.setTraceSpec("*=all=disabled");

            SharedTraceNLSResolver.beQuiet();
            SharedTraceNLSResolver.setInstance(false);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGetFormattedMessage() {
        final String m = "testGetFormattedMessage";

        try {
            TraceNLSResolver instance = TraceNLSResolver.getInstance();

            String msg;

            // A-1) null msg: return null for null message
            msg = instance.getFormattedMessage(null, null);
            assertEquals("A-1-1: msg should be null", msg, null);
            msg = instance.getFormattedMessage(null, new Object[] {});
            assertEquals("A-1-2: msg should be null", msg, null);
            msg = instance.getFormattedMessage(null, new Object[] { "p1" });
            assertEquals("A-1-3: msg should be null", msg, null);

            // A-2) null args: if args are null, just returns message
            msg = instance.getFormattedMessage("message {0}", null);
            assertEquals("A-2: msg should match original parameter", msg, "message {0}");

            // A-3) empty args: leave {0} if args is empty
            msg = instance.getFormattedMessage("message {0}", new Object[] {});
            assertEquals("A-3: msg should match original parameter", msg, "message {0}");

            // A-4) no substitutions: return message if args present w/o
            // substitution
            msg = instance.getFormattedMessage("message", new Object[] { "p1" });
            assertEquals("A-4: msg should match original parameter", msg, "message");

            // A-5) substitutions: return message with substitutions
            msg = instance.getFormattedMessage("message {0}", new Object[] { "p1" });
            assertEquals("A-5: msg should have substitutions", msg, "message p1");

            // A-6) substitutions: tolerate more substitutions than object
            // arguments
            msg = instance.getFormattedMessage("message {0} {1}", new Object[] { "p1" });
            assertEquals("A-6: msg should contain un-substituted parameter", msg, "message p1 {1}");

            // A-7) bad formatting string will just return badly formatted
            // string
            msg = instance.getFormattedMessage("message {0} {1", new Object[] { "p1" });
            assertEquals("A-7: msg should match original parameter", msg, "message {0} {1");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGetResource() {
        final String m = "testGetResource";
        try {
            TraceNLSResolver instance = TraceNLSResolver.getInstance();

            ResourceBundle rb;

            // B-1) Null bundleName
            try {
                rb = instance.getResourceBundle(null, null, (Locale) null);
                fail("B-1: Null bundleName should result in a NullPointerException");
            } catch (NullPointerException ex) {
                // Caught expected runtime exception
            }

            // B-2) Null class, invalid bundle name
            // Will look for (and find) this class & it's classLoader
            try {
                rb = instance.getResourceBundle(null, "dummy", (Locale) null);
                fail("B-2: Imaginary bundleName should result in a MissingResourceException");
            } catch (MissingResourceException ex) {
                // Caught expected runtime exception
            }

            // B-3) Supply valid class (skip call to StackFinder) and valid
            // bundle
            rb = instance.getResourceBundle(myClass, TEST_NLS, (Locale) null);
            assertNotNull("B-3: Expect existing/real bundle name to return a resource bundle", rb);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Gets a resource bundle for a given Locale.
     * Ensures that the resource bundle is resolved, but does not check the Locale
     * which it was resolved to.
     */
    @Test
    public void getResourceBundle_en() {
        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, new Locale("en"));
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
    }

    /**
     * Gets a resource bundle for a given unknown Locale.
     * Ensures that the resource bundle is resolved, and that the resulting
     * Locale for the resource bundle is not the unknown Locale.
     */
    @Test
    public void getResourceBundle_aa() {
        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        Locale aaLocale = new Locale("aa");
        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, aaLocale);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertFalse("The Locale of the loaded resource bundle should not be 'aa'",
                    rb.getLocale().equals(aaLocale));
    }

    /**
     * Gets a resource bundle for a given unknown Locale.
     * Ensures that the resource bundle is resolved, and that the resulting
     * Locale for the resource bundle is not the unknown Locale.
     */
    @Test
    public void getResourceBundle_aaList() {
        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        Locale aaLocale = new Locale("aa");
        List<Locale> locales = Arrays.asList(aaLocale);
        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, locales);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertFalse("The Locale of the loaded resource bundle should not be 'aa'",
                    rb.getLocale().equals(aaLocale));
    }

    /**
     * Gets a resource bundle for a given unknown Locale.
     * Ensures that the resource bundle is resolved, and that the resulting
     * Locale for the resource bundle is not the known, non-default Locale.
     */
    @Test
    public void getResourceBundle_zz() {
        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        Locale zzLocale = new Locale("zz");
        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, zzLocale);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'zz'",
                   rb.getLocale().equals(zzLocale));
    }

    /**
     * Gets a resource bundle for a given unknown Locale.
     * Ensures that the resource bundle is resolved, and that the resulting
     * Locale for the resource bundle is not the known, non-default Locale.
     */
    @Test
    public void getResourceBundle_zzList() {
        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        Locale zzLocale = new Locale("zz");
        List<Locale> locales = Arrays.asList(zzLocale);
        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, locales);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'zz'",
                   rb.getLocale().equals(zzLocale));
    }

    /**
     * Gets a resource bundle for a given unknown Locale.
     * Ensures that the resource bundle is resolved, and that the resulting
     * Locale for the resource bundle is not the known, non-default Locale.
     */
    @Test
    public void getResourceBundle_resolvesToBestMatch_java17() {

        TraceNLSResolver instance = TraceNLSResolver.getInstance();

        Locale aa = new Locale("aa");
        Locale xx = new Locale("zz");
        Locale zz = new Locale("zz");
        List<Locale> aazz = Arrays.asList(aa, zz);
        List<Locale> zzaa = Arrays.asList(zz, aa);
        List<Locale> aaxxzz = Arrays.asList(aa, xx, zz);
        List<Locale> xxzzaa = Arrays.asList(xx, zz, aa);

        ResourceBundle rb = instance.getResourceBundle(myClass, TEST_NLS, aazz);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'zz'",
                   rb.getLocale().equals(zz));

        rb = instance.getResourceBundle(myClass, TEST_NLS, zzaa);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'zz'",
                   rb.getLocale().equals(zz));

        rb = instance.getResourceBundle(myClass, TEST_NLS, aaxxzz);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'xx'",
                   rb.getLocale().equals(xx));

        rb = instance.getResourceBundle(myClass, TEST_NLS, xxzzaa);
        assertNotNull("Expect existing/real bundle name to return a resource bundle", rb);
        assertTrue("The Locale of the loaded resource bundle should be 'xx'",
                   rb.getLocale().equals(xx));
    }

    @Test
    public void testGetMessage() {
        final String m = "testGetMessage";

        try {
            TraceNLSResolver instance = TraceNLSResolver.getInstance();
            String msg;

            // GROUP 1: null bundleName

            // C-1-1) Class=null, ResourceBundle=null, bundleName=null,
            // key=null, args=null, defaultString=null, format=false,
            // locale=null, quiet=false
            // Null class (will call StackFinder to fill in)
            // Null bundleName & null key & null defaultString ==> custom
            // message
            msg = instance.getMessage(null, null, null, null, null, null, false, null, false);
            assertEquals("C-1-1: custom message expected", msg, "Resource Bundle name is null, key = null");

            // C-1-2) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Non-null key with null defaultString: returned message should
            // equal key
            msg = instance.getMessage(myClass, null, null, "keyNotExist", null, null, false, null, false);
            assertEquals("C-1-2: msg should equal key", msg, "keyNotExist");

            // C-1-3) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=FALSE, locale=null, quiet=false
            // non-null default string with args & format == FALSE: returned
            // message should equal default string with no substitutions
            msg = instance.getMessage(myClass, null, null, "keyNotExist", null, "default {0}", false, null, false);
            assertEquals("C-1-3: msg should equal default string w/o substitutions", msg, "default {0}");

            // C-1-4) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=TRUE, locale=null, quiet=false
            // non-null default string with null args: returned message should
            // equal default string with substitutions
            msg = instance.getMessage(myClass, null, null, "keyNotExist", null, "default {0}", true, null, false);
            assertEquals("C-1-4: msg should equal default string w/ missing substitutions", msg, "default {0}");

            // C-1-5) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=TRUE, locale=null, quiet=false
            // non-null default string with args: returned message should equal
            // default string with substitutions
            msg = instance.getMessage(myClass, null, null, "keyNotExist", new Object[] { "p1" }, "default {0}", true, null, false);
            assertEquals("C-1-5: msg should equal default string w/ substitutions", msg, "default p1");

            // GROUP 2: non-existent bundleName

            // C-2-1) Class, ResourceBundle=null, bundleName, key=null,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Invalid bundleName & null key & null defaultString ==> custom
            // message
            msg = instance.getMessage(myClass, null, "dummy", null, null, null, false, null, false);
            assertEquals("C-2-1: custom message expected", msg, "Unable to load ResourceBundle dummy");

            // C-2-2) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false)
            // Invalid bundleName & Non-null key & null defaultString: returned
            // message should equal key
            msg = instance.getMessage(myClass, null, "dummy", "keyNotExist", null, null, false, null, false);
            assertEquals("C-2-2: msg should equal key", msg, "keyNotExist");

            // C-2-3) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=false, locale=null, quiet=false
            // Invalid bundleName & non-null default string: returned message
            // should equal default string
            msg = instance.getMessage(myClass, null, "dummy", "keyNotExist", null, "default {0}", false, null, false);
            assertEquals("C-2-3: msg should equal default string", msg, "default {0}");

            // C-2-4) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=TRUE, locale=null, quiet=false
            // Invalid bundleName & non-null default string with null args:
            // returned message should equal default string with no
            // substitutions
            msg = instance.getMessage(myClass, null, "dummy", "keyNotExist", null, "default {0}", true, null, false);
            assertEquals("C-2-4: msg should equal default string w/o substitutions", msg, "default {0}");

            // C-2-5) Class, ResourceBundle=null, bundleName=null, key,
            // args=null, defaultString, format=TRUE, locale=null, quiet=false
            // Invalid bundleName & non-null default string with args: returned
            // message should equal default string with substitutions
            msg = instance.getMessage(myClass, null, "dummy", "keyNotExist", new Object[] { "p1" }, "default {0}", true, null, false);
            assertEquals("C-2-5: msg should equal default string w/ substitutions", msg, "default p1");

            // GROUP 3: valid bundleName

            // C-3-1) Class, ResourceBundle=null, bundleName, key=null,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Valid bundleName & null key & null defaultString: custom msg
            msg = instance.getMessage(myClass, null, TEST_NLS, null, null, null, false, null, false);
            assertEquals("C-3-1: custom message expected", msg, "Null key passed while using ResourceBundle test.resources.Messages");

            // C-3-2) Class, ResourceBundle=null, bundleName, key=null,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Valid bundleName & null key & defaultString: custom msg
            msg = instance.getMessage(myClass, null, TEST_NLS, null, null, "default {0}", false, null, false);
            assertEquals("C-3-2: message should default string w/o substitutions", msg, "default {0}");

            // C-3-3) Class, ResourceBundle=null, bundleName, key=null,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Valid bundleName & null key & null defaultString: custom msg
            msg = instance.getMessage(myClass, null, TEST_NLS, null, null, "default {0}", true, null, false);
            assertEquals("C-3-3: message should default string w/ missing substitutions", msg, "default {0}");

            // C-3-4) Class, ResourceBundle=null, bundleName, key=null,
            // args=null, defaultString=null, format=false, locale=null,
            // quiet=false
            // Valid bundleName & null key & null defaultString: custom msg
            msg = instance.getMessage(myClass, null, TEST_NLS, null, new Object[] { "p1" }, "default {0}", true, null, false);
            assertEquals("C-3-4: message should default string w/ substitutions", msg, "default p1");

            // C-3-5) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString=null, format=false, locale=null, quiet=false
            // Valid bundleName & key NOT in bundle & null defaultString: msg
            // should equal key
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyNotExist", null, null, false, null, false);
            assertEquals("C-3-5: message should equal key", msg, "keyNotExist");

            // C-3-6) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=false, locale=null, quiet=false
            // Valid bundleName & key NOT in bundle & defaultString & format is
            // FALSE: msg should equal default string
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyNotExist", null, "default {0}", false, null, false);
            assertEquals("C-3-6: message should default string w/o substitutions", msg, "default {0}");

            // C-3-7) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName & key NOT in bundle & defaultString & format is
            // TRUE & args = null : msg should equal default string
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyNotExist", null, "default {0}", true, null, false);
            assertEquals("C-3-7: message should equal default string w/ missing substitutions", msg, "default {0}");

            // C-3-8) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName, but key NOT in bundle, defaultString, format is
            // TRUE: msg should equal default string w/ substitutions
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyNotExist", new Object[] { "p1" }, "default {0}", true, null, false);
            assertEquals("C-3-8: message should equal default string w/ substitutions", msg, "default p1");

            // C-3-9) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=false, locale=null, quiet=false
            // Valid bundleName & key in bundle & format is FALSE: msg should
            // equal message string w/o substitutions
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyExists", null, "default {0}", false, null, false);
            assertEquals("C-3-9: message should default string w/o substitutions", msg, "message {0}");

            // C-3-10) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName & key in bundle & format is TRUE & args = null :
            // msg should equal default string
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyExists", null, "default {0}", true, null, false);
            assertEquals("C-3-10: message should equal default string w/ missing substitutions", msg, "message {0}");

            // C-3-11) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName & key in bundle & format is TRUE: msg should
            // equal default string w/ substitutions
            msg = instance.getMessage(myClass, null, TEST_NLS, "keyExists", new Object[] { "p1" }, "default {0}", true, null, false);
            assertEquals("C-3-11: message should equal default string w/ substitutions", msg, "message p1");

            // C-3-12) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName & key in bundle but value is empty, no
            // defaultString
            msg = instance.getMessage(myClass, null, TEST_NLS, "emptyKey", null, null, true, null, false);
            assertEquals("C-3-12: message should equal key", msg, "emptyKey");

            // C-3-13) Class, ResourceBundle=null, bundleName, key, args=null,
            // defaultString, format=TRUE, locale=null, quiet=false
            // Valid bundleName & key in bundle but value is empty
            msg = instance.getMessage(myClass, null, TEST_NLS, "emptyKey", null, "default {0}", true, null, false);
            assertEquals("C-3-13: message should equal default string", msg, "default {0}");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    static class BlankStackFinder extends StackFinder {
        private static Field instanceField = null;

        protected static Field getInstanceField() throws Exception {
            if (instanceField == null) {
                Class<?> inner[] = StackFinder.class.getDeclaredClasses();
                for (Class<?> c : inner) {
                    if (c.getSimpleName().equals("StackFinderSingleton")) {
                        instanceField = c.getDeclaredField("instance");
                        instanceField.setAccessible(true);
                        break;
                    }
                }

                if (instanceField == null)
                    throw new IllegalStateException("Could not find StackFinder.StackFinderSingleton.instance field");
            }

            return instanceField;
        }

        protected static StackFinder originalFinder;

        // Replace finder with this one
        public synchronized static void init() throws Exception {
            Field f = getInstanceField();
            if (originalFinder == null) {
                originalFinder = (StackFinder) f.get(null);
            }

            f.set(null, new BlankStackFinder());
        }

        public synchronized static void clear() throws Exception {
            Field f = getInstanceField();
            if (originalFinder != null) {
                f.set(null, originalFinder);
                originalFinder = null;
            }
        }

        @Override
        public Class<Object> getCaller() {
            return null;
        }
    }
}
