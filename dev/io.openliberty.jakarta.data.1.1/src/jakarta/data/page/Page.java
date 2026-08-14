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

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Method signatures copied from proposals in the Jakarta Data git repo.
 */
public interface Page<T> extends Iterable<T> {

    @Nonnull
    List<T> content();

    boolean hasContent();

    boolean hasNext();

    boolean hasTotals();

    boolean hasPrevious();

    @Nonnull
    PageRequest nextPageRequest();

    int numberOfElements();

    @Nonnull
    PageRequest pageRequest();

    @Nonnull
    PageRequest previousPageRequest();

    @Nonnull
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    long totalElements();

    long totalPages();
}
