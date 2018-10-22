/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import org.junit.Test;
import static com.ibm.ws.kernel.instrument.serialfilter.config.SpecifierFormat.*;

import static org.junit.Assert.*;

public class SpecifierFormatTest {

    @Test public void testEmptyString() {assertFormat(UNKNOWN, "");}
    @Test public void testSimpleString() {assertFormat(CLASS, "a1bc");}
    @Test public void testBadStartChar() {assertFormat(UNKNOWN, "1abc");}
    @Test public void testBadMiddleChar() {assertFormat(UNKNOWN, "a+bc");}

    @Test public void testEmptyPrefix() {assertFormat(PREFIX, "*");}
    @Test public void testSimplePrefix() {assertFormat(PREFIX, "a1bc*");}
    @Test public void testClassPrefix() {assertFormat(PREFIX, "a.b.c.D*");}
    @Test public void testPackagePrefix() {assertFormat(PREFIX, "a.b.c.*");}

    @Test public void testEmptyPrefixWithTrailingJunk() {assertFormat(UNKNOWN, "*a");}
    @Test public void testSimplePrefixWithTrailingJunk() {assertFormat(UNKNOWN, "a1bc**");}
    @Test public void testClassPrefixWithTrailingJunk() {assertFormat(UNKNOWN, "a.b.c.D*:");}
    @Test public void testPackagePrefixWithTrailingJunk() {assertFormat(UNKNOWN, "a.b.c.*#");}

    @Test public void testClassInPackage() {assertFormat(CLASS, "a.b.c.D");}
    @Test public void testClassInBadPackage() {assertFormat(UNKNOWN, "a.b.1.c.D");}
    @Test public void testClassInMalformedPackage() {assertFormat(UNKNOWN, "a.b..c.D");}
    @Test public void testBadClassInPackage() {assertFormat(UNKNOWN, "a.b.c.1D");}
    @Test public void testEmptyClassInPackage() {assertFormat(UNKNOWN, "a.b.c.");}

    @Test public void testClassWithDigest() {assertFormat(DIGEST, "A:123456789_123456789_123456789_123456789_1234");}
    @Test public void testClassWithEmptyDigest() {assertFormat(UNKNOWN, "A:");}
    @Test public void testClassWithBadDigest() {assertFormat(UNKNOWN, "A:123456789_123456789_123456789_123456789_123]");}
    @Test public void testClassWithShortDigest() {assertFormat(UNKNOWN, "A:123456789_123456789_123456789_123456789_123");}
    @Test public void testClassWithLongDigest() {assertFormat(UNKNOWN, "A:123456789_123456789_123456789_123456789_12345");}

    @Test public void testClassWithMethod() {assertFormat(METHOD, "a.b.c#d");}
    @Test public void testClassWithEmptyMethod() {assertFormat(UNKNOWN, "a.b.c#");}
    @Test public void testEmptyClassWithMethod() {assertFormat(UNKNOWN, "a.b.c.#d");}
    @Test public void testClassWithBadMethodStartChar() {assertFormat(UNKNOWN, "a.b.c#1");}
    @Test public void testClassWithBadMethodChar() {assertFormat(UNKNOWN, "a.b.c#a+");}

    @Test public void testClassWithMethodPrefix() {assertFormat(METHOD_PREFIX, "a.b.c#d*");}
    @Test public void testClassWithEmptyMethodPrefix() {assertFormat(METHOD_PREFIX, "a.b.c#*");}

    @Test public void testClassWithMethodPrefixWithTrailingJunk() {assertFormat(UNKNOWN, "a.b.c#d**");}
    @Test public void testClassWithEmptyMethodPrefixWithTrailingJunk() {assertFormat(UNKNOWN, "a.b.c#*:");}

    @Test public void testInternalFormForPrefix()             {assertInternalForm("a.b.", "a.b.*");}
    @Test public void testInternalFormForEmptyPrefix()        {assertInternalForm("", "*");}
    @Test public void testInternalFormForClass()              {assertInternalForm("a.b.c!", "a.b.c");}
    @Test public void testInternalFormForMethod()             {assertInternalForm("a.b.c!#d!", "a.b.c#d");}
    @Test public void testInternalFormForMethodPrefix()       {assertInternalForm("a.b.c!#d", "a.b.c#d*");}
    @Test public void testInternalFormForEmptyMethodPrefix()  {assertInternalForm("a.b.c!#", "a.b.c#*");}
    @Test public void testInternalFormForDigest()             {assertInternalForm("a.b.c!:000", "a.b.c:000");}

    private void assertInternalForm(String internal, String external) {
        assertEquals(internal, SpecifierFormat.internalize(external));
        assertEquals(external, SpecifierFormat.externalize(internal));
    }

    private void assertFormat(SpecifierFormat format, String s) {
        assertEquals(format, SpecifierFormat.fromString(s));
    }
}
