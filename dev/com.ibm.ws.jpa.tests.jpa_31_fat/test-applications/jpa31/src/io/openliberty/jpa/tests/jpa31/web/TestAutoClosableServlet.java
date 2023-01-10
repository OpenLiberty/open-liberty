/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.jpa.tests.jpa31.web;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/TestAutoClosableServlet")
public class TestAutoClosableServlet extends JPADBTestServlet {
    private static final long serialVersionUID = 1L;

    private final static String PUNAME = "AutoClosable";

    @PersistenceUnit(unitName = PUNAME + "_JTA")
    private EntityManagerFactory emfJta;

    @PersistenceUnit(unitName = PUNAME + "_RL")
    private EntityManagerFactory emfRl;

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testEntityManagerAutoClosable_JTA() {
        EntityManager tem = null;

        try {
            EntityManager em_test = emfJta.createEntityManager();
            Assert.assertNotNull(em_test);
            Assert.assertTrue(em_test.isOpen());
            tem = em_test;

            try (em_test) {
                // no-op
            }

            Assert.assertFalse(em_test.isOpen());
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    @Test
    public void testEntityManagerAutoClosable_RL() {
        EntityManager tem = null;

        try {
            EntityManager em_test = emfRl.createEntityManager();
            Assert.assertNotNull(em_test);
            Assert.assertTrue(em_test.isOpen());
            tem = em_test;

            try (em_test) {
                // no-op
            }

            Assert.assertFalse(em_test.isOpen());
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    @Test
    public void testEntityManagerAutoClosable_CMTS() {
        Assert.assertNotNull(em);
        Assert.assertTrue(em.isOpen());

        try {
            EntityManager tem = em;
            try (tem) {
                // No op
            }
            Assert.fail("Container managed EntityManager did not throw Exception.");
        } catch (java.lang.IllegalStateException t) {
            // Container managed EntityManager should not permit em.close() to be called.
        }
    }

}
