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

import java.util.List;
import java.util.Optional;

/**
 * Method signatures are copied from jakarta.data.repository.PageRequest from the Jakarta Data repo.
 */
public interface PageRequest {
    public static enum Mode {
        CURSOR_NEXT, CURSOR_PREVIOUS, OFFSET
    }

    public interface Cursor {
        public static Cursor forKey(Object... componentsOfKey) {
            return new PageRequestCursor(componentsOfKey);
        }

        public List<?> elements();

        public Object get(int index);

        public int size();
    }

    public static PageRequest afterCursor(Cursor cursor, long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.CURSOR_NEXT, cursor, withTotal);
    }

    public static PageRequest beforeCursor(Cursor cursor, long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.CURSOR_PREVIOUS, cursor, withTotal);
    }

    public static PageRequest ofPage(long page) {
        return new Pagination(page, 10, Mode.OFFSET, null, true);
    }

    public static PageRequest ofPage(long page, int size, boolean withTotal) {
        return new Pagination(page, size, Mode.OFFSET, null, withTotal);
    }

    public static PageRequest ofSize(int size) {
        return new Pagination(1, size, Mode.OFFSET, null, true);
    }

    public PageRequest afterCursor(PageRequest.Cursor cursor);

    public PageRequest beforeCursor(PageRequest.Cursor cursor);

    public Optional<Cursor> cursor();

    public Mode mode();

    public long page();

    public PageRequest page(long pageNum);

    public boolean requestTotal();

    public int size();

    public PageRequest size(int size);

    public PageRequest withoutTotal();

    public PageRequest withTotal();

}
