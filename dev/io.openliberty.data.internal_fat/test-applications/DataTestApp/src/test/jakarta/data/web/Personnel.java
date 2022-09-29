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
import java.util.function.Consumer;
import java.util.stream.Collector;

import jakarta.data.Delete;
import jakarta.data.Limit;
import jakarta.data.Paginated;
import jakarta.data.Select;
import jakarta.data.Update;
import jakarta.data.Where;
import jakarta.data.repository.Repository;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This is a second repository interface for the Person entity,
 * where the focus is on returning CompletableFuture/CompletionStage
 * and experimenting with how generated repository method implementations
 * fit with asynchronous methods.
 */
@Repository
public interface Personnel {
    @Asynchronous
    @Update("o.lastName = ?2")
    @Where("o.lastName = ?1 AND o.ssn IN ?3")
    CompletionStage<Long> changeSurnames(String oldSurname, String newSurname, List<Long> ssnList);

    @Asynchronous
    CompletionStage<List<Person>> findByLastNameOrderByFirstName(String lastName);

    @Asynchronous
    @Select("firstName")
    void findByLastNameOrderByFirstNameDesc(String lastName, Consumer<String> callback);

    @Asynchronous
    @Paginated(4)
    CompletableFuture<Void> findByOrderBySsnDesc(Consumer<Person> callback);

    @Asynchronous
    @Limit(1) // indicates single result (rather than list) for the completion stage
    CompletableFuture<Person> findBySsn(long ssn);

    @Asynchronous
    @Select("firstName")
    @Where("o.firstName LIKE CONCAT(?1, '%')")
    @Paginated(3)
    CompletableFuture<Long> namesThatStartWith(String beginningOfFirstName,
                                               Collector<String, ?, Long> collector);

    // An alternative to the above would be to make the Collector class a parameter
    // of the Paginated annotation, although this would rule out easily accessing the
    // various built-in collectors that are provided by Java's Collectors interface.

    @Asynchronous
    @Delete
    CompletableFuture<Long> removeAll();

    @Asynchronous
    CompletableFuture<List<Person>> save(Person... p);

    @Update("o.lastName = ?1")
    @Where("o.ssn = ?2")
    long setSurname(String newSurname, long ssn);

    @Asynchronous
    @Update("o.lastName = ?1")
    @Where("o.ssn = ?2")
    CompletableFuture<Long> setSurnameAsync(String newSurname, long ssn);
}
