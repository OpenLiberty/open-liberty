/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injectiondpu.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.injectiondpu.entities.DPUInjectionEntity;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
public class InjectionDPUTestLogic extends AbstractTestLogic {
    private boolean isSupportedDatabase(String dbProductName, String dbProductVersion, String jdbcDriverVersion) {
        return true;
    }

    public void testDefaultPersistenceUnitInjection(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
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

        if (!isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion)) {
            return;
        }

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Thread.sleep(50); // Sleep 50ms to ensure that the clock has ticked forward from the last test.
            long testPK = System.currentTimeMillis();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (isAMJTA) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of DPUInjectionEntity (id=" + testPK + ")...");
            DPUInjectionEntity new_entity = new DPUInjectionEntity();
            new_entity.setId(testPK);
            new_entity.setDataStr("Data: " + testPK);
            new_entity.setDataInt(42);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (isAMJTA) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding DPUInjectionEntity (id=" + testPK + ")...");
            DPUInjectionEntity find_entity = jpaResource.getEm().find(DPUInjectionEntity.class, testPK);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity));
            Assert.assertEquals(
                                "Assert that the entity's id is " + testPK,
                                find_entity.getId(),
                                testPK);

        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void template(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                         Object managedComponentObject) throws Throwable {
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

        if (!isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion)) {
            System.out.println("Test does not support database: " + dbProductName);
            return;
        }

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

        } finally {
            System.out.println(testName + ": End");
        }
    }
}
