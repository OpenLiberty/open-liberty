/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.concurrent.CompletableFuture;

import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 */
@Repository
public interface Primes {

    long countByNumberLessThan(long number);

    @Asynchronous
    CompletableFuture<Short> countByNumberBetweenAndEvenNot(long first, long last, boolean isOdd);

    Integer countNumberBetween(long first, long last);

    @OrderBy(value = "name", descending = true)
    Prime[] findFirst5ByNumberLessThanEqual(long maxNumber);

    Prime findFirstByNameLikeOrderByNumber(String namePattern);

    boolean existsByNumber(long number);

    Boolean existsNumberBetween(Long first, Long last);

    void save(Prime... primes);
}
