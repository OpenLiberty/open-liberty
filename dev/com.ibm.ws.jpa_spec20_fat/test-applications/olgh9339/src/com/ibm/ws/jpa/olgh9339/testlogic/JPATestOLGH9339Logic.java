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

package com.ibm.ws.jpa.olgh9339.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH9339Logic extends AbstractTestLogic {

    public void testCoalesceJPQLQueryWithNullParameterValue(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Derby does not support NULL values in COALESCE?
        //  Exception: java.sql.SQLSyntaxErrorException: Syntax error: Encountered "NULL"
        if (isDerby(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            String jpql = "select count(1) from SimpleEntityOLGH9339 s where s.str2 = 'Johanssen' and s.int1 = coalesce(s.int1, :val)";
            Query query = em.createQuery(jpql);
            query = query.setParameter("val", null); // deliberate null parameter value
            Long result = (Long) query.getSingleResult(); // query should still function

            Assert.assertNotNull("Query result should be non-null", result);
            Assert.assertEquals("Incorrect query results", new Long(2), result); // result value from db
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
