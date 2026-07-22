/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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
package jakarta.data;

import java.util.Iterator;
import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Method signatures copied from jakarta.data.Order from the Jakarta Data repo.
 */
public class Order<T> implements Iterable<Sort<? super T>> {

    private final List<Sort<? super T>> sortBy;

    private Order(@Nonnull List<Sort<? super T>> sortBy) {
        this.sortBy = sortBy;
    }

    @Nonnull
    public static <T> Order<T> by(@Nonnull List<? extends Sort<? super T>> sorts) {
        return new Order<T>(List.copyOf(sorts));
    }

    @SafeVarargs
    @Nonnull
    public static <T> Order<T> by(@Nonnull Sort<? super T>... sortBy) {
        return new Order<T>(List.of(sortBy));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return this == other ||
               other instanceof Order && sortBy.equals(((Order<?>) other).sortBy);
    }

    @Nonnull
    public List<Sort<? super T>> sorts() {
        return sortBy;
    }

    @Override
    public int hashCode() {
        return sortBy.hashCode();
    }

    @Override
    @Nonnull
    public Iterator<Sort<? super T>> iterator() {
        return sortBy.iterator();
    }

    @Override
    @Nonnull
    public String toString() {
        return sortBy.toString();
    }
}
