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
 * Method signatures are copied from jakarta.data.repository.PageRequest from the Jakarta Data repo.
 */
public interface PageRequest<T> {
    public static enum Mode {
        CURSOR_NEXT, CURSOR_PREVIOUS, OFFSET
    }

    public interface Cursor {
        public static Cursor forKeyset(Object... keysetValues) {
            return new KeysetCursor(keysetValues);
        }

        public List<?> elements();

        public Object getKeysetElement(int index);

        public int size();
    }

    public static <T> PageRequest<T> of(Class<T> entityClass) {
        return new Pagination<T>(1, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static <T> PageRequest<T> ofPage(long page) {
        return new Pagination<T>(page, 10, Collections.emptyList(), Mode.OFFSET, null);
    }

    public static <T> PageRequest<T> ofSize(int size) {
        return new Pagination<T>(1, size, Collections.emptyList(), Mode.OFFSET, null);
    }

    public PageRequest<T> afterKeyset(Object... keyset);

    public PageRequest<T> afterKeysetCursor(PageRequest.Cursor cursor);

    public PageRequest<T> asc(String property);

    public PageRequest<T> ascIgnoreCase(String property);

    public PageRequest<T> beforeKeyset(Object... keyset);

    public PageRequest<T> beforeKeysetCursor(PageRequest.Cursor cursor);

    public Optional<Cursor> cursor();

    public PageRequest<T> desc(String property);

    public PageRequest<T> descIgnoreCase(String property);

    public Mode mode();

    public PageRequest<T> next();

    public long page();

    public int size();

    public List<Sort<? super T>> sorts();

    public PageRequest<T> page(long page);

    public PageRequest<T> size(int size);

    public PageRequest<T> sortBy(Iterable<Sort<? super T>> sorts);

    public PageRequest<T> sortBy(Sort<? super T> sort);

    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2);

    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3);

    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3,
                                 Sort<? super T> sort4);

    public PageRequest<T> sortBy(Sort<? super T> sort1, Sort<? super T> sort2, Sort<? super T> sort3,
                                 Sort<? super T> sort4, Sort<? super T> sort5);
}
