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

package com.ibm.ws.jpa.olgh8461.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH8461Logic extends AbstractTestLogic {

    public void testSQLCastPropertyDetection(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.sessions.Session session = eclEm.getActiveSession();
                org.eclipse.persistence.platform.database.DatabasePlatform platform = session.getPlatform();

                //Test that EclipseLink applied the `eclipselink.jdbc.sql-cast` property
                Assert.assertTrue("Persistent property 'eclipselink.jdbc.sql-cast' did not apply", platform.isCastRequired());
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
