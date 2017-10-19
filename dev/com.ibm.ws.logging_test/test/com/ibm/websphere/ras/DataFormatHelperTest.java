/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.portable.UnknownException;

import test.common.junit.matchers.RegexMatcher;

public class DataFormatHelperTest {
    private static Matcher<String> identityToStringMatcher(Class<?> c) {
        return new RegexMatcher(Pattern.quote(c.getName()) + "@[a-f0-9]+");
    }

    @Test
    public void testIdentityToString() {
        Assert.assertNull(DataFormatHelper.identityToString(null));
        Assert.assertThat(DataFormatHelper.identityToString(new Object()), identityToStringMatcher(Object.class));
        Assert.assertThat(DataFormatHelper.identityToString("abcd"), identityToStringMatcher(String.class));
        Assert.assertThat(DataFormatHelper.identityToString(new TestToString()), identityToStringMatcher(TestToString.class));
    }

    private static Matcher<String> sensitiveToStringMatcher(Class<?> c) {
        return new RegexMatcher("<sensitive " + Pattern.quote(c.getName()) + "@[a-f0-9]+>");
    }

    @Test
    public void testSensitiveToString() {
        Assert.assertNull(DataFormatHelper.sensitiveToString(null));
        Assert.assertThat(DataFormatHelper.sensitiveToString(new Object()), sensitiveToStringMatcher(Object.class));
        Assert.assertThat(DataFormatHelper.sensitiveToString("abcd"), sensitiveToStringMatcher(String.class));
        Assert.assertThat(DataFormatHelper.sensitiveToString(new TestToString()), sensitiveToStringMatcher(TestToString.class));
    }

    private static class TestToString {
        @Override
        public String toString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testThrowableToString() {
        final String AT_LINES = "(\\tat [^\\n]*\\n)*";

        Assert.assertThat(DataFormatHelper.throwableToString(new Throwable("test message")),
                          new RegexMatcher("java\\.lang\\.Throwable: test message\\r?\\n" +
                                           AT_LINES));
        Assert.assertThat(DataFormatHelper.throwableToString(new Throwable("test message", new Throwable("cause message"))),
                          new RegexMatcher("java\\.lang\\.Throwable: test message\\r?\\n" +
                                           AT_LINES +
                                           "Caused by: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES));

        Assert.assertThat(DataFormatHelper.throwableToString(new UnknownException(null)),
                          new RegexMatcher("org\\.omg\\.CORBA\\.portable\\.UnknownException: [^\\n]*\\n" +
                                           AT_LINES));
        Assert.assertThat(DataFormatHelper.throwableToString(new UnknownException(new Throwable("cause message"))),
                          new RegexMatcher("org\\.omg\\.CORBA\\.portable\\.UnknownException: [^\\n]*\\n" +
                                           AT_LINES +
                                           "originalEx: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES));
        Assert.assertThat(DataFormatHelper.throwableToString(new UnknownException(new UnknownException(new Throwable("cause message")))),
                          new RegexMatcher("org\\.omg\\.CORBA\\.portable\\.UnknownException: [^\\n]*\\n" +
                                           AT_LINES +
                                           "originalEx: org\\.omg\\.CORBA\\.portable\\.UnknownException: [^\\n]*\\n" +
                                           AT_LINES +
                                           "originalEx: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES));
    }
}
