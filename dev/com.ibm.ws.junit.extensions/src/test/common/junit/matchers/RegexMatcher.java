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
package test.common.junit.matchers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * <b>How to use this class</b>
 * <p>
 * Create a factory method like this:
 * <p>
 * <blockquote><pre> {@literal @Factory} public static Matcher&lt;String&gt; matching(String regex) {
 * return new RegexMatcher(regex);
 * }
 * </pre></blockquote>
 * <p>
 * Use this static factory method in your mock:
 * <p>
 * <pre> {@code  one(stderr).println(with(matching(".*9443.*"))); } </pre>
 */
public class RegexMatcher extends TypeSafeMatcher<String> {
    private final String regex;
    private final Pattern pattern;

    public RegexMatcher(String regex) {
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    public static boolean match(String input, String regex) {
        RegexMatcher matcher = new RegexMatcher(regex);
        return matcher.matchesSafely(input);
    }

    /** {@inheritDoc} */
    @Override
    public boolean matchesSafely(String toMatch) {
        if (toMatch != null) {
            Matcher m = pattern.matcher(toMatch);
            if (m.find())
                return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("A string which contains ").appendValue(regex);
    }
}
