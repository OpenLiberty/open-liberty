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
package componenttest.matchers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Extensions and wrappers for Hamcrest {@link Matcher}s.
 */
public final class Matchers {

    /**
     * Decorates another {@link Matcher}, retaining its behaviour, but allowing tests to be slightly more expressive.
     */
    public static <T> Matcher<T> does(final Matcher<T> matcher) {
        return matcher;
    }

    public static Matcher<String> containString(final String subString) {
        return containsString(subString);
    }

    public static <T> Matcher<Iterable<? super T>> haveItem(T element) {
        return hasItem(element);
    }

    /**
     * Creates a matcher for {@code Iterable}s that matches when the given items are a <a href="http://en.wikipedia.org/wiki/Subsequence">subsequence</a> of the {@code Iterable}.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Subsequence">Subsequence</a>
     */
    public static final <T> Matcher<Iterable<T>> hasSubsequence(T... items) {
        return new DoesCollectionHaveSubsequence<T>(Arrays.asList(items));
    }

    /**
     * Convenience method for {@link #hasSubsequence(T...)} to add extra readability. E.g. {@code assertThat(list, item(a).isBefore(b))}
     */
    public static final <T> Positional<T> item(T item) {
        return new Positional<T>(item);
    }

    public static final class Positional<T> {
        private final T item1;

        public Positional(T item1) {
            this.item1 = item1;
        }

        @SuppressWarnings("unchecked")
        public Matcher<Iterable<T>> isBefore(T item2) {
            return hasSubsequence(item1, item2);
        }

        @SuppressWarnings("unchecked")
        public Matcher<Iterable<T>> isAfter(T item2) {
            return hasSubsequence(item2, item1);
        }
    }

    /**
     * Creates a matcher that matches if a string contains a substring which matches the given regex pattern
     */
    public static final Matcher<String> containsPattern(Pattern pattern) {
        return new ContainsPattern(pattern);
    }

    /**
     * Creates a matcher that matches if a string contains a substring which matches the given regex
     */
    public static final Matcher<String> containsPattern(String regex) {
        return new ContainsPattern(Pattern.compile(regex));
    }

    /**
     * A matcher for a checking whether a list of items occur in a collection in a given order. Extra items between the given items are allowed.
     * <p>
     * For example, if checking that a collection contains {@code ["A", "C"]} in order:
     * <p> {@code ["A", "C"]} will <b>pass</b>.
     * <p> {@code ["A", "B", "C"]} will <b>pass</b>.
     * <p> {@code ["C", "B", "A"]} will <b>fail</b>.
     * <p> {@code ["C", "B", "A", "B", "C"]} will <b>pass</b>.
     */
    public static final class DoesCollectionHaveSubsequence<T> extends TypeSafeMatcher<Iterable<T>> {

        private final List<T> elements;

        public DoesCollectionHaveSubsequence(final List<T> elements) {
            this.elements = elements;
        }

        private static <T> boolean findInOrder(final Iterable<T> iterToSearch, final Iterator<T> itemsToFind) {
            if (itemsToFind.hasNext()) {
                T itemToFind = itemsToFind.next();
                for (T candidate : iterToSearch) {
                    if (candidate.equals(itemToFind)) {
                        if (itemsToFind.hasNext()) {
                            itemToFind = itemsToFind.next();
                        } else {
                            return true;
                        }
                    }
                }
            } else {
                return true;
            }
            return false;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("a collection containing, in order (ignoring extra entries): " + elements.toString());
        }

        @Override
        public boolean matchesSafely(final Iterable<T> item) {
            return findInOrder(item, elements.iterator());
        }
    }

    /**
     * Checks whether the a given regex pattern can be found anywhere in a string
     */
    public static final class ContainsPattern extends TypeSafeMatcher<String> {

        private final Pattern pattern;

        public ContainsPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        protected boolean matchesSafely(String item) {
            return pattern.matcher(item).find();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("String containing text matching ").appendValue(pattern);
        }
    }

    private Matchers() {}
}
