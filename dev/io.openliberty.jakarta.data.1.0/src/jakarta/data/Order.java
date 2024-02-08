/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package jakarta.data;

import java.util.Iterator;
import java.util.List;

import jakarta.data.page.Pageable;

/**
 * Method signatures copied from jakarta.data.Order from the Jakarta Data repo.
 */
public class Order<T> implements Iterable<Sort<T>> {

    private final List<Sort<T>> sortBy;

    private Order(List<Sort<T>> sortBy) {
        this.sortBy = sortBy;
    }

    @SafeVarargs
    public static final <T> Order<T> by(Sort<T>... sortBy) {
        return new Order<T>(List.of(sortBy));
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
               other instanceof Order && sortBy.equals(((Order<?>) other).sortBy);
    }

    @Override
    public int hashCode() {
        return sortBy.hashCode();
    }

    @Override
    public Iterator<Sort<T>> iterator() {
        return sortBy.iterator();
    }

    public Pageable<T> page(long pageNumber) {
        return Pageable.<T> ofPage(pageNumber).sortBy(sortBy);
    }

    public Pageable<T> pageSize(int size) {
        return Pageable.<T> ofSize(size).sortBy(sortBy);
    }

    @Override
    public String toString() {
        return sortBy.toString();
    }
}
