/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.data.tests.models.AsciiCharacter;
import io.openliberty.jpa.data.tests.models.Coordinate;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaDataRecreate")
public class JakartaDataRecreateServlet extends FATServlet {

    @PersistenceContext(unitName = "RecreatePersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28912")
    public void testOLGH28912() throws Exception {
        Coordinate original = Coordinate.of("testOLGH28912", 10, 15f);
        UUID id = original.id;
        Coordinate result;

        tx.begin();

        try {
            em.persist(original);

            em.createQuery("UPDATE Coordinate SET x = :newX, y = y / :yDivisor WHERE id = :id") //FAILURE PARSING QUERY HERE
                            .setParameter("newX", 11)
                            .setParameter("yDivisor", 5)
                            .setParameter("id", id)
                            .executeUpdate();

            result = em.createQuery("SELECT Coordinate WHERE id = :id", Coordinate.class)
                            .setParameter("id", id)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * java.lang.IllegalArgumentException: An exception occurred while creating a query in EntityManager:
             * Exception Description: Syntax error parsing [UPDATE Coordinate SET x = :newX, y = y / :yDivisor WHERE id = :id].
             * [37, 38] The left expression is not an arithmetic expression.
             */
            throw e;
        }

        assertEquals(id, result.id);
        assertEquals(11, result.x, 0.001);
        assertEquals(5f, result.y, 0.001);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28913")
    public void testOLGH28913() throws Exception {
        AsciiCharacter character = AsciiCharacter.of(80); //P
        String result;

        tx.begin();

        try {
            em.persist(character);

            result = em.createQuery("SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1", String.class) //FAILURE PARSING QUERY HERE
                            .setParameter(1, character.getThisCharacter())
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * java.lang.IllegalArgumentException: An exception occurred while creating a query in EntityManager:
             * Exception Description: Problem compiling [SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1].
             * [7, 18] The identification variable 'hexadecimal' is not defined in the FROM clause.
             */
            throw e;
        }

        assertEquals(character.getHexadecimal(), result);
    }

}
