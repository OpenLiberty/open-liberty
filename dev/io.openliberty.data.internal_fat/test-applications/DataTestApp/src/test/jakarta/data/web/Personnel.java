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
package test.jakarta.data.web;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.concurrent.Asynchronous;

import io.openliberty.data.Data;
import io.openliberty.data.Delete;
import io.openliberty.data.Update;
import io.openliberty.data.Where;

/**
 * This is a second repository interface for the Person entity,
 * where the focus is on returning CompletableFuture/CompletionStage
 * and experimenting with how generated repository method implementations
 * fit with asynchronous methods.
 */
@Data(Person.class) // TODO infer the entity class?
public interface Personnel {
    @Asynchronous
    @Update("o.lastName = ?2")
    @Where("o.lastName = ?1 AND o.ssn IN ?3")
    CompletionStage<Long> changeSurnames(String oldSurname, String newSurname, List<Long> ssnList);

    @Asynchronous
    CompletionStage<List<Person>> findByLastNameOrderByFirstName(String lastName);

    @Asynchronous
    @Delete
    CompletableFuture<Long> removeAll();

    @Asynchronous
    CompletableFuture<List<Person>> save(Person... p);
}
