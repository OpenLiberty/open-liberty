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

import jakarta.data.page.PageRequest;

/**
 * Method signatures copied from jakarta.data.Order from the Jakarta Data repo.
 */
public class Order<T> implements Iterable<Sort<? super T>> {

    private final List<Sort<? super T>> sortBy;

    private Order(List<Sort<? super T>> sortBy) {
        this.sortBy = sortBy;
    }

    @SafeVarargs
    public static <T> Order<T> by(Sort<? super T>... sortBy) {
        return new Order<T>(List.of(sortBy));
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
               other instanceof Order && sortBy.equals(((Order<?>) other).sortBy);
    }

    public List<Sort<? super T>> sorts() {
        return sortBy;
    }

    @Override
    public int hashCode() {
        return sortBy.hashCode();
    }

    @Override
    public Iterator<Sort<? super T>> iterator() {
        return sortBy.iterator();
    }

    public PageRequest<T> page(long pageNumber) {
        return PageRequest.<T> ofPage(pageNumber).sortBy(sortBy);
    }

    public PageRequest<T> pageSize(int size) {
        return PageRequest.<T> ofSize(size).sortBy(sortBy);
    }

    @Override
    public String toString() {
        return sortBy.toString();
    }
}
