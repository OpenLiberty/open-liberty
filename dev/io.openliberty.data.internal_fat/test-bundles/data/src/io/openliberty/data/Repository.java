/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data;

import java.util.Optional;

/**
 * Copied from jakarta.nosql.mapping.Repository to investigate how well
 * the NoSQL repository interface works for relational database access.
 */
public interface Repository<T, K> {
    long count();

    void deleteById(Iterable<K> ids);

    void deleteById(K id);

    boolean existsById(K id);

    Iterable<T> findById(Iterable<K> ids);

    Optional<T> findById(K id);

    <S extends T> Iterable<S> save(Iterable<S> entities);

    <S extends T> S save(S entity);
}
