/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh21204.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh21204.model.SimpleEntityOLGH21204;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH21204Logic extends AbstractTestLogic {

    private static long IDENTITY_VALUE = 0;

    public void testRefreshWithTriggers(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.1 / 3.0 is updated to include the fix
        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);
        if ((isUsingJPA30Feature() || isUsingJPA31Feature()) && JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);

        // Test is only valid for the following databases
        if (!(isDB2 || isDerby || isMySQL || isSQLServer)) {
            return;
        }

        String name = "Capa";
        double price = 4.2;

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            System.out.println("Creating new object instance of " + SimpleEntityOLGH21204.class + " (id=GENERATED)...");
            SimpleEntityOLGH21204 new_entity = new SimpleEntityOLGH21204(name, price);

            System.out.println("Persisting " + new_entity);
            em.persist(new_entity);

            System.out.println("Committing transaction...");
            tj.commitTransaction();

            IDENTITY_VALUE++;

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Merging " + SimpleEntityOLGH21204.class + " (id=" + new_entity.getId() + ")...");
                new_entity = em.merge(new_entity);

                System.out.println("Refreshing " + SimpleEntityOLGH21204.class + " (id=" + new_entity.getId() + ")...");
                em.refresh(new_entity);

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                em.clear();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                // Query the nonJPA AUDIT table to validate the generated id values
                List<Object[]> result = em.createNativeQuery("SELECT AUDIT_ID, ENTITY_ID FROM SimpleEntityOLGH21204_AUDIT WHERE NAME = '" + name + "' AND PRICE = " + price + "")
                                .getResultList();

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                Assert.assertNotNull(result);
                Assert.assertEquals(1, result.size());
                Assert.assertEquals(2, result.get(0).length);

                // Add 3 because there should be 3 existing rows INSERT'd into the AUDIT table
                switch (provider) {
                    case ECLIPSELINK:
                    case OPENJPA:
                        if (isSQLServer) {
                            Assert.assertEquals(new java.math.BigDecimal(IDENTITY_VALUE + 3), result.get(0)[0]);
                            Assert.assertEquals(new java.math.BigDecimal(IDENTITY_VALUE), result.get(0)[1]);
                        } else {
                            Assert.assertEquals(IDENTITY_VALUE + 3, result.get(0)[0]);
                            Assert.assertEquals(IDENTITY_VALUE, result.get(0)[1]);
                        }
                        break;
                    case HIBERNATE:
                        if (isSQLServer) {
                            Assert.assertEquals(new java.math.BigDecimal(IDENTITY_VALUE + 3), result.get(0)[0]);
                            Assert.assertEquals(new java.math.BigDecimal(IDENTITY_VALUE), result.get(0)[1]);
                        } else {
                            Assert.assertEquals(java.math.BigInteger.valueOf(IDENTITY_VALUE + 3), result.get(0)[0]);
                            Assert.assertEquals(java.math.BigInteger.valueOf(IDENTITY_VALUE), result.get(0)[1]);
                        }
                        break;
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                Long id = new_entity.getId();
                Assert.assertNotNull(id);

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                em.clear();

                System.out.println("Finding " + SimpleEntityOLGH21204.class + " (id=" + id + ")...");
                SimpleEntityOLGH21204 find_remove_entity = em.find(SimpleEntityOLGH21204.class, id);
                System.out.println("Object returned by find: " + find_remove_entity);
                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity);

                System.out.println("Removing " + find_remove_entity);
                em.remove(find_remove_entity);

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                // Clear the audit table now that validation has occurred
                em.createNativeQuery("DELETE FROM SimpleEntityOLGH21204_AUDIT WHERE NAME = '" + name + "' AND PRICE = " + price + "").executeUpdate();

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                em.clear();
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
