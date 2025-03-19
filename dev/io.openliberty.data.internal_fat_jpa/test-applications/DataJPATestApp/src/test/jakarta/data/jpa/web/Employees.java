/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository that infers its primary entity type from the entity result class of find operations.
 * Do not add add a superinterface for this class, and do not add any lifecycle methods.
 * (A save method for Employee can be found on the Businesses repository)
 */
@Repository
public interface Employees {

    void deleteByLastName(String lastName);

    @OrderBy(value = "badge.accessLevel", ignoreCase = true)
    @OrderBy("empNum")
    // The parameter would more naturally be char because the entity attribute
    // has type char, but JPA only allows String for LOWER(string_expression)
    Stream<Employee> findByBadgeAccessLevelIgnoreCaseGreaterThan(String exclusiveMin);

    Employee findByBadgeNumber(long badgeNumber);

    Employee findByEmpNum(long employeeNumber);

    @OrderBy("badge.number")
    Stream<Employee> findByFirstNameLike(String pattern);

    @OrderBy("id")
    Stream<Employee> findByFirstNameStartsWith(String prefix);

    List<Employee> findByFirstNameStartsWithOrderByEmpNumDesc(String prefix);

    Stream<Employee> findByFirstNameStartsWithOrderByIdDesc(String prefix);

    @OrderBy("badge.number")
    @OrderBy("badge.accessLevel")
    Stream<Badge> findByLastName(String lastName);

    // "IN" is not supported for embeddables, but EclipseLink generates SQL that leads to an SQLDataException rather than rejecting outright
    @Query("SELECT e FROM Employee e WHERE e.badge IN ?1")
    List<Employee> withBadge(Iterable<Badge> badges);

    @Find
    Optional<Employee> withId(@By("id") String id);
}