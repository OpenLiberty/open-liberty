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
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository for an unannotated entity with a record attribute
 * that should be interpreted as an embeddable.
 */
@Repository
public interface Participants extends DataRepository<Participant, Integer> {

    @Insert
    void add(Participant... p);

    @Query("SELECT name.first WHERE id = ?1")
    Optional<String> getFirstName(int id);

    @Delete
    long remove(@By("name.last") String lastName);

    @Find
    @OrderBy("name.first")
    @OrderBy("id")
    Stream<Participant> withSurname(@By("name.last") String lastName);
}