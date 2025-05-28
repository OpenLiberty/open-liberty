/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence32")
public class JakartaPersistenceServlet extends FATServlet {
    @PersistenceContext(unitName = "JakartaPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testSetOperationsJPQL() {
        // UNION
        List<String> unionResult = em.createQuery(
                                                  "SELECT p.name FROM Person p " +
                                                  "UNION " +
                                                  "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(unionResult);

        // INTERSECT
        List<String> intersectResult = em.createQuery(
                                                      "SELECT p.name FROM Person p " +
                                                      "INTERSECT " +
                                                      "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(intersectResult);

        // EXCEPT
        List<String> exceptResult = em.createQuery(
                                                   "SELECT p.name FROM Person p " +
                                                   "EXCEPT " +
                                                   "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(exceptResult);
    }
}
