/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import java.util.LinkedList;
import java.util.stream.Stream;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

/**
 * Repository that has no primary entity type and allows queries for different entity classes.
 * Do not add any lifecycle methods.
 */
@Repository
public interface MixedRepository { // Do not inherit from a supertype

    @Query(value = "FROM Business WHERE location.address.zip = location.address.zip",
           count = "FROM Business")
    Page<Business> findAll(PageRequest<Business> pageRequest);

    @OrderBy("name")
    Business[] findByLocationAddressCity(String cityName);

    @OrderBy("stateName")
    Stream<City> findByName(String name);

    LinkedList<Unpopulated> findBySomethingStartsWith(String prefix);

    @Query(value = "WHERE location.address.city=?1",
           count = "FROM Business WHERE location.address.city=?1")
    Page<Business> locatedIn(String city, PageRequest<Business> pageRequest);

    @Query(value = "FROM Business WHERE location.address.city=:city AND location.address.state=:state")
    CursoredPage<Business> locatedIn(String city, String state, PageRequest<Business> pageRequest);
}
