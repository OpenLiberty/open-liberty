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

package com.ibm.ws.jpa.olgh9339.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.testtooling.database.DatabaseVendor;
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
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);

        // Derby does not support the value 'NULL' in COALESCE
        //  Exception: java.sql.SQLSyntaxErrorException: Syntax error: Encountered "NULL"
        // DB2 does not support the value 'NULL' in COALESCE
        //  com.ibm.db2.jcc.am.SqlSyntaxErrorException: NULL IS NOT VALID IN THE CONTEXT WHERE IT IS USED. SQLCODE=-206, SQLSTATE=42703, DRIVER=4.20.30

        // Setting `eclipselink.jdbc.allow-partial-bind-parameters` resolves this issue, as it enables EclipseLink to know how to handle binding, but this test does not use that property
        if (isDerby || isDB2ZOS) {
            System.out.println("Skipping test; platform (" + dbProductName + ", " + dbProductVersion + ")");
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
