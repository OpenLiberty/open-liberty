/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.data.Streamable;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.enterprise.concurrent.Asynchronous;

import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.comparison.In;
import io.openliberty.data.repository.comparison.StartsWith;
import io.openliberty.data.repository.update.Assign;

/**
 * This is a second repository interface for the Person entity,
 * where the focus is on returning CompletableFuture/CompletionStage
 * and experimenting with how generated repository method implementations
 * fit with asynchronous methods.
 */
@Repository
public interface Personnel {
    @Asynchronous
    @Update
    CompletionStage<Integer> changeSurnames(@By("lastName") String oldSurname,
                                            @By("ssn_id") @In List<Long> ssnList,
                                            @Assign("lastName") String newSurname);

    @Asynchronous
    CompletableFuture<Long> countByFirstNameStartsWith(String beginningOfFirstName);

    @Asynchronous
    void deleteByFirstName(String firstName);

    @Asynchronous
    CompletableFuture<Void> deleteById(long ssn);

    @Asynchronous
    @Delete
    CompletableFuture<Void> deleteMultiple(Person... people);

    @Asynchronous
    @Delete
    CompletableFuture<Integer> deleteSeveral(Stream<Person> people);

    @Asynchronous
    CompletionStage<List<Person>> findByLastNameOrderByFirstName(String lastName);

    @Asynchronous
    CompletableFuture<Person> findBySSN_Id(long ssn);

    @Asynchronous
    @Query("SELECT o.firstName FROM Person o WHERE o.lastName=?1 ORDER BY o.firstName")
    CompletableFuture<Stream<String>> firstNames(String lastName);

    @Asynchronous
    @Insert
    CompletableFuture<Void> insertAll(Person... people);

    @Asynchronous
    @Query("SELECT DISTINCT o.lastName FROM Person o ORDER BY o.lastName")
    CompletionStage<String[]> lastNames();

    @Find
    @Select("firstName")
    Streamable<String> namesThatStartWith(@By("firstName") @StartsWith String beginningOfFirstName);

    // An alternative to the above would be to make the Collector class a parameter
    // of the Paginated annotation, although this would rule out easily accessing the
    // various built-in collectors that are provided by Java's Collectors interface.

    @Asynchronous
    @Delete
    CompletableFuture<Long> removeAll();

    @Asynchronous
    @Save
    CompletableFuture<List<Person>> save(Person... p);

    @Update
    long setSurname(@By("ssn_id") long ssn, @Assign("lastName") String newSurname);

    @Asynchronous
    @Update
    CompletableFuture<Boolean> setSurnameAsync(long ssn_id, @Assign String lastName);
}
