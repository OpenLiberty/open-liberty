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

package com.ibm.ws.jpa.olgh17407.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17407.model.SimpleEntityOLGH17407;
import com.ibm.ws.jpa.olgh17407.model.SimpleEntityOLGH17407_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH17407Logic extends AbstractTestLogic {

    public void testQueryHavingLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            // test 1
            TypedQuery<String> query = em.createQuery(""
                                                      + "SELECT t.itemString1 FROM SimpleEntityOLGH17407 t "
                                                      + "GROUP BY t.itemString1 HAVING COUNT(t.itemString1) > 2", String.class);

            List<String> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());
            Assert.assertEquals("A", dto01.get(0));

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cquery = cb.createQuery(String.class);
            Root<SimpleEntityOLGH17407> root = cquery.from(SimpleEntityOLGH17407.class);
            cquery.multiselect(root.get(SimpleEntityOLGH17407_.itemString1));

            cquery.groupBy(root.get(SimpleEntityOLGH17407_.itemString1));
            cquery.having(cb.greaterThan(cb.count(root.get(SimpleEntityOLGH17407_.itemString1)), 2L));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());
            Assert.assertEquals("A", dto01.get(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryHavingParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            // test 1
            TypedQuery<String> query = em.createQuery(""
                                                      + "SELECT t.itemString1 FROM SimpleEntityOLGH17407 t "
                                                      + "GROUP BY t.itemString1 HAVING COUNT(t.itemString1) > ?1", String.class);
            query.setParameter(1, 2);

            List<String> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());
            Assert.assertEquals("A", dto01.get(0));

            // equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cquery = cb.createQuery(String.class);
            Root<SimpleEntityOLGH17407> root = cquery.from(SimpleEntityOLGH17407.class);
            cquery.multiselect(root.get(SimpleEntityOLGH17407_.itemString1));

            ParameterExpression<Long> checkParam1 = cb.parameter(Long.class);
            cquery.groupBy(root.get(SimpleEntityOLGH17407_.itemString1));
            cquery.having(cb.greaterThan(cb.count(root.get(SimpleEntityOLGH17407_.itemString1)), checkParam1));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 2L);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());
            Assert.assertEquals("A", dto01.get(0));
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
