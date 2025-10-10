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
package test.jakarta.data.jpa.web;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository for testing some less common Jakarta Data basic types
 * that aren't tested anywhere else.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface Triangles extends BasicRepository<Triangle, Integer> {

    long deleteByHypotenuseNot(byte hypotenuseToKeep);

    int deleteByHypotenuseNull();

    @Query("SELECT sides FROM Triangle WHERE id(this) = :key")
    byte[] getSides(Integer key);

    @Query("SELECT sides FROM Triangle WHERE id(this) = ?1")
    Optional<byte[]> getSidesIfPresent(int key);

    @Query("UPDATE Triangle SET sides=?2, perimeter=?3 WHERE id(this)=?1")
    boolean resizePreservingHypotenuse(int key, byte[] newSides, short newPerimeter);

    @Query("SELECT t.sameLengthSides, length(t.sides) FROM Triangle t WHERE t.hypotenuse=?1")
    int[][] sidesInfo(byte hypotenuse);

    @Query("SELECT sides FROM Triangle WHERE hypotenuse >= :min AND hypotenuse <= :max")
    @OrderBy("hypotenuse")
    @OrderBy("distinctKey")
    byte[][] sidesWhereHypotenuseWithin(byte min, byte max);

    @Query("SELECT sides WHERE sameLengthSides = ?1")
    @OrderBy("hypotenuse")
    @OrderBy("distinctKey")
    List<byte[]> sidesWhereNumSidesEqual(short sameLengthSides);

    @Query("SELECT sides WHERE perimeter = :perimeter")
    Stream<byte[]> sidesWherePerimeter(short perimeter);

    @Find
    @OrderBy("distinctKey")
    List<Triangle> withPerimeter(short perimeter);
}