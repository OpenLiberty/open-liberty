/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.Sort;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.persistence.EntityManager;

/**
 * Repository for an entity with embeddables that are Java records.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface Segments {

    @Save
    Segment addOrModify(Segment s);

    // starting West of
    long countByPointAXLessThan(int xExclusiveMax);

    @Query("WHERE pointB.y < :yExclusiveMax ORDER BY pointB.y ASC, id ASC")
    Stream<Segment> endingSouthOf(int yExclusiveMax);

    @Delete
    long erase();

    @Query("WHERE (pointA.x - pointB.x) * (pointA.x - pointB.x)" +
           "    + (pointA.y - pointB.y) * (pointA.y - pointB.y)" +
           "    > :len * :len")
    List<Segment> longerThan(@Param("len") int length,
                             Sort<?>... sortBy);

    EntityManager manager();

    @Delete
    long removeStartingAt(@By("pointA.x") int x,
                          @By("pointA.y") int y);

    @Query("SELECT pointB WHERE id=?1")
    Optional<Point> terminalPoint(long id);
}
