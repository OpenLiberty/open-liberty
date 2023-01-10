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

package com.ibm.ws.jpa.olgh16970.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.junit.Assert;

import com.ibm.ws.jpa.olgh16970.model.SimpleEntityOLGH16970;
import com.ibm.ws.jpa.olgh16970.model.SimpleEntityOLGH16970_;
import com.ibm.ws.jpa.query.sqlcapture.SQLListener;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH16970Logic extends AbstractTestLogic {

    /**
     * Simple test of using literal values in JPQL.
     */
    public void testSimpleQuery1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            SQLListener.getAndClearCallList();

            // Test JPQL query
            Query jpqlQuery = em.createQuery("SELECT 'HELLO' FROM SimpleEntityOLGH16970 s");
            // Set parameter binding off so that we can validate the arguments
            jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            List<?> resultList = jpqlQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            List<String> sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO' FROM SIMPLEENTITYOLGH16970", sql.get(0));

            // Test equivalent criteriabuilder query
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            cquery.from(SimpleEntityOLGH16970.class);
            cquery.multiselect(cb.literal("HELLO"));

            Query criteriaQuery = em.createQuery(cquery);
            // Set parameter binding off so that we can validate the arguments
            criteriaQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            resultList = criteriaQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO' FROM SIMPLEENTITYOLGH16970", sql.get(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Simple test of using literal values in JPQL.
     */
    public void testSimpleQuery2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            SQLListener.getAndClearCallList();

            // Test JPQL query
            Query jpqlQuery = em.createQuery("SELECT 'HELLO', s.intVal1 FROM SimpleEntityOLGH16970 s");
            // Set parameter binding off so that we can validate the arguments
            jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            List<?> resultList = jpqlQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            List<String> sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO', INTVAL1 FROM SIMPLEENTITYOLGH16970", sql.get(0));

            // Test equivalent criteriabuilder query
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<SimpleEntityOLGH16970> root = cquery.from(SimpleEntityOLGH16970.class);
            cquery.multiselect(cb.literal("HELLO"), root.get(SimpleEntityOLGH16970_.intVal1));

            Query criteriaQuery = em.createQuery(cquery);
            // Set parameter binding off so that we can validate the arguments
            criteriaQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            resultList = criteriaQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO', INTVAL1 FROM SIMPLEENTITYOLGH16970", sql.get(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Simple test of using literal values in JPQL with parameters in the query.
     */
    public void testSimpleQuery3(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            SQLListener.getAndClearCallList();

            // Test JPQL query
            Query jpqlQuery = em.createQuery("SELECT 'HELLO' FROM SimpleEntityOLGH16970 s WHERE ?1 = ?2");
            jpqlQuery.setParameter(1, 1);
            jpqlQuery.setParameter(2, 1);
            // Set parameter binding off so that we can validate the arguments
            jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            List<?> resultList = jpqlQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            List<String> sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO' FROM SIMPLEENTITYOLGH16970 WHERE (1 = 1)", sql.get(0));

            // Test equivalent criteriabuilder query
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            cquery.from(SimpleEntityOLGH16970.class);
            cquery.multiselect(cb.literal("HELLO"));
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.equal(intParam1, intParam2));

            Query criteriaQuery = em.createQuery(cquery);
            criteriaQuery.setParameter(intParam1, 1);
            criteriaQuery.setParameter(intParam2, 1);
            // Set parameter binding off so that we can validate the arguments
            criteriaQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);
            resultList = criteriaQuery.getResultList();
            Assert.assertNotNull(resultList);
            Assert.assertEquals(2, resultList.size());

            sql = SQLListener.getAndClearCallList();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            Assert.assertEquals("SELECT 'HELLO' FROM SIMPLEENTITYOLGH16970 WHERE (1 = 1)", sql.get(0));
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
