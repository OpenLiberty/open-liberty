/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Interface methods copied from Jakarta Data.
 */
public interface BasicRepository<T, K> extends DataRepository<T, K> {
    long countBy();

    @Delete
    void delete(T entity);

    @Delete
    void deleteAll();

    @Delete
    void deleteAll(Iterable<? extends T> entities);

    void deleteByIdIn(Iterable<K> ids);

    void deleteById(K id);

    boolean existsById(K id);

    @Find
    Stream<T> findAll();

    Stream<T> findByIdIn(Iterable<K> ids);

    Optional<T> findById(K id);

    @Save
    <S extends T> S save(S entity);

    @Save
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
}
