/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
public interface CrudRepository<T, K> extends DataRepository<T, K> {
    long count();

    void delete(T entity);

    void deleteAll();

    void deleteAll(Iterable<? extends T> entities);

    void deleteAllById(Iterable<K> ids);

    void deleteById(K id);

    boolean existsById(K id);

    Stream<T> findAll();

    Stream<T> findAllById(Iterable<K> ids);

    Optional<T> findById(K id);

    <S extends T> S save(S entity);

    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
}
