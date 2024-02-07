/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import jakarta.data.Sort;

/**
 * Method signatures are copied from jakarta.data.repository.Pageable from the Jakarta Data repo.
 */
public interface Pageable<T> {
    public static enum Mode {
        CURSOR_NEXT, CURSOR_PREVIOUS, OFFSET
    }

    public interface Cursor {
        public Object getKeysetElement(int index);

        public int size();
    }

    public static <T> Pageable<T> of(Class<T> entityClass) {
        return new Pagination<T>(1, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static <T> Pageable<T> ofPage(long page) {
        return new Pagination<T>(page, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static <T> Pageable<T> ofSize(int size) {
        return new Pagination<T>(1, size, Collections.emptyList(), Mode.OFFSET, null);
    }

    public Pageable<T> afterKeyset(Object... keyset);

    public Pageable<T> afterKeysetCursor(Pageable.Cursor cursor);

    public Pageable<T> beforeKeyset(Object... keyset);

    public Pageable<T> beforeKeysetCursor(Pageable.Cursor cursor);

    public Optional<Cursor> cursor();

    public Mode mode();

    public Pageable<T> next();

    public long page();

    public int size();

    public List<Sort<T>> sorts();

    public Pageable<T> page(long page);

    public Pageable<T> size(int size);

    public Pageable<T> sortBy(Iterable<Sort<T>> sorts);

    public Pageable<T> sortBy(Sort<T> sort);

    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2);

    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3);

    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3, Sort<T> sort4);

    public Pageable<T> sortBy(Sort<T> sort1, Sort<T> sort2, Sort<T> sort3, Sort<T> sort4, Sort<T> sort5);
}
