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
package jakarta.data.page.impl;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;

/**
 * Method signatures are copied from Jakarta Data.
 */
public record CursoredPageRecord<T>(
                List<T> content,
                List<Cursor> cursors,
                long totalElements,
                PageRequest<T> pageRequest,
                PageRequest<T> nextPageRequest,
                PageRequest<T> previousPageRequest)
                implements CursoredPage<T> {

    @Override
    public Cursor getKeysetCursor(int i) {
        return cursors.get(i);
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public boolean hasNext() {
        return nextPageRequest != null;
    }

    @Override
    public boolean hasPrevious() {
        return previousPageRequest != null;
    }

    @Override
    public boolean hasTotals() {
        return totalElements >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return content.iterator();
    }

    @Override
    public PageRequest<T> nextPageRequest() {
        if (nextPageRequest == null)
            throw new NoSuchElementException();
        else
            return nextPageRequest;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> PageRequest<E> nextPageRequest(Class<E> entityClass) {
        return (PageRequest<E>) nextPageRequest();
    }

    @Override
    public int numberOfElements() {
        return content.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> PageRequest<E> pageRequest(Class<E> entityClass) {
        return (PageRequest<E>) pageRequest;
    }

    @Override
    public PageRequest<T> previousPageRequest() {
        if (previousPageRequest == null)
            throw new NoSuchElementException();
        else
            return previousPageRequest;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> PageRequest<E> previousPageRequest(Class<E> entityClass) {
        return (PageRequest<E>) previousPageRequest();
    }

    @Override
    public long totalElements() {
        if (totalElements >= 0)
            return totalElements;
        else
            throw new IllegalStateException("total elements are not available");
    }

    @Override
    public long totalPages() {
        if (totalElements >= 0) {
            int maxPageSize = pageRequest.size();
            return (totalElements + (maxPageSize - 1)) / maxPageSize;
        } else {
            throw new IllegalStateException("total elements are not available");
        }
    }
}
