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
package jakarta.data.page;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.data.Sort;
import jakarta.data.page.Pageable.Cursor;
import jakarta.data.page.Pageable.Mode;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
record Pagination(long page,
                int size,
                List<Sort> sorts,
                Mode mode,
                Cursor type)
                implements Pageable {

    Pagination {
        if (page < 1)
            throw new IllegalArgumentException("pageNumber: " + page);
        if (size < 1)
            throw new IllegalArgumentException("maxPageSize: " + size);
        if (mode != Mode.OFFSET && (type == null || type.size() == 0))
            throw new IllegalArgumentException("No keyset values were provided.");
    }

    @Override
    public Pageable afterKeyset(Object... keyset) {
        return new Pagination(page, size, sorts, Mode.CURSOR_NEXT, new KeysetCursor(keyset));
    }

    @Override
    public Pageable afterKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination(page, size, sorts, Mode.CURSOR_NEXT, cursor);
    }

    @Override
    public Pageable beforeKeyset(Object... keyset) {
        return new Pagination(page, size, sorts, Mode.CURSOR_PREVIOUS, new KeysetCursor(keyset));
    }

    @Override
    public Pageable beforeKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination(page, size, sorts, Mode.CURSOR_PREVIOUS, cursor);
    }

    @Override
    public Optional<Cursor> cursor() {
        return type == null ? Optional.empty() : Optional.of(type);
    }

    @Override
    public Pagination next() {
        if (mode == Mode.OFFSET)
            return new Pagination(page + 1, size, sorts, mode, null);
        else
            throw new UnsupportedOperationException("Not supported for keyset pagination. Instead use afterKeyset or afterKeysetCursor to provide the next keyset values or obtain the nextPageable from a KeysetAwareSlice.");
    }

    public static Pagination ofPage(long page) {
        return new Pagination(page, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static Pagination ofSize(int size) {
        return new Pagination(1, size, Collections.emptyList(), Mode.OFFSET, null);
    }

    @Override
    public Pagination page(long pageNumber) {
        return new Pagination(pageNumber, size, sorts, mode, type);
    }

    @Override
    public Pagination size(int maxPageSize) {
        return new Pagination(page, maxPageSize, sorts, mode, type);
    }

    @Override
    public Pagination sortBy(Iterable<Sort> sorts) {
        List<Sort> order;
        if (sorts == null)
            order = Collections.emptyList();
        else
            order = StreamSupport.stream(sorts.spliterator(), false).collect(Collectors.toUnmodifiableList());

        return new Pagination(page, size, order, mode, type);
    }

    @Override
    public Pagination sortBy(Sort... sorts) {
        @SuppressWarnings("unchecked")
        List<Sort> order = sorts == null ? Collections.EMPTY_LIST : List.of(sorts);

        return new Pagination(page, size, order, mode, type);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Pageable{page=").append(page).append(", size=").append(size);

        if (type != null)
            b.append(", mode=").append(mode).append(", ").append(type.size()).append(" keys");

        for (Sort o : sorts) {
            b.append(", ").append(o.property()).append(o.ignoreCase() ? " IGNORE CASE" : "").append(o.isDescending() ? " DESC" : " ASC");
        }

        b.append("}");

        return b.toString();
    }
}
