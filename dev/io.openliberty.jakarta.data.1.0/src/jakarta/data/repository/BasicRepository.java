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

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

/**
 * Interface methods copied from Jakarta Data.
 */
public interface BasicRepository<T, K> extends DataRepository<T, K> {

    @Delete
    void delete(T entity);

    @Delete
    void deleteAll(List<? extends T> entities);

    @Delete
    void deleteById(@By(ID) K id);

    @Find
    Stream<T> findAll();

    @Find
    Page<T> findAll(PageRequest<T> pageRequest);

    @Find
    Optional<T> findById(@By(ID) K id);

    @Save
    <S extends T> S save(S entity);

    @Save
    <S extends T> List<S> saveAll(List<S> entities);
}
