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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.data.Limit;

import test.jakarta.data.inmemory.web.Composite;
import test.jakarta.data.inmemory.web.Composites;

/**
 * Hard coded fake repository implementation.
 */
public class CompositeRepository implements Composites {

    private Map<Long, Composite> data = new HashMap<>();

    CompositeRepository() {
        for (long n = 4; n < 100; n++) {
            HashSet<Long> factors = new HashSet<>();
            for (long f = 2; f < n; f++)
                if (n % f == 0)
                    factors.add(f);
            if (!factors.isEmpty()) {
                factors.add(1L);
                factors.add(n);
                data.put(n, new Composite(n, factors));
            }
        }
    }

    @Override
    public List<Composite> findByFactorsContainsOrderByIdAsc(long factor, Limit limit) {
        return data.values()
                        .stream()
                        .filter(c -> c.factors.contains(factor))
                        .sorted(Comparator.comparing(c -> c.id))
                        .skip(limit.startAt() - 1)
                        .limit(limit.maxResults())
                        .collect(Collectors.toList());
    }

    @Override
    public List<Composite> findByNumUniqueFactorsOrderByIdAsc(int numFactors, Limit limit) {
        return data.values()
                        .stream()
                        .filter(c -> c.numUniqueFactors == numFactors)
                        .sorted(Comparator.comparing(c -> c.id))
                        .skip(limit.startAt() - 1)
                        .limit(limit.maxResults())
                        .collect(Collectors.toList());
    }
}
