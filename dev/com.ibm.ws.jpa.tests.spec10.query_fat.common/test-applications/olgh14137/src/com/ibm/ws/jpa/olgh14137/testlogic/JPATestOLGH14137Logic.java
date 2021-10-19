/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh14137.testlogic;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh14137.model.OverrideEmbeddableOLGH14137;
import com.ibm.ws.jpa.olgh14137.model.OverrideEntityOLGH14137;
import com.ibm.ws.jpa.olgh14137.model.OverrideNestedEmbeddableOLGH14137;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH14137Logic extends AbstractTestLogic {

    /**
     * This test is to validate that the Collection table was created with the correct attribute
     * override column name.
     */
    public void testOverrideLowerCaseElementCollectionObjectMapping(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        Integer id = new Integer(41);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            TransactionJacket tj = jpaResource.getTj();

            // Validate that the tables were generated with the correct column names
            Query query = em.createNativeQuery("select t0.b_id from override_entity_b t0");
            query.getResultList();

            // CREATE TABLE ct_override_entity_b (ct_b_override_value INTEGER, value2 INTEGER, ct_b_override_nested_value INTEGER, nested_value2 INTEGER, entity_b_ct_entity_b INTEGER)
            query = em.createNativeQuery("select t0.ct_b_override_value, t0.value2, t0.ct_b_override_nested_value, t0.entity_b_ct_entity_b, t0.nested_value2 from ct_override_entity_b t0");
            query.getResultList();

            OverrideEmbeddableOLGH14137 emb1 = new OverrideEmbeddableOLGH14137(43, 44, new OverrideNestedEmbeddableOLGH14137(45, 46));
            OverrideEmbeddableOLGH14137 emb2 = new OverrideEmbeddableOLGH14137(47, 48, new OverrideNestedEmbeddableOLGH14137(49, 50));
            OverrideEmbeddableOLGH14137 emb3 = new OverrideEmbeddableOLGH14137(51, 52, new OverrideNestedEmbeddableOLGH14137(53, 54));
            Set<OverrideEmbeddableOLGH14137> set = new HashSet<OverrideEmbeddableOLGH14137>(Arrays.asList(emb1, emb2, emb3));

            OverrideEntityOLGH14137 ent = new OverrideEntityOLGH14137(id, set);

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist operation: " + ent);
            em.persist(ent);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing merge operation: " + ent);
            ent = em.merge(ent);

            System.out.println("Performing persist operation: " + ent);
            em.remove(ent);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
