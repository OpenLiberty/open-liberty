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

package com.ibm.ws.jpa.query.jpql.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.query.jpql.model.SimpleJPQLEntity;
import com.ibm.ws.jpa.query.sqlcapture.SQLListener;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPQLTestLogic extends AbstractTestLogic {
    private boolean isSupportedDatabase(String dbProductName, String dbProductVersion, String jdbcDriverVersion) {
        return true;
    }

    public void test001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            String jpql = null;
            Query q = null;
            {
                System.out.println("JPQL Test #001 - 001");
                SQLListener.getAndClear();
                jpql = "SELECT e FROM SimpleJPQLEntity e WHERE e.int1 IN ?1";
                q = em.createQuery(jpql);
                List parms = new ArrayList();
                parms.add(42);

                q.setParameter(1, parms);

                System.out.println("Parm = { 42 } ");
                List resultList = q.getResultList();
                Assert.assertNotNull(resultList);
                Assert.assertEquals(1, resultList.size());

                SimpleJPQLEntity entity = (SimpleJPQLEntity) resultList.get(0);
                Assert.assertNotNull(entity);
                Assert.assertEquals(1, entity.getId());

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (lDbProductName.contains("derby") || lDbProductName.contains("db2")) {
                    String expected = "SELECT ID, BOOLEAN1, BOOLEAN2, BOOLEAN3, BYTE1, BYTE2, BYTE3, CHAR1, CHAR2, CHAR3, DOUBLE1, DOUBLE2, DOUBLE3, FLOAT1, FLOAT2, FLOAT3, INT1, INT2, INT3, LONG1, LONG2, LONG3, SHORT1, SHORT2, SHORT3, STR1, STR2, STR3 FROM SimpleJPQL WHERE (INT1 IN ?)";
                    Assert.assertEquals(expected, sql.get(0));
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

    public void template(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

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
