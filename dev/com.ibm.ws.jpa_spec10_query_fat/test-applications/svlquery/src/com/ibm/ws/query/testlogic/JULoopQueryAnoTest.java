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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.query.entities.ano.AddressBean;
import com.ibm.ws.query.entities.ano.DeptBean;
import com.ibm.ws.query.entities.ano.EmpBean;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JULoopQueryAnoTest extends AbstractTestLogic {

//    TEST1; select d.no, d.name, d.mgr.empid, d.mgr.name, d.reportsTo.no from DeptBean d order by d.no
    public void testLoop001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.no, d.name, d.mgr.empid, d.mgr.name, d.reportsTo.no from DeptBean d order by d.no";
            Query q = em.createQuery(qStr);

//          TEST1; select d.no, d.name, d.mgr.empid, d.mgr.name, d.reportsTo.no from DeptBean d order by d.no
//          d.no   d.name    d.mgr.empid  d.mgr.name  d.reportsTo.no
//          ~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~~ ~~~~~~~~~~~~~~
//          100      CEO         10      Catalina Wei      100
//          200     Admin         8      Tom Rayburn       100
//          210  Development      3         minmei         200
//          220    Service        4         george         200
//          300     Sales         6         ahmad          100
//           TEST1; 5 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            // 100      CEO         10      Catalina Wei      100
            Object[] r1 = rList.get(0);
            Assert.assertNotNull(r1);
            Assert.assertEquals(100, (int) r1[0]);
            Assert.assertEquals("CEO", r1[1]);
            Assert.assertEquals(10, (int) r1[2]);
            Assert.assertEquals("Catalina Wei", r1[3]);
            Assert.assertEquals(100, (int) r1[4]);

            // 200     Admin         8      Tom Rayburn       100
            Object[] r2 = rList.get(1);
            Assert.assertNotNull(r2);
            Assert.assertEquals(200, (int) r2[0]);
            Assert.assertEquals("Admin", r2[1]);
            Assert.assertEquals(8, (int) r2[2]);
            Assert.assertEquals("Tom Rayburn", r2[3]);
            Assert.assertEquals(100, (int) r2[4]);

            // 210  Development      3         minmei         200
            Object[] r3 = rList.get(2);
            Assert.assertNotNull(r3);
            Assert.assertEquals(210, (int) r3[0]);
            Assert.assertEquals("Development", r3[1]);
            Assert.assertEquals(3, (int) r3[2]);
            Assert.assertEquals("minmei", r3[3]);
            Assert.assertEquals(200, (int) r3[4]);

            // 220    Service        4         george         200
            Object[] r4 = rList.get(3);
            Assert.assertNotNull(r4);
            Assert.assertEquals(220, (int) r4[0]);
            Assert.assertEquals("Service", r4[1]);
            Assert.assertEquals(4, (int) r4[2]);
            Assert.assertEquals("george", r4[3]);
            Assert.assertEquals(200, (int) r4[4]);

            // 300     Sales         6         ahmad          100
            Object[] r5 = rList.get(4);
            Assert.assertNotNull(r5);
            Assert.assertEquals(300, (int) r5[0]);
            Assert.assertEquals("Sales", r5[1]);
            Assert.assertEquals(6, (int) r5[2]);
            Assert.assertEquals("ahmad", r5[3]);
            Assert.assertEquals(100, (int) r5[4]);

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

    // TEST2; select e.empid, e.name, e.dept.no from EmpBean e order by e.empid
    public void testLoop002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select e.empid, e.name, e.dept.no from EmpBean e order by e.empid";
            Query q = em.createQuery(qStr);

//            TEST2; select e.empid, e.name, e.dept.no from EmpBean e order by e.empid
//            e.empid   e.name    e.dept.no
//            ~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~
//               1       david       210
//               2      andrew       210
//               3      minmei       200
//               4      george       200
//               5      ritika       220
//               6       ahmad       100
//               7     charlene      210
//               8    Tom Rayburn    100
//               9       harry       210
//             TEST2; 9 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            // 1       david       210
            Object[] r1 = rList.get(0);
            Assert.assertNotNull(r1);
            Assert.assertEquals(1, (int) r1[0]);
            Assert.assertEquals("david", r1[1]);
            Assert.assertEquals(210, (int) r1[2]);

            // 2      andrew       210
            Object[] r2 = rList.get(1);
            Assert.assertNotNull(r2);
            Assert.assertEquals(2, (int) r2[0]);
            Assert.assertEquals("andrew", r2[1]);
            Assert.assertEquals(210, (int) r2[2]);

            // 3      minmei       200
            Object[] r3 = rList.get(2);
            Assert.assertNotNull(r3);
            Assert.assertEquals(3, (int) r3[0]);
            Assert.assertEquals("minmei", r3[1]);
            Assert.assertEquals(200, (int) r3[2]);

            // 4      george       200
            Object[] r4 = rList.get(3);
            Assert.assertNotNull(r4);
            Assert.assertEquals(4, (int) r4[0]);
            Assert.assertEquals("george", r4[1]);
            Assert.assertEquals(200, (int) r4[2]);

            // 5      ritika       220
            Object[] r5 = rList.get(4);
            Assert.assertNotNull(r5);
            Assert.assertEquals(5, (int) r5[0]);
            Assert.assertEquals("ritika", r5[1]);
            Assert.assertEquals(220, (int) r5[2]);

            // 6       ahmad       100
            Object[] r6 = rList.get(5);
            Assert.assertNotNull(r6);
            Assert.assertEquals(6, (int) r6[0]);
            Assert.assertEquals("ahmad", r6[1]);
            Assert.assertEquals(100, (int) r6[2]);

            // 7     charlene      210
            Object[] r7 = rList.get(6);
            Assert.assertNotNull(r7);
            Assert.assertEquals(7, (int) r7[0]);
            Assert.assertEquals("charlene", r7[1]);
            Assert.assertEquals(210, (int) r7[2]);

            // 8    Tom Rayburn    100
            Object[] r8 = rList.get(7);
            Assert.assertNotNull(r8);
            Assert.assertEquals(8, (int) r8[0]);
            Assert.assertEquals("Tom Rayburn", r8[1]);
            Assert.assertEquals(100, (int) r8[2]);

            // 9       harry       210
            Object[] r9 = rList.get(8);
            Assert.assertNotNull(r9);
            Assert.assertEquals(9, (int) r9[0]);
            Assert.assertEquals("harry", r9[1]);
            Assert.assertEquals(210, (int) r9[2]);

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

    // TEST3; select e.empid, e.name, d. no from DeptBean d left join d.emps e order by e.empid
    public void testLoop003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select e.empid, e.name, d.no from DeptBean d left join d.emps e order by e.empid";
            Query q = em.createQuery(qStr);

//            TEST3; select e.empid, e.name, d. no from DeptBean d left join d.emps e order by e.empid
//            e.empid   e.name    d. no
//            ~~~~~~~ ~~~~~~~~~~~ ~~~~~
//               1       david     210
//               2      andrew     210
//               3      minmei     200
//               4      george     200
//               5      ritika     220
//               6       ahmad     100
//               7     charlene    210
//               8    Tom Rayburn  100
//               9       harry     210
//             null      null      300
//             TEST3; 10 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets[] = {
                                   // e.empid   e.name    d. no
                                   new Object[] { 1, "david", 210 },
                                   new Object[] { 2, "andrew", 210 },
                                   new Object[] { 3, "minmei", 210 },
                                   new Object[] { 4, "george", 200 },
                                   new Object[] { 5, "ritika", 220 },
                                   new Object[] { 6, "ahmad", 100 },
                                   new Object[] { 7, "charlene", 210 },
                                   new Object[] { 8, "Tom Rayburn", 100 },
                                   new Object[] { 9, "harry", 210 },
                                   new Object[] { null, null, 300 }
            };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    for (int idx2 = 0; idx2 < targetVals.length; idx2++) {
                        if (targetVals[idx2] == null) {
                            if (result[idx2] != null)
                                continue;
                        } else {
                            if (!targetVals[idx2].equals(result[idx2]))
                                continue;
                        }
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

//
//            List<Object[]> rList = q.getResultList();
//            Assert.assertNotNull(rList);
//            Assert.assertEquals(10, rList.size());
//
//            // 1       david       210
//            Object[] r1 = rList.get(0);
//            Assert.assertNotNull(r1);
//            Assert.assertEquals(1, (int) r1[0]);
//            Assert.assertEquals("david", r1[1]);
//            Assert.assertEquals(210, (int) r1[2]);
//
//            // 2      andrew       210
//            Object[] r2 = rList.get(1);
//            Assert.assertNotNull(r2);
//            Assert.assertEquals(2, (int) r2[0]);
//            Assert.assertEquals("andrew", r2[1]);
//            Assert.assertEquals(210, (int) r2[2]);
//
//            // 3      minmei       200
//            Object[] r3 = rList.get(2);
//            Assert.assertNotNull(r3);
//            Assert.assertEquals(3, (int) r3[0]);
//            Assert.assertEquals("minmei", r3[1]);
//            Assert.assertEquals(200, (int) r3[2]);
//
//            // 4      george       200
//            Object[] r4 = rList.get(3);
//            Assert.assertNotNull(r4);
//            Assert.assertEquals(4, (int) r4[0]);
//            Assert.assertEquals("george", r4[1]);
//            Assert.assertEquals(200, (int) r4[2]);
//
//            // 5      ritika       220
//            Object[] r5 = rList.get(4);
//            Assert.assertNotNull(r5);
//            Assert.assertEquals(5, (int) r5[0]);
//            Assert.assertEquals("ritika", r5[1]);
//            Assert.assertEquals(220, (int) r5[2]);
//
//            // 6       ahmad       100
//            Object[] r6 = rList.get(5);
//            Assert.assertNotNull(r6);
//            Assert.assertEquals(6, (int) r6[0]);
//            Assert.assertEquals("ahmad", r6[1]);
//            Assert.assertEquals(100, (int) r6[2]);
//
//            // 7     charlene      210
//            Object[] r7 = rList.get(6);
//            Assert.assertNotNull(r7);
//            Assert.assertEquals(7, (int) r7[0]);
//            Assert.assertEquals("charlene", r7[1]);
//            Assert.assertEquals(210, (int) r7[2]);
//
//            // 8    Tom Rayburn    100
//            Object[] r8 = rList.get(7);
//            Assert.assertNotNull(r8);
//            Assert.assertEquals(8, (int) r8[0]);
//            Assert.assertEquals("Tom Rayburn", r8[1]);
//            Assert.assertEquals(100, (int) r8[2]);
//
//            // 9       harry       210
//            Object[] r9 = rList.get(8);
//            Assert.assertNotNull(r9);
//            Assert.assertEquals(9, (int) r9[0]);
//            Assert.assertEquals("harry", r9[1]);
//            Assert.assertEquals(210, (int) r9[2]);
//
//            // null      null      300
//            Object[] r10 = rList.get(9);
//            Assert.assertNotNull(r10);
//            Assert.assertEquals(null, r10[0]);
//            Assert.assertEquals(null, r10[1]);
//            Assert.assertEquals(300, (int) r10[2]);

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

    // TEST4; select e.empid, e.name, e.work, e.home from EmpBean e order by e.empid
    public void testLoop004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select e.empid, e.name, e.work, e.home from EmpBean e order by e.empid";
            Query q = em.createQuery(qStr);

//            TEST4; select e.empid, e.name, e.work, e.home from EmpBean e order by e.empid
//            e.empid    e.name           AddressBean               AddressBean
//            ~~~~~~~ ~~~~~~~~~~~~ ~~~~~~~~~~~~~~~~~~~~~~~~~~ ~~~~~~~~~~~~~~~~~~~~~~~~
//               1       david     [555 Silicon Valley Drive]    [1780 Mercury Way]
//               2       andrew    [555 Silicon Valley Drive]    [1780 Mercury Way]
//               3       minmei    [555 Silicon Valley Drive]    [1780 Mercury Way]
//               4       george    [555 Silicon Valley Drive]    [512 Venus Drive]
//               5       ritika    [555 Silicon Valley Drive]  [12440 Vulcan Avenue]
//               6       ahmad      [4983 Plutonium Avenue]   [4983 Plutonium Avenue]
//               7      charlene   [555 Silicon Valley Drive]   [182 Martian Street]
//               8    Tom Rayburn  [555 Silicon Valley Drive]    [6200 Vegas Drive]
//               9       harry        [8900 Jupiter Park]     [150 North First Apt E1]
//              10    Catalina Wei [555 Silicon Valley Drive]           null
//             TEST4; 10 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            // 1       david     [555 Silicon Valley Drive]    [1780 Mercury Way]
            Object[] r1 = rList.get(0);
            Assert.assertNotNull(r1);
            Assert.assertEquals(1, (int) r1[0]);
            Assert.assertEquals("david", r1[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r1[2]).getStreet());
            Assert.assertEquals("1780 Mercury Way", ((AddressBean) r1[3]).getStreet());

            // 2       andrew    [555 Silicon Valley Drive]    [1780 Mercury Way]
            Object[] r2 = rList.get(1);
            Assert.assertNotNull(r2);
            Assert.assertEquals(2, (int) r2[0]);
            Assert.assertEquals("andrew", r2[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r2[2]).getStreet());
            Assert.assertEquals("1780 Mercury Way", ((AddressBean) r2[3]).getStreet());

            // 3       minmei    [555 Silicon Valley Drive]    [1780 Mercury Way]
            Object[] r3 = rList.get(2);
            Assert.assertNotNull(r3);
            Assert.assertEquals(3, (int) r3[0]);
            Assert.assertEquals("minmei", r3[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r3[2]).getStreet());
            Assert.assertEquals("1780 Mercury Way", ((AddressBean) r3[3]).getStreet());

            // 4       george    [555 Silicon Valley Drive]    [512 Venus Drive]
            Object[] r4 = rList.get(3);
            Assert.assertNotNull(r4);
            Assert.assertEquals(4, (int) r4[0]);
            Assert.assertEquals("george", r4[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r4[2]).getStreet());
            Assert.assertEquals("512 Venus Drive", ((AddressBean) r4[3]).getStreet());

            // 5       ritika    [555 Silicon Valley Drive]  [12440 Vulcan Avenue]
            Object[] r5 = rList.get(4);
            Assert.assertNotNull(r5);
            Assert.assertEquals(5, (int) r5[0]);
            Assert.assertEquals("ritika", r5[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r5[2]).getStreet());
            Assert.assertEquals("12440 Vulcan Avenue", ((AddressBean) r5[3]).getStreet());

            // 6       ahmad      [4983 Plutonium Avenue]   [4983 Plutonium Avenue]
            Object[] r6 = rList.get(5);
            Assert.assertNotNull(r6);
            Assert.assertEquals(6, (int) r6[0]);
            Assert.assertEquals("ahmad", r6[1]);
            Assert.assertEquals("4983 Plutonium Avenue", ((AddressBean) r6[2]).getStreet());
            Assert.assertEquals("4983 Plutonium Avenue", ((AddressBean) r6[3]).getStreet());

            // 7      charlene   [555 Silicon Valley Drive]   [182 Martian Street]
            Object[] r7 = rList.get(6);
            Assert.assertNotNull(r7);
            Assert.assertEquals(7, (int) r7[0]);
            Assert.assertEquals("charlene", r7[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r7[2]).getStreet());
            Assert.assertEquals("182 Martian Street", ((AddressBean) r7[3]).getStreet());

            // 8    Tom Rayburn  [555 Silicon Valley Drive]    [6200 Vegas Drive]
            Object[] r8 = rList.get(7);
            Assert.assertNotNull(r8);
            Assert.assertEquals(8, (int) r8[0]);
            Assert.assertEquals("Tom Rayburn", r8[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r8[2]).getStreet());
            Assert.assertEquals("6200 Vegas Drive", ((AddressBean) r8[3]).getStreet());

            // 9    harry        [8900 Jupiter Park]     [150 North First Apt E1]
            Object[] r9 = rList.get(8);
            Assert.assertNotNull(r9);
            Assert.assertEquals(9, (int) r9[0]);
            Assert.assertEquals("harry", r9[1]);
            Assert.assertEquals("8900 Jupiter Park", ((AddressBean) r9[2]).getStreet());
            Assert.assertEquals("150 North First Apt E1", ((AddressBean) r9[3]).getStreet());

            // 10    Catalina Wei [555 Silicon Valley Drive]           null
            Object[] r10 = rList.get(9);
            Assert.assertNotNull(r10);
            Assert.assertEquals(10, (int) r10[0]);
            Assert.assertEquals("Catalina Wei", r10[1]);
            Assert.assertEquals("555 Silicon Valley Drive", ((AddressBean) r10[2]).getStreet());
            Assert.assertEquals(null, r10[3]);

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

    // TEST5; select p.projid, p.name, p.dept.no from ProjectBean p order by p.projid
    public void testLoop005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select p.projid, p.name, p.dept.no from ProjectBean p order by p.projid";
            Query q = em.createQuery(qStr);

//            TEST5; select p.projid, p.name, p.dept.no from ProjectBean p order by p.projid
//            p.projid    p.name    p.dept.no
//            ~~~~~~~~ ~~~~~~~~~~~~ ~~~~~~~~~
//              1000   Project:1000    210
//              2000   Project:2000    220
//             TEST5; 2 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            // 1000   Project:1000    210
            Object[] r1 = rList.get(0);
            Assert.assertNotNull(r1);
            Assert.assertEquals(1000, (int) r1[0]);
            Assert.assertEquals("Project:1000", r1[1]);
            Assert.assertEquals(210, r1[2]);

            // 2000   Project:2000    220
            Object[] r2 = rList.get(1);
            Assert.assertNotNull(r2);
            Assert.assertEquals(2000, (int) r2[0]);
            Assert.assertEquals("Project:2000", r2[1]);
            Assert.assertEquals(220, r2[2]);

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

    // TEST6; select p.projid, p.name, t9.taskid, t9.name, e.empid, e.name from ProjectBean p left join p.tasks t9 left join t9.emps e
    public void testLoop006(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select p.projid, p.name, t9.taskid, t9.name, e.empid, e.name from ProjectBean p left join p.tasks t9 left join t9.emps e";
            Query q = em.createQuery(qStr);

//            TEST6; select p.projid, p.name, t9.taskid, t9.name, e.empid, e.name from ProjectBean p left join p.tasks t9 left join t9.emps e
//            p.projid    p.name    t9.taskid  t9.name   e.empid e.name
//            ~~~~~~~~ ~~~~~~~~~~~~ ~~~~~~~~~ ~~~~~~~~~~ ~~~~~~~ ~~~~~~
//              1000   Project:1000   1010     Design       1    david
//              1000   Project:1000   1020       Code       1    david
//              1000   Project:1000   1020       Code       2    andrew
//              1000   Project:1000   1020       Code       9    harry
//              1000   Project:1000   1030       Test       5    ritika
//              1000   Project:1000   1030       Test       9    harry
//              2000   Project:2000   2010      Design      1    david
//              2000   Project:2000   2020    Code, Test  null    null
//              3000   Project:3000   null       null     null    null
//             TEST6; 9 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false };
            Object[] targets[] = {
                                   // p.projid    p.name    t9.taskid  t9.name   e.empid e.name
                                   new Object[] { 1000, "Project:1000", 1010, "Design", 1, "david" },
                                   new Object[] { 1000, "Project:1000", 1020, "Code", 1, "david" },
                                   new Object[] { 1000, "Project:1000", 1020, "Code", 2, "andrew" },
                                   new Object[] { 1000, "Project:1000", 1020, "Code", 9, "harry" },
                                   new Object[] { 1000, "Project:1000", 1030, "Test", 5, "ritika" },
                                   new Object[] { 1000, "Project:1000", 1030, "Test", 9, "harry" },
                                   new Object[] { 2000, "Project:2000", 2010, "Design", 1, "david" },
                                   new Object[] { 2000, "Project:2000", 2020, "Code, Test", null, null },
                                   new Object[] { 3000, "Project:3000", null, null, null, null },
            };

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    for (int idx2 = 0; idx2 < targetVals.length; idx2++) {
                        if (targetVals[idx2] == null) {
                            if (result[idx2] != null)
                                continue;
                        } else {
                            if (!targetVals[idx2].equals(result[idx2]))
                                continue;
                        }
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST7; select count(d) from DeptBean d
    public void testLoop007(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select count(d) from DeptBean d";
            Query q = em.createQuery(qStr);

//            TEST7; select count(d) from DeptBean d
//            count(d)
//            ~~~~~~~~
//               5
//             TEST7; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertEquals(5l, rList.get(0));

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

    // TEST8; select count(e) from EmpBean e
    public void testLoop008(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select count(e) from EmpBean e";
            Query q = em.createQuery(qStr);

//            TEST8; select count(e) from EmpBean e
//            count(e)
//            ~~~~~~~~
//               10
//             TEST8; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertEquals(10l, rList.get(0));

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

    // TEST9; select count(a) from AddressBean a
    public void testLoop009(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select count(a) from AddressBean a";
            Query q = em.createQuery(qStr);

//            TEST9; select count(a) from AddressBean a
//            count(a)
//            ~~~~~~~~
//               9
//             TEST9; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertEquals(9l, rList.get(0));

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

    //  TEST10; select count(p) from ProjectBean p
    public void testLoop010(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select count(p) from ProjectBean p";
            Query q = em.createQuery(qStr);

//            TEST10; select count(p) from ProjectBean p
//            count(p)
//            ~~~~~~~~
//               3
//             TEST10; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertEquals(3l, rList.get(0));

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

    // TEST11; select count(t) from TaskBean t
    public void testLoop011(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select count(t) from TaskBean t";
            Query q = em.createQuery(qStr);

//            TEST11; select count(t) from TaskBean t
//            count(t)
//            ~~~~~~~~
//               5
//             TEST11; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertEquals(5l, rList.get(0));

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

    // TEST12; select d from DeptBean d
    public void testLoop012(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d";
            Query q = em.createQuery(qStr);

//            TEST12; select d from DeptBean d
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST12; 5 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            boolean found[] = { false, false, false, false, false };
            int targets[] = { 100, 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST13; select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name
    public void testLoop013(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name";
            Query q = em.createQuery(qStr);

//            TEST13; select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST13; 5 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            boolean found[] = { false, false, false, false, false };
            int targets[] = { 100, 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST14; select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm order by dm.name asc
    public void testLoop014(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm order by dm.name asc";
            Query q = em.createQuery(qStr);

//            TEST14; select d from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm order by dm.name asc
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST14; 5 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            boolean found[] = { false, false, false, false, false };
            int targets[] = { 100, 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST15; select d from DeptBean d left join d.mgr m left join m.dept md order by md.name asc
    public void testLoop015(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d left join d.mgr m left join m.dept md order by md.name asc";
            Query q = em.createQuery(qStr);

//            TEST15; select d from DeptBean d left join d.mgr m left join m.dept md order by md.name asc
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST15; 5 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            boolean found[] = { false, false, false, false, false };
            int targets[] = { 100, 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST16; select d from DeptBean d order by d.mgr.dept.mgr.dept.name
    public void testLoop016(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d order by d.mgr.dept.mgr.dept.name ";
            Query q = em.createQuery(qStr);

//            TEST16; select d from DeptBean d order by d.mgr.dept.mgr.dept.name
//            DeptBean
//            ~~~~~~~~
//             [210]
//             [220]
//             TEST16; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 210, 220 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST17; select d from DeptBean d order by d.mgr.dept.mgr.name asc
    public void testLoop017(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d order by d.mgr.dept.mgr.name asc ";
            Query q = em.createQuery(qStr);

//            TEST17; select d from DeptBean d order by d.mgr.dept.mgr.name asc
//            DeptBean
//            ~~~~~~~~
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST17; 4 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(4, rList.size());

            boolean found[] = { false, false, false, false };
            int targets[] = { 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST18; select d from DeptBean d order by d.mgr.dept.name asc
    public void testLoop018(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d order by d.mgr.dept.name asc ";
            Query q = em.createQuery(qStr);

//            TEST18; select d from DeptBean d order by d.mgr.dept.name asc
//            DeptBean
//            ~~~~~~~~
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST18; 4 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(4, rList.size());

            boolean found[] = { false, false, false, false };
            int targets[] = { 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST19; select d from DeptBean d where (d.mgr = (select d.mgr from DeptBean d where d.no = 100))
    public void testLoop019(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (d.mgr = (select d.mgr from DeptBean d where d.no = 100)) ";
            Query q = em.createQuery(qStr);

//            TEST19; select d from DeptBean d where (d.mgr = (select d.mgr from DeptBean d where d.no = 100))
//            DeptBean
//            ~~~~~~~~
//             [100]
//             TEST19; 1 tuple

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            boolean found[] = { false };
            int targets[] = { 100 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST20; select d from DeptBean d where (d.name = 'Dave''s Dept')
    public void testLoop020(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (d.name = 'Dave''s Dept') ";
            Query q = em.createQuery(qStr);

//            TEST20; select d from DeptBean d where (d.name = 'Dave''s Dept')
//            TEST20; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST21; select d from DeptBean d where (d.name = 'WebSphere')
    public void testLoop021(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (d.name = 'WebSphere')";
            Query q = em.createQuery(qStr);

//            TEST21; select d from DeptBean d where (d.name = 'WebSphere')
//            TEST21; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST22; select d from DeptBean d where (d.no < 200 and d.no = 200 or d.no >= 300)
    public void testLoop022(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (d.no < 200 and d.no = 200 or d.no >= 300) ";
            Query q = em.createQuery(qStr);

//            TEST22; select d from DeptBean d where (d.no < 200 and d.no = 200 or d.no >= 300)
//            DeptBean
//            ~~~~~~~~
//             [300]
//             TEST22; 1 tuple

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            boolean found[] = { false };
            int targets[] = { 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST23; select d from DeptBean d where (d.no <= 200 or d.no > 301)
    public void testLoop023(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (d.no <= 200 or d.no > 301) ";
            Query q = em.createQuery(qStr);

//            TEST23; select d from DeptBean d where (d.no <= 200 or d.no > 301)
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             TEST23; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 100, 200 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST24; select d from DeptBean d where (not d.no > 300 or d.no =4)
    public void testLoop024(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where (not d.no > 300 or d.no =4) ";
            Query q = em.createQuery(qStr);

//            TEST24; select d from DeptBean d where (not d.no > 300 or d.no =4)
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [200]
//             [210]
//             [220]
//             [300]
//             TEST24; 5 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            boolean found[] = { false, false, false, false, false };
            int targets[] = { 100, 200, 210, 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST25; select d from DeptBean d where d = d.mgr.dept
    public void testLoop025(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where d = d.mgr.dept ";
            Query q = em.createQuery(qStr);

//            TEST25; select d from DeptBean d where d = d.mgr.dept
//            TEST25; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST26; select d from DeptBean d where d.mgr.salary=10
    public void testLoop026(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where d.mgr.salary=10  ";
            Query q = em.createQuery(qStr);

//            TEST26; select d from DeptBean d where d.mgr.salary=10
//            TEST26; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST27; select d from DeptBean d where d.mgr.salary>0.0 and d.name = 'Sales'
    public void testLoop027(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where d.mgr.salary>0.0 and d.name = 'Sales' ";
            Query q = em.createQuery(qStr);

//            TEST27; select d from DeptBean d where d.mgr.salary>0.0 and d.name = 'Sales'
//            TEST27; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST28; select d from DeptBean d where d.name = 'Sales' or d.name = 'Service'
    public void testLoop028(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where d.name = 'Sales' or d.name = 'Service' ";
            Query q = em.createQuery(qStr);

//            TEST28; select d from DeptBean d where d.name = 'Sales' or d.name = 'Service'
//                            DeptBean
//                            ~~~~~~~~
//                             [220]
//                             [300]
//                             TEST28; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 220, 300 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST29; select d from DeptBean d where d.name = 'nonexisting'
    public void testLoop029(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d where d.name = 'nonexisting' ";
            Query q = em.createQuery(qStr);

//          TEST29; select d from DeptBean d where d.name = 'nonexisting'
//          TEST29; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST30; select d from DeptBean d, in(d.emps) as e where e.salary > 13
    public void testLoop030(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d, in(d.emps) as e where e.salary > 13 ";
            Query q = em.createQuery(qStr);

//            TEST30; select d from DeptBean d, in(d.emps) as e where e.salary > 13
//            DeptBean
//            ~~~~~~~~
//             [200]
//             [210]
//             TEST30; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 200, 210 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST31; select d from DeptBean d, in(d.emps) e where (e.isManager = FALSE)
    public void testLoop031(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d, in(d.emps) e where (e.isManager = FALSE) ";
            Query q = em.createQuery(qStr);

//            TEST31; select d from DeptBean d, in(d.emps) e where (e.isManager = FALSE)
//            DeptBean
//            ~~~~~~~~
//             [210]
//             [210]
//             TEST31; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 210, 210 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST32; select d from DeptBean d, in(d.projects) as p
    public void testLoop032(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from DeptBean d, in(d.projects) as p ";
            Query q = em.createQuery(qStr);

//            TEST32; select d from DeptBean d, in(d.projects) as p
//            DeptBean
//            ~~~~~~~~
//             [210]
//             [220]
//             TEST32; 2 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            boolean found[] = { false, false };
            int targets[] = { 210, 220 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST33; select d from EmpBean e join e.dept d
    public void testLoop033(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e join e.dept d";
            Query q = em.createQuery(qStr);

//            TEST33; select d from EmpBean e join e.dept d
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [100]
//             [200]
//             [200]
//             [210]
//             [210]
//             [210]
//             [210]
//             [220]
//             TEST33; 9 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false };
            int targets[] = { 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST34; select d from EmpBean e join e.dept d where d.name = 'dept1'
    public void testLoop034(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e join e.dept d where d.name = 'dept1'";
            Query q = em.createQuery(qStr);

//          TEST34; select d from EmpBean e join e.dept d where d.name = 'dept1'
//          TEST34; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST35; select d from EmpBean e join e.dept d where d.no > 0
    public void testLoop035(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e join e.dept d where d.no > 0";
            Query q = em.createQuery(qStr);

//            TEST35; select d from EmpBean e join e.dept d where d.no > 0
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [100]
//             [200]
//             [200]
//             [210]
//             [210]
//             [210]
//             [210]
//             [220]
//             TEST35; 9 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false };
            int targets[] = { 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            for (DeptBean result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == result.getNo()) {
                        found[idx] = true;
                        break;
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST36; select d from EmpBean e left join e.dept d
    public void testLoop036(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d";
            Query q = em.createQuery(qStr);

//            TEST36; select d from EmpBean e left join e.dept d
//            DeptBean
//            ~~~~~~~~
//              null
//             [100]
//             [100]
//             [200]
//             [200]
//             [210]
//             [210]
//             [210]
//             [210]
//             [220]
//             TEST36; 10 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false, false };
            Integer targets[] = { null, 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            for (DeptBean result : rList) {
                System.out.println("Result = " + result);
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == null) {
                        if (result == null) {
                            found[idx] = true;
                            break;
                        }
                    } else {
                        if (result != null && targets[idx].equals(result.getNo())) {
                            found[idx] = true;
                            break;
                        }
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST37; select d from EmpBean e left join e.dept d where (e.name = 'john' and e.name = 'ahmad')
    public void testLoop037(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where (e.name = 'john' and e.name = 'ahmad')";
            Query q = em.createQuery(qStr);

//            TEST37; select d from EmpBean e left join e.dept d where (e.name = 'john' and e.name = 'ahmad')
//            TEST37; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST38; select d from EmpBean e left join e.dept d where (e.name = 'john' or e.name = 'ahmad') and d.name is null
    public void testLoop038(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where (e.name = 'john' or e.name = 'ahmad') and d.name is null";
            Query q = em.createQuery(qStr);

//            TEST38; select d from EmpBean e left join e.dept d where (e.name = 'john' or e.name = 'ahmad') and d.name is null
//            TEST38; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST39; select d from EmpBean e left join e.dept d where d.mgr.name = 'Dave' or e.empid > 0
    public void testLoop039(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where d.mgr.name = 'Dave' or e.empid > 0";
            Query q = em.createQuery(qStr);

//            TEST39; select d from EmpBean e left join e.dept d where d.mgr.name = 'Dave' or e.empid > 0
//                            DeptBean
//                            ~~~~~~~~
//                              null
//                             [100]
//                             [100]
//                             [200]
//                             [200]
//                             [210]
//                             [210]
//                             [210]
//                             [210]
//                             [220]
//                             TEST39; 10 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false, false };
            Integer targets[] = { null, 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            for (DeptBean result : rList) {
                System.out.println("Result = " + result);
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == null) {
                        if (result == null) {
                            found[idx] = true;
                            break;
                        }
                    } else {
                        if (result != null && targets[idx].equals(result.getNo())) {
                            found[idx] = true;
                            break;
                        }
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST40; select d from EmpBean e left join e.dept d where d.name = 'dept1'
    public void testLoop040(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where d.name = 'dept1'";
            Query q = em.createQuery(qStr);

//          TEST40; select d from EmpBean e left join e.dept d where d.name = 'dept1'
//          TEST40; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST41; select d from EmpBean e left join e.dept d where d.name is null
    public void testLoop041(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where d.name is null";
            Query q = em.createQuery(qStr);

//          TEST41; select d from EmpBean e left join e.dept d where d.name is null
//            d
//           ~~~~
//           null
//            TEST41; 1 tuple

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());
            Assert.assertNull(rList.get(0));

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

    // TEST42; select d from EmpBean e left join e.dept d where d.no > 0
    public void testLoop042(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where d.no > 0";
            Query q = em.createQuery(qStr);

//            TEST42; select d from EmpBean e left join e.dept d where d.no > 0
//            DeptBean
//            ~~~~~~~~
//             [100]
//             [100]
//             [200]
//             [200]
//             [210]
//             [210]
//             [210]
//             [210]
//             [220]
//             TEST42; 9 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false };
            Integer targets[] = { 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            for (DeptBean result : rList) {
                System.out.println("Result = " + result);
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    if (targets[idx] == null) {
                        if (result == null) {
                            found[idx] = true;
                            break;
                        }
                    } else {
                        if (result != null && targets[idx].equals(result.getNo())) {
                            found[idx] = true;
                            break;
                        }
                    }
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST43; select d from EmpBean e left join e.dept d where e.name = 'john' and e.name = 'ahmad'
    public void testLoop043(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.name = 'john' and e.name = 'ahmad'";
            Query q = em.createQuery(qStr);

//          TEST43; select d from EmpBean e left join e.dept d where e.name = 'john' and e.name = 'ahmad'
//          TEST43; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST44; select d from EmpBean e left join e.dept d where e.name is null
    public void testLoop044(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.name is null";
            Query q = em.createQuery(qStr);

//            TEST44; select d from EmpBean e left join e.dept d where e.name is null
//            TEST44; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST45; select d from EmpBean e left join e.dept d where e.name='name1'
    public void testLoop045(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.name='name1' ";
            Query q = em.createQuery(qStr);

//          TEST45; select d from EmpBean e left join e.dept d where e.name='name1'
//          TEST45; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST46; select d from EmpBean e left join e.dept d where e.name='name3' and e.bonus > 100
    public void testLoop046(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.name='name3' and e.bonus > 100 ";
            Query q = em.createQuery(qStr);

//          TEST46; select d from EmpBean e left join e.dept d where e.name='name3' and e.bonus > 100
//          TEST46; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST47; select d from EmpBean e left join e.dept d where e.salary = 1000 and e.name = 'Bijan'
    public void testLoop047(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.salary = 1000 and e.name = 'Bijan'";
            Query q = em.createQuery(qStr);

//          TEST47; select d from EmpBean e left join e.dept d where e.salary = 1000 and e.name = 'Bijan'
//          TEST47; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST48; select d from EmpBean e left join e.dept d where e.salary = 1000 or e.name = 'Ahmad'
    public void testLoop048(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d where e.salary = 1000 or e.name = 'Ahmad'";
            Query q = em.createQuery(qStr);

//          TEST48; select d from EmpBean e left join e.dept d where e.salary = 1000 or e.name = 'Ahmad'
//          TEST48; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST49; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget
    public void testLoop049(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget";
            Query q = em.createQuery(qStr);

//          TEST49; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget
//          TEST49; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST50; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(d.name is null)
    public void testLoop050(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(d.name is null) ";
            Query q = em.createQuery(qStr);

//           TEST50; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(d.name is null)
//           TEST50; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST51; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and d.name is null
    public void testLoop051(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and d.name is null";
            Query q = em.createQuery(qStr);

//          TEST51; select d from EmpBean e left join e.dept d, ProjectBean p where e.name = 'john' and e.salary = p.budget and d.name is null
//          TEST51; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST52; select d, e.name, p.name from DeptBean as d left outer join d.emps as e left outer join d.projects as p
    public void testLoop052(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d, e.name, p.name from DeptBean as d left outer join d.emps as e left outer join d.projects as p ";
            Query q = em.createQuery(qStr);

//            TEST52; select d, e.name, p.name from DeptBean as d left outer join d.emps as e left outer join d.projects as p
//            DeptBean   e.name       p.name
//            ~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~~
//             [100]      ahmad        null
//             [100]   Tom Rayburn     null
//             [200]     george        null
//             [200]     minmei        null
//             [210]      david    Project:1000
//             [210]      harry    Project:1000
//             [210]     andrew    Project:1000
//             [210]    charlene   Project:1000
//             [220]     ritika    Project:2000
//             [300]      null         null
//             TEST52; 10 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false, false };
            Object[] targets[] = {
                                   // DeptBean.   e.name,       p.name
                                   new Object[] { 100, "ahmad", null },
                                   new Object[] { 100, "Tom Rayburn", null },
                                   new Object[] { 200, "george", null },
                                   new Object[] { 200, "minmei", null },
                                   new Object[] { 210, "david", "Project:1000" },
                                   new Object[] { 210, "harry", "Project:1000" },
                                   new Object[] { 210, "andrew", "Project:1000" },
                                   new Object[] { 210, "charlene", "Project:1000" },
                                   new Object[] { 220, "ritika", "Project:2000" },
                                   new Object[] { 300, null, null }
            };

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    DeptBean db = (DeptBean) result[0];
                    if (db == null) {
                        if (targetVals[0] != null)
                            continue;
                    } else {
                        if (db.getNo() != (int) targetVals[0])
                            continue;
                    }

                    for (int idx2 = 1; idx2 < targetVals.length; idx2++) {
                        if (targetVals[idx2] == null) {
                            if (result[idx2] != null)
                                continue;
                        } else {
                            if (!targetVals[idx2].equals(result[idx2]))
                                continue;
                        }
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            Assert.assertTrue(allFound);

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

    // TEST53; select d,e from EmpBean e join e.dept d where e.bonus<100.02 or e.name='name6'
    public void testLoop053(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d,e from EmpBean e join e.dept d where e.bonus<100.02 or e.name='name6'";
            Query q = em.createQuery(qStr);

//            TEST53; select d,e from EmpBean e join e.dept d where e.bonus<100.02 or e.name='name6'
//            DeptBean EmpBean
//            ~~~~~~~~ ~~~~~~~
//             [100]     [6]
//             [100]     [8]
//             [200]     [3]
//             [200]     [4]
//             [210]     [1]
//             [210]     [2]
//             [210]     [7]
//             [210]     [9]
//             [220]     [5]
//             TEST53; 9 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets[] = {
                                   // DeptBean,  EmpBean
                                   new Object[] { 100, 6 },
                                   new Object[] { 100, 8 },
                                   new Object[] { 200, 3 },
                                   new Object[] { 200, 4 },
                                   new Object[] { 210, 1 },
                                   new Object[] { 210, 2 },
                                   new Object[] { 210, 7 },
                                   new Object[] { 210, 9 },
                                   new Object[] { 220, 5 }
            };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    DeptBean db = (DeptBean) result[0];
                    System.out.println("DeptBean = " + db);
                    if (db == null) {
                        if (targetVals[0] != null)
                            continue;
                    } else {
                        if (db.getNo() != (int) targetVals[0])
                            continue;
                    }

                    EmpBean eb = (EmpBean) result[1];
                    System.out.println("EmpBean = " + eb);
                    if (eb == null) {
                        if (targetVals[1] != null)
                            continue;
                    } else {
                        if (eb.getEmpid() != (int) targetVals[1])
                            continue;
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST54; select d,e from EmpBean e left join e.dept d where d.name='dept1'
    public void testLoop054(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d,e from EmpBean e left join e.dept d where d.name='dept1' ";
            Query q = em.createQuery(qStr);

//          TEST54; select d,e from EmpBean e left join e.dept d where d.name='dept1'
//          TEST54; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST55; select d,e from EmpBean e left join e.dept d where e.bonus<100.02 or e.name='name2'
    public void testLoop055(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d,e from EmpBean e left join e.dept d where e.bonus<100.02 or e.name='name2' ";
            Query q = em.createQuery(qStr);

//            TEST55; select d,e from EmpBean e left join e.dept d where e.bonus<100.02 or e.name='name2'
//                            DeptBean EmpBean
//                            ~~~~~~~~ ~~~~~~~
//                              null    [10]
//                             [100]     [6]
//                             [100]     [8]
//                             [200]     [3]
//                             [200]     [4]
//                             [210]     [1]
//                             [210]     [2]
//                             [210]     [7]
//                             [210]     [9]
//                             [220]     [5]
//                             TEST55; 10 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets[] = {
                                   // DeptBean,  EmpBean
                                   new Object[] { null, 10 },
                                   new Object[] { 100, 6 },
                                   new Object[] { 100, 8 },
                                   new Object[] { 200, 3 },
                                   new Object[] { 200, 4 },
                                   new Object[] { 210, 1 },
                                   new Object[] { 210, 2 },
                                   new Object[] { 210, 7 },
                                   new Object[] { 210, 9 },
                                   new Object[] { 220, 5 },

            };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    DeptBean db = (DeptBean) result[0];
                    System.out.println("DeptBean = " + db);
                    if (db == null) {
                        if (targetVals[0] != null)
                            continue;
                    } else if (targetVals[0] != null) {
                        if (db.getNo() != (int) targetVals[0])
                            continue;
                    } else
                        continue;

                    EmpBean eb = (EmpBean) result[1];
                    System.out.println("EmpBean = " + eb);
                    if (eb == null) {
                        if (targetVals[1] != null)
                            continue;
                    } else {
                        if (eb.getEmpid() != (int) targetVals[1])
                            continue;
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST56; select d,e from EmpBean e left join e.dept d where e.name='name1'
    public void testLoop056(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d,e from EmpBean e left join e.dept d where e.name='name1' ";
            Query q = em.createQuery(qStr);

//          TEST56; select d,e from EmpBean e left join e.dept d where e.name='name1'
//          TEST56; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST57; select d,m,md,dm from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name
    public void testLoop057(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d,m,md,dm from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name ";
            Query q = em.createQuery(qStr);

//           TEST57; select d,m,md,dm from DeptBean d left join d.mgr m left join m.dept md left join md.mgr dm left join dm.dept x order by x.name
//            DeptBean EmpBean DeptBean EmpBean
//            ~~~~~~~~ ~~~~~~~ ~~~~~~~~ ~~~~~~~
//             [100]    [10]     null    null
//             [200]     [8]    [100]    [10]
//             [210]     [3]    [200]     [8]
//             [220]     [4]    [200]     [8]
//             [300]     [6]    [100]    [10]
//             TEST57; 5 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            Object[] targets[] = {
                                   // DeptBean EmpBean DeptBean EmpBean
                                   new Object[] { 100, 10, null, null },
                                   new Object[] { 200, 8, 100, 10 },
                                   new Object[] { 210, 3, 200, 8 },
                                   new Object[] { 220, 4, 200, 8 },
                                   new Object[] { 300, 6, 100, 10 },

            };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Object[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    DeptBean db1 = (DeptBean) result[0];
                    System.out.println("DeptBean = " + db1);
                    if (db1 == null) {
                        if (targetVals[0] != null)
                            continue;
                    } else {
                        if (db1.getNo() != (int) targetVals[0])
                            continue;
                    }

                    DeptBean db2 = (DeptBean) result[2];
                    System.out.println("DeptBean 2 = " + db2);
                    if (db2 == null) {
                        if (targetVals[2] != null)
                            continue;
                    } else {
                        if (db2.getNo() != (int) targetVals[2])
                            continue;
                    }

                    EmpBean eb1 = (EmpBean) result[1];
                    System.out.println("EmpBean = " + eb1);
                    if (eb1 == null) {
                        if (targetVals[1] != null)
                            continue;
                    } else {
                        if (eb1.getEmpid() != (int) targetVals[1])
                            continue;
                    }

                    EmpBean eb2 = (EmpBean) result[3];
                    System.out.println("EmpBean 2 = " + eb2);
                    if (eb2 == null) {
                        if (targetVals[3] != null)
                            continue;
                    } else {
                        if (eb2.getEmpid() != (int) targetVals[3])
                            continue;
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    // TEST58; select d.mgr from EmpBean e join e.dept d where d.mgr.name = 'Ahmad'
    public void testLoop058(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.mgr from EmpBean e join e.dept d where d.mgr.name = 'Ahmad' ";
            Query q = em.createQuery(qStr);

//          TEST58; select d.mgr from EmpBean e join e.dept d where d.mgr.name = 'Ahmad'
//          TEST58; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST59; select d.mgr from EmpBean e join e.dept d where d.name = 'davedept' or e.empid > 0
    public void testLoop059(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.mgr from EmpBean e join e.dept d where d.name = 'davedept' or e.empid > 0 ";
            Query q = em.createQuery(qStr);

//          TEST59; select d.mgr from EmpBean e join e.dept d where d.name = 'davedept' or e.empid > 0
//            EmpBean
//            ~~~~~~~
//              [3]
//              [3]
//              [3]
//              [3]
//              [4]
//              [8]
//              [8]
//             [10]
//             [10]
//             TEST59; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            int targets[] = { 3, 3, 3, 3, 4, 8, 8, 10, 10 };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    int targetVal = targets[idx];

                    EmpBean eb1 = (EmpBean) result;
                    System.out.println("EmpBean = " + eb1);
                    if (eb1 == null) {
                        continue;
                    } else {
                        if (eb1.getEmpid() != targetVal)
                            continue;
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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

    //  TEST60; select d.mgr from EmpBean e join e.dept d where e.empid > 1000
    public void testLoop060(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.mgr from EmpBean e join e.dept d where e.empid > 1000 ";
            Query q = em.createQuery(qStr);

//          TEST60; select d.mgr from EmpBean e join e.dept d where e.empid > 1000
//            TEST60; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST61; select d.mgr from EmpBean e left join e.dept d where d.mgr.name = 'Ahmad'
    public void testLoop061(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.mgr from EmpBean e left join e.dept d where d.mgr.name = 'Ahmad' ";
            Query q = em.createQuery(qStr);

//          TEST61; select d.mgr from EmpBean e left join e.dept d where d.mgr.name = 'Ahmad'
//            TEST61; 0 tuples

            List<DeptBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

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

    // TEST62; select d.mgr from EmpBean e left join e.dept d where d.name = 'davedept' or e.empid > 0
    public void testLoop062(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            String qStr = "select d.mgr from EmpBean e left join e.dept d where d.name = 'davedept' or e.empid > 0";
            Query q = em.createQuery(qStr);

//            TEST62; select d.mgr from EmpBean e left join e.dept d where d.name = 'davedept' or e.empid > 0
//                            EmpBean
//                            ~~~~~~~
//                              [3]
//                              [3]
//                              [3]
//                              [3]
//                              [4]
//                              [8]
//                              [8]
//                             [10]
//                             [10]
//                             null
//                             TEST62; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Integer targets[] = { 3, 3, 3, 3, 4, 8, 8, 10, 10, null };
            boolean found[] = new boolean[targets.length];
            Arrays.fill(found, false);

            for (Object result : rList) {
                System.out.println("Empbean = " + result);
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    Integer targetVal = targets[idx];
                    if (targetVal == null) {
                        if (result != null)
                            continue;
                    } else {
                        EmpBean eb1 = (EmpBean) result;
                        if (eb1 == null) {
                            continue;
                        } else {
                            if (!targetVal.equals(eb1.getEmpid()))
                                continue;
                        }
                    }

                    found[idx] = true;
                    break;
                }
            }

            boolean allFound = true;
            for (boolean b : found) {
                allFound = allFound && b;
            }

            System.out.print("Results: ");
            for (boolean f : found) {
                System.out.print(f + " ");
            }
            System.out.println();
            Assert.assertTrue(allFound);

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
}
