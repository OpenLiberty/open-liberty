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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.Prime;
import io.openliberty.jpa.persistence.tests.models.RomanNumeral;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence32DataRecreate")
public class JakartaPersistenceDataRecreateServlet extends FATServlet {

    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    @Ignore
    public void testOLGH30501ConstructorWithArrayListShouldFail() throws Exception {
        // Delete all entities of type Prime
        deleteAllEntities(Prime.class);

        em.getTransaction().begin();
        em.persist(Prime.of(2, "II", "two"));
        em.getTransaction().commit();

        // Expect a PersistenceException due to invalid constructor expression using ArrayList
        try {
            em.createQuery(
                           "SELECT NEW io.openliberty.jpa.data.tests.models.RomanNumeral(" +
                           "name, romanNumeral, CAST(COLLECT(romanNumeralSymbols) AS java.util.ArrayList)) " +
                           "FROM Prime WHERE numberId <= :num GROUP BY name, romanNumeral", RomanNumeral.class)
                            .setParameter("num", 2)
                            .getResultList();

            // If no exception is thrown, the test should fail
            fail("Expected PersistenceException due to invalid constructor expression using ArrayList");
        } catch (PersistenceException e) {
            // Ensure an exception was thrown
            assertNotNull("Exception", e);
            assertTrue("Constructor or Query issue", e.getMessage().toLowerCase().contains("constructor") || e.getMessage().toLowerCase().contains("query"));
        }
    }

    /**
     * Utility method to drop all entities from table.
     *
     * Order to tests is not guaranteed and thus we should be pessimistic and
     * delete all entities when we reuse an entity between tests.
     *
     * @param clazz - the entity class
     */
    private void deleteAllEntities(Class<?> clazz) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + clazz.getSimpleName())
                        .executeUpdate();
        tx.commit();
    }

}
