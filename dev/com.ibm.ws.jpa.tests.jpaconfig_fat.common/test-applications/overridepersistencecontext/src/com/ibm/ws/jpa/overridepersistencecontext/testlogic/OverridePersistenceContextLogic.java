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

package com.ibm.ws.jpa.overridepersistencecontext.testlogic;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OverridePersistenceContextLogic extends AbstractTestLogic {

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
    public void testOverridePersistenceContext(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            String ovstrvalue = "34567";
            Integer ovintvalue = 34567;

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

                PersistenceContextType type = jpaResource.getPcCtxInfo().getPcType();

                // PersistenceContexts override the persistence property. On EclipseLink, that value overrides the JPAComponent configuration
                // PersistenceContexts override the persistence property. On Hibernate 2.2+, that value overrides the JPAComponent configuration
                if (PersistenceContextType.CONTAINER_MANAGED_TS.equals(type) || PersistenceContextType.CONTAINER_MANAGED_ES.equals(type)) {
                    switch (provider) {
                        case ECLIPSELINK:
                            Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + ovstrvalue + "'",
                                                ovstrvalue, propertyValue);
                            break;
                        case OPENJPA:
                            // OpenJPA returns an Integer value rather than a String value
                            Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + intvalue + "'",
                                                intvalue, propertyValue);
                            break;
                        case HIBERNATE:
                            // Behavior changed starting with Hibernate 2.2
                            if (isUsingJPA21ContainerFeature(false)) {
                                Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + strvalue + "'",
                                                    strvalue, propertyValue);
                            } else {
                                Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + ovstrvalue + "'",
                                                    ovstrvalue, propertyValue);
                            }
                            break;
                    }
                } else {
                    // PersistenceUnits should have the JPAComponent configuration persistence property value
                    switch (provider) {
                        case ECLIPSELINK:
                            Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + strvalue + "'",
                                                strvalue, propertyValue);
                            break;
                        case OPENJPA:
                            // OpenJPA returns an Integer value rather than a String value
                            Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + intvalue + "'",
                                                intvalue, propertyValue);
                            break;
                        case HIBERNATE:
                            Assert.assertEquals("The persistence property '" + key + "' contains '" + propertyValue + "' instead of '" + strvalue + "'",
                                                strvalue, propertyValue);
                            break;
                    }
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
