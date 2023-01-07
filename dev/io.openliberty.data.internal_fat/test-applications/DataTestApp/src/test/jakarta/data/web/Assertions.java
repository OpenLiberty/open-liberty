/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.data.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import junit.framework.AssertionFailedError;

/**
 * Junit 5 isn't currently available to these tests, so we have our own implementation of
 * assertIterableEquals. Should eventually replace this with Junit assertions.
 */
public class Assertions {

    public static <T> void assertArrayEquals(T[] expected, T[] actual, Comparator<T> comparator) {
        String errorMessage = "expected: " + Arrays.toString(expected) + " but was: " + Arrays.toString(actual);

        if (expected == actual) // covers if both are null
            return;
        if (expected == null || actual == null || expected.length != actual.length)
            throw new AssertionFailedError(errorMessage);

        for (int i = 0; i < expected.length; i++)
            assertEquals(errorMessage, 0, comparator.compare(expected[i], actual[i]));
    }

    public static void assertIterableEquals(Iterable<?> expected, Iterable<?> actual) {
        String errorMessage = "expected: " + expected + " but was: " + actual;

        if (expected == actual) // covers if both are null
            return;
        if (expected == null || actual == null)
            throw new AssertionFailedError(errorMessage);

        Iterator<?> expectIt = expected.iterator();
        Iterator<?> actualIt = actual.iterator();

        boolean hasNextExpected;
        do {
            hasNextExpected = expectIt.hasNext();
            if (hasNextExpected != actualIt.hasNext())
                throw new AssertionFailedError(errorMessage);
            if (hasNextExpected)
                assertEquals(errorMessage, expectIt.next(), actualIt.next());
        } while (hasNextExpected);
    }

}
