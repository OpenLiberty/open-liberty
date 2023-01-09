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

package com.ibm.ws.jpa.clientcontainer.fat.basic.methodinj;

import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import com.ibm.ws.jpa.clientcontainer.fat.basic.methodinj.entities.AnotherSimpleEntity;
import com.ibm.ws.jpa.clientcontainer.fat.basic.methodinj.entities.SimpleEntity;

public class BasicJPAMethodInjAppClient {
    private final static String JPA_ACLI_EYECATCHER = "JPAACLI:";
    private final static String JPA_ACLI_PASS_EYECATCHER = JPA_ACLI_EYECATCHER + "PASS:";
    private final static String JPA_ACLI_FAIL_EYECATCHER = JPA_ACLI_EYECATCHER + "FAIL:";
    private final static AtomicLong SEQ_ID = new AtomicLong(0);

    private static EntityManagerFactory emfAno;
    private static EntityManagerFactory emfDD;
    private static EntityManagerFactory emfAnoDDMerge;

    static {
        System.out.println(JPA_ACLI_EYECATCHER +
                           "BasicJPAMethodInjAppClient static initializer verifying AppClient method injection class has been loaded.");
    }

    @PersistenceUnit(unitName = "AppCliPU_1")
    public static void setEmfAno(EntityManagerFactory emfAno) {
        BasicJPAMethodInjAppClient.emfAno = emfAno;
    }

    // Deployment Descriptor injection
    public static void setEmfDD(EntityManagerFactory emfDD) {
        BasicJPAMethodInjAppClient.emfDD = emfDD;
    }

    // Annotation/Deployment Descriptor injection
    @PersistenceUnit(unitName = "AppCliPU_1", name = "AppCliPU_2_EMF_AnoDDMerge")
    public static void setEmfAnoDDMerge(EntityManagerFactory emfAnoDDMerge) {
        BasicJPAMethodInjAppClient.emfAnoDDMerge = emfAnoDDMerge;
    }

    public static void main(String[] args) {
        System.out.println(JPA_ACLI_EYECATCHER + "Hello BasicJPAMethodInjAppClient");
        System.out.println(JPA_ACLI_EYECATCHER + "emfAno = " + emfAno);
        System.out.println(JPA_ACLI_EYECATCHER + "emfDD = " + emfDD);

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

        System.out.println(JPA_ACLI_EYECATCHER + "BasicJPAMethodInjAppClient exiting.");
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

            boolean entEnhanced = verifyClassEnhancement(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Entity is enhanced = " + entEnhanced);

            if (seFind == null || entEnhanced == false) {
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

            boolean entEnhanced = verifyClassEnhancement(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Entity is enhanced = " + entEnhanced);

            if (seFind == null || entEnhanced == false) {
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

            boolean entEnhanced = verifyClassEnhancement(se);
            System.out.println(JPA_ACLI_EYECATCHER + "Entity is enhanced = " + entEnhanced);

            if (seFind == null || entEnhanced == false) {
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

    private final static String[] providerEnhancementSignatures = {
                                                                    "org.eclipse.persistence.internal.descriptors.PersistenceEntity",
                                                                    "org.apache.openjpa.enhance.PersistenceCapable"
    };

    private static boolean verifyClassEnhancement(Object o) {
        boolean retVal = false;
        Class cls = o.getClass();
        Class[] interfaces = cls.getInterfaces();

        if (interfaces != null && interfaces.length > 0) {
            out: for (Class iFaceClass : interfaces) {
                String clsName = iFaceClass.getName();
                for (String enhSig : providerEnhancementSignatures) {
                    if (enhSig.equals(clsName)) {
                        retVal = true;
                        break out;
                    }
                }
            }
        }

        return retVal;
    }
}
