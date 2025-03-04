/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.data.Sort;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository interface for the Rating entity which is a record
 * with embeddable attributes and a collection attribute.
 */
@Repository
public interface Ratings {

    @Insert
    void add(Rating rating);

    @Delete
    long clear();

    @Find
    Optional<Rating> get(int id);

    Stream<Rating> findByCommentsContainsOrderByIdDesc(String comment);

    Stream<Rating> findByItemPriceBetween(float min, float max, Sort<?>... sorts);

    @Query("SELECT comments WHERE ID=?1")
    Set<String> getComments(int id);

    @Find
    @OrderBy("item.price")
    @OrderBy("ID")
    Stream<Rating> search(int numStars);
}
