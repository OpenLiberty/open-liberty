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

package com.ibm.ws.query.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.query.entities.ano.Part;
import com.ibm.ws.query.entities.ano.PartBase;
import com.ibm.ws.query.entities.ano.PartComposite;
import com.ibm.ws.query.entities.ano.Usage;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JUConstraintTest extends AbstractTestLogic {
    public void testSelectAllParts(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged())
                em.joinTransaction();
            String q = "select p from Part p";
            System.err.println("EJBQL:" + q);
            List<Part> l = em.createQuery(q).getResultList();
//            jpaResource.getTj().commitTransaction();
//
//            em.clear();

            Object[] expectedAnswers = new Object[] {
                                                      // PartBase: type, partno (i), name (s), cost (d), mass (d), supplies (Collection<Supplier>)
                                                      new Object[] { PartBase.class, 10, "P10", 10.0, 15.25, new int[] { 1, 2 } },
                                                      new Object[] { PartBase.class, 11, "P11", 110.0, 25.8, new int[] { 1 } },
                                                      new Object[] { PartBase.class, 12, "P12", 114.0, 81.02, new int[] { 1, 2 } },
                                                      // PartComposite:  type, partno (i), name (s), assemblyCost (d), massIncrement (d)
                                                      new Object[] { PartComposite.class, 20, "C20", 7.5, 1.0 },
                                                      new Object[] { PartComposite.class, 21, "C21", 0.0, 15.0 },
                                                      new Object[] { PartComposite.class, 99, "C99", 10.0, 20.0 },
            };
            boolean[] foundAnswers = new boolean[] { false, false, false, false, false, false };

            for (Part p : l) {
                System.out.println("Part = " + p);
                for (int idx = 0; idx < foundAnswers.length; idx++) {
                    if (foundAnswers[idx])
                        continue;

                    Object[] ta = (Object[]) expectedAnswers[idx];

                    int partno = (int) ta[1];
                    String name = (String) ta[2];
                    if (!(partno == p.getPartno() && name.equals(p.getName()))) {
                        continue;
                    }

                    if (p instanceof PartComposite) {
                        PartComposite pc = (PartComposite) p;
                        if (ta[0] != PartComposite.class) {
                            continue;
                        }

                        double assemblyCost = (double) ta[3];
                        double massIncrement = (double) ta[3];

                        if (!(Math.abs(pc.getAssemblyCost()) - assemblyCost < 0.2) && (Math.abs(pc.getMassIncrement()) - massIncrement < 0.2)) {
                            continue;
                        }

                        foundAnswers[idx] = true;
                    } else if (p instanceof PartBase) {
                        if (ta[0] != PartBase.class) {
                            continue;
                        }

                        PartBase pb = (PartBase) p;
                        double cost = (double) ta[3];
                        double mass = (double) ta[4];
                        int[] supplies = (int[]) ta[5];

                        if (!(Math.abs(pb.getCost()) - cost < 0.2) && (Math.abs(pb.getMass()) - mass < 0.2)) {
                            continue;
                        }

                        boolean[] scheck = new boolean[supplies.length];
                        Arrays.fill(scheck, false);

                        Assert.assertNotNull(pb.getSuppliers());
                        for (com.ibm.ws.query.entities.ano.Supplier s : pb.getSuppliers()) {
                            int sid = s.getSid();
                            for (int idx2 = 0; idx2 < supplies.length; idx2++) {
                                if (sid == supplies[idx2] && scheck[idx2] == false) {
                                    scheck[idx2] = true;
                                    break;
                                }
                            }
                        }

                        boolean result = true;
                        for (boolean b : scheck) {
                            result = result & b;
                        }
                        if (result)
                            foundAnswers[idx] = true;
                    }
                }

            }

            System.out.println("Outcome table:");
            for (int idx = 0; idx < foundAnswers.length; idx++) {
                System.out.println(" " + idx + " : " + foundAnswers[idx]);
            }

            for (int idx = 0; idx < foundAnswers.length; idx++) {
                Assert.assertTrue("Assert idx " + idx, foundAnswers[idx]);
            }

            jpaResource.getTj().commitTransaction();

            em.clear();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSelectExpensiveParts(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            jpaResource.getTj().beginTransaction();
            String q = "select p from PartBase p where p.cost>100";
            System.err.println("EJBQL:" + q);
            List<Part> l = em.createQuery(q).getResultList();
//            jpaResource.getTj().commitTransaction();
//
//            em.clear();

            Object[] expectedAnswers = new Object[] {
                                                      // PartBase: type, partno (i), name (s), cost (d), mass (d), supplies (Collection<Supplier>)
                                                      new Object[] { PartBase.class, 11, "P11", 110.0, 25.8, new int[] { 1 } },
                                                      new Object[] { PartBase.class, 12, "P12", 114.0, 81.02, new int[] { 1, 2 } },
            };
            boolean[] foundAnswers = new boolean[] { false, false };

            for (Part p : l) {
                System.out.println("Part = " + p);
                for (int idx = 0; idx < foundAnswers.length; idx++) {
                    if (foundAnswers[idx])
                        continue;

                    Object[] ta = (Object[]) expectedAnswers[idx];

                    int partno = (int) ta[1];
                    String name = (String) ta[2];
                    if (!(partno == p.getPartno() && name.equals(p.getName()))) {
                        continue;
                    }

                    if (p instanceof PartBase) {
                        if (ta[0] != PartBase.class) {
                            continue;
                        }

                        PartBase pb = (PartBase) p;
                        double cost = (double) ta[3];
                        double mass = (double) ta[4];
                        int[] supplies = (int[]) ta[5];

                        if (!(Math.abs(pb.getCost()) - cost < 0.2) && (Math.abs(pb.getMass()) - mass < 0.2)) {
                            continue;
                        }

                        boolean[] scheck = new boolean[supplies.length];
                        Arrays.fill(scheck, false);

                        Assert.assertNotNull(pb.getSuppliers());
                        for (com.ibm.ws.query.entities.ano.Supplier s : pb.getSuppliers()) {
                            int sid = s.getSid();
                            for (int idx2 = 0; idx2 < supplies.length; idx2++) {
                                if (sid == supplies[idx2] && scheck[idx2] == false) {
                                    scheck[idx2] = true;
                                    break;
                                }
                            }
                        }

                        boolean result = true;
                        for (boolean b : scheck) {
                            result = result & b;
                        }
                        if (result)
                            foundAnswers[idx] = true;
                    }
                }

            }

            System.out.println("Outcome table:");
            for (int idx = 0; idx < foundAnswers.length; idx++) {
                System.out.println(" " + idx + " : " + foundAnswers[idx]);
            }

            for (int idx = 0; idx < foundAnswers.length; idx++) {
                Assert.assertTrue("Assert idx " + idx, foundAnswers[idx]);
            }

            jpaResource.getTj().commitTransaction();

            em.clear();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCheckCompositePartAssemblyForCycle(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            jpaResource.getTj().beginTransaction();
            String q = "select p from PartComposite p";
            System.err.println("EJBQL:" + q);
            Query query = em.createQuery(q);
            List<PartComposite> l = query.getResultList();
            for (PartComposite p : l) {
                String cycleMsg = null;
                if (checkCycle(p)) {
                    System.err.println("Error; cycle involving part:" + p.getPartno());
                    cycleMsg = "Error; cycle involving part:" + p.getPartno();
                } else {
                    System.err.println("No cycle in part:" + p.getPartno());
                    cycleMsg = "No cycle in part:" + p.getPartno();
                }
                Assert.assertEquals(cycleMsg, ("No cycle in part:" + p.getPartno()));
            }

            jpaResource.getTj().commitTransaction();

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCalculateCompositePartAssemblyTotalCostAndTotalMass(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        String dbProductName = "";
        String dbProductVersion = "";
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
                if ("dbProductName".equals(key)) {
                    dbProductName = key;
                }
                if ("dbProductVersion".equals(key)) {
                    dbProductVersion = key;
                }
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();
            String q = "select p FROM Part p";
            System.err.println("EJBQL:" + q);
            Query query = em.createQuery(q);
            List<Part> pplist = query.getResultList();

            Assert.assertNotNull(pplist);

            // [Part:10 totalMass:15.25 totalCost:10.0, Part:11 totalMass:25.8 totalCost:110.0, Part:12 totalMass:82.01 totalCost:114.0,
            //  Part:20 totalMass:62.0 totalCost:47.5, Part:21 totalMass:183.81 totalCost:264.0, Part:99 totalMass:326.81 totalCost:361.5]

            Object[] expectedAnswers = new Object[] {
                                                      // PartBase: type, partno (i), totalMass (d), totalCost (d)
                                                      // Part: type, partno,
                                                      new Object[] { PartBase.class, 10, 15.25d, 10.0d },
                                                      new Object[] { PartBase.class, 11, 25.8d, 110.0d },
                                                      new Object[] { PartBase.class, 12, 82.01d, 114.0d },
                                                      new Object[] { PartComposite.class, 20, 62.0d, 47.5d },
                                                      new Object[] { PartComposite.class, 21, 183.81d, 264.0d },
                                                      new Object[] { PartComposite.class, 99, 326.81d, 361.5d },

            };
            boolean[] foundAnswers = new boolean[] { false, false, false, false, false, false };

            for (Part p : pplist) {
                Assert.assertNotNull(p);
                final int partNo = p.getPartno();

                if (p instanceof PartComposite) {
                    if (checkCycle((PartComposite) p)) {
                        System.err.println("Error; cycle involving part:" + p.getPartno());
                        continue;
                    }
                }

                final CostAndMassForPart cm = calcCostAndMass(p);
                System.out.println(cm);

                // Find the matching answer entry
                Object[] answer = null;
                int answerIdx = -1;
                for (int index = 0; index < expectedAnswers.length; index++) {
                    Object[] entry = (Object[]) expectedAnswers[index];
                    int entryId = (int) entry[1];
                    if (entryId == partNo && !foundAnswers[index]) {
                        answer = entry;
                        answerIdx = index;
                        break;
                    }
                }

                Assert.assertNotNull("Failed to find an answer entry for partNo = " + partNo, answer);
                Assert.assertEquals((double) answer[2], cm.totalMass, 0.1);
                Assert.assertEquals((double) answer[3], cm.totalCost, 0.1);

                foundAnswers[answerIdx] = true;
            }

            System.out.println("Outcome table:");
            for (int idx = 0; idx < foundAnswers.length; idx++) {
                System.out.println(" " + idx + " : " + foundAnswers[idx]);
            }

            for (int idx = 0; idx < foundAnswers.length; idx++) {
                Assert.assertTrue("Assert idx " + idx, foundAnswers[idx]);
            }

//            TreeSet<String> ts = new TreeSet();
//            for (Part p : pplist) {
//                if (p instanceof PartComposite) {
//                    if (checkCycle((PartComposite) p)) {
//                        System.err.println("Error; cycle involving part:" + p.getPartno());
//                        continue;
//                    }
//
//                }
//                CostAndMassForPart cm = calcCostAndMass(p);
//                ts.add(cm.toString());
//                System.err.println("Part:" + p.getPartno() + " totalMass:" + cm.totalMass + " totalCost:" + cm.totalCost);
//            }
//            jpaResource.getTj().commitTransaction();
//
//            em.clear();
//
//            ArrayList<String> cmal = new ArrayList(ts);
//            System.err.println(cmal);
//            String expectedText = "[Part:10 totalMass:15.25 totalCost:10.0, Part:11 totalMass:25.8 totalCost:110.0, Part:12 totalMass:82.01 totalCost:114.0, Part:20 totalMass:62.0 totalCost:47.5, Part:21 totalMass:183.81 totalCost:264.0, Part:99 totalMass:326.81 totalCost:361.5]";
//            if ("DB2".equals(dbProductName) && dbProductVersion.startsWith("DSN")) {
//                // DB2 on Z has floating point precision issues.  This is not a bug in JPA.
//                expectedText = "[Part:10 totalMass:15.25 totalCost:10.0, Part:11 totalMass:25.8 totalCost:110.0, Part:12 totalMass:82.00999999999999 totalCost:114.0, Part:20 totalMass:62.0 totalCost:47.5, Part:21 totalMass:183.81 totalCost:264.0, Part:99 totalMass:326.81 totalCost:361.5]";
//            }
//            Assert.assertEquals(cmal.toString(), expectedText);

        } finally {
            System.out.println(testName + ": End");
        }
    }

    static boolean checkCycle(PartComposite p, ArrayList list) {
        boolean rc = false;
        Collection<Part> plist = getPartsUsed(p);
        for (Part p1 : plist) {
            if (p1 instanceof PartComposite) {
                if (list.contains(p1))
                    return true;
                list.add(p1);
                PartComposite pc = (PartComposite) p1;
                rc = checkCycle(pc, list);
                if (rc == true)
                    return true;
            }
        }
        return rc;
    }

    static boolean checkCycle(PartComposite p) {
        System.err.println("checkCycle - PartComposite.getPartsUsed for  partno:" + p.getPartno());
        if (p.getPartsUsed().isEmpty()) {
            System.err.println("Error: Composite part contains no subparts. " + p.getPartno());
            return false;
        }
        return checkCycle(p, new ArrayList());
    }

    static Collection<Part> getPartsUsed(PartComposite p) {
        System.err.println("PartComposite getPartsUsed for partno:" + p.getPartno());
        Collection<Usage> ulist = p.getPartsUsed();
        ArrayList<Part> rc = new ArrayList<Part>();
        for (Usage u : ulist) {
            System.err.println("PartComposite partno:" + p.getPartno() + " uses " + u + " " + u.getChild());
            rc.add(u.getChild());
        }
        return rc;
    }

    private CostAndMassForPart calcCostAndMass(Part p) {
        CostAndMassForPart rc = new CostAndMassForPart(p.getPartno());
        if (p instanceof PartBase) {
            rc.totalCost = ((PartBase) p).getCost();
            rc.totalMass = ((PartBase) p).getMass();
            return rc;
        } else {
            PartComposite pc = (PartComposite) p;
            Collection<Usage> ulist = pc.getPartsUsed();
            for (Usage u : ulist) {
                CostAndMassForPart cm = calcCostAndMass(u.getChild());
                rc.totalCost += u.getQuantity() * cm.totalCost;
                rc.totalMass += u.getQuantity() * cm.totalMass;
            }
            rc.totalCost += pc.getAssemblyCost();
            rc.totalMass += pc.getMassIncrement();
            return rc;
        }
    }

    private class CostAndMassForPart {
        int partno;
        double totalCost = 0;
        double totalMass = 0;

        CostAndMassForPart(int partno) {
            this.partno = partno;
        }

        @Override
        public String toString() {
            return "Part:" + partno + " totalMass:" + totalMass + " totalCost:" + totalCost;
        }
    }
}
