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
package jakarta.data.repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
class Pagination implements Pageable {
    private final Cursor cursor;
    private final Mode mode;
    private final List<Sort> order;
    private final long pageNumber;
    private final int pageSize;
    private final int hash;

    Pagination(long pageNumber, int pageSize, List<Sort> order, Mode mode, Cursor cursor) {
        if (pageNumber < 1)
            throw new IllegalArgumentException("pageNumber: " + pageNumber);
        if (pageSize < 1)
            throw new IllegalArgumentException("pageSize: " + pageSize);
        if (mode != Mode.OFFSET && (cursor == null || cursor.size() == 0))
            throw new IllegalArgumentException("No keyset values were provided.");
        this.cursor = cursor;
        this.mode = mode;
        this.order = order;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hash = Objects.hash(pageSize, pageNumber, order, mode, cursor);
    }

    @Override
    public Pageable afterKeyset(Object... keyset) {
        return new Pagination(pageNumber, pageSize, order, Mode.CURSOR_NEXT, new KeysetCursor(keyset));
    }

    @Override
    public Pageable afterKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination(pageNumber, pageSize, order, Mode.CURSOR_NEXT, cursor);
    }

    @Override
    public Pageable beforeKeyset(Object... keyset) {
        return new Pagination(pageNumber, pageSize, order, Mode.CURSOR_PREVIOUS, new KeysetCursor(keyset));
    }

    @Override
    public Pageable beforeKeysetCursor(Pageable.Cursor cursor) {
        return new Pagination(pageNumber, pageSize, order, Mode.CURSOR_PREVIOUS, cursor);
    }

    @Override
    public Cursor cursor() {
        return cursor;
    }

    @Override
    public boolean equals(Object p) {
        Pagination pagination;
        return this == p
               || p != null
                  && p.getClass() == getClass()
                  && (pagination = (Pagination) p).hash == hash
                  && pagination.mode == mode
                  && pagination.pageNumber == pageNumber
                  && pagination.pageSize == pageSize
                  && pagination.order.equals(order)
                  && Objects.equals(pagination.cursor, cursor);
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public long page() {
        return pageNumber;
    }

    @Override
    public int size() {
        return pageSize;
    }

    @Override
    public List<Sort> sorts() {
        return order;
    }

    @Override
    public Pagination next() {
        if (mode == Mode.OFFSET)
            return new Pagination(pageNumber + 1, pageSize, order, mode, null);
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
    public Pagination page(long page) {
        return new Pagination(page, pageSize, order, mode, cursor);
    }

    @Override
    public Pagination size(int size) {
        return new Pagination(pageNumber, size, order, mode, cursor);
    }

    @Override
    public Pagination sortBy(Iterable<Sort> sorts) {
        List<Sort> order;
        if (sorts == null)
            order = Collections.emptyList();
        else
            order = StreamSupport.stream(sorts.spliterator(), false).collect(Collectors.toUnmodifiableList());

        return new Pagination(pageNumber, pageSize, order, mode, cursor);
    }

    @Override
    public Pagination sortBy(Sort... sorts) {
        @SuppressWarnings("unchecked")
        List<Sort> order = sorts == null ? Collections.EMPTY_LIST : List.of(sorts);

        return new Pagination(pageNumber, pageSize, order, mode, cursor);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Pageable{page=").append(pageNumber).append(", size=").append(pageSize);

        if (cursor != null)
            b.append(", mode=").append(mode).append(", ").append(cursor.size()).append(" keys");

        for (Sort o : order)
            b.append(", ").append(o.property()).append(o.isDescending() ? " DESC" : " ASC");

        b.append("}");

        return b.toString();
    }
}
