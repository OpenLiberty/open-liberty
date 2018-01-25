/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.common.utils.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

import com.ibm.ws.repository.common.utils.internal.RepositoryCommonUtils;

/**
 * Tests for {@link RepositoryCommonUtils}
 */
public class RepositoryUtilsTest {

    /**
     * Test of {@link RepositoryCommonUtils#localeForString(String)} just supplying a language.
     */
    @Test
    public void testLocaleForString_languageOnly() {
        Locale locale = RepositoryCommonUtils.localeForString("en");
        assertEquals("The locale should have been parsed correctly", Locale.ENGLISH, locale);
    }

    /**
     * Test of {@link RepositoryCommonUtils#localeForString(String)} supplying a language and country.
     */
    @Test
    public void testLocaleForString_languageAndCountry() {
        Locale locale = RepositoryCommonUtils.localeForString("en_GB");
        assertEquals("The locale should have been parsed correctly", Locale.UK, locale);
    }

    /**
     * Test of {@link RepositoryCommonUtils#localeForString(String)} supplying a language, country and varient.
     */
    @Test
    public void testLocaleForString_allParts() {
        Locale locale = RepositoryCommonUtils.localeForString("en_GB_welsh");
        assertEquals("The locale should have been parsed language", "en", locale.getLanguage());
        assertEquals("The locale should have been parsed country", "GB", locale.getCountry());
        assertEquals("The locale should have been parsed varient", "welsh", locale.getVariant());
    }

    /**
     * Test of {@link RepositoryCommonUtils#localeForString(String)} supplying four parts, the last should be added to the vairent as well.
     */
    @Test
    public void testLocaleForString_fourParts() {
        Locale locale = RepositoryCommonUtils.localeForString("en_GB_welsh_valleys");
        assertEquals("The locale should have been parsed language", "en", locale.getLanguage());
        assertEquals("The locale should have been parsed country", "GB", locale.getCountry());
        assertEquals("The locale should have been parsed varient", "welsh_valleys", locale.getVariant());
    }

    /**
     * Test of {@link RepositoryCommonUtils#localeForString(String)} supplying <code>null</code>.
     */
    @Test
    public void testLocaleForString_null() {
        assertNull("A null entry should make a null locale", RepositoryCommonUtils.localeForString(null));
        assertNull("An empty string should make a null locale", RepositoryCommonUtils.localeForString(""));
    }

}
