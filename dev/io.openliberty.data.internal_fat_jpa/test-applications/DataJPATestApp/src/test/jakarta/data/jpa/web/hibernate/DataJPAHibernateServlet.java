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

import static org.junit.Assert.assertEquals;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.jpa.web.Business;

/**
 * For tests that only run on the Hibernate Persistence provider.
 * Also creates a datastore reference to the persistence unit used for the common set of tests.
 */
@PersistenceUnit(name = "java:app/env/data/DataStoreRef",
                 unitName = "HibernatePersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/DataJPAEclipseLinkServlet")
public class DataJPAHibernateServlet extends FATServlet {

    @Inject
    Companies companies;

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM clause. Ensure that a count query can be inferred and
     * used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_FROM_SELECT() {

        Order<Business> sorts = Order.by(Sort.asc("location.address.zip"),
                                         Sort.asc("name"));
        PageRequest page1Req = PageRequest.ofSize(5);

        CursoredPage<Business> page1 = companies.all(page1Req, sorts);

        assertEquals(15, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM clause and also has an ORDER clause. Ensure that a count
     * query can be inferred and used to compute the correct total count of
     * results.
     */
    @Test
    public void testCountQuery_FROM_SELECT_ORDER() {

        PageRequest page1Req = PageRequest.ofSize(5);
        Page<Business> page1 = companies.alphabetized(page1Req);

        assertEquals(15, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM and WHERE clauses. Ensure that a count query can be
     * inferred and used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_FROM_WHERE_SELECT() {

        PageRequest pageReq = PageRequest.ofSize(2);
        Page<String> page1 = companies.withStreetNamePattern("%th %", pageReq);

        assertEquals(4, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM and WHERE clauses, but before the ORDER BY clause. Ensure
     * that a count query can be inferred and used to compute the correct total
     * count of results.
     */
    @Test
    public void testCountQuery_FROM_WHERE_SELECT_ORDER() {

        PageRequest pageReq = PageRequest.ofSize(8);
        Page<Business> page1 = companies.inCity("Rochester", pageReq);

        assertEquals(13, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after WHERE clause. Ensure that a count query can be inferred and used to
     * compute the correct total count of results.
     */
    @Test
    public void testCountQuery_WHERE_SELECT() {

        Order<Business> sorts = Order.by(Sort.desc("name"));
        PageRequest pageReq = PageRequest.ofSize(6);

        Page<Business> page1 = companies.namedLike("%____ ____%",
                                                   sorts,
                                                   pageReq);

        assertEquals(8, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after WHERE clause, but before the ORDER clause. Ensure that a count query
     * can be inferred and used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_WHERE_SELECT_ORDER() {

        PageRequest pageReq = PageRequest.ofSize(7);

        Page<Business> page1 = companies.westOfBroadway("Rochester",
                                                        pageReq);

        assertEquals(10, page1.totalElements());
    }

}
