/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.cdi.simple;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.cdi.simple.model.Widget;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class CDISimpleTestLogic extends AbstractTestLogic {
    private final static String testLogicName = CDISimpleTestLogic.class.getSimpleName();

    /**
     * Points: 6
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     */
    public void testInjectionOccursBeforePostConstructAndInsertionCallbacks(
                                                                            TestExecutionContext testExecCtx,
                                                                            TestExecutionResources testExecResources,
                                                                            Object managedComponentObject) {
        final String testMethodName = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testLogicName + "." + testMethodName + "(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        final boolean isAmJta = jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA;

        // Need access to resources on the managed component
        CDITestComponent cdiTestComp = null;
        if (managedComponentObject instanceof CDITestComponent) {
            cdiTestComp = (CDITestComponent) managedComponentObject;
        } else {
            Assert.fail("The managedComponentObject is not a CDITestComponent implementor.  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println(testLogicName + "." + testMethodName + "(): Begin");

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            // Create a circle entity
            cdiTestComp.getEntityListenerMessages(); // Clear messages
            insert(em, tj, isAmJta, "circle", "A round widget");

            // Expect to see PrePersist, then PostPersist.  There may be a PostConstruct
            // which should be ignored.
            List<String> listenerMessages = cdiTestComp.getEntityListenerMessages();
            Assert.assertNotNull("Assert cdiTestComp.getEntityListenerMessages() did not return null.", listenerMessages);
            if (listenerMessages == null) {
                return;
            }

            int indexOfPrePersist = -1;
            int indexOfPostPersist = -1;
            int index = 0;
            for (String s : listenerMessages) {
                System.out.println("Listener Message: " + s);
                boolean isPersistCallback = false;

                if (s.contains("prePersist")) {
                    indexOfPrePersist = index;
                    isPersistCallback = true;
                }
                if (s.contains("postPersist")) {
                    indexOfPostPersist = index;
                    isPersistCallback = true;
                }

                if (isPersistCallback) {
                    // Make sure there's "name=circle" in there.
                    Assert.assertTrue("Assert \"name=circle\" is within callback message \"" + s + "\".",
                                      s.contains("name=circle"));
                }

                index++;
            }
            Assert.assertNotSame("Assert indexOfPrePersist != -1", -1, indexOfPrePersist);
            Assert.assertNotSame("Assert indexOfPostPersist != -1", -1, indexOfPostPersist);
            Assert.assertTrue("Assert indexOfPostPersist > indexOfPrePersist", indexOfPostPersist > indexOfPrePersist);

            System.out.println("Ending test.");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testLogicName + "." + testMethodName + "(): End");
        }
    }

    private void insert(EntityManager em, TransactionJacket tran, boolean isAmJta,
                        String name, String description) throws Exception {
        try {
            tran.beginTransaction();
            if (isAmJta) {
                em.joinTransaction();
            }
            Widget w = new Widget();
            w.setName(name);
            w.setDescription(description);
            em.persist(w);
            System.out.println("Persisted " + w);
        } finally {
            tran.commitTransaction();
        }
    }

}
