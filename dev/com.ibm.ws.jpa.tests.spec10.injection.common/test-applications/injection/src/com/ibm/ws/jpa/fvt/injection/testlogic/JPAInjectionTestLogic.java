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

package com.ibm.ws.jpa.fvt.injection.testlogic;

import java.util.List;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.injection.entities.core.CoreInjectionEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPAInjectionTestLogic extends AbstractTestLogic {
    public void testInjectionTarget(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testInjectionTarget: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Injection Pattern
        List<JPAInjectionTestTarget> targetPattern = null;
        String expectedInjectionPattern = (String) testExecCtx.getProperties().get("expected.injection.pattern");
        if ("EJB_NOOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EJB_NOOVERRIDE;
        } else if ("EJB_YESOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EJB_YESOVERRIDE;
        } else if ("WEB_NOOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.WEB_NOOVERRIDE;
        } else if ("WEB_YESOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.WEB_YESOVERRIDE;
        } else if ("EARLIB_NOOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EARLIB_NOOVERRIDE;
        } else if ("EARLIB_YESOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EARLIB_YESOVERRIDE;
        } else if ("EARROOT_NOOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EARROOT_NOOVERRIDE;
        } else if ("EARROOT_YESOVERRIDE".equalsIgnoreCase(expectedInjectionPattern)) {
            targetPattern = JPAInjectionTestTargetList.EARROOT_YESOVERRIDE;
        } else {
            Assert.fail("Invalid injection pattern specified ('" + targetPattern + "').  Cannot execute the test.");
            return;
        }

//        Integer pkMultiplier = new Integer((String) testExecCtx.getProperties().get("Injection.PK.Multiplier"));

        // Execute Test Case
        try {
            System.out.println("JPAInjectionTestLogic.testInjectionTarget(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("********************************************************************************");
            System.out.println("Target Pattern: " + expectedInjectionPattern);
            for (JPAInjectionTestTarget testTarget : targetPattern) {
                System.out.println(testTarget.getEntity() + " = " + testTarget.isEnabledEntity());
            }

            int pkIndex = 1;
            for (JPAInjectionTestTarget target : targetPattern) {
                System.out.println("********************************************************************************");
                System.out.println("Testing Entity " + target.getEntity() + ", Is-Enabled: " + target.isEnabledEntity());

                // Calculate the PK to use for this entity instance
                int pk = pkIndex++; // 1; // pkMultiplier.intValue() * JPAInjectionEntityEnum.values().length + target.getEntity().ordinal();
                String strData = target.getEntity().getEntityName() + " " + pk;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Constructing a new instance of " + target.getEntity().getEntityClassName() + "...");
                CoreInjectionEntity entity = (CoreInjectionEntity) constructNewEntityObject(target.getEntity());
                entity.setId(pk);
                entity.setStrData(strData);

                // Try to persist the entity.  If the Persistence Unit supports the target entity, then the persist
                // method should not throw an IllegalArgumentException.  If this is a Persistence Unit that doesn't
                // support the target entity, then an IllegalArgumentException should be thrown.
                try {
                    jpaResource.getEm().persist(entity);

                    // No Exception was thrown by the em.persist() call.  Check if that was the right result
                    if (target.isEnabledEntity() == true) {
                        // This is the expected result
//                        log.assertPass(
//                                       "Validated that Entity " + target.getEntity() +
//                                       " was permitted persistence with the EntityManager's persistence unit.");
                    } else {
                        // Nope, this should not have been permitted.
                        Assert.fail("The Entity " + target.getEntity() + " was unexpectedly granted persistence.");
                    }

                    // Commit the transaction
                    jpaResource.getTj().commitTransaction();
                } catch (IllegalArgumentException iae) {
                    if (target.isEnabledEntity() == false) {
                        // This is the expected result
//                        log.assertPass(
//                                       "Validated that Entity " + target.getEntity() +
//                                       " was not permitted persistence with the EntityManager's persistence unit.");
                    } else {
                        // Oops.
                        Assert.fail("Caught an unexpected IllegalStateException with Entity " + target.getEntity());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Assert.fail("Caught unexpected Exception during test execution. " + t);
                    break;
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back active transaction...");
                        try {
                            jpaResource.getTj().rollbackTransaction();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            Assert.fail("Caught unexpected Exception rolling back the transaction." + t);
                        }
                    }
                }
            }

            System.out.println("********************************************************************************");
            System.out.println("Ending test.");
        } finally {
            System.out.println("JPAInjectionTestLogic.testInjectionTarget(): End");
        }

    }
}
