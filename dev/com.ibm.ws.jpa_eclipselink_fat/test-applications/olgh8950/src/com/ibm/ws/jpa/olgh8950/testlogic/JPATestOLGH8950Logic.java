/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh8950.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh8950.model.SimpleMappingEmbeddableOLGH8950;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH8950Logic extends AbstractTestLogic {
    private boolean isSupportedDatabase(String dbProductName, String dbProductVersion, String jdbcDriverVersion) {
        return true;
    }

    public void testEclipseLinkCursorWithAggregateObjectMapping(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        if (isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion) == false) {
            // This test does not currently support the target database vendor.
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                Query query = em.createQuery("SELECT e.id, e.mappingField1, e.aggregateObjectMapping FROM SimpleMappingEntityOLGH8950 e");
                query.setHint(org.eclipse.persistence.config.QueryHints.CURSOR, org.eclipse.persistence.config.HintValues.TRUE);

                org.eclipse.persistence.queries.Cursor cursor = (org.eclipse.persistence.queries.Cursor) query.getSingleResult();

                Assert.assertTrue("Cursor was empty", cursor.hasNext());
                Object[] results = (Object[]) cursor.next();
                Assert.assertArrayEquals(new Object[] { 101L, 1001, new SimpleMappingEmbeddableOLGH8950("SimpleMappingEmbeddable1", "SimpleMappingEmbeddable2") }, results);
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
