/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package jakarta.data.page;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.data.Sort;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
record Pagination<T>(long page,
                int size,
                List<Sort<T>> sorts,
                Mode mode,
                Cursor type)
                implements Pageable<T> {

    Pagination {
        if (page < 1)
            throw new IllegalArgumentException("pageNumber: " + page);
        if (size < 1)
            throw new IllegalArgumentException("maxPageSize: " + size);
        if (mode != Mode.OFFSET && (type == null || type.size() == 0))
            throw new IllegalArgumentException("No keyset values were provided.");
    }

    @Override
    public Pageable<T> afterKeyset(Object... keyset) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_NEXT, new KeysetCursor(keyset));
    }

    @Override
    public Pageable<T> afterKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_NEXT, cursor);
    }

    @Override
    public Pageable<T> beforeKeyset(Object... keyset) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_PREVIOUS, new KeysetCursor(keyset));
    }

    @Override
    public Pageable<T> beforeKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_PREVIOUS, cursor);
    }

    @Override
    public Optional<Cursor> cursor() {
        return type == null ? Optional.empty() : Optional.of(type);
    }

    @Override
    public Pagination<T> next() {
        if (mode == Mode.OFFSET)
            return new Pagination<T>(page + 1, size, sorts, mode, null);
        else
            throw new UnsupportedOperationException("Not supported for keyset pagination. Instead use afterKeyset or afterKeysetCursor to provide the next keyset values or obtain the nextPageable from a KeysetAwareSlice.");
    }

    @Override
    public Pagination<T> page(long pageNumber) {
        return new Pagination<T>(pageNumber, size, sorts, mode, type);
    }

    @Override
    public Pagination<T> size(int maxPageSize) {
        return new Pagination<T>(page, maxPageSize, sorts, mode, type);
    }

    @Override
    public Pagination<T> sortBy(Iterable<Sort<T>> sorts) {
        List<Sort<T>> sortList = sorts instanceof List ? List.copyOf((List<Sort<T>>) sorts) : sorts == null ? Collections.emptyList() : StreamSupport.stream(sorts.spliterator(),
                                                                                                                                                             false).collect(Collectors.toUnmodifiableList());
        return new Pagination<T>(page, size, sortList, mode, type);
    }

    @Override
    public Pageable<T> sortBy(Sort<T> sort) {
        return new Pagination<T>(page, size, List.of(sort), mode, type);
    }

    @Override
    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2) {
        return new Pagination<T>(page, size, List.of(sort1, sort2), mode, type);
    }

    @Override
    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3), mode, type);
    }

    @Override
    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3, Sort<T> sort4) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3, sort4), mode, type);
    }

    @Override
    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3, Sort<T> sort4, Sort<T> sort5) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3, sort4, sort5), mode, type);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Pageable{page=").append(page).append(", size=").append(size);

        if (type != null)
            b.append(", mode=").append(mode).append(", ").append(type.size()).append(" keys");

        for (Sort<T> o : sorts) {
            b.append(", ").append(o.property()).append(o.ignoreCase() ? " IGNORE CASE" : "").append(o.isDescending() ? " DESC" : " ASC");
        }

        b.append("}");

        return b.toString();
    }

}
