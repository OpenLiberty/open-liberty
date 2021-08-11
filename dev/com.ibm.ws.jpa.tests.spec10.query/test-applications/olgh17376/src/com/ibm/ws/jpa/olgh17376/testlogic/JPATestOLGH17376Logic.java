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

package com.ibm.ws.jpa.olgh17376.testlogic;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17376.model.SimpleEntityOLGH17376;
import com.ibm.ws.jpa.olgh17376.model.SimpleEntityOLGH17376_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH17376Logic extends AbstractTestLogic {

    public void testQueryINLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN ('HELLO', 'ONE', 'WORLD', 'PEOPLE')", SimpleEntityOLGH17376.class);
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in("HELLO", "ONE", "WORLD", "PEOPLE"));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            In<String> in = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in.value("HELLO").value("ONE").value("WORLD").value("PEOPLE"));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 3
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            List<String> strCollection = Arrays.asList("HELLO", "ONE", "WORLD", "PEOPLE");
            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(strCollection));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryINParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN (?1, ?2, ?3, ?4)", SimpleEntityOLGH17376.class);
            query.setParameter(1, "HELLO");
            query.setParameter(2, "ONE");
            query.setParameter(3, "WORLD");
            query.setParameter(4, "PEOPLE");
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent JPQL 2
            query = em.createQuery(""
                                   + "SELECT t FROM SimpleEntityOLGH17376 t "
                                   + "WHERE t.itemString1 IN ?1", SimpleEntityOLGH17376.class);
            query.setParameter(1, Arrays.asList("HELLO", "ONE", "WORLD", "PEOPLE"));
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            ParameterExpression<String> strValue2 = cb.parameter(String.class);
            ParameterExpression<String> strValue3 = cb.parameter(String.class);
            ParameterExpression<String> strValue4 = cb.parameter(String.class);
            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(strValue1, strValue2, strValue3, strValue4));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(strValue2, "ONE");
            query.setParameter(strValue3, "WORLD");
            query.setParameter(strValue4, "PEOPLE");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            strValue1 = cb.parameter(String.class);
            strValue2 = cb.parameter(String.class);
            strValue3 = cb.parameter(String.class);
            strValue4 = cb.parameter(String.class);
            In<String> in = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in.value(strValue1).value(strValue2).value(strValue3).value(strValue4));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(strValue2, "ONE");
            query.setParameter(strValue3, "WORLD");
            query.setParameter(strValue4, "PEOPLE");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 3
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(colValue));

            query = em.createQuery(cquery);
            query.setParameter(colValue, Arrays.asList("HELLO", "ONE", "WORLD", "PEOPLE"));

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("ONE", dto01.get(0).getItemString1());
            Assert.assertEquals("TWO", dto01.get(0).getItemString2());
            Assert.assertEquals("THREE", dto01.get(0).getItemString3());
            Assert.assertEquals("FIVE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryINSUBQUERYLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN ("
                                                                     + "SELECT u.itemString1 FROM SimpleEntityOLGH17376 u "
                                                                     + "WHERE u.itemString2 = 'SEVEN')", SimpleEntityOLGH17376.class);
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            Subquery<String> subquery = cquery.subquery(String.class);
            Root<SimpleEntityOLGH17376> subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(cb.equal(subroot.get(SimpleEntityOLGH17376_.itemString2), "SEVEN"));

            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(subquery));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            subquery = cquery.subquery(String.class);
            subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(cb.equal(subroot.get(SimpleEntityOLGH17376_.itemString2), "SEVEN"));

            In<String> in2 = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in2.value(subquery));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryINSUBQUERYParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN ("
                                                                     + "SELECT u.itemString1 FROM SimpleEntityOLGH17376 u "
                                                                     + "WHERE u.itemString2 = ?2)", SimpleEntityOLGH17376.class);
            query.setParameter(2, "SEVEN");
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<String> subquery = cquery.subquery(String.class);
            Root<SimpleEntityOLGH17376> subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(cb.equal(subroot.get(SimpleEntityOLGH17376_.itemString2), strValue1));

            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "SEVEN");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            strValue1 = cb.parameter(String.class);
            subquery = cquery.subquery(String.class);
            subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(cb.equal(subroot.get(SimpleEntityOLGH17376_.itemString2), strValue1));

            In<String> in2 = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in2.value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "SEVEN");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryINSUBQUERYLiterals2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN ("
                                                                     + "SELECT u.itemString1 FROM SimpleEntityOLGH17376 u "
                                                                     + "WHERE u.itemString2 IN ('TEN', 'SEVEN', 'ELEVEN'))", SimpleEntityOLGH17376.class);
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            Subquery<String> subquery = cquery.subquery(String.class);
            Root<SimpleEntityOLGH17376> subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(subroot.get(SimpleEntityOLGH17376_.itemString2).in("TEN", "SEVEN", "ELEVEN"));

            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(subquery));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            subquery = cquery.subquery(String.class);
            subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            In<String> in = cb.in(subroot.get(SimpleEntityOLGH17376_.itemString2));
            subquery.where(in.value("TEN").value("SEVEN").value("ELEVEN"));

            In<String> in2 = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in2.value(subquery));

            query = em.createQuery(cquery);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryINSUBQUERYParameters2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TypedQuery<SimpleEntityOLGH17376> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17376 t "
                                                                     + "WHERE t.itemString1 IN ("
                                                                     + "SELECT u.itemString1 FROM SimpleEntityOLGH17376 u "
                                                                     + "WHERE u.itemString2 IN (?2, ?3, ?4))", SimpleEntityOLGH17376.class);
            query.setParameter(2, "TEN");
            query.setParameter(3, "SEVEN");
            query.setParameter(4, "ELEVEN");
            List<SimpleEntityOLGH17376> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 1
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17376> cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            Root<SimpleEntityOLGH17376> root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            ParameterExpression<String> strValue2 = cb.parameter(String.class);
            ParameterExpression<String> strValue3 = cb.parameter(String.class);
            Subquery<String> subquery = cquery.subquery(String.class);
            Root<SimpleEntityOLGH17376> subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            subquery.where(root.get(SimpleEntityOLGH17376_.itemString2).in(strValue1, strValue2, strValue3));

            cquery.where(root.get(SimpleEntityOLGH17376_.itemString1).in(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "TEN");
            query.setParameter(strValue2, "SEVEN");
            query.setParameter(strValue3, "ELEVEN");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder 2
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17376.class);
            root = cquery.from(SimpleEntityOLGH17376.class);
            cquery.select(root);

            strValue1 = cb.parameter(String.class);
            strValue2 = cb.parameter(String.class);
            strValue3 = cb.parameter(String.class);
            subquery = cquery.subquery(String.class);
            subroot = subquery.from(SimpleEntityOLGH17376.class);
            subquery.select(subroot.get(SimpleEntityOLGH17376_.itemString1));
            In<String> in = cb.in(subroot.get(SimpleEntityOLGH17376_.itemString2));
            subquery.where(in.value(strValue1).value(strValue2).value(strValue3));

            In<String> in2 = cb.in(root.get(SimpleEntityOLGH17376_.itemString1));
            cquery.where(in2.value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "TEN");
            query.setParameter(strValue2, "SEVEN");
            query.setParameter(strValue3, "ELEVEN");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("SIX", dto01.get(0).getItemString1());
            Assert.assertEquals("SEVEN", dto01.get(0).getItemString2());
            Assert.assertEquals("EIGHT", dto01.get(0).getItemString3());
            Assert.assertEquals("NINE", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(2), dto01.get(0).getItemInteger1());
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
