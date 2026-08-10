/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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
package jakarta.data.repository;

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

/**
 * Interface methods copied from Jakarta Data.
 */
public interface BasicRepository<T, K> extends DataRepository<T, K> {

    @Delete
    void delete(@Nonnull T entity);

    @Delete
    void deleteAll(@Nonnull List<? extends T> entities);

    @Delete
    void deleteById(@By(ID) @Nonnull K id);

    @Find
    @Nonnull
    Stream<T> findAll();

    @Find
    @Nonnull
    Page<T> findAll(@Nonnull PageRequest pageRequest,
                    @Nonnull Order<T> sortBy);

    @Find
    @Nonnull
    Optional<T> findById(@By(ID) @Nonnull K id);

    @Nonnull
    @Save
    <S extends T> S save(@Nonnull S entity);

    @Nonnull
    @Save
    <S extends T> List<S> saveAll(@Nonnull List<S> entities);
}
