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

package com.ibm.ws.jpa.olgh14457.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh14457.model.SimpleEntityOLGH14457;
import com.ibm.ws.jpa.olgh14457.model.SimpleEntityOLGH14457_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH14457Logic extends AbstractTestLogic {

    public void testCaseExpressionReturnType(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            TypedQuery<Boolean> query = em.createQuery("SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = 1 THEN TRUE "
                                                       + "ELSE FALSE "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH14457 t ORDER BY t.itemInteger1 ASC", Boolean.class);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());
            Assert.assertEquals(true, boolList.get(0));
            Assert.assertEquals(false, boolList.get(1));

            // test equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Boolean> cquery = cb.createQuery(Boolean.class);
            Root<SimpleEntityOLGH14457> root = cquery.from(SimpleEntityOLGH14457.class);

            SimpleCase<Integer, Boolean> selectCase = cb.selectCase(root.get(SimpleEntityOLGH14457_.itemInteger1));
            selectCase.when(1, true)
                            .otherwise(false);

            cquery.select(selectCase);
            cquery.orderBy(cb.asc(root.get(SimpleEntityOLGH14457_.itemInteger1)));

            query = em.createQuery(cquery);

            boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());
            Assert.assertEquals(true, boolList.get(0));
            Assert.assertEquals(false, boolList.get(1));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCaseExpressionReturnTypeParameter(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            TypedQuery<Boolean> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                                       + "ELSE ?3 "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH14457 t ORDER BY t.itemInteger1 ASC", Boolean.class);
            query.setParameter(1, 1);
            query.setParameter(2, true);
            query.setParameter(3, false);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());
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
