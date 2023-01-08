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
package io.openliberty.microprofile.telemetry.internal.utils;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

public class TestUtils {

    /**
     * Assert that only one element of {@code collection} matches {@code matcher} and return it
     *
     * @param <T> the element type
     * @param collection the collection
     * @param matcher the matcher to test against elements of {@code collection}
     * @return the singular matching element
     * @throws AssertionError if there is not exactly one matching element
     */
    public static <T> T findOneFrom(Collection<T> collection, Matcher<? super T> matcher) {
        List<T> results = collection.stream()
                                    .filter(e -> matcher.matches(e))
                                    .collect(Collectors.toList());
        assertThat(results, contains(singletonList(matcher)));
        return results.get(0);
    }

}
