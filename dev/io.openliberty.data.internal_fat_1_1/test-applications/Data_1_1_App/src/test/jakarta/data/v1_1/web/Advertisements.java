/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.data.constraint.In;
import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.First;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.Update;

/**
 * Repository for Inheritance, where the root entity is abstract,
 * and one of the entity subtypes is not referenced anywhere except
 * by a Find annotation and Query text.
 */
@Repository
public interface Advertisements extends DataRepository<Advertisement, Integer> {

    // Do not add any methods that directly reference the subtype Commercial

    @Insert
    Advertisement add(Advertisement ad);

    // Only valid for subtype Commercial, which has a showAt attribute
    @Query("UPDATE Commercial SET showAt = ?2 WHERE id = ?1")
    boolean changeTimeShown(int id, LocalDateTime newTime);

    @Update
    void redeploy(Billboard billboard);

    long removeBySponsorIn(List<String> sponsors);

    @Find
    @OrderBy(By.ID)
    List<Advertisement> sponsoredBy(String sponsor);

    // TODO write a test that actually uses this method after we implement @First, ...
    // For now, its only purpose is to make the Data provider aware that Commercial
    // is an entity class.
    @Find(Commercial.class)
    @First
    @Select("numSeconds")
    @OrderBy(value = "numSeconds", descending = true)
    Optional<Integer> longest(@By("sponsor") In<String> sponsors);

}
