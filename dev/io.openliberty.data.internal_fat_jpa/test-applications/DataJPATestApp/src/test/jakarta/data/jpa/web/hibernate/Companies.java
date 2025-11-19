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
package test.jakarta.data.jpa.web.hibernate;

import jakarta.data.Order;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import test.jakarta.data.jpa.web.Business;

/**
 * Repository that includes Hibernate-specific queries for the Business entity.
 */
@Repository(dataStore = "java:app/env/data/DataStoreRef")
public interface Companies {

    @Query("FROM Business b SELECT b")
    CursoredPage<Business> all(PageRequest pageReq, Order<Business> order);

    @Query("FROM Business SELECT this ORDER BY name")
    Page<Business> alphabetized(PageRequest pageReq);

    @Query("WHERE location.address.city=:city SELECT this ORDER BY name DESC")
    Page<Business> inCity(String city, PageRequest pageReq);

    @Query("WHERE name like ?1 SELECT this")
    Page<Business> namedLike(String pattern,
                             Order<Business> order,
                             PageRequest pageReq);

    @Query("""
                      FROM Business
                     WHERE location.address.city=:city
                       AND location.address.street.direction LIKE '%W'
                    SELECT this
                     ORDER BY name DESC""")
    Page<Business> westOfBroadway(String city, PageRequest pageReq);

    @Query("FROM Business" +
           " WHERE location.address.street.name LIKE :pattern" +
           " SELECT name")
    @OrderBy("name")
    Page<String> withStreetNamePattern(String pattern, PageRequest pageReq);
}