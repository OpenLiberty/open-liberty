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

import java.time.Duration;
import java.util.Optional;

/**
 * Copied from jakarta.nosql.mapping.Template to investigate how well
 * the NoSQL template interface works for relational database access.
 */
public interface Template {
    <T, K> void delete(Class<T> entityClass, K id);

    <T, K> Optional<T> find(Class<T> entityClass, K id);

    <T> T insert(T entity);

    <T> T insert(T entity, Duration ttl);

    <T> Iterable<T> insert(Iterable<T> entities);

    <T> Iterable<T> insert(Iterable<T> entities, Duration ttl);

    <T> T update(T entity);

    <T> Iterable<T> update(Iterable<T> entities);
}
