/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.websphere.ras;

import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

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

    /**
     * An exception that, like org.omg.CORBA.portable.UnknownException, embeds
     * cause information inside its own message rather than relying solely on the
     * standard JVM cause chain. Used to verify that throwableToString handles
     * such exceptions without depending on any third-party library's formatting.
     */
    private static class WrapperException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        WrapperException(Throwable cause) {
            super(cause == null ? null : "wrappedEx: " + cause, cause);
        }
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

        Assert.assertThat(DataFormatHelper.throwableToString(new WrapperException(null)),
                          new RegexMatcher(Pattern.quote(WrapperException.class.getName()) + "\\r?\\n" +
                                           AT_LINES));
        Assert.assertThat(DataFormatHelper.throwableToString(new WrapperException(new Throwable("cause message"))),
                          new RegexMatcher(Pattern.quote(WrapperException.class.getName()) + ": wrappedEx: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES +
                                           "Caused by: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES));
        Assert.assertThat(DataFormatHelper.throwableToString(new WrapperException(new WrapperException(new Throwable("cause message")))),
                          new RegexMatcher(Pattern.quote(WrapperException.class.getName()) + ": wrappedEx: " + Pattern.quote(WrapperException.class.getName()) + ": wrappedEx: java\\.lang\\.Throwable: cause message\\r?\\n" +
                                           AT_LINES +
                                           "(?:Caused by: [^\\n]*\\n" +
                                           AT_LINES + ")+"));
    }
}
