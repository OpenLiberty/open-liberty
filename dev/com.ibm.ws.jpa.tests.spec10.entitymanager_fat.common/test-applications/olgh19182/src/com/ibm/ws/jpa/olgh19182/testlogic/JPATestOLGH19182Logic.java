/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh19182.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh19182.model.HelmetEntityOLGH19182;
import com.ibm.ws.jpa.olgh19182.model.ShelfEntityOLGH19182;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH19182Logic extends AbstractTestLogic {

    public void testFetchGroupForCachedReference(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0 is updated to include the fix
        //TODO: Disable test until EclipseLink 2.7 is updated to include the fix
        if ((isUsingJPA22Feature() || isUsingJPA30Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        long id = 1L;
        long id2 = 2L;
        int id3 = 3;

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Creating new object instance of " + ShelfEntityOLGH19182.class + " (id=" + id + ")...");
            ShelfEntityOLGH19182 new_entity = new ShelfEntityOLGH19182();
            new_entity.setId(id);
            new_entity.setName("original");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Creating new object instance of " + ShelfEntityOLGH19182.class + " (id=" + id2 + ")...");
            ShelfEntityOLGH19182 new_entity2 = new ShelfEntityOLGH19182();
            new_entity2.setId(id2);
            new_entity2.setName("modified");

            System.out.println("Persisting " + new_entity2);
            jpaResource.getEm().persist(new_entity2);

            System.out.println("Creating new object instance of " + HelmetEntityOLGH19182.class + " (id=" + id3 + ")...");
            HelmetEntityOLGH19182 new_entity3 = new HelmetEntityOLGH19182();
            new_entity3.setId(id3);
            new_entity3.setShelf(new_entity);

            System.out.println("Persisting " + new_entity3);
            jpaResource.getEm().persist(new_entity3);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            try {
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + HelmetEntityOLGH19182.class + " (id=" + id3 + ")...");
                HelmetEntityOLGH19182 find_entity = jpaResource.getEm().find(HelmetEntityOLGH19182.class, id3);
                System.out.println("Object returned by find: " + find_entity);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entity);

                System.out.println("Getting Reference " + ShelfEntityOLGH19182.class + " (id=" + id2 + ")...");
                ShelfEntityOLGH19182 ref_entity = jpaResource.getEm().getReference(ShelfEntityOLGH19182.class, id2);
                System.out.println("Object returned by getReference: " + ref_entity);
                Assert.assertNotNull("Assert that the getReference operation did not return null", ref_entity);

                find_entity.setShelf(ref_entity);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + ShelfEntityOLGH19182.class + " (id=" + id2 + ")...");
                ShelfEntityOLGH19182 find_entity2 = jpaResource.getEm().find(ShelfEntityOLGH19182.class, id2);
                System.out.println("Object returned by find: " + find_entity2);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
                Assert.assertNotNull("Assert that the entity name is set", find_entity2.getName());
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Finding " + HelmetEntityOLGH19182.class + " (id=" + id3 + ")...");
                HelmetEntityOLGH19182 find_remove_entity3 = jpaResource.getEm().find(HelmetEntityOLGH19182.class, id3);
                System.out.println("Object returned by find: " + find_remove_entity3);
                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity3);

                System.out.println("Removing " + find_remove_entity3);
                jpaResource.getEm().remove(find_remove_entity3);

                System.out.println("Finding " + ShelfEntityOLGH19182.class + " (id=" + id2 + ")...");
                ShelfEntityOLGH19182 find_remove_entity2 = jpaResource.getEm().find(ShelfEntityOLGH19182.class, id2);
                System.out.println("Object returned by find: " + find_remove_entity2);
                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

                System.out.println("Removing " + find_remove_entity2);
                jpaResource.getEm().remove(find_remove_entity2);

                System.out.println("Finding " + ShelfEntityOLGH19182.class + " (id=" + id + ")...");
                ShelfEntityOLGH19182 find_remove_entity = jpaResource.getEm().find(ShelfEntityOLGH19182.class, id);
                System.out.println("Object returned by find: " + find_remove_entity);
                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity);

                System.out.println("Removing " + find_remove_entity);
                jpaResource.getEm().remove(find_remove_entity);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
