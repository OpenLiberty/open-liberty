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
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.UUIDIdEntity;
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

    /**
     *
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a2202
     * Primary key values generated using the SEQUENCE, TABLE, or UUID strategy are
     * available in the PrePersist method. Primary key values generated using the
     * IDENTITY strategy are not available in the PrePersist method
     *
     * @throws Exception
     */
    @Test
    public void testPrimaryKeyAvailabilityInUUIDGenerationType() throws Exception {
        UUIDIdEntity uuiIdEntity = UUIDIdEntity.of("uuid entity 1");
        tx.begin();
        try {
            em.persist(uuiIdEntity);
            tx.commit();
        } catch (IllegalStateException e) {
            System.out.println("testPrimaryKeyAvailabilityInUUIDGenerationType: Exception occured while persisting: " + e.getMessage());
            fail("ID not available in PrePersist method. Primary key values generated using the UUID strategy are expected to be available in the PrePersist method");
        } catch (Exception e) {
            System.out.println("testPrimaryKeyAvailabilityInUUIDGenerationType: Unexpected Exception occured while persisting: " + e.getMessage());
            fail("Unexpected Exception occured while persisting uuiIdEntity");
        }
    }

}
