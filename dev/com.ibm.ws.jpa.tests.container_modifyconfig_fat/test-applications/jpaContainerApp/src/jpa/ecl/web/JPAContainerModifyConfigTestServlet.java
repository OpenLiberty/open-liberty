/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package jpa.ecl.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import componenttest.app.FATServlet;
import jpa.entity.Person;

@SuppressWarnings("serial")
@WebServlet("JPAContainerModifyConfigTestServlet")
public class JPAContainerModifyConfigTestServlet extends FATServlet {

    @Resource
    UserTransaction tx;

    @Resource(name = "java:comp/env/jdbc/ds", lookup = "jdbc/ds")
    DataSource ds;

    @PersistenceContext(unitName = "PU_datasource")
    EntityManager em;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String id = request.getParameter("id");
        final String test = request.getParameter(FATServlet.TEST_METHOD);
        final String invokedBy = request.getParameter("invokedBy");
        System.out.println("BEGIN " + test + " #" + id + "  (invoked by " + invokedBy + ')');
        try {
            super.doGet(request, response);
        } finally {
            System.out.println("END " + test + " #" + id + "  (invoked by " + invokedBy + ')');
        }
    }

    /**
     * A simple insert/delete JPA test that cleans up after itself so it can be ran multiple times
     */
    public void testInsertDelete() throws Exception {
        String name = "A";

        // Insert
        tx.begin();
        Person p = new Person(name);
        em.persist(p);
        tx.commit();

        // Delete
        tx.begin();
        Person found = em.find(Person.class, name);
        assertEquals(name, found.getName());
        em.remove(found);
        Person deleted = em.find(Person.class, name);
        tx.commit();
        assertNull(deleted);
    }
}
