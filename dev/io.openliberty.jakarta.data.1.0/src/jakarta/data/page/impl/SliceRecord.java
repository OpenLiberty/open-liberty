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

import jakarta.data.page.PageRequest;
import jakarta.data.page.Slice;

/**
 * Method signatures are copied from Jakarta Data.
 */
public record SliceRecord<T>(PageRequest<T> pageRequest,
                List<T> content,
                boolean moreResults)
                implements Slice<T> {

    public SliceRecord(PageRequest<T> req,
                       List<T> content) {
        this(req, //
             content, //
             req.size() == content.size());
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
    public Iterator<T> iterator() {
        return content.iterator();
    }

    @Override
    public PageRequest<T> nextPageRequest() {
        if (moreResults)
            return pageRequest.next();
        else
            return null;
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
}
