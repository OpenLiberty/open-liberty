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
package test.jakarta.data.inmemory.provider;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.data.repository.Limit;

import test.jakarta.data.inmemory.web.Palindrome;
import test.jakarta.data.inmemory.web.Palindromes;

/**
 * Hard coded fake repository implementation.
 */
public class PalindromeRepository implements Palindromes {

    private Map<Long, Palindrome> data = new HashMap<Long, Palindrome>();

    PalindromeRepository() {
        data.put(126L, new Palindrome(126L, "level"));
        data.put(109L, new Palindrome(109L, "bib"));
        data.put(100L, new Palindrome(100L, "rotator"));
        data.put(139L, new Palindrome(139L, "gag"));
        data.put(178L, new Palindrome(178L, "did"));
        data.put(192L, new Palindrome(192L, "deed"));
        data.put(105L, new Palindrome(105L, "pip"));
        data.put(168L, new Palindrome(168L, "tot"));
        data.put(161L, new Palindrome(161L, "civic"));
        data.put(112L, new Palindrome(112L, "noon"));
        data.put(123L, new Palindrome(123L, "kayak"));
        data.put(184L, new Palindrome(184L, "aha"));
        data.put(101L, new Palindrome(101L, "sees"));
        data.put(183L, new Palindrome(183L, "a"));
        data.put(147L, new Palindrome(147L, "yay"));
        data.put(154L, new Palindrome(154L, "eve"));
        data.put(133L, new Palindrome(133L, "refer"));
        data.put(118L, new Palindrome(118L, "pup"));
        data.put(172L, new Palindrome(172L, "radar"));
        data.put(134L, new Palindrome(134L, "stats"));
    }

    @Override
    public List<Palindrome> findByLengthOrderByLettersAsc(int length, Limit limit) {
        return data.values()
                        .stream()
                        .filter(p -> p.length == length)
                        .sorted(Comparator.comparing(p -> p.letters))
                        .limit(limit.maxResults())
                        .collect(Collectors.toList());
    }
}
