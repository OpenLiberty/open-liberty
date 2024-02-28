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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.data.Sort;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.data.page.PageRequest.Mode;

/**
 * Method signatures are copied from jakarta.data.repository.PageRequest from the Jakarta Data repo.
 */
record Pagination<T>(long page,
                int size,
                List<Sort<? super T>> sorts,
                Mode mode,
                Cursor type)
                implements PageRequest<T> {

    Pagination {
        if (page < 1)
            throw new IllegalArgumentException("pageNumber: " + page);
        if (size < 1)
            throw new IllegalArgumentException("maxPageSize: " + size);
        if (mode != Mode.OFFSET && (type == null || type.size() == 0))
            throw new IllegalArgumentException("No keyset values were provided.");
    }

    @Override
    public PageRequest<T> afterKeyset(Object... keyset) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_NEXT, new KeysetCursor(keyset));
    }

    @Override
    public PageRequest<T> afterKeysetCursor(PageRequest.Cursor cursor) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_NEXT, cursor);
    }

    private static final <E> List<E> append(List<E> list, E element) {
        int size = list.size();
        if (size == 0) {
            return List.of(element);
        } else {
            Object[] array = list.toArray(new Object[size + 1]);
            array[size] = element;
            @SuppressWarnings("unchecked")
            List<E> newList = (List<E>) Collections.unmodifiableList(Arrays.asList(array));
            return newList;
        }
    }

    @Override
    public PageRequest<T> asc(String property) {
        return new Pagination<T>(page, size, append(sorts, Sort.asc(property)), mode, type);
    }

    @Override
    public PageRequest<T> ascIgnoreCase(String attribute) {
        return new Pagination<T>(page, size, append(sorts, Sort.ascIgnoreCase(attribute)), mode, type);
    }

    @Override
    public PageRequest<T> beforeKeyset(Object... keyset) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_PREVIOUS, new KeysetCursor(keyset));
    }

    @Override
    public PageRequest<T> beforeKeysetCursor(PageRequest.Cursor cursor) {
        return new Pagination<T>(page, size, sorts, Mode.CURSOR_PREVIOUS, cursor);
    }

    @Override
    public Optional<Cursor> cursor() {
        return type == null ? Optional.empty() : Optional.of(type);
    }

    @Override
    public PageRequest<T> desc(String attribute) {
        return new Pagination<T>(page, size, append(sorts, Sort.desc(attribute)), mode, type);
    }

    @Override
    public PageRequest<T> descIgnoreCase(String attribute) {
        return new Pagination<T>(page, size, append(sorts, Sort.descIgnoreCase(attribute)), mode, type);
    }

    @Override
    public Pagination<T> next() {
        if (mode == Mode.OFFSET)
            return new Pagination<T>(page + 1, size, sorts, mode, null);
        else
            throw new UnsupportedOperationException("Not supported for keyset pagination. Instead use afterKeyset or afterKeysetCursor to provide the next keyset values or obtain the nextPageRequest from a KeysetAwareSlice.");
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
    public Pagination<T> sortBy(Iterable<Sort<? super T>> sorts) {
        List<Sort<? super T>> sortList = sorts instanceof List //
                        ? List.copyOf((List<Sort<? super T>>) sorts) //
                        : sorts == null //
                                        ? Collections.emptyList() //
                                        : StreamSupport.stream(sorts.spliterator(), false) //
                                                        .collect(Collectors.toUnmodifiableList());
        return new Pagination<T>(page, size, sortList, mode, type);
    }

    @Override
    public PageRequest<T> sortBy(Sort<? super T> sort) {
        return new Pagination<T>(page, size, List.of(sort), mode, type);
    }

    @Override
    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2) {
        return new Pagination<T>(page, size, List.of(sort1, sort2), mode, type);
    }

    @Override
    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3), mode, type);
    }

    @Override
    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3,
                                 Sort<? super T> sort4) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3, sort4), mode, type);
    }

    @Override
    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3,
                                 Sort<? super T> sort4, Sort<? super T> sort5) {
        return new Pagination<T>(page, size, List.of(sort1, sort2, sort3, sort4, sort5), mode, type);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("PageRequest{page=").append(page).append(", size=").append(size);

        if (type != null)
            b.append(", mode=").append(mode).append(", ").append(type.size()).append(" keys");

        for (Sort<? super T> o : sorts) {
            b.append(", ").append(o.property()).append(o.ignoreCase() ? " IGNORE CASE" : "").append(o.isDescending() ? " DESC" : " ASC");
        }

        b.append("}");

        return b.toString();
    }

}
