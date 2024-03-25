/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.inmemory.web;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.data.Limit;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/*")
public class ProviderTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    Composites composites;

    @Inject
    Palindromes palindromes;

    /**
     * Uses a repository from a mock Jakarta Data provider that is based on the
     * CDI build compatible extension to query for entities.
     */
    @Test
    public void testDataProviderWithBuildCompatibleExtension() {
        assertEquals(List.of(6L, 9L, 12L, 15L),
                     composites.findByFactorsContainsOrderByIdAsc(3, Limit.of(4))
                                     .stream()
                                     .map(c -> c.id)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(4L, 9L, 25L),
                     composites.findByNumUniqueFactorsOrderByIdAsc(3, Limit.of(3))
                                     .stream()
                                     .map(c -> c.id)
                                     .collect(Collectors.toList()));
    }

    /**
     * Uses a repository from a mock Jakarta Data provider that is based on the CDI extension
     * to insert, update, find, and delete entities.
     */
    @Test
    public void testDataProviderWithExtension() {
        assertEquals(List.of("civic", "kayak", "level", "radar"),
                     palindromes.findByLengthOrderByLettersAsc(5, Limit.of(4))
                                     .stream()
                                     .map(p -> p.letters)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("deed", "noon"),
                     palindromes.findByLengthOrderByLettersAsc(4, Limit.of(2))
                                     .stream()
                                     .map(p -> p.letters)
                                     .collect(Collectors.toList()));
    }
}
