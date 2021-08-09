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

package com.ibm.ws.jpa.olgh14426.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh14426.model.EventEntityOLGH14426;
import com.ibm.ws.jpa.olgh14426.model.JobEntityOLGH14426;
import com.ibm.ws.jpa.olgh14426.model.JobImplEntityOLGH14426;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH14426Logic extends AbstractTestLogic {

    /**
     * Eclipselink offers a capability, "eclipselink.jdbc.exclusive-connection.mode", which was not functioning
     * correctly until fixed with Eclipselink bug Bug 547173. This test case verifies behavior in EE mode.
     */
    public void testEclipseLinkCopy(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            JobEntityOLGH14426 job = new JobImplEntityOLGH14426();
            job.setId(99L);
            org.eclipse.persistence.sessions.CopyGroup eventsCG = new org.eclipse.persistence.sessions.CopyGroup();
            eventsCG.addAttribute("id");
            eventsCG.addAttribute("datef");
            org.eclipse.persistence.sessions.CopyGroup jobCG = new org.eclipse.persistence.sessions.CopyGroup();
            jobCG.addAttribute("id");
            eventsCG.addAttribute("job", jobCG);
            jobCG.addAttribute("events", eventsCG);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist operation: " + job);
            em.persist(job);

            System.out.println("Performing flush operation");
            em.flush();

            System.out.println("Performing copy operation");
            job = (JobEntityOLGH14426) em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).copy(job, jobCG);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            for (int i = 0; i < 10; i++) {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing merge operation: " + job);
                job = em.merge(job);
                System.out.println("Returned merge object: " + job);

                System.out.println("Performing flush operation");
                em.flush();

                System.out.println("Add EventEntityOLGH14426 to " + job);
                EventEntityOLGH14426 e = new EventEntityOLGH14426();
                e.setId(50L + i);
                e.setJob(job);
                job.getEvents().add(e);

                System.out.println("Performing flush operation");
                em.flush();

                org.eclipse.persistence.sessions.CopyGroup eventsCG2 = new org.eclipse.persistence.sessions.CopyGroup();
                eventsCG2.addAttribute("id");
                eventsCG2.addAttribute("datef");
                org.eclipse.persistence.sessions.CopyGroup jobCG2 = new org.eclipse.persistence.sessions.CopyGroup();
                jobCG2.addAttribute("id");
                eventsCG2.addAttribute("job", jobCG2);
                jobCG2.addAttribute("events", eventsCG2);

                System.out.println("Performing copy operation");
                job = (JobEntityOLGH14426) em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).copy(job, jobCG2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
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
