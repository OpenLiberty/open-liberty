/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh19185.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh19185.model.SimpleEntityOLGH19185;
import com.ibm.ws.jpa.olgh19185.model.SimpleEntityOLGH19185_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH19185Logic extends AbstractTestLogic {

    public void testQueryUpdateLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0 is updated to include the fix
        //TODO: Disable test until EclipseLink 2.7 is updated to include the fix
        if ((isUsingJPA22Feature() || isUsingJPA30Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 1
                int updt = em.createQuery("UPDATE SimpleEntityOLGH19185 t SET t.itemInteger1 = 9 WHERE t.itemString1 = 'A'").executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 1 equivalent CriteriaBuilder
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaUpdate<SimpleEntityOLGH19185> cquery = cb.createCriteriaUpdate(SimpleEntityOLGH19185.class);
                Root<SimpleEntityOLGH19185> root = cquery.from(SimpleEntityOLGH19185.class);

                cquery.set(root.get(SimpleEntityOLGH19185_.itemInteger1), 9);
                cquery.where(cb.equal(root.get(SimpleEntityOLGH19185_.itemString1), "A"));

                int updt = em.createQuery(cquery).executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 2 equivalent CriteriaBuilder
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaUpdate<SimpleEntityOLGH19185> cquery = cb.createCriteriaUpdate(SimpleEntityOLGH19185.class);
                Root<SimpleEntityOLGH19185> root = cquery.from(SimpleEntityOLGH19185.class);

                cquery.set("itemInteger1", 9);
                cquery.where(cb.equal(root.get("itemString1"), "A"));

                int updt = em.createQuery(cquery).executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
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

    public void testQueryUpdateParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0 is updated to include the fix
        //TODO: Disable test until EclipseLink 2.7 is updated to include the fix
        if ((isUsingJPA22Feature() || isUsingJPA30Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 1
                Query query = em.createQuery("UPDATE SimpleEntityOLGH19185 t SET t.itemInteger1 = ?1 WHERE t.itemString2 = ?2");
                query.setParameter(1, 9);
                query.setParameter(2, "B");

                int updt = query.executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 1 equivalent CriteriaBuilder
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaUpdate<SimpleEntityOLGH19185> cquery = cb.createCriteriaUpdate(SimpleEntityOLGH19185.class);
                Root<SimpleEntityOLGH19185> root = cquery.from(SimpleEntityOLGH19185.class);

                ParameterExpression<Integer> intValue = cb.parameter(Integer.class);
                ParameterExpression<String> strValue = cb.parameter(String.class);
                cquery.set(root.get(SimpleEntityOLGH19185_.itemInteger1), intValue);
                cquery.where(cb.equal(root.get(SimpleEntityOLGH19185_.itemString2), strValue));

                Query query = em.createQuery(cquery);
                query.setParameter(intValue, 9);
                query.setParameter(strValue, "B");

                int updt = query.executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            try {
                // test 2 equivalent CriteriaBuilder
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaUpdate<SimpleEntityOLGH19185> cquery = cb.createCriteriaUpdate(SimpleEntityOLGH19185.class);
                Root<SimpleEntityOLGH19185> root = cquery.from(SimpleEntityOLGH19185.class);

                ParameterExpression<Integer> intValue = cb.parameter(Integer.class);
                ParameterExpression<String> strValue = cb.parameter(String.class);
                cquery.set("itemInteger1", intValue);
                cquery.where(cb.equal(root.get("itemString2"), strValue));

                Query query = em.createQuery(cquery);
                query.setParameter(intValue, 9);
                query.setParameter(strValue, "B");

                int updt = query.executeUpdate();
                Assert.assertEquals(3, updt);
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
