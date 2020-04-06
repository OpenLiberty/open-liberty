/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.error.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.web.TraceConstants;
import com.ibm.ws.security.test.common.CommonTestClass;

public class BrowserAndServerLogMessageTest extends CommonTestClass {

    private static TraceComponent tc = Tr.register(BrowserAndServerLogMessageTest.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String MSG_KEY_NO_INSERTS = "OAUTH_ROLE_CONFIG_PROCESSED";
    private static final String MSG_KEY_WITH_INSERTS = "OAUTH_INVALID_CLIENT";
    private static final String MSG_REGEX_NO_INSERTS_EN = "CWWKS1404I.*successfully processed";
    private static final String MSG_REGEX_NO_INSERTS_FR = "CWWKS1404I.*a abouti";
    private static final String MSG_REGEX_NO_INSERTS_ZH = "CWWKS1404I.*\u914d\u7f6e";
    private static final String MSG_REGEX_WITH_INSERTS_EN = "CWWKS1406E.*" + "%s" + ".*invalid client credential.*" + "%s";
    private static final String MSG_REGEX_WITH_INSERTS_FR = "CWWKS1406E.*" + "%s" + ".*identification utilisateur non valides.*" + "%s";

    @Before
    public void beforeTest() {
        System.out.println("Entering test " + testName.getMethodName());
    }

    @After
    public void afterTest() {
        System.out.println("Exiting test " + testName.getMethodName());
    }

    @Test
    public void testUnknownMessageBundle() {
        TraceComponent tcUnknownBundle = Tr.register(BrowserAndServerLogMessageTest.class, "SOMEGROUP", "com.ibm.ws.unknown.bundle");
        Enumeration<Locale> requestLocales = getLocales("en");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tcUnknownBundle, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        assertEquals("Server error message should have just been the message key since the registered message bundle is unknown.", MSG_KEY_NO_INSERTS, msg.getServerErrorMessage());
        assertEquals("Browser error message should have just been the message key since the registered message bundle is unknown.", MSG_KEY_NO_INSERTS, msg.getBrowserErrorMessage());
    }

    @Test
    public void testNullLocales() {
        Enumeration<Locale> requestLocales = null;
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Browser error message did not match expected regex.");
    }

    @Test
    public void testUnknownLocales_expectEnglishMessages() {
        Enumeration<Locale> requestLocales = getLocales("xyz", "123", "does not exist");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Browser error message did not match expected regex.");
    }

    @Test
    public void testAllEnglishMessagesExpected_noInserts() {
        Enumeration<Locale> requestLocales = getLocales("en");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Browser error message did not match expected regex.");
    }

    @Test
    public void testFrenchBrowserMessagesExpected_noInserts() {
        Enumeration<Locale> requestLocales = getLocales("fr");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_FR, "Browser error message did not match expected regex.");
    }

    @Test
    public void testFrenchExtendedBrowserMessagesExpected_noInserts() {
        Enumeration<Locale> requestLocales = getLocales("fr", "fr-FR", "fr-ca", "fr-CA", "en");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_FR, "Browser error message did not match expected regex.");
    }

    @Test
    public void testChineseBrowserMessagesExpected_noInserts() {
        Enumeration<Locale> requestLocales = getLocales("zh", "fr", "en");
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_NO_INSERTS);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), MSG_REGEX_NO_INSERTS_EN, "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), MSG_REGEX_NO_INSERTS_ZH, "Browser error message did not match expected regex.");
    }

    @Test
    public void testAllEnglishMessagesExpected_withInserts() {
        Enumeration<Locale> requestLocales = getLocales("en");
        String insert0 = "insert0";
        String insert1 = "insert1";
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_WITH_INSERTS, insert0, insert1);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), String.format(MSG_REGEX_WITH_INSERTS_EN, insert0, insert1), "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), String.format(MSG_REGEX_WITH_INSERTS_EN, insert0, insert1), "Browser error message did not match expected regex.");
    }

    @Test
    public void testFrenchBrowserMessagesExpected_withInserts() {
        Enumeration<Locale> requestLocales = getLocales("fr");
        String insert0 = "insert0";
        String insert1 = "insert1";
        BrowserAndServerLogMessage msg = new BrowserAndServerLogMessage(tc, MSG_KEY_WITH_INSERTS, insert0, insert1);
        msg.setLocales(requestLocales);
        verifyPattern(msg.getServerErrorMessage(), String.format(MSG_REGEX_WITH_INSERTS_EN, insert0, insert1), "Server error message did not match expected regex.");
        verifyPattern(msg.getBrowserErrorMessage(), String.format(MSG_REGEX_WITH_INSERTS_FR, insert0, insert1), "Browser error message did not match expected regex.");
    }

    private Enumeration<Locale> getLocales(String... locales) {
        List<Locale> localeList = new ArrayList<Locale>();
        if (locales != null) {
            for (String locale : locales) {
                localeList.add(new Locale(locale));
            }
        }
        return Collections.enumeration(localeList);
    }

}
