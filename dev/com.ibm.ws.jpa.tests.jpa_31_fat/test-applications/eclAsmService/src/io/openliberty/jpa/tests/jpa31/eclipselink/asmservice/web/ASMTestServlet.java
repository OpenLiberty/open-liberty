/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.jpa.tests.jpa31.eclipselink.asmservice.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.openliberty.jpa.tests.jpa31.eclipselink.asmservice.models.ASMTestServiceEntity;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/ASMTestServlet")
public class ASMTestServlet extends HttpServlet {
    @PersistenceUnit(unitName = "ASMTestPu")
    private EntityManagerFactory emf;

    @Resource
    private UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        EntityManager em = emf.createEntityManager();
        ServletOutputStream out = resp.getOutputStream();

        Set<String> expectedInterfaces = new HashSet<String>();
        expectedInterfaces.add("org.eclipse.persistence.internal.weaving.PersistenceWeaved");
        expectedInterfaces.add("org.eclipse.persistence.internal.descriptors.PersistenceEntity");
        expectedInterfaces.add("org.eclipse.persistence.internal.descriptors.PersistenceObject");
        expectedInterfaces.add("org.eclipse.persistence.queries.FetchGroupTracker");
        expectedInterfaces.add("org.eclipse.persistence.internal.weaving.PersistenceWeavedFetchGroups");
        expectedInterfaces.add("org.eclipse.persistence.descriptors.changetracking.ChangeTracker");
        expectedInterfaces.add("org.eclipse.persistence.internal.weaving.PersistenceWeavedChangeTracking");

        try {
            ASMTestServiceEntity newEntity = new ASMTestServiceEntity();
            newEntity.setStrData("Some data");

            tx.begin();
            em.joinTransaction();
            em.persist(newEntity);;
            tx.commit();

            em.clear();

            ASMTestServiceEntity findEntity = em.find(ASMTestServiceEntity.class, newEntity.getId());
            Assert.assertNotNull(findEntity);

            // Now Verify Enhancement
            System.out.println("Verifying Enhancement");
            Class entityClass = ASMTestServiceEntity.class;
            Class[] ifaces = entityClass.getInterfaces();
            if (ifaces != null && ifaces.length > 0) {
                System.out.println("<br>Interfaces:");
                for (Class i : ifaces) {
                    System.out.println("<br>     " + i.getName());
                    expectedInterfaces.remove(i.getName());
                }

                // The set expectedInterfaces should be empty if all of the expected interfaces were found.
                Assert.assertEquals("Assert that all expected interfaces were found", 0, expectedInterfaces.size());
            } else {
                Assert.fail("No Interfaces Found for " + entityClass + ", which means it has not been enhanced.");
            }

            out.println("[TEST PASSED]");
        } catch (Throwable t) {
            t.printStackTrace();
            out.println("TEST FAILED: \n" + t);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
