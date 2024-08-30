/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

 package com.ibm.ws.jpa.olgh29319.testlogic;

 import java.io.Serializable;
 import java.util.Map;
 import java.util.UUID;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import javax.persistence.EntityManager;
 import javax.persistence.Query;
 
 import org.junit.Assert;
 
 import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
 import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
 import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
 import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
 import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;
 
 public class JPATestOLGH29319Logic extends AbstractTestLogic {
 
     public void testEclipseLinkCaseQueryConcurrency(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
 
         JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);
 
         // Execute Test Case
         try {
             final AtomicInteger count = new AtomicInteger();
             final AtomicInteger error = new AtomicInteger();
 
             final int threads = 100;
             final ExecutorService taskExecutor = Executors.newFixedThreadPool(threads);
 
             // Spawn 100 threads
             for (int i = 0; i < threads; i++) {
                 taskExecutor.execute(new Runnable() {
                     @Override
                     public void run() {
                         count.incrementAndGet();
 
                         EntityManager em = jpaResource.getEmf().createEntityManager();
                         try {
                             Query query = em.createNamedQuery("ECLIPSELINK_CASE_QUERY");
                             query.setParameter("holderId", "exampleHolderId");
                             query.getResultList();
                         } catch (Exception e) {
                             // print the stack for the first exception
                             if (error.incrementAndGet() == 1) {
                                 e.printStackTrace();
                             }
                         } finally {
                             if (em != null) {
                                 em.close();
                             }
                         }
                     }
                 });
             }
             taskExecutor.shutdown();
             taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
 
             Assert.assertEquals("Expected no failures, but " + error.intValue() + "/" + count.intValue() + " threads failed", 0, error.intValue());
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
 