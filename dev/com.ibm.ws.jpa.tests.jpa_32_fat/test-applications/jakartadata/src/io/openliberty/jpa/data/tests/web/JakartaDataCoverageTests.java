/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.data.tests.models.AsciiCharacter;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaDataCoverageTests")
public class JakartaDataCoverageTests extends FATServlet {

    @PersistenceContext(unitName = "RecreatePersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue("This test always passes", true);
    }

    @Test // Verifies that a JPQL query using an alias returns the correct hexadecimal value for a persisted AsciiCharacter
    public void testAsciiCharacterQueryReturnsHexadecimalWithAlias() {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        AsciiCharacter character = new AsciiCharacter();
        character.setId(id);
        character.setThisCharacter('P');
        character.setHexadecimal("50");
        character.setNumericValue(80);
        character.setControl(false);

        try {
            tx.begin();
            em.createQuery("DELETE FROM AsciiCharacter a WHERE a.thisCharacter = :char")
                            .setParameter("char", character.getThisCharacter())
                            .executeUpdate();
            em.persist(character);
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (SystemException se) {
                throw new RuntimeException("Rollback failed during testOLGH28913QueryHexadecimalWithAlias", se);
            }
            throw new RuntimeException("Transaction failed during testOLGH28913QueryHexadecimalWithAlias", e);
        }

        TypedQuery<String> query = em.createQuery(
                                                  "SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.thisCharacter = :char", String.class);
        query.setParameter("char", character.getThisCharacter());

        List<String> results = query.getResultList();

        assertNotNull("Query result should not be null", results);
        assertFalse("Query result should not be empty", results.isEmpty());
        assertTrue("Expected hexadecimal value not found in results", results.contains(character.getHexadecimal()));
    }

    @Test
    public void testAsciiCharacterInvalidFieldAccessWithoutAlias() {
        try {
            em.createQuery("SELECT nonExistentField FROM AsciiCharacter", String.class)
                            .getResultList();
            fail("Expected exception due to invalid field was not thrown");
        } catch (RuntimeException e) { // Includes PersistenceException
            String msg = e.getMessage();
            assertNotNull("Exception message should not be null", msg);
            assertTrue("Exception message should reference 'nonExistentField'", msg.contains("nonExistentField"));
        }
    }

    @Test
    public void testAsciiCharacterMultipleResultsQuery() {
        try {
            tx.begin();
            em.persist(AsciiCharacter.of(65)); // 'A'
            em.persist(AsciiCharacter.of(66)); // 'B'
            tx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            throw new RuntimeException("Transaction failed during testOLGH28913MultipleResultsQuery", e);
        }

        List<String> results = em.createQuery(
                                              "SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.hexadecimal IS NOT NULL", String.class)
                        .getResultList();

        assertTrue("Expected hex value 41 not found", results.contains("41")); // 65 in hex
        assertTrue("Expected hex value 42 not found", results.contains("42")); // 66 in hex
    }
}
