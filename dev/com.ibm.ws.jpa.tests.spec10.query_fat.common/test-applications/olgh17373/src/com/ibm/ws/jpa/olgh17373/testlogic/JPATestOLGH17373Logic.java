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

package com.ibm.ws.jpa.olgh17373.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17373.model.SimpleEntityOLGH17373;
import com.ibm.ws.jpa.olgh17373.model.SimpleEntityOLGH17373_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH17373Logic extends AbstractTestLogic {

    public void testQueryCoalesceLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17373> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17373 t "
                                                                     + "WHERE t.itemString2 = "
                                                                     + "COALESCE (t.itemString1, 'Sample')", SimpleEntityOLGH17373.class);

            List<SimpleEntityOLGH17373> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            TypedQuery<String> query2 = em.createQuery(""
                                                       + "SELECT COALESCE (t.itemString2, 'Sample') FROM SimpleEntityOLGH17373 t ORDER BY t.itemInteger1 ASC", String.class);

            List<String> dto02 = query2.getResultList();
            Assert.assertNotNull(dto02);
            Assert.assertEquals(2, dto02.size());

            Assert.assertEquals("Sample", dto02.get(0));
            Assert.assertEquals("B", dto02.get(1));

            // test 1 equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17373> cquery = cb.createQuery(SimpleEntityOLGH17373.class);
            Root<SimpleEntityOLGH17373> root = cquery.from(SimpleEntityOLGH17373.class);
            cquery.select(root);

            Expression<String> coalesce = cb.coalesce(root.get(SimpleEntityOLGH17373_.itemString1), "Sample");
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17373_.itemString2), coalesce));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2 equivalent CriteriaBuilder
            cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cquery2 = cb.createQuery(String.class);
            root = cquery2.from(SimpleEntityOLGH17373.class);
            Expression<String> coalesce2 = cb.coalesce(root.get(SimpleEntityOLGH17373_.itemString2), "Sample");
            cquery2.multiselect(coalesce2);

            cquery2.orderBy(cb.asc(root.get(SimpleEntityOLGH17373_.itemInteger1)));

            query2 = em.createQuery(cquery2);
            dto02 = query2.getResultList();
            Assert.assertNotNull(dto02);
            Assert.assertEquals(2, dto02.size());

            Assert.assertEquals("Sample", dto02.get(0));
            Assert.assertEquals("B", dto02.get(1));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCoalesceParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17373> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17373 t "
                                                                     + "WHERE t.itemString2 = "
                                                                     + "COALESCE (t.itemString1, ?1)", SimpleEntityOLGH17373.class);
            query.setParameter(1, "Sample");

            List<SimpleEntityOLGH17373> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            TypedQuery<String> query2 = em.createQuery(""
                                                       + "SELECT COALESCE (t.itemString2, ?1) FROM SimpleEntityOLGH17373 t ORDER BY t.itemInteger1 ASC", String.class);
            query2.setParameter(1, "Sample");

            List<String> dto02 = query2.getResultList();
            Assert.assertNotNull(dto02);
            Assert.assertEquals(2, dto02.size());

            Assert.assertEquals("Sample", dto02.get(0));
            Assert.assertEquals("B", dto02.get(1));

            // test 1 equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17373> cquery = cb.createQuery(SimpleEntityOLGH17373.class);
            Root<SimpleEntityOLGH17373> root = cquery.from(SimpleEntityOLGH17373.class);
            cquery.select(root);

            ParameterExpression<String> checkParam1 = cb.parameter(String.class);
            Expression<String> coalesce = cb.coalesce(root.get(SimpleEntityOLGH17373_.itemString1), checkParam1);
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17373_.itemString2), coalesce));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, "Sample");
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2 equivalent CriteriaBuilder
            cb = em.getCriteriaBuilder();
            CriteriaQuery<String> cquery2 = cb.createQuery(String.class);
            root = cquery2.from(SimpleEntityOLGH17373.class);
            ParameterExpression<String> checkParam2 = cb.parameter(String.class);
            Expression<String> coalesce2 = cb.coalesce(root.get(SimpleEntityOLGH17373_.itemString2), checkParam2);
            cquery2.multiselect(coalesce2);

            cquery2.orderBy(cb.asc(root.get(SimpleEntityOLGH17373_.itemInteger1)));

            query2 = em.createQuery(cquery2);
            query2.setParameter(checkParam2, "Sample");
            dto02 = query2.getResultList();
            Assert.assertNotNull(dto02);
            Assert.assertEquals(2, dto02.size());

            Assert.assertEquals("Sample", dto02.get(0));
            Assert.assertEquals("B", dto02.get(1));
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
