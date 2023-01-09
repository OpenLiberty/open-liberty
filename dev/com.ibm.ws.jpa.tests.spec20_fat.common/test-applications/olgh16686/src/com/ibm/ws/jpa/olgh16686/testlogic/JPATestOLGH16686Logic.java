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

package com.ibm.ws.jpa.olgh16686.testlogic;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh16686.model.ElemCollEmbedTemporalOLGH16686;
import com.ibm.ws.jpa.olgh16686.model.ElemCollEntityOLGH16686;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH16686Logic extends AbstractTestLogic {

    public void testJoinOnCollectionTable(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TransactionJacket tj = jpaResource.getTj();

            try {
                ElemCollEntityOLGH16686 newEntity = new ElemCollEntityOLGH16686();
                newEntity.setId(99);

                Map<Date, ElemCollEmbedTemporalOLGH16686> map = new HashMap<Date, ElemCollEmbedTemporalOLGH16686>();
                map.put(new Date(1), new ElemCollEmbedTemporalOLGH16686(new Date(System.currentTimeMillis() - 200000000)));
                newEntity.setMapKeyTemporalValueEmbed(new HashMap<Date, ElemCollEmbedTemporalOLGH16686>(map));

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing merge operation: " + newEntity);
                em.merge(newEntity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }

                System.out.println("Clearing persistence context");
                em.clear();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing merge operation: " + newEntity);
                em.merge(newEntity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
