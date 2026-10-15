/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.hibernate;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map.Entry;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.v1_1.web.Fraction;
import test.jakarta.data.v1_1.web.Fractions;

@SuppressWarnings("serial")
@WebServlet("/Hibernate/*")
public class Data_1_1_HibernateServlet extends FATServlet {

    @Inject
    Fractions fractions;

    @Inject
    ResourceAccess resourceAccessor;

    @Resource
    UserTransaction tx;

    /**
     * Use a resource accessor method to obtain an EntityAgent from a stateless
     * repository. Use the EntityAgent to perform a query and assert that the
     * expected results are returned.
     */
    @Test
    public void testEntityAgentAccessor() {

        EntityAgent agent = resourceAccessor.agent();
        assertEquals(true,
                     agent.isOpen());

        // select all fractions that are equivalent to 1/2, ordered by name
        TypedQuery<Fraction> query = agent
                        .createQuery("""
                                        FROM Fraction
                                        WHERE denominator = 2 * numerator
                                        ORDER BY name
                                        """,
                                     Fraction.class);
        List<Fraction> results = query.getResultList();

        assertEquals(List.of("Eight Sixteenths",
                             "Five Tenths",
                             "Four Eighths",
                             "Nine Eighteenths",
                             "One Half",
                             "Seven Fourteenths",
                             "Six Twelfths",
                             "Ten Twentieths",
                             "Three Sixths",
                             "Two Fourths"),
                     results.stream()
                                     .map(f -> f.name)
                                     .toList());

        assertEquals(true,
                     agent.isOpen());
        agent.close();
        assertEquals(false,
                     agent.isOpen());
    }

    /**
     * Use a default method that accesses a resource accessor method to obtain
     * an EntityAgent from a stateless repository. Use the EntityAgent to
     * perform a query and assert that the expected results are returned.
     */
    @Test
    public void testEntityAgentUsedByDefaultMethod() {

        Entry<EntityAgent, List<Fraction>> entry = resourceAccessor
                        .runQuery("""
                                        FROM Fraction
                                        WHERE LOCATE(SUBSTRING(name, 1, 3),
                                                     SUBSTRING(name, 4))
                                                    > 0
                                        ORDER BY name
                                        """,
                                  Fraction.class);

        EntityAgent agent = entry.getKey();
        assertEquals(false,
                     agent.isOpen());

        List<Fraction> results = entry.getValue();

        assertEquals(List.of("Eight Eighteenths",
                             "Four Fourteenths",
                             "Nine Nineteenths",
                             "Seven Seventeenths",
                             "Six Sixteenths",
                             "Twelve Twentieths"),
                     results.stream()
                                     .map(f -> f.name)
                                     .toList());
    }

}
