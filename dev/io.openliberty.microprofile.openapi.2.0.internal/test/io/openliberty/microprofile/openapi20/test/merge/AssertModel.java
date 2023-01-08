/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.test.merge;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.junit.Assert;

import io.openliberty.microprofile.openapi20.internal.merge.ModelType;
import io.openliberty.microprofile.openapi20.internal.merge.ModelType.ModelParameter;

public class AssertModel {

    /**
     * Recursively traverse two OpenAPI models and assert that they are equal
     *
     * @param expected the expected result
     * @param actual the model to test for equality to {@code expected}
     */
    public static void assertModelsEqual(Constructible expected, Constructible actual) {
        assertModelsEqual(expected, actual, new ArrayDeque<>());
    }

    /**
     * Recursively traverse two maps containing OpenAPI model objects and assert that they are equal
     *
     * @param expected the expected result
     * @param actual the map to test for equality to {@code expected}
     */
    public static void assertModelMaps(Map<?, ?> expected, Map<?, ?> actual) {
        assertEqualMap(expected, actual, new ArrayDeque<>());
    }

    private static void assertModelsEqual(Object expected, Object actual, Deque<String> context) {

        if (expected == null) {
            // When smallrye merges model parts, if a map is null it sometimes gets initialized to an empty map
            // so treat an empty map as being equal to null
            assertNullOrEmptyMap(context, actual);
            return;
        } else {
            assertNotNull(context, actual);
        }

        Optional<ModelType> mtExpected = ModelType.getModelObject(expected.getClass());
        Optional<ModelType> mtActual = ModelType.getModelObject(actual.getClass());
        assertEquals("Model types not equal", context, mtExpected, mtActual);

        if (mtExpected.isPresent()) {
            assertEqualModelObject(expected, actual, mtExpected.get(), context);
        } else if (expected instanceof Map) {
            assertEqualMap(expected, actual, context);
        } else if (expected instanceof List) {
            assertEqualList(expected, actual, context);
        } else {
            // assert that non-model, non-collection objects are not copied
            assertEquals("Values not equal", context, expected, actual);
        }
    }

    private static void assertEqualModelObject(Object expected, Object actual, ModelType mt, Deque<String> context) {
        for (ModelParameter desc : mt.getParameters()) {
            context.push(desc.toString());
            assertModelsEqual(desc.get(expected), desc.get(actual), context);
            context.pop();
        }
    }

    private static void assertEqualMap(Object expected, Object actual, Deque<String> context) {
        Map<?, ?> expectedMap = (Map<?, ?>) expected;
        assertThat(actual, instanceOf(Map.class));
        Map<?, ?> actualMap = (Map<?, ?>) actual;

        assertEquals("Different key set", context, expectedMap.keySet(), actualMap.keySet());
        for (Object key : expectedMap.keySet()) {
            context.push(key.toString());
            assertModelsEqual(expectedMap.get(key), actualMap.get(key), context);
            context.pop();
        }
    }

    private static void assertEqualList(Object expected, Object actual, Deque<String> context) {
        List<?> expectedList = (List<?>) expected;
        assertThat(actual, instanceOf(List.class));
        List<?> actualList = (List<?>) actual;

        assertThat("List has wrong size at " + contextString(context), actualList, hasSize(expectedList.size()));

        Iterator<?> expectedIterator = expectedList.iterator();
        Iterator<?> actualIterator = actualList.iterator();
        int i = 0;
        while (expectedIterator.hasNext()) {
            context.push("[" + i + "]");
            assertModelsEqual(expectedIterator.next(), actualIterator.next(), context);
            context.pop();
            i++;
        }
    }

    private static void assertEquals(String message, Deque<String> context, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            Assert.assertEquals(message + " at " + contextString(context), expected, actual);
        }
    }

    private static void assertNullOrEmptyMap(Deque<String> context, Object actual) {
        if (actual != null) {
            // When smallrye merges models, if a map is null, it sometimes gets initialized to an empty map
            Optional<ModelType> mt = ModelType.getModelObject(actual.getClass());
            if (!mt.isPresent() && actual instanceof Map) {
                if (!((Map<?, ?>) actual).isEmpty()) {
                    throw new AssertionError("Value is neither null nor empty map at " + contextString(context) + ". Was: " + actual);
                }
            } else {
                throw new AssertionError("Value not null at " + contextString(context) + ". Was: " + actual);
            }
        }
    }

    private static void assertNotNull(Deque<String> context, Object actual) {
        if (actual == null) {
            throw new AssertionError("Value null at " + contextString(context));
        }
    }

    private static String contextString(Deque<String> context) {
        // Most recent context is pushed onto the front of the queue
        // Reverse it to get a hierarchical path
        List<String> contextCopy = new ArrayList<>(context);
        Collections.reverse(contextCopy);
        return String.join("/", contextCopy);
    }

}
