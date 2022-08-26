/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh20890.testlogic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh20890.model.ReturnHolderOLGH2089;
import com.ibm.ws.jpa.olgh20890.model.SimpleEntityOLGH20890;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH20890Logic extends AbstractTestLogic {

    public void testNegFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);
        //TODO: Disable test until EclipseLink 3.0 or 3.1 is updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA31Feature()) && JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
            return;
        }

        // Test fails on OpenJPA
        if (JPAPersistenceProvider.OPENJPA.equals(provider)) {
            return;
        }

        int id = 1;

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("Clearing persistence context...");
            em.clear();

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            SimpleEntityOLGH20890 e1 = new SimpleEntityOLGH20890();
            e1.setId(id);
            e1.setValue(new BigDecimal("3.14"));

            System.out.println("Performing persist(" + e1 + ") operation");
            em.persist(e1);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            System.out.println("Clearing persistence context...");
            em.clear();

            try {
                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
                CriteriaQuery<ReturnHolderOLGH2089> criteriaQuery = criteriaBuilder.createQuery(ReturnHolderOLGH2089.class);
                Root<SimpleEntityOLGH20890> entityRoot = criteriaQuery.from(SimpleEntityOLGH20890.class);

                Collection<Selection<?>> selections = new ArrayList<Selection<?>>();

                Expression<BigDecimal> valExpr = entityRoot.get("value");

                selections.add(criteriaBuilder.sum(criteriaBuilder.<BigDecimal> selectCase()
                                .when(criteriaBuilder.equal(entityRoot.get("id"), 0), valExpr)
                                .otherwise(criteriaBuilder.neg(valExpr))));

                criteriaQuery.multiselect(selections.toArray(new Selection[] {}));

                List<ReturnHolderOLGH2089> retList = em.createQuery(criteriaQuery).getResultList();

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Throwable t) {
                throw t;
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing find(" + SimpleEntityOLGH20890.class + ", " + id + ") operation");
                SimpleEntityOLGH20890 remove_e1 = em.find(SimpleEntityOLGH20890.class, id);

                System.out.println("Performing remove(" + remove_e1 + ") operation");
                em.remove(remove_e1);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
