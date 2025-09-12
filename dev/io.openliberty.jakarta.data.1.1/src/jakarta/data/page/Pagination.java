/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import java.util.Optional;

import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from jakarta.data.repository.PageRequest from the Jakarta Data repo.
 */
record Pagination(long page,
                int size,
                Mode mode,
                Cursor type,
                boolean requestTotal)
                implements PageRequest {

    Pagination {
        if (page < 1)
            throw new IllegalArgumentException("pageNumber: " + page);
        if (size < 1)
            throw new IllegalArgumentException("maxPageSize: " + size);
        if (mode != Mode.OFFSET && (type == null || type.size() == 0))
            throw new IllegalArgumentException(Messages.get("006.zero.size.key"));
    }

    @Override
    public PageRequest afterCursor(PageRequest.Cursor cursor) {
        return new Pagination(page, size, Mode.CURSOR_NEXT, cursor, requestTotal);
    }

    @Override
    public PageRequest beforeCursor(PageRequest.Cursor cursor) {
        return new Pagination(page, size, Mode.CURSOR_PREVIOUS, cursor, requestTotal);
    }

    @Override
    public Optional<Cursor> cursor() {
        return type == null ? Optional.empty() : Optional.of(type);
    }

    @Override
    public PageRequest page(long pageNum) {
        return new Pagination(pageNum, size, mode, type, requestTotal);
    }

    @Override
    public Pagination size(int maxPageSize) {
        return new Pagination(page, maxPageSize, mode, type, requestTotal);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("PageRequest{page=").append(page) //
                        .append(", size=").append(size) //
                        .append(", mode=").append(mode);

        if (type != null)
            b.append(", cursor size=").append(type.size());

        b.append("}");

        return b.toString();
    }

    @Override
    public PageRequest withoutTotal() {
        return new Pagination(page, size, mode, type, false);
    }

    @Override
    public PageRequest withTotal() {
        return new Pagination(page, size, mode, type, true);
    }

}
