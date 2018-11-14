/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import test.common.junit.matchers.RegexMatcher;

public class FileLogHeaderTest {
    private String printFileLogHeader(boolean trace, boolean javaLangInstrument) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos, true, "UTF-8");

            final String header = "header" + LoggingConstants.nl;
            FileLogHeader flh = new FileLogHeader(header, trace, javaLangInstrument, false);
            flh.print(out);
            byte[] bytes = baos.toByteArray();

            String s = new String(bytes, "UTF-8");
            Assert.assertThat(s, Matchers.containsString("header" + LoggingConstants.nl));
            Assert.assertThat(s, new RegexMatcher("\n$"));

            return s;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testPrintTrace() {
        Assert.assertThat(printFileLogHeader(false, false), Matchers.not(Matchers.containsString("trace.specification")));
        Assert.assertThat(printFileLogHeader(true, false), Matchers.containsString("trace.specification"));
    }

    @Test
    public void testPrintJavaLangInstrument() {
        Assert.assertThat(printFileLogHeader(false, false), Matchers.not(Matchers.containsString("java.lang.instrument")));
        Assert.assertThat(printFileLogHeader(true, false), Matchers.containsString("java.lang.instrument = false"));
        Assert.assertThat(printFileLogHeader(false, true), Matchers.not(Matchers.containsString("java.lang.instrument")));
        Assert.assertThat(printFileLogHeader(true, true), Matchers.not(Matchers.containsString("java.lang.instrument")));
    }
}
