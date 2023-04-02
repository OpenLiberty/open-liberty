/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.overridepersistencexml.testlogic;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OverridePersistenceXmlLogic extends AbstractTestLogic {

    /**
     * Simple test to ensure that a persistence property set in the server.xml, using the
     * "defaultProperties" configuration element, will be presented to EntityManager instances.
     *
     * <jpa>
     * <defaultProperties>
     * ...
     * </defaultProperties>
     * </jpa>
     *
     */
    public void testOverridePersistenceXml(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            String key = "javax.persistence.lock.timeout";
            String strvalue = "12345";
            Integer intvalue = 12345;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                Assert.assertTrue("The EntityManager properties map does not contain key '" + key + "'",
                                  em.getProperties().containsKey(key));
                Object propertyValue = em.getProperties().get(key);

                if (JPAPersistenceProvider.OPENJPA.equals(provider)) {
                    Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + intvalue + "'",
                                        intvalue, propertyValue);
                } else {
                    Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + strvalue + "'",
                                        strvalue, propertyValue);
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
