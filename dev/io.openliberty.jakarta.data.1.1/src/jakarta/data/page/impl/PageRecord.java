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
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

/**
 * Method signatures are copied from Jakarta Data.
 */
public record PageRecord<T>(PageRequest pageRequest,
                List<T> content,
                long totalElements,
                boolean moreResults)
                implements Page<T> {

    public PageRecord(PageRequest req,
                      List<T> content,
                      long total) {
        this(req, //
             content, //
             total, //
             content.size() == req.size() && (total < 0 || req.page() * req.size() < total));
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public boolean hasNext() {
        return moreResults;
    }

    @Override
    public boolean hasPrevious() {
        return pageRequest.page() > 1;
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
        if (hasNext())
            return PageRequest.ofPage(pageRequest.page() + 1L, pageRequest.size(), pageRequest.requestTotal());
        else
            throw new NoSuchElementException();
    }

    @Override
    public int numberOfElements() {
        return content.size();
    }

    @Override
    public PageRequest previousPageRequest() {
        if (hasPrevious())
            return PageRequest.ofPage(pageRequest.page() - 1L, pageRequest.size(), pageRequest.requestTotal());
        else
            throw new NoSuchElementException();
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
