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
package com.ibm.websphere.ras;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import junit.framework.Assert;

public class TrFormatMessageTest {
    private static final String TEST_NLS = "test.resources.Messages";

    /**
     * Creates an Enumeration which wraps the List's iterator.
     *
     * @return an Enumeration which wraps the List's iterator.
     */
    private Enumeration<Locale> getEnumeration(List<Locale> locales) {
        final Iterator<Locale> itr = locales.iterator();
        return new Enumeration<Locale>() {

            @Override
            public boolean hasMoreElements() {
                return itr.hasNext();
            }

            @Override
            public Locale nextElement() {
                return itr.next();
            }

        };
    }

    @Test
    public void testFormatMessage() {
        List<Locale> locales = Arrays.asList(new Locale("zz"));

        TraceComponent tc = Tr.register(TrFormatMessageTest.class, null, TEST_NLS);
        Assert.assertEquals("missingKey", Tr.formatMessage(tc, "missingKey"));
        Assert.assertEquals("", Tr.formatMessage(tc, "emptyKey"));
        Assert.assertEquals("message a", Tr.formatMessage(tc, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, new Locale("zz"), "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, locales, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, getEnumeration(locales), "keyExists", "a"));
    }

    /**
     * Checks that when given a list of Locales which are unknown, the default
     * Locale is used.
     */
    @Test
    public void testFormatMessage_resolvesToDefaultLocale() {
        List<Locale> locales = Arrays.asList(new Locale("aa"));

        TraceComponent tc = Tr.register(TrFormatMessageTest.class, null, TEST_NLS);
        Assert.assertEquals("message a", Tr.formatMessage(tc, new Locale("aa"), "keyExists", "a"));
        Assert.assertEquals("message a", Tr.formatMessage(tc, locales, "keyExists", "a"));
        Assert.assertEquals("message a", Tr.formatMessage(tc, getEnumeration(locales), "keyExists", "a"));
    }

    /**
     * Checks that when given a list of Locales which does not start with a
     * recognized Locale, subsequent known Locales will be used.
     */
    @Test
    public void testFormatMessage_resolvesToBestMatch_java17() {
        Locale aa = new Locale("aa");
        Locale xx = new Locale("xx");
        Locale zz = new Locale("zz");
        List<Locale> aazz = Arrays.asList(aa, zz);
        List<Locale> zzaa = Arrays.asList(zz, aa);
        List<Locale> aaxxzz = Arrays.asList(aa, xx, zz);
        List<Locale> xxzzaa = Arrays.asList(xx, zz, aa);

        TraceComponent tc = Tr.register(TrFormatMessageTest.class, null, TEST_NLS);
        Assert.assertEquals("message a", Tr.formatMessage(tc, aa, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, aazz, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, getEnumeration(aazz), "keyExists", "a"));

        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, zz, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, zzaa, "keyExists", "a"));
        Assert.assertEquals("zzmessage a", Tr.formatMessage(tc, getEnumeration(zzaa), "keyExists", "a"));

        Assert.assertEquals("xxmessage a", Tr.formatMessage(tc, xx, "keyExists", "a"));
        Assert.assertEquals("xxmessage a", Tr.formatMessage(tc, aaxxzz, "keyExists", "a"));
        Assert.assertEquals("xxmessage a", Tr.formatMessage(tc, getEnumeration(aaxxzz), "keyExists", "a"));

        Assert.assertEquals("xxmessage a", Tr.formatMessage(tc, xxzzaa, "keyExists", "a"));
        Assert.assertEquals("xxmessage a", Tr.formatMessage(tc, getEnumeration(xxzzaa), "keyExists", "a"));
    }
}
