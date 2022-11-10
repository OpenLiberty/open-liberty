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
    private final List<Sort> order;
    private final long pageNumber;
    private final int pageSize;

    Pagination(long pageNumber, int pageSize, List<Sort> order) {
        if (pageNumber < 1 || pageSize < 1)
            throw new IllegalArgumentException();
        this.order = order;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    @Override
    public KeysetPageable afterKeyset(Object... keyset) {
        return new KeysetPagination(this, KeysetPageable.Mode.NEXT, new KeysetPageable.CursorImpl(keyset));
    }

    @Override
    public KeysetPageable afterKeysetCursor(KeysetPageable.Cursor cursor) {
        return new KeysetPagination(this, KeysetPageable.Mode.NEXT, cursor);
    }

    @Override
    public KeysetPageable beforeKeyset(Object... keyset) {
        return new KeysetPagination(this, KeysetPageable.Mode.PREVIOUS, new KeysetPageable.CursorImpl(keyset));
    }

    @Override
    public KeysetPageable beforeKeysetCursor(KeysetPageable.Cursor cursor) {
        return new KeysetPagination(this, KeysetPageable.Mode.PREVIOUS, cursor);
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
        return new Pagination(pageNumber + 1, pageSize, order);
    }

    public static Pagination ofPage(long page) {
        return new Pagination(page, 10, Collections.emptyList());
    }

    public static Pagination ofSize(int size) {
        return new Pagination(1, size, Collections.emptyList());
    }

    @Override
    public Pagination newPage(long page) {
        return new Pagination(page, pageSize, order);
    }

    @Override
    public Pagination newSize(int size) {
        return new Pagination(pageNumber, size, order);
    }

    @Override
    public Pagination sortBy(Iterable<Sort> sorts) {
        List<Sort> order;
        if (sorts == null)
            order = Collections.emptyList();
        else
            order = StreamSupport.stream(sorts.spliterator(), false).collect(Collectors.toUnmodifiableList());

        return new Pagination(pageNumber, pageSize, order);
    }

    @Override
    public Pagination sortBy(Sort... sorts) {
        @SuppressWarnings("unchecked")
        List<Sort> order = sorts == null ? Collections.EMPTY_LIST : List.of(sorts);

        return new Pagination(pageNumber, pageSize, order);
    }

    @Override
    public String toString() {
        return new StringBuilder("Pageable{size=").append(pageSize).append(", page=").append(pageNumber).append("}").toString();
    }
}
