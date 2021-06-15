/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh10310.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh10310.model.SimpleEmbeddableOLGH10310;
import com.ibm.ws.jpa.olgh10310.model.SimpleEmbeddableOLGH10310Id;
import com.ibm.ws.jpa.olgh10310.model.SimpleEntityOLGH10310;
import com.ibm.ws.jpa.olgh10310.model.SimpleNestedEmbeddableOLGH10310;
import com.ibm.ws.jpa.olgh10310.model.SimpleNestedEmbeddableOLGH10310Id;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH10310Logic extends AbstractTestLogic {

    public void testOverrideColumnAggregateObjectMapping(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            TransactionJacket tj = jpaResource.getTj();

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("1) Performing find operation");
            SimpleEntityOLGH10310 t1 = em.find(SimpleEntityOLGH10310.class, new SimpleEmbeddableOLGH10310Id(1, new SimpleNestedEmbeddableOLGH10310Id(1)));
            Assert.assertNotNull("Assert find() did not return null.", t1);

            System.out.println("2) Updating the entity....");
            t1.setId2(new SimpleEmbeddableOLGH10310(2, new SimpleNestedEmbeddableOLGH10310(2)));

            System.out.println("3) Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
