/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.wsspi.rest.handler.RESTRequest;

import test.common.SharedOutputManager;

/**
 *
 */
public class RequestNLSTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final RESTRequest request = mock.mock(RESTRequest.class);

    @Before
    public void setUp() {
        RequestNLS.clearThreadLocal();
    }

    @After
    public void tearDown() {
        RequestNLS.clearThreadLocal();
    }

    @Test
    public void getSetAndClear() {
        RESTRequest stored = RequestNLS.getRESTRequest();
        assertNull("FAIL: The initial value of the ThreadLocal should be null",
                   stored);

        RequestNLS.setRESTRequest(request);
        stored = RequestNLS.getRESTRequest();
        assertSame("FAIL: The stored RESTRequest was not the expected value",
                   request, stored);

        RequestNLS.clearThreadLocal();
        stored = RequestNLS.getRESTRequest();
        assertNull("FAIL: The ThreadLocal should be null after a clear",
                   stored);
    }

    @Test
    public void getLocale_en() {
        final Locale en = new Locale("en");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(en);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertSame("FAIL: When the requested locale is english, that locale should be returned",
                   en, bestMatch);
    }

    @Test
    public void getLocale_enUS() {
        final Locale enUS = new Locale("en", "us");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(enUS);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertSame("FAIL: When the requested locale is english, that locale should be returned",
                   enUS, bestMatch);
    }

    @Test
    public void getLocale_fr() {
        final Locale fr = new Locale("fr");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(fr);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: When the requested locale is french, that locale should be returned",
                     fr, bestMatch);
    }

    @Test
    public void getLocale_frCA() {
        final Locale frCA = new Locale("fr", "ca");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(frCA);

        final Locale fr = new Locale("fr");
        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: When the requested locale a dialect of French, French should be returned",
                     fr, bestMatch);
    }

    @Test
    public void getLocale_pt() {
        final Locale pt = new Locale("pt");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(pt);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: Portugese is not supported. The JVM default should be returned",
                     Locale.getDefault(), bestMatch);
    }

    @Test
    public void getLocale_ptBR() {
        final Locale ptBR = new Locale("pt", "br");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(ptBR);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: When the requested locale is Portugese Brazillian, that locale should be returned",
                     ptBR, bestMatch);
    }

    @Test
    public void getLocale_az() {
        final Locale az = new Locale("az");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(az);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: Azerbaijanii is not supported. The JVM default should be returned",
                     Locale.getDefault(), bestMatch);
    }

    @Test
    public void getLocale_az_enUS() {
        final Locale az = new Locale("az");
        final Locale enUS = new Locale("en", "us");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(az);
        locales.add(enUS);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: AZ is not supported, but en_us is. en_us should be returned.",
                     enUS, bestMatch);
    }

    @Test
    public void getLocale_az_pt_ru() {
        final Locale az = new Locale("az");
        final Locale pt = new Locale("pt");
        final Locale ru = new Locale("ru");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(az);
        locales.add(pt);
        locales.add(ru);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: Only a requested locale should be returned",
                     ru, bestMatch);
    }

    @Test
    public void getLocale_az_pt() {
        final Locale az = new Locale("az");
        final Locale pt = new Locale("pt");
        final List<Locale> locales = new ArrayList<Locale>();
        locales.add(az);
        locales.add(pt);

        Locale bestMatch = RequestNLS.getLocale(Collections.enumeration(locales));
        assertEquals("FAIL: When no locales are supported, return JVM default",
                     Locale.getDefault(), bestMatch);
    }

}
