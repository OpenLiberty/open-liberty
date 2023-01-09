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

package com.ibm.ws.jpa.clientcontainer.fat.basic.jndi;

import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;

import com.ibm.ws.jpa.clientcontainer.fat.basic.jndi.entities.AnotherSimpleEntity;
import com.ibm.ws.jpa.clientcontainer.fat.basic.jndi.entities.SimpleEntity;

@PersistenceUnits({
                    @PersistenceUnit(unitName = "AppCliPU_1", name = "java:app/env/AppCli_Ano"),
                    @PersistenceUnit(unitName = "AppCliPU_1", name = "java:app/env/AppCliPU_2_EMF_AnoDDMerge") })
public class BasicJPAJNDIAppClient {
    private final static String JPA_ACLI_EYECATCHER = "JPAACLI:";
    private final static String JPA_ACLI_PASS_EYECATCHER = JPA_ACLI_EYECATCHER + "PASS:";
    private final static String JPA_ACLI_FAIL_EYECATCHER = JPA_ACLI_EYECATCHER + "FAIL:";
    private final static AtomicLong SEQ_ID = new AtomicLong(0);

    private static EntityManagerFactory emfAno;
    private static EntityManagerFactory emfDD;
    private static EntityManagerFactory emfAnoDDMerge;

    static {
        System.out.println(JPA_ACLI_EYECATCHER +
                           "BasicJPAJNDIAppClient static initializer verifying AppClient JNDI Injection class has been loaded.");
    }

    public static void main(String[] args) {
        System.out.println(JPA_ACLI_EYECATCHER + "Hello BasicJPAJNDIAppClient");

        InitialContext ic = null;
        try {
            ic = new InitialContext();

            emfAno = (EntityManagerFactory) ic.lookup("java:app/env/AppCli_Ano");
            emfDD = (EntityManagerFactory) ic.lookup("java:app/env/AppCliPU_2_EMF");
            emfAnoDDMerge = (EntityManagerFactory) ic.lookup("java:app/env/AppCliPU_2_EMF_AnoDDMerge");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        System.out.println(JPA_ACLI_EYECATCHER + "emfAno = " + emfAno);
        System.out.println(JPA_ACLI_EYECATCHER + "emfDD = " + emfDD);
        System.out.println(JPA_ACLI_EYECATCHER + "emfAnoDDMerge = " + emfAnoDDMerge);

        if (emfAno == null) {
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "emfAno is null.");
        }
        if (emfDD == null) {
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "emfDD is null.");
        }
        if (emfAnoDDMerge == null) {
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "emfDD_noRefName is null.");
        }

        testAnnotationInjectedEMF();
        testDeploymentDescriptorInjectedEMF();
        testAnnotationDeploymentDescriptorMergeInjectedMF();

        System.out.println(JPA_ACLI_EYECATCHER + "BasicJPAJNDIAppClient exiting.");
    }

    private static void testAnnotationInjectedEMF() {
        final String testName = "testAnnotationInjectedEMF";
        System.out.println("**********************************************************************");
        System.out.println(JPA_ACLI_EYECATCHER + "Running Test: " + testName);

        EntityManagerFactory emf = emfAno;
        EntityManager em = null;

        try {
            System.out.println(JPA_ACLI_EYECATCHER + "Using EMF: " + emf);

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Beginning Transaction ...");
            em.getTransaction().begin();

            long id = SEQ_ID.getAndIncrement();
            System.out.println(JPA_ACLI_EYECATCHER + "Creating SimpleEntity with id=" + id + " ...");
            SimpleEntity se = new SimpleEntity();
            se.setId(id);
            se.setStrData("Simple String");
            em.persist(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Persisted SimpleEntity: " + se);

            System.out.println(JPA_ACLI_EYECATCHER + "Committing Transaction ...");
            em.getTransaction().commit();
            System.out.println(JPA_ACLI_EYECATCHER + "Committed Transaction.");

            System.out.println(JPA_ACLI_EYECATCHER + "Disposing EntityManager ...");
            em.close();

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Finding SimpleEntity with id=" + id + " ...");
            SimpleEntity seFind = em.find(SimpleEntity.class, id);
            System.out.println(JPA_ACLI_EYECATCHER + "Find returned SimpleEntity: " + seFind);

            if (seFind == null) {
                System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Failed to find target SimpleEntity (find returned null.)");
            } else {
                System.out.println(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + testName);
            }
        } catch (Throwable t) {
            // No Exceptions are expected.
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Caught Unexpected Exception.");
            t.printStackTrace();
        } finally {
            if (em != null) {
                try {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    em.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }

        System.out.println(JPA_ACLI_EYECATCHER + "Test Ended:" + testName);
    }

    private static void testDeploymentDescriptorInjectedEMF() {
        final String testName = "testDeploymentDescriptorInjectedEMF";
        System.out.println("**********************************************************************");
        System.out.println(JPA_ACLI_EYECATCHER + "Running Test: " + testName);

        EntityManagerFactory emf = emfDD;
        EntityManager em = null;

        try {
            System.out.println(JPA_ACLI_EYECATCHER + "Using EMF: " + emf);

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Beginning Transaction ...");
            em.getTransaction().begin();

            long id = SEQ_ID.getAndIncrement();
            System.out.println(JPA_ACLI_EYECATCHER + "Creating AnotherSimpleEntity with id=" + id + " ...");
            AnotherSimpleEntity se = new AnotherSimpleEntity();
            se.setId(id);
            se.setStrData("Simple String");
            em.persist(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Persisted AnotherSimpleEntity: " + se);

            System.out.println(JPA_ACLI_EYECATCHER + "Committing Transaction ...");
            em.getTransaction().commit();
            System.out.println(JPA_ACLI_EYECATCHER + "Committed Transaction.");

            System.out.println(JPA_ACLI_EYECATCHER + "Disposing EntityManager ...");
            em.close();

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Finding AnotherSimpleEntity with id=" + id + " ...");
            AnotherSimpleEntity seFind = em.find(AnotherSimpleEntity.class, id);
            System.out.println(JPA_ACLI_EYECATCHER + "Find returned AnotherSimpleEntity: " + seFind);

            if (seFind == null) {
                System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Failed to find target AnotherSimpleEntity (find returned null.)");
            } else {
                System.out.println(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + testName);
            }
        } catch (Throwable t) {
            // No Exceptions are expected.
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Caught Unexpected Exception.");
            t.printStackTrace();
        } finally {
            if (em != null) {
                try {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    em.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }

        System.out.println(JPA_ACLI_EYECATCHER + "Test Ended:" + testName);
    }

    private static void testAnnotationDeploymentDescriptorMergeInjectedMF() {
        final String testName = "testAnnotationDeploymentDescriptorMergeInjectedMF";
        System.out.println("**********************************************************************");
        System.out.println(JPA_ACLI_EYECATCHER + "Running Test: " + testName);

        EntityManagerFactory emf = emfAnoDDMerge;
        EntityManager em = null;

        try {
            System.out.println(JPA_ACLI_EYECATCHER + "Using EMF: " + emf);

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Beginning Transaction ...");
            em.getTransaction().begin();

            long id = SEQ_ID.getAndIncrement();
            System.out.println(JPA_ACLI_EYECATCHER + "Creating AnotherSimpleEntity with id=" + id + " ...");
            AnotherSimpleEntity se = new AnotherSimpleEntity();
            se.setId(id);
            se.setStrData("Simple String");
            em.persist(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Persisted AnotherSimpleEntity: " + se);

            System.out.println(JPA_ACLI_EYECATCHER + "Committing Transaction ...");
            em.getTransaction().commit();
            System.out.println(JPA_ACLI_EYECATCHER + "Committed Transaction.");

            System.out.println(JPA_ACLI_EYECATCHER + "Disposing EntityManager ...");
            em.close();

            System.out.println(JPA_ACLI_EYECATCHER + "Creating new EntityManager ...");
            em = emf.createEntityManager();
            System.out.println(JPA_ACLI_EYECATCHER + "EM = " + em);

            System.out.println(JPA_ACLI_EYECATCHER + "Finding AnotherSimpleEntity with id=" + id + " ...");
            AnotherSimpleEntity seFind = em.find(AnotherSimpleEntity.class, id);
            System.out.println(JPA_ACLI_EYECATCHER + "Find returned AnotherSimpleEntity: " + seFind);

            if (seFind == null) {
                System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Failed to find target AnotherSimpleEntity (find returned null.)");
            } else {
                System.out.println(JPA_ACLI_PASS_EYECATCHER + "Test PASSED:" + testName);
            }
        } catch (Throwable t) {
            // No Exceptions are expected.
            System.out.println(JPA_ACLI_FAIL_EYECATCHER + "Test FAILED:" + testName + ":Caught Unexpected Exception.");
            t.printStackTrace();
        } finally {
            if (em != null) {
                try {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    em.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }

        System.out.println(JPA_ACLI_EYECATCHER + "Test Ended:" + testName);
    }
}
