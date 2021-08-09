/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpaappcli.client.jndiinjection;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import jpaappcli.entity.AnotherAppClientEntity;
import jpaappcli.entity.AppClientEntity;

@PersistenceUnit(unitName = "AppCliPU", name = "java:app/env/emf") // emf
@PersistenceUnit(unitName = "AppCliPU_RESREF", name = "java:app/env/emfRR") // emfRR
// @PersistenceUnit(unitName = "AppCliPU", name = "java:app/env/emfDD") // emfDD -- declared in DD only
@PersistenceUnit(unitName = "AppCliPU", name = "java:app/env/AppCliPU_DDOVRD_EMF") // emfMerge -- DD override
public class JPAApplicationJNDIInjectClient {
    private static final String EYECATCHER = "JPACLI: ";
    private static final String FAIL_EYECATCHER = EYECATCHER + "FAILED: ";
    private static final String PASS_EYECATCHER = EYECATCHER + "PASSED: ";

    private static EntityManagerFactory emf;
    private static EntityManagerFactory emfRR;
    private static EntityManagerFactory emfDD;
    private static EntityManagerFactory emfMerge;

    private static boolean passed = true;

    static {
        System.out.println(EYECATCHER + JPAApplicationJNDIInjectClient.class.getName() + " has initialized.");
    }

    public static void main(String[] args) throws Throwable {
        System.out.println(EYECATCHER + JPAApplicationJNDIInjectClient.class.getName() + ": hello");

        InitialContext ic = new InitialContext();
        System.out.println(EYECATCHER + "ic=" + ic);
        if (ic == null) {
            // Injection has failed.
            fail(" ic is null");
        }
        emf = (EntityManagerFactory) ic.lookup("java:app/env/emf");
        emfRR = (EntityManagerFactory) ic.lookup("java:app/env/emfRR");
        emfDD = (EntityManagerFactory) ic.lookup("java:app/env/emfDD");
        emfMerge = (EntityManagerFactory) ic.lookup("java:app/env/AppCliPU_DDOVRD_EMF");

        // Verify injection was successful
        System.out.println(EYECATCHER + "emf=" + emf);
        if (emf == null) {
            // Injection has failed.
            fail(" emf is null");
        }

        System.out.println(EYECATCHER + "emfDD=" + emfDD);
        if (emfDD == null) {
            // Injection has failed.
            fail(" emfDD is null");
        }

        System.out.println(EYECATCHER + "emfRR=" + emfRR);
        if (emfRR == null) {
            // Injection has failed.
            fail(" emfRR is null");
        }

        System.out.println(EYECATCHER + "emfMerge=" + emfMerge);
        if (emfMerge == null) {
            // Injection has failed.
            fail(" emfMerge is null");
        }

        // Run Tests
        System.out.println(EYECATCHER + " Running Tests");
        testAnnotationInjectedEMF(emf, "Basic_Field_Inject");
        testAnnotationInjectedEMF(emfRR, "Basic_Field_ResourceRefDS_Inject");
        testAnnotationInjectedEMF(emfDD, "Basic_Field_DD_Inject");
        testAnnotationInjectedEMFOverride(emfMerge, "Basic_Field_DDOverride_Inject");

        if (passed)
            System.out.println(PASS_EYECATCHER + JPAApplicationJNDIInjectClient.class.getName() + ": test ended successfully");
        else
            System.out.println(FAIL_EYECATCHER + JPAApplicationJNDIInjectClient.class.getName() + ": test ended unsuccessfully");
    }

    private static void testAnnotationInjectedEMF(final EntityManagerFactory emf, final String testqualifier) throws Throwable {
        System.out.println(EYECATCHER + " Starting testAnnotationInjectedEMF_" + testqualifier + "...");

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            System.out.println(EYECATCHER + "em=" + em);
            if (em == null) {
                fail(" em is null");
            }

            System.out.println(EYECATCHER + " Beginning transaction ...");
            em.getTransaction().begin();

            System.out.println(EYECATCHER + " Creating new AppClientEntity ...");
            final AppClientEntity newEntity = new AppClientEntity();
            newEntity.setStrData("Simple String");
            em.persist(newEntity);

            System.out.println(EYECATCHER + " Commmitting transaction ...");
            em.getTransaction().commit();

            em.clear();

            final AppClientEntity findEntity = em.find(AppClientEntity.class, newEntity.getId());
            if (findEntity == null) {
                fail(" em.find() returned null.");
            }

            boolean entityIsEnhanced = verifyClassEnhancement(findEntity);
            if (entityIsEnhanced) {
                System.out.println(EYECATCHER + " Entity AppClientEntity is enhanced.");
            } else {
                fail(" Entity AppClientEntity is not enhanced.");
            }

        } catch (Throwable t) {
            System.out.println(FAIL_EYECATCHER + " Caught unexpected Exception " + t.toString());
            t.printStackTrace();
            throw t;
        } finally {
            System.out.println(EYECATCHER + " Exiting testAnnotationInjectedEMF_" + testqualifier + " ...");

            if (em != null) {
                em.close();
            }
        }
    }

    private static void testAnnotationInjectedEMFOverride(final EntityManagerFactory emf, final String testqualifier) throws Throwable {
        System.out.println(EYECATCHER + " Starting testAnnotationInjectedEMFOverride_" + testqualifier + "...");

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            System.out.println(EYECATCHER + "em=" + em);
            if (em == null) {
                fail("em is null.");
            }

            System.out.println(EYECATCHER + " Beginning transaction ...");
            em.getTransaction().begin();

            System.out.println(EYECATCHER + " Creating new AppClientEntity ...");
            final AppClientEntity newEntity = new AppClientEntity();
            newEntity.setStrData("Simple String");
            try {
                em.persist(newEntity);
                fail("The persist operation did not throw an Exception.");
            } catch (IllegalArgumentException iae) {
                // Expected
            }

            System.out.println(EYECATCHER + " Rolling back transaction ...");
            em.getTransaction().rollback();

            System.out.println(EYECATCHER + " Beginning transaction ...");
            em.getTransaction().begin();

            System.out.println(EYECATCHER + " Creating new AnotherAppClientEntity ...");
            final AnotherAppClientEntity newEntity2 = new AnotherAppClientEntity();
            newEntity2.setStrData("Simple String");
            em.persist(newEntity2);

            System.out.println(EYECATCHER + " Commmitting transaction ...");
            em.getTransaction().commit();

            em.clear();

            final AnotherAppClientEntity findEntity = em.find(AnotherAppClientEntity.class, newEntity2.getId());
            if (findEntity == null) {
                fail(" em.find() returned null.");
            }

            boolean entityIsEnhanced = verifyClassEnhancement(findEntity);
            if (entityIsEnhanced) {
                System.out.println(EYECATCHER + " Entity AppClientEntity is enhanced.");
            } else {
                fail(" Entity AppClientEntity is not enhanced.");
            }

        } catch (Throwable t) {
            System.out.println(FAIL_EYECATCHER + " Caught unexpected Exception " + t.toString());
            t.printStackTrace();
            throw t;
        } finally {
            System.out.println(EYECATCHER + " Exiting testAnnotationInjectedEMF_" + testqualifier + " ...");

            if (em != null) {
                em.close();
            }
        }
    }

    private final static void fail(String message) {
        passed = false;
        System.out.println(FAIL_EYECATCHER + message);
        throw new RuntimeException("Test has failed.");
    }

    private final static String[] providerEnhancementSignatures = {
                                                                    "org.eclipse.persistence.internal.descriptors.PersistenceEntity",
                                                                    "org.apache.openjpa.enhance.PersistenceCapable"
    };

    @SuppressWarnings("rawtypes")
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
