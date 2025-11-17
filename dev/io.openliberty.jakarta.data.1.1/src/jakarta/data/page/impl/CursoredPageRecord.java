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

import jakarta.data.messages.Messages;
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
                PageRequest pageRequest,
                PageRequest nextPageRequest,
                PageRequest previousPageRequest)
                implements CursoredPage<T> {

    public CursoredPageRecord(List<T> content,
                              List<PageRequest.Cursor> cursors,
                              long totalElements,
                              PageRequest pageRequest,
                              boolean first,
                              boolean last) {
        this(content, //
             cursors, //
             totalElements, //
             pageRequest, //
             last ? null : PageRequest.afterCursor(cursors.get(cursors.size() - 1),
                                                   1L + pageRequest.page(),
                                                   pageRequest.size(),
                                                   pageRequest.requestTotal()), //
             first ? null : PageRequest.beforeCursor(cursors.get(0),
                                                     pageRequest.page() == 1 ? 1 : pageRequest.page() - 1,
                                                     pageRequest.size(),
                                                     pageRequest.requestTotal()));
    }

    @Override
    public Cursor cursor(int i) {
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
    public PageRequest nextPageRequest() {
        if (nextPageRequest == null)
            throw new NoSuchElementException();
        else
            return nextPageRequest;
    }

    @Override
    public int numberOfElements() {
        return content.size();
    }

    @Override
    public PageRequest previousPageRequest() {
        if (previousPageRequest == null)
            throw new NoSuchElementException();
        else
            return previousPageRequest;
    }

    @Override
    public long totalElements() {
        if (totalElements >= 0)
            return totalElements;
        else
            throw new IllegalStateException(Messages.get("010.unknown.total"));
    }

    @Override
    public long totalPages() {
        if (totalElements >= 0) {
            int maxPageSize = pageRequest.size();
            return (totalElements + (maxPageSize - 1)) / maxPageSize;
        } else {
            throw new IllegalStateException(Messages.get("010.unknown.total"));
        }
    }
}
