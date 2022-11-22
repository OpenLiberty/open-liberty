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
package jakarta.data.repository;

import java.util.Collections;
import java.util.List;
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

    Pagination(long pageNumber, int pageSize, List<Sort> order, Mode mode, Cursor cursor) {
        if (pageNumber < 1 || pageSize < 1 || mode != Mode.OFFSET && (cursor == null || cursor.size() == 0))
            throw new IllegalArgumentException();
        this.cursor = cursor;
        this.mode = mode;
        this.order = order;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
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
            throw new UnsupportedOperationException();
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
        return new StringBuilder("Pageable{size=").append(pageSize).append(", page=").append(pageNumber).append("}").toString();
    }
}
