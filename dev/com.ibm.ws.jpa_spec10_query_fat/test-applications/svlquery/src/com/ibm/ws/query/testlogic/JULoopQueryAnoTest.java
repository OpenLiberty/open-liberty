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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.query.entities.ano.AddressBean;
import com.ibm.ws.query.entities.ano.AddressPK;
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

            Object[] targets[] = {
                                   //             d.no   d.name    d.mgr.empid  d.mgr.name  d.reportsTo.no
                                   new Object[] { 100, "CEO", 10, "Catalina Wei", 100 },
                                   new Object[] { 200, "Admin", 8, "Tom Rayburn", 100 },
                                   new Object[] { 210, "Development", 3, "minmei", 200 },
                                   new Object[] { 220, "Service", 4, "george", 200 },
                                   new Object[] { 300, "Sales", 6, "ahmad", 100 }
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets[] = {
                                   //             e.empid   e.name    e.dept.no
                                   new Object[] { 1, "david", 210 },
                                   new Object[] { 2, "andrew", 210 },
                                   new Object[] { 3, "minmei", 200 },
                                   new Object[] { 4, "george", 200 },
                                   new Object[] { 5, "ritika", 220 },
                                   new Object[] { 6, "ahmad", 100 },
                                   new Object[] { 7, "charlene", 210 },
                                   new Object[] { 8, "Tom Rayburn", 100 },
                                   new Object[] { 9, "harry", 210 },
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            // SQLServer treats null as lower value
            Object[] targets[] = (!isSQLServer(lDbProductName)) ? new Object[][] {
                                                                                   // e.empid   e.name    d. no
                                                                                   new Object[] { 1, "david", 210 },
                                                                                   new Object[] { 2, "andrew", 210 },
                                                                                   new Object[] { 3, "minmei", 200 },
                                                                                   new Object[] { 4, "george", 200 },
                                                                                   new Object[] { 5, "ritika", 220 },
                                                                                   new Object[] { 6, "ahmad", 100 },
                                                                                   new Object[] { 7, "charlene", 210 },
                                                                                   new Object[] { 8, "Tom Rayburn", 100 },
                                                                                   new Object[] { 9, "harry", 210 },
                                                                                   new Object[] { null, null, 300 }
            } : new Object[][] {
                                 // e.empid   e.name    d. no
                                 new Object[] { null, null, 300 },
                                 new Object[] { 1, "david", 210 },
                                 new Object[] { 2, "andrew", 210 },
                                 new Object[] { 3, "minmei", 200 },
                                 new Object[] { 4, "george", 200 },
                                 new Object[] { 5, "ritika", 220 },
                                 new Object[] { 6, "ahmad", 100 },
                                 new Object[] { 7, "charlene", 210 },
                                 new Object[] { 8, "Tom Rayburn", 100 },
                                 new Object[] { 9, "harry", 210 }

            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (

        java.lang.AssertionError ae) {
            throw ae;
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

            AddressPK _555SiliconValleyDrive = new AddressPK("555 Silicon Valley Drive");
            AddressPK _1780MercuryWay = new AddressPK("1780 Mercury Way");
            AddressPK _512VenusDrive = new AddressPK("512 Venus Drive");
            AddressPK _12440VulcaneAve = new AddressPK("12440 Vulcan Avenue");
            AddressPK _4983PlutoniumAve = new AddressPK("4983 Plutonium Avenue");
            AddressPK _182MartianSt = new AddressPK("182 Martian Street");
            AddressPK _6200VegasDr = new AddressPK("6200 Vegas Drive");
            AddressPK _8900JupiterPark = new AddressPK("8900 Jupiter Park");
            AddressPK _150NFAptE1 = new AddressPK("150 North First Apt E1");

            Object[] targets[] = {
                                   // e.empid    e.name           AddressBean               AddressBean
                                   new Object[] { 1, "david", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _1780MercuryWay) },
                                   new Object[] { 2, "andrew", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _1780MercuryWay) },
                                   new Object[] { 3, "minmei", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _1780MercuryWay) },
                                   new Object[] { 4, "george", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _512VenusDrive) },
                                   new Object[] { 5, "ritika", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _12440VulcaneAve) },
                                   new Object[] { 6, "ahmad", new EntityValue(AddressBean.class, "street", _4983PlutoniumAve),
                                                  new EntityValue(AddressBean.class, "street", _4983PlutoniumAve) },
                                   new Object[] { 7, "charlene", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _182MartianSt) },
                                   new Object[] { 8, "Tom Rayburn", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive),
                                                  new EntityValue(AddressBean.class, "street", _6200VegasDr) },
                                   new Object[] { 9, "harry", new EntityValue(AddressBean.class, "street", _8900JupiterPark),
                                                  new EntityValue(AddressBean.class, "street", _150NFAptE1) },
                                   new Object[] { 10, "Catalina Wei", new EntityValue(AddressBean.class, "street", _555SiliconValleyDrive), null }
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets[] = {
                                   //  p.projid    p.name    p.dept.no
                                   new Object[] { 1000, "Project:1000", 210 },
                                   new Object[] { 2000, "Project:2000", 220 },
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets = { 5l };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

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

            Object[] targets = { 10l };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets = { 9l };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets = { 3l };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets = { 5l };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            // TODO: Validator needs to have smarts for alternative correct orders.
//            validateQueryResult(testName, qStr, rList, targets, true);

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220)
            };

            // TODO: Validator needs to have smarts for alternative correct orders.
//            validateQueryResult(testName, qStr, rList, targets, true);
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            // TODO: Validator needs to have smarts for alternative correct orders.
//          validateQueryResult(testName, qStr, rList, targets, true);
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            // TODO: Validator needs to have smarts for alternative correct orders.
//          validateQueryResult(testName, qStr, rList, targets, true);
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
            };

            validateQueryResult(testName, qStr, rList, targets);

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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 220),
                                 new EntityValue(DeptBean.class, "no", 300),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);
            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {
                                 null,
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = { null };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            Object[] targets[] = {
                                   // DeptBean.   e.name,       p.name
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), "ahmad", null },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), "Tom Rayburn", null },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), "george", null },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), "minmei", null },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), "david", "Project:1000" },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), "harry", "Project:1000" },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), "andrew", "Project:1000" },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), "charlene", "Project:1000" },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 220), "ritika", "Project:2000" },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 300), null, null }
            };

            validateQueryResult("testLoop052", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 6) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 8) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 3) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 4) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 1) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 2) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 7) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 9) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 220), new EntityValue(EmpBean.class, "empid", 5) }
            };

            validateQueryResult("testLoop053", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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
                                   new Object[] { null, new EntityValue(EmpBean.class, "empid", 10) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 6) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 8) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 3) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 4) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 1) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 2) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 7) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 9) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 220), new EntityValue(EmpBean.class, "empid", 5) },

            };

            validateQueryResult("testLoop056", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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
                                   new Object[] { new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 10), null, null },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 8),
                                                  new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 10) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 210), new EntityValue(EmpBean.class, "empid", 3),
                                                  new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 8) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 220), new EntityValue(EmpBean.class, "empid", 4),
                                                  new EntityValue(DeptBean.class, "no", 200), new EntityValue(EmpBean.class, "empid", 8) },
                                   new Object[] { new EntityValue(DeptBean.class, "no", 300), new EntityValue(EmpBean.class, "empid", 6),
                                                  new EntityValue(DeptBean.class, "no", 100), new EntityValue(EmpBean.class, "empid", 10) },

            };
            validateQueryResult("testLoop057", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
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

    // TEST63; select d.mgr from EmpBean e left join e.dept d where d.name = 'dmqa' and e.salary = 20
    public void testLoop063(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.mgr from EmpBean e left join e.dept d where d.name = 'dmqa' and e.salary = 20";
            Query q = em.createQuery(qStr);

//            TEST63; select d.mgr from EmpBean e left join e.dept d where d.name = 'dmqa' and e.salary = 20
//            TEST63; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST64; select d.mgr from EmpBean e left join e.dept d where d.name is null
    public void testLoop064(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.mgr from EmpBean e left join e.dept d where d.name is null";
            Query q = em.createQuery(qStr);

//            TEST64; select d.mgr from EmpBean e left join e.dept d where d.name is null
//            d.mgr
//            ~~~~~
//            null
//             TEST64; 1 tuple

            List rList = q.getResultList();

            Object[] targets = { null };
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST65; select d.mgr from EmpBean e left join e.dept d where e.empid > 1000
    public void testLoop065(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.mgr from EmpBean e left join e.dept d where e.empid > 1000";
            Query q = em.createQuery(qStr);

//            TEST65; select d.mgr from EmpBean e left join e.dept d where e.empid > 1000
//            TEST65; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST66; select d.mgr.name from EmpBean e join e.dept d where e.name = 'Dave'
    public void testLoop066(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.mgr.name from EmpBean e join e.dept d where e.name = 'Dave'";
            Query q = em.createQuery(qStr);

//            TEST66; select d.mgr.name from EmpBean e join e.dept d where e.name = 'Dave'
//            TEST66; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST67; select d.name from DeptBean as d left outer join d.emps as e  where e.dept = d
    public void testLoop067(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from DeptBean as d left outer join d.emps as e  where e.dept = d";
            Query q = em.createQuery(qStr);

//            TEST67; select d.name from DeptBean as d left outer join d.emps as e  where e.dept = d
//                            d.name
//                          ~~~~~~~~~~~
//                              CEO
//                              CEO
//                             Admin
//                             Admin
//                            Service
//                          Development
//                          Development
//                          Development
//                          Development
//                           TEST67; 9 tuples

            List<String> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            String[] expList = { "CEO", "CEO", "Admin", "Admin", "Service", "Development", "Development", "Development", "Development" };

            for (int idx = 0; idx < expList.length; idx++) {
                final String str = expList[idx];
                Assert.assertNotNull(str);
                Assert.assertEquals(expList[idx], str);
            }

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

    // TEST68; select d.name from DeptBean as d
    public void testLoop068(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from DeptBean as d ";
            Query q = em.createQuery(qStr);

//            TEST68; select d.name from DeptBean as d
//            d.name
//          ~~~~~~~~~~~
//              CEO
//             Admin
//             Sales
//            Service
//          Development
//           TEST68; 5 tuples

            List<String> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            String[] expList = { "CEO", "Admin", "Sales", "Service", "Development" };

            for (int idx = 0; idx < expList.length; idx++) {
                final String str = expList[idx];
                Assert.assertNotNull(str);
                Assert.assertEquals(expList[idx], str);
            }

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

    // TEST69; select d.name from DeptBean d left join d.emps e where d.name = 'DEV'
    public void testLoop069(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from DeptBean d left join d.emps e where d.name = 'DEV' ";
            Query q = em.createQuery(qStr);

//            TEST69; select d.name from DeptBean d left join d.emps e where d.name = 'DEV'
//            TEST69; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST70; select d.name from DeptBean d left join d.emps e where d.name = 'DEV' or e.name = 'harry'
    public void testLoop070(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from DeptBean d left join d.emps e where d.name = 'DEV' or e.name = 'harry'";
            Query q = em.createQuery(qStr);

//            TEST70; select d.name from DeptBean d left join d.emps e where d.name = 'DEV' or e.name = 'harry'
//                            d.name
//                          ~~~~~~~~~~~
//                          Development
//                           TEST70; 1 tuple

            List<String> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            String[] expList = { "Development" };

            for (int idx = 0; idx < expList.length; idx++) {
                final String str = expList[idx];
                Assert.assertNotNull(str);
                Assert.assertEquals(expList[idx], str);
            }

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

    // TEST71; select d.name from DeptBean d where d.name = ''
    public void testLoop071(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from DeptBean d where d.name = ''";
            Query q = em.createQuery(qStr);

//            TEST71; select d.name from DeptBean d where d.name = ''
//            TEST71; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST72; select d.name from EmpBean e join e.dept d where e.name = 'Dave' or e.empid > 0
    public void testLoop072(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name from EmpBean e join e.dept d where e.name = 'Dave' or e.empid > 0";
            Query q = em.createQuery(qStr);

//            TEST72; select d.name from EmpBean e join e.dept d where e.name = 'Dave' or e.empid > 0
//                            d.name
//                          ~~~~~~~~~~~
//                              CEO
//                              CEO
//                             Admin
//                             Admin
//                            Service
//                          Development
//                          Development
//                          Development
//                          Development
//                           TEST72; 9 tuples

            List<String> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            String[] expList = { "CEO", "CEO", "Admin", "Admin", "Service", "Development", "Development", "Development", "Development" };

            for (int idx = 0; idx < expList.length; idx++) {
                final String str = expList[idx];
                Assert.assertNotNull(str);
                Assert.assertEquals(expList[idx], str);
            }

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

    // TEST73; select d.name, e.name from DeptBean d join d.emps e
    public void testLoop073(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name from DeptBean d join d.emps e";
            Query q = em.createQuery(qStr);

//            TEST73; select d.name, e.name from DeptBean d join d.emps e
//            d.name      e.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~
//              CEO        ahmad
//              CEO     Tom Rayburn
//             Admin      george
//             Admin      minmei
//            Service     ritika
//          Development    david
//          Development    harry
//          Development   andrew
//          Development  charlene
//           TEST73; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets[] = {
                                   //             d.name      e.name
                                   new String[] { "CEO", "ahmad" },
                                   new String[] { "CEO", "Tom Rayburn" },
                                   new String[] { "Admin", "george" },
                                   new String[] { "Admin", "minmei" },
                                   new String[] { "Service", "ritika" },
                                   new String[] { "Development", "david" },
                                   new String[] { "Development", "harry" },
                                   new String[] { "Development", "andrew" },
                                   new String[] { "Development", "charlene" }
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST74; select d.name, e.name from DeptBean d left join d.emps e
    public void testLoop074(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name from DeptBean d left join d.emps e";
            Query q = em.createQuery(qStr);

//            TEST74; select d.name, e.name from DeptBean d left join d.emps e
//            d.name      e.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~
//              CEO        ahmad
//              CEO     Tom Rayburn
//             Admin      george
//             Admin      minmei
//             Sales       null
//            Service     ritika
//          Development    david
//          Development    harry
//          Development   andrew
//          Development  charlene
//           TEST74; 10 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false, false };
            String[] targets[] = {
                                   //             d.name      e.name
                                   new String[] { "CEO", "ahmad" },
                                   new String[] { "CEO", "Tom Rayburn" },
                                   new String[] { "Admin", "george" },
                                   new String[] { "Admin", "minmei" },
                                   new String[] { "Sales", null },
                                   new String[] { "Service", "ritika" },
                                   new String[] { "Development", "david" },
                                   new String[] { "Development", "harry" },
                                   new String[] { "Development", "andrew" },
                                   new String[] { "Development", "charlene" }
            };

            validateQueryResult("testLoop074", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST75; select d.name, e.name, m.name from DeptBean d left join d.emps e left join d.mgr m
    public void testLoop075(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name, m.name from DeptBean d left join d.emps e left join d.mgr m";
            Query q = em.createQuery(qStr);

//            TEST75; select d.name, e.name, m.name from DeptBean d left join d.emps e left join d.mgr m
//            d.name      e.name       m.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~~
//              CEO        ahmad    Catalina Wei
//              CEO     Tom Rayburn Catalina Wei
//             Admin      george    Tom Rayburn
//             Admin      minmei    Tom Rayburn
//             Sales       null        ahmad
//            Service     ritika       george
//          Development    david       minmei
//          Development    harry       minmei
//          Development   andrew       minmei
//          Development  charlene      minmei
//           TEST75; 10 tuples

            List<String[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            boolean found[] = { false, false, false, false, false, false, false, false, false, false };
            String[] targets[] = {
                                   //             d.name      e.name     n.name
                                   new String[] { "CEO", "ahmad", "Catalina Wei" },
                                   new String[] { "CEO", "Tom Rayburn", "Catalina Wei" },
                                   new String[] { "Admin", "george", "Tom Rayburn" },
                                   new String[] { "Admin", "minmei", "Tom Rayburn" },
                                   new String[] { "Sales", null, "ahmad" },
                                   new String[] { "Service", "ritika", "george" },
                                   new String[] { "Development", "david", "minmei" },
                                   new String[] { "Development", "harry", "minmei" },
                                   new String[] { "Development", "andrew", "minmei" },
                                   new String[] { "Development", "charlene", "minmei" }
            };

            for (Object[] result : rList) {
                for (int idx = 0; idx < targets.length; idx++) {
                    if (found[idx] == true) {
                        continue;
                    }

                    String[] targetVals = targets[idx];
                    if (targetVals == null && result == null) {
                        found[idx] = true;
                        break;
                    }

                    String dName = (String) result[0];
                    if (dName == null) {
                        if (targetVals[0] != null)
                            continue;
                    } else {
                        if (!targetVals[0].equals(dName))
                            continue;
                    }
                    String eName = (String) result[1];
                    if (eName == null) {
                        if (targetVals[1] != null)
                            continue;
                    } else {
                        if (!targetVals[1].equals(eName))
                            continue;
                    }

                    String nName = (String) result[2];
                    if (nName == null) {
                        if (targetVals[2] != null)
                            continue;
                    } else {
                        if (!targetVals[2].equals(nName))
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

    // TEST76; select d.name, e.name, m.name, p.name from DeptBean d left join d.emps e left join d.mgr m left join e.tasks p
    public void testLoop076(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name, m.name, p.name from DeptBean d left join d.emps e left join d.mgr m left join e.tasks p";
            Query q = em.createQuery(qStr);

//            TEST76; select d.name, e.name, m.name, p.name from DeptBean d left join d.emps e left join d.mgr m left join e.tasks p
//            d.name      e.name       m.name    p.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~~~~~~~ ~~~~~~
//              CEO        ahmad    Catalina Wei  null
//              CEO     Tom Rayburn Catalina Wei  null
//             Admin      george    Tom Rayburn   null
//             Admin      minmei    Tom Rayburn   null
//             Sales       null        ahmad      null
//            Service     ritika       george     Test
//          Development    david       minmei     Code
//          Development    david       minmei    Design
//          Development    david       minmei    Design
//          Development    harry       minmei     Code
//          Development    harry       minmei     Test
//          Development   andrew       minmei     Code
//          Development  charlene      minmei     null
//           TEST76; 13 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(13, rList.size());

//            boolean found[] = { false, false, false, false, false, false, false, false, false, false, false, false, false };
            String[] targets[] = {
                                   //             d.name      e.name     n.name    p.name
                                   new String[] { "CEO", "ahmad", "Catalina Wei", null },
                                   new String[] { "CEO", "Tom Rayburn", "Catalina Wei", null },
                                   new String[] { "Admin", "george", "Tom Rayburn", null },
                                   new String[] { "Admin", "minmei", "Tom Rayburn", null },
                                   new String[] { "Sales", null, "ahmad", null },
                                   new String[] { "Service", "ritika", "george", "Test" },
                                   new String[] { "Development", "david", "minmei", "Code" },
                                   new String[] { "Development", "david", "minmei", "Design" },
                                   new String[] { "Development", "david", "minmei", "Design" },
                                   new String[] { "Development", "harry", "minmei", "Code" },
                                   new String[] { "Development", "harry", "minmei", "Test" },
                                   new String[] { "Development", "andrew", "minmei", "Code" },
                                   new String[] { "Development", "charlene", "minmei", null }
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST77; select d.name, e.name, p.name from DeptBean d join d.emps e join e.tasks p
    public void testLoop077(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name, p.name from DeptBean d join d.emps e join e.tasks p";
            Query q = em.createQuery(qStr);

//            TEST77; select d.name, e.name, p.name from DeptBean d join d.emps e join e.tasks p
//            d.name    e.name p.name
//          ~~~~~~~~~~~ ~~~~~~ ~~~~~~
//            Service   ritika  Test
//          Development andrew  Code
//          Development david   Code
//          Development david  Design
//          Development david  Design
//          Development harry   Code
//          Development harry   Test
//           TEST77; 7 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(7, rList.size());

            String[] targets[] = {
                                   //             d.name    e.name p.name
                                   new String[] { "Service", "ritika", "Test" },
                                   new String[] { "Development", "andrew", "Code" },
                                   new String[] { "Development", "david", "Code" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "harry", "Code" },
                                   new String[] { "Development", "harry", "Test" },
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST78; select d.name, e.name, p.name from DeptBean d join d.emps e, in (e.tasks) p
    public void testLoop078(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name, p.name from DeptBean d join d.emps e, in (e.tasks) p ";
            Query q = em.createQuery(qStr);

//            TEST78; select d.name, e.name, p.name from DeptBean d join d.emps e, in (e.tasks) p
//            d.name    e.name p.name
//          ~~~~~~~~~~~ ~~~~~~ ~~~~~~
//            Service   ritika  Test
//          Development andrew  Code
//          Development david   Code
//          Development david  Design
//          Development david  Design
//          Development harry   Code
//          Development harry   Test
//           TEST78; 7 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(7, rList.size());

            String[] targets[] = {
                                   //             d.name    e.name p.name
                                   new String[] { "Service", "ritika", "Test" },
                                   new String[] { "Development", "andrew", "Code" },
                                   new String[] { "Development", "david", "Code" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "harry", "Code" },
                                   new String[] { "Development", "harry", "Test" },
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST79; select d.name, e.name, p.name from DeptBean d left join d.emps e left join e.tasks p
    public void testLoop079(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, e.name, p.name from DeptBean d left join d.emps e left join e.tasks p";
            Query q = em.createQuery(qStr);

//            TEST79; select d.name, e.name, p.name from DeptBean d left join d.emps e left join e.tasks p
//            d.name      e.name    p.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~ ~~~~~~
//              CEO        ahmad     null
//              CEO     Tom Rayburn  null
//             Admin      george     null
//             Admin      minmei     null
//             Sales       null      null
//            Service     ritika     Test
//          Development    david     Code
//          Development    david    Design
//          Development    david    Design
//          Development    harry     Code
//          Development    harry     Test
//          Development   andrew     Code
//          Development  charlene    null
//           TEST79; 13 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(13, rList.size());

            String[] targets[] = {
                                   //             d.name      e.name    p.name
                                   new String[] { "CEO", "ahmad", null },
                                   new String[] { "CEO", "Tom Rayburn", null },
                                   new String[] { "Admin", "george", null },
                                   new String[] { "Admin", "minmei", null },
                                   new String[] { "Sales", null, null },
                                   new String[] { "Service", "ritika", "Test" },
                                   new String[] { "Development", "david", "Code" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "david", "Design" },
                                   new String[] { "Development", "harry", "Code" },
                                   new String[] { "Development", "harry", "Test" },
                                   new String[] { "Development", "andrew", "Code" },
                                   new String[] { "Development", "charlene", null }
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST80; select d.name, m.name from DeptBean d, in (d.mgr) m
    public void testLoop080(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, m.name from DeptBean d, in (d.mgr) m";
            Query q = em.createQuery(qStr);

//            TEST80; select d.name, m.name from DeptBean d, in (d.mgr) m
//            d.name       m.name
//          ~~~~~~~~~~~ ~~~~~~~~~~~~
//              CEO     Catalina Wei
//             Admin    Tom Rayburn
//             Sales       ahmad
//            Service      george
//          Development    minmei
//           TEST80; 5 tuples

            List<Object[]> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            String[] targets[] = {
                                   //             d.name       m.name
                                   new String[] { "CEO", "Catalina Wei" },
                                   new String[] { "Admin", "Tom Rayburn" },
                                   new String[] { "Sales", "ahmad" },
                                   new String[] { "Service", "george" },
                                   new String[] { "Development", "minmei" }
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST81; select d.name, p.name from DeptBean d join d.mgr m join m.tasks p
    public void testLoop081(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.name, p.name from DeptBean d join d.mgr m join m.tasks p";
            Query q = em.createQuery(qStr);

//            TEST81; select d.name, p.name from DeptBean d join d.mgr m join m.tasks p
//            TEST81; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST82; select d.no from  DeptBean d, in(d.emps) e
    public void testLoop082(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.no from  DeptBean d, in(d.emps) e";
            Query q = em.createQuery(qStr);

//            TEST82; select d.no from  DeptBean d, in(d.emps) e
//            d.no
//            ~~~~
//            100
//            100
//            200
//            200
//            210
//            210
//            210
//            210
//            220
//             TEST82; 9 tuples

            List rList = q.getResultList();
            Object[] targets = { 100, 100, 200, 200, 210, 210, 210, 210, 220 };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST83; select d.no from DeptBean d where d.name <> 'Department1'
    public void testLoop083(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.no from DeptBean d where d.name <> 'Department1'   ";
            Query q = em.createQuery(qStr);

//            TEST83; select d.no from DeptBean d where d.name <> 'Department1'
//            d.no
//            ~~~~
//            100
//            200
//            210
//            220
//            300
//             TEST83; 5 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            Object[] targets = { 100, 200, 210, 220, 300 };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST84; select d.no,d.name,d.budget,d.mgr from DeptBean d
    public void testLoop084(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select d.no,d.name,d.budget,d.mgr from DeptBean d ";
            Query q = em.createQuery(qStr);

//            TEST84; select d.no,d.name,d.budget,d.mgr from DeptBean d
//            d.no   d.name    d.budget EmpBean
//            ~~~~ ~~~~~~~~~~~ ~~~~~~~~ ~~~~~~~
//            100      CEO       2.1     [10]
//            200     Admin      2.1      [8]
//            210  Development   2.1      [3]
//            220    Service     2.1      [4]
//            300     Sales      2.1      [6]
//             TEST84; 5 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            Object[] targets[] = {
                                   new Object[] { 100, "CEO", 2.1f, new EntityValue(EmpBean.class, "empid", 10) },
                                   new Object[] { 200, "Admin", 2.1f, new EntityValue(EmpBean.class, "empid", 8) },
                                   new Object[] { 210, "Development", 2.1f, new EntityValue(EmpBean.class, "empid", 3) },
                                   new Object[] { 220, "Service", 2.1f, new EntityValue(EmpBean.class, "empid", 4) },
                                   new Object[] { 300, "Sales", 2.1f, new EntityValue(EmpBean.class, "empid", 6) },
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST85; select e from DeptBean d inner join d.emps e
    public void testLoop085(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d inner join d.emps e";
            Query q = em.createQuery(qStr);

//            TEST85; select e from DeptBean d inner join d.emps e
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             TEST85; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST86; select e from DeptBean d join d.emps e
    public void testLoop086(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d join d.emps e ";
            Query q = em.createQuery(qStr);

//            TEST86; select e from DeptBean d join d.emps e
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             TEST86; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST87; select e from DeptBean d left join d.emps e
    public void testLoop087(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d left join d.emps e";
            Query q = em.createQuery(qStr);

//            TEST87; select e from DeptBean d left join d.emps e
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             null
//             TEST87; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            final Object[] targets = {
                                       new EntityValue(EmpBean.class, "empid", 1),
                                       new EntityValue(EmpBean.class, "empid", 2),
                                       new EntityValue(EmpBean.class, "empid", 3),
                                       new EntityValue(EmpBean.class, "empid", 4),
                                       new EntityValue(EmpBean.class, "empid", 5),
                                       new EntityValue(EmpBean.class, "empid", 6),
                                       new EntityValue(EmpBean.class, "empid", 7),
                                       new EntityValue(EmpBean.class, "empid", 8),
                                       new EntityValue(EmpBean.class, "empid", 9),
                                       null
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST88; select e from DeptBean d, EmpBean e where d.no = e.salary
    public void testLoop088(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d, EmpBean e where d.no = e.salary ";
            Query q = em.createQuery(qStr);

//          TEST88; select e from DeptBean d, EmpBean e where d.no = e.salary
//          TEST88; 0 tuples

            List<EmpBean> rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(0, rList.size());

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST89; select e from DeptBean d, EmpBean e where e.name = 'john'
    public void testLoop089(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d, EmpBean e where e.name = 'john' ";
            Query q = em.createQuery(qStr);

//            TEST89; select e from DeptBean d, EmpBean e where e.name = 'john'
//                            TEST89; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST90; select e from DeptBean d, EmpBean e where e.salary=d.no
    public void testLoop090(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d, EmpBean e where e.salary=d.no ";
            Query q = em.createQuery(qStr);

//            TEST90; select e from DeptBean d, EmpBean e where e.salary=d.no
//                            TEST90; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST91; select e from DeptBean d, in(d.emps) e where d.name='Sales'
    public void testLoop091(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from DeptBean d, in(d.emps) e where d.name='Sales' ";
            Query q = em.createQuery(qStr);

//            TEST91; select e from DeptBean d, in(d.emps) e where d.name='Sales'
//                            TEST91; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST92; select e from EmpBean e  order by e.name
    public void testLoop092(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e  order by e.name ";
            Query q = em.createQuery(qStr);

//            TEST92; select e from EmpBean e  order by e.name
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST92; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST93; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by d.name, q.salary, m.name
    public void testLoop093(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by d.name, q.salary, m.name";
            Query q = em.createQuery(qStr);

//            TEST93; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by d.name, q.salary, m.name
//            TEST93; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST94; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, d.mgr.name
    public void testLoop094(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, d.mgr.name";
            Query q = em.createQuery(qStr);

//            TEST94; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, d.mgr.name
//            TEST94; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST95; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, e.dept.mgr.name
    public void testLoop095(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, e.dept.mgr.name";
            Query q = em.createQuery(qStr);

//            TEST95; select e from EmpBean e join e.dept d join d.mgr m join m.tasks p , DeptBean d2 left join d2.emps q left join q.tasks p2 where p2.name = 'abc' and p.name is null order by e.dept.name, q.salary, e.dept.mgr.name
//            TEST95; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST96; select e from EmpBean e join e.dept d left join d.mgr m left join m.tasks p left join e.tasks p2 where p2.name = 'abc'
    public void testLoop096(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e join e.dept d left join d.mgr m left join m.tasks p left join e.tasks p2 where p2.name = 'abc'";
            Query q = em.createQuery(qStr);

//            TEST96; select e from EmpBean e join e.dept d left join d.mgr m left join m.tasks p left join e.tasks p2 where p2.name = 'abc'
//            TEST96; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST97; select e from EmpBean e left join e.dept d left join d.mgr m order by m.name asc
    public void testLoop097(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e left join e.dept d left join d.mgr m order by m.name asc";
            Query q = em.createQuery(qStr);

//            TEST97; select e from EmpBean e left join e.dept d left join d.mgr m order by m.name asc
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST97; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST98; select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john' order by ed.name
    public void testLoop098(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john' order by ed.name ";
            Query q = em.createQuery(qStr);

//            TEST98; select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john' order by ed.name
//            TEST98; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST99; select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john'and ed.name = 'dept1'  order by ed.name
    public void testLoop099(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john'and ed.name = 'dept1'  order by ed.name ";
            Query q = em.createQuery(qStr);

//            TEST99; select e from EmpBean e left join e.dept ed, DeptBean d where e.name = 'john'and ed.name = 'dept1'  order by ed.name
//            TEST99; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST100; select e from EmpBean e order  by e.name desc
    public void testLoop100(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e order  by e.name desc ";
            Query q = em.createQuery(qStr);

//            TEST100; select e from EmpBean e order  by e.name desc
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST100; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST101; select e from EmpBean e order by e.dept.mgr.name asc
    public void testLoop101(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e order by e.dept.mgr.name asc ";
            Query q = em.createQuery(qStr);

//            TEST101; select e from EmpBean e order by e.dept.mgr.name asc
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             TEST101; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9)
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST102; select e from EmpBean e order by e.name asc
    public void testLoop102(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e order by e.name asc ";
            Query q = em.createQuery(qStr);

//            TEST102; select e from EmpBean e order by e.name asc
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST102; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10)
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST103; select e from EmpBean e order by e.name asc, e.dept.mgr.name desc , e.salary asc, e.dept.budget desc
    public void testLoop103(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e order by e.name asc, e.dept.mgr.name desc , e.salary asc, e.dept.budget desc ";
            Query q = em.createQuery(qStr);

//            TEST103; select e from EmpBean e order by e.name asc, e.dept.mgr.name desc , e.salary asc, e.dept.budget desc
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             TEST103; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9)
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST104; select e from EmpBean e where (NOT (e.isManager = true))
    public void testLoop104(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (NOT (e.isManager = true))";
            Query q = em.createQuery(qStr);

//            TEST104; select e from EmpBean e where (NOT (e.isManager = true))
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//             TEST104; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST105; select e from EmpBean e where (e.dept.mgr.dept.mgr.name ='Tom')
    public void testLoop105(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.dept.mgr.dept.mgr.name ='Tom') ";
            Query q = em.createQuery(qStr);

//            TEST105; select e from EmpBean e where (e.dept.mgr.dept.mgr.name ='Tom')
//            TEST105; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST106; select e from EmpBean e where (e.dept.mgr.name = 'Tom')
    public void testLoop106(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.dept.mgr.name = 'Tom')";
            Query q = em.createQuery(qStr);

//            TEST106; select e from EmpBean e where (e.dept.mgr.name = 'Tom')
//            TEST106; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST107; select e from EmpBean e where (e.dept.no = 10)
    public void testLoop107(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.dept.no = 10)";
            Query q = em.createQuery(qStr);

//            TEST107; select e from EmpBean e where (e.dept.no = 10)
//            TEST107; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST108; select e from EmpBean e where (e.empid > 5) order by e.name
    public void testLoop108(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.empid > 5) order by e.name ";
            Query q = em.createQuery(qStr);

//            TEST108; select e from EmpBean e where (e.empid > 5) order by e.name
//            EmpBean
//            ~~~~~~~
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST108; 5 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(5, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10),
            };

            validateQueryResult(testName, qStr, rList, targets, true);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST109; select e from EmpBean e where (e.isManager <> TRUE)
    public void testLoop109(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.isManager <> TRUE)";
            Query q = em.createQuery(qStr);

//            TEST109; select e from EmpBean e where (e.isManager <> TRUE)
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//             TEST109; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST110; select e from EmpBean e where (e.isManager = FALSE)
    public void testLoop110(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.isManager = FALSE) ";
            Query q = em.createQuery(qStr);

//            TEST110; select e from EmpBean e where (e.isManager = FALSE)
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//             TEST110; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST111; select e from EmpBean e where (e.isManager = TRUE)
    public void testLoop111(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.isManager = TRUE) ";
            Query q = em.createQuery(qStr);

//            TEST111; select e from EmpBean e where (e.isManager = TRUE)
//            EmpBean
//            ~~~~~~~
//              [3]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST111; 8 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(8, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST112; select e from EmpBean e where (e.salary = 65034.28)
    public void testLoop112(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where (e.salary = 65034.28) ";
            Query q = em.createQuery(qStr);

//            TEST112; select e from EmpBean e where (e.salary = 65034.28)
//            TEST112; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST113; select e from EmpBean e where 12 <= e.salary and 15 > e.salary
    public void testLoop113(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where 12 <= e.salary and 15 > e.salary";
            Query q = em.createQuery(qStr);

//            TEST113; select e from EmpBean e where 12 <= e.salary and 15 > e.salary
//                            EmpBean
//                            ~~~~~~~
//                              [1]
//                              [2]
//                             TEST113; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST114; select e from EmpBean e where 12.1 <= e.salary and 15.5 > e.salary
    public void testLoop114(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where 12.1 <= e.salary and 15.5 > e.salary ";
            Query q = em.createQuery(qStr);

//            TEST114; select e from EmpBean e where 12.1 <= e.salary and 15.5 > e.salary
//                            EmpBean
//                            ~~~~~~~
//                              [1]
//                              [2]
//                             TEST114; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST115; select e from EmpBean e where 5 < e.salary
    public void testLoop115(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where 5 < e.salary";
            Query q = em.createQuery(qStr);

//            TEST115; select e from EmpBean e where 5 < e.salary
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//             TEST115; 3 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(3, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST116; select e from EmpBean e where e = e.dept.mgr
    public void testLoop116(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e = e.dept.mgr ";
            Query q = em.createQuery(qStr);

//            TEST116; select e from EmpBean e where e = e.dept.mgr
//                            TEST116; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST117; select e from EmpBean e where e = e.dept.mgr and e.name = 'abc'
    public void testLoop117(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e = e.dept.mgr and e.name = 'abc' ";
            Query q = em.createQuery(qStr);

//            TEST117; select e from EmpBean e where e = e.dept.mgr and e.name = 'abc'
//                            TEST117; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST118; select e from EmpBean e where e.dept <> e.dept.mgr.dept
    public void testLoop118(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.dept <> e.dept.mgr.dept";
            Query q = em.createQuery(qStr);

//            TEST118; select e from EmpBean e where e.dept <> e.dept.mgr.dept
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [5]
//              [7]
//              [9]
//             TEST118; 7 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(7, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 9)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST119; select e from EmpBean e where e.dept.mgr.salary = 10
    public void testLoop119(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.dept.mgr.salary = 10 ";
            Query q = em.createQuery(qStr);

//            TEST119; select e from EmpBean e where e.dept.mgr.salary = 10
//                            TEST119; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST120; select e from EmpBean e where e.dept.name = 'Sales'
    public void testLoop120(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.dept.name = 'Sales'";
            Query q = em.createQuery(qStr);

//            TEST120; select e from EmpBean e where e.dept.name = 'Sales'
//                            TEST120; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST121; select e from EmpBean e where e.empid = 0
    public void testLoop121(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.empid = 0 ";
            Query q = em.createQuery(qStr);

//            TEST121; select e from EmpBean e where e.empid = 0
//                            TEST121; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST122; select e from EmpBean e where e.empid = 8 and e.dept.no = 2
    public void testLoop122(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.empid = 8 and e.dept.no = 2 ";
            Query q = em.createQuery(qStr);

//            TEST122; select e from EmpBean e where e.empid = 8 and e.dept.no = 2
//                            TEST122; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST123; select e from EmpBean e where e.empid<3 order by e.empid
    public void testLoop123(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.empid<3 order by e.empid";
            Query q = em.createQuery(qStr);

//            TEST123; select e from EmpBean e where e.empid<3 order by e.empid
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//             TEST123; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST124; select e from EmpBean e where e.name = 'david' or e.name = 'andrew'
    public void testLoop124(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.name = 'david' or e.name = 'andrew' ";
            Query q = em.createQuery(qStr);

//            TEST124; select e from EmpBean e where e.name = 'david' or e.name = 'andrew'
//                            EmpBean
//                            ~~~~~~~
//                              [1]
//                              [2]
//                             TEST124; 2 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(2, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2)
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST125; select e from EmpBean e where e.salary < (select avg(e.salary) from EmpBean e where e.salary >0)
    public void testLoop125(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.salary < (select avg(e.salary) from EmpBean e where e.salary >0) ";
            Query q = em.createQuery(qStr);

//            TEST125; select e from EmpBean e where e.salary < (select avg(e.salary) from EmpBean e where e.salary >0)
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST125; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST126; select e from EmpBean e where e.salary > 10 and not (e.name='david' and e.salary = 13.1)
    public void testLoop126(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.salary > 10 and not (e.name='david' and e.salary = 13.1) ";
            Query q = em.createQuery(qStr);

//            TEST126; select e from EmpBean e where e.salary > 10 and not (e.name='david' and e.salary = 13.1)
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//             TEST126; 3 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(3, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST127; select e from EmpBean e where e.salary >0 and e.salary <  100
    public void testLoop127(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.salary >0 and e.salary <  100 ";
            Query q = em.createQuery(qStr);

//            TEST127; select e from EmpBean e where e.salary >0 and e.salary <  100
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//             TEST127; 3 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(3, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST128; select e from EmpBean e where e.salary >0.0 and e.salary <  100.0
    public void testLoop128(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.salary >0.0 and e.salary <  100.0 ";
            Query q = em.createQuery(qStr);

//            TEST128; select e from EmpBean e where e.salary >0.0 and e.salary <  100.0
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//             TEST128; 3 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(3, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST129; select e from EmpBean e where e.salary between 0 and 15.4
    public void testLoop129(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where e.salary between 0 and 15.4";
            Query q = em.createQuery(qStr);

//            TEST129; select e from EmpBean e where e.salary between 0 and 15.4
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST129; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST130; select e from EmpBean e where not ( e.salary > 10)
    public void testLoop130(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where not ( e.salary > 10) ";
            Query q = em.createQuery(qStr);

//            TEST130; select e from EmpBean e where not ( e.salary > 10)
//            EmpBean
//            ~~~~~~~
//              [4]
//              [5]
//              [6]
//              [7]
//              [8]
//              [9]
//             [10]
//             TEST130; 7 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(7, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST131; select e from EmpBean e where not e.salary > 10 and e.salary =10 or e.salary < 20
    public void testLoop131(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e where not e.salary > 10 and e.salary =10 or e.salary < 20 ";
            Query q = em.createQuery(qStr);

//            TEST131; select e from EmpBean e where not e.salary > 10 and e.salary =10 or e.salary < 20
//                            EmpBean
//                            ~~~~~~~
//                              [1]
//                              [2]
//                              [3]
//                              [4]
//                              [5]
//                              [6]
//                              [7]
//                              [8]
//                              [9]
//                             [10]
//                             TEST131; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 5),
                                 new EntityValue(EmpBean.class, "empid", 6),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 8),
                                 new EntityValue(EmpBean.class, "empid", 9),
                                 new EntityValue(EmpBean.class, "empid", 10),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST132; select e from EmpBean e, DeptBean d where d.no = e.salary
    public void testLoop132(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e, DeptBean d where d.no = e.salary ";
            Query q = em.createQuery(qStr);

//            TEST132; select e from EmpBean e, DeptBean d where d.no = e.salary
//                            TEST132; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST133; select e from EmpBean e, DeptBean d where d.no = e.salary+90
    public void testLoop133(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e, DeptBean d where d.no = e.salary+90 ";
            Query q = em.createQuery(qStr);

//            TEST133; select e from EmpBean e, DeptBean d where d.no = e.salary+90
//                            TEST133; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST134; select count(e) from EmpBean e, DeptBean d where e.name = 'john'
    public void testLoop134(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select count(e) from EmpBean e, DeptBean d where e.name = 'john'";
            Query q = em.createQuery(qStr);

//            TEST134; select count(e) from EmpBean e, DeptBean d where e.name = 'john'
//                            count(e)
//                            ~~~~~~~~
//                               0
//                             TEST134; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            Object[] targets = { 0l };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST135; select count(e) from EmpBean e, DeptBean d where e.name = 'john'and e.dept.name = 'dept1'
    public void testLoop135(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select count(e) from EmpBean e, DeptBean d where e.name = 'john'and e.dept.name = 'dept1' ";
            Query q = em.createQuery(qStr);

//            TEST135; select count(e) from EmpBean e, DeptBean d where e.name = 'john'and e.dept.name = 'dept1'
//                            count(e)
//                            ~~~~~~~~
//                               0
//                             TEST135; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            Object[] targets = { 0l };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST136; select count(e) from EmpBean e, DeptBean d where e.salary=d.no
    public void testLoop136(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select count(e) from EmpBean e, DeptBean d where e.salary=d.no ";
            Query q = em.createQuery(qStr);

//            TEST136; select count(e) from EmpBean e, DeptBean d where e.salary=d.no
//                            count(e)
//                            ~~~~~~~~
//                               0
//                             TEST136; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            Object[] targets = { 0l };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST137; select e from EmpBean e, in( e.dept) as d where d.name = 'Sales'
    public void testLoop137(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e, in(e.dept) as d where d.name = 'Sales' ";
            Query q = em.createQuery(qStr);

//            TEST137; select e from EmpBean e, in( e.dept) as d where d.name = 'Sales'
//                            TEST137; 0 tuples

            List rList = q.getResultList();
            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST138; select e from EmpBean e, in( e.dept.mgr) as m where m.salary > 13
    public void testLoop138(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e, in( e.dept.mgr) as m where m.salary > 13 ";
            Query q = em.createQuery(qStr);

//            TEST138; select e from EmpBean e, in( e.dept.mgr) as m where m.salary > 13
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [7]
//              [9]
//             TEST138; 4 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(4, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 9),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST139; select e from EmpBean e, in(e.dept.emps) as emps where emps.salary > 13
    public void testLoop139(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e from EmpBean e, in(e.dept.emps) as emps where emps.salary > 13 ";
            Query q = em.createQuery(qStr);

//            TEST139; select e from EmpBean e, in(e.dept.emps) as emps where emps.salary > 13
//            EmpBean
//            ~~~~~~~
//              [1]
//              [2]
//              [3]
//              [4]
//              [7]
//              [9]
//             TEST139; 6 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(6, rList.size());

            Object[] targets = {
                                 new EntityValue(EmpBean.class, "empid", 1),
                                 new EntityValue(EmpBean.class, "empid", 2),
                                 new EntityValue(EmpBean.class, "empid", 3),
                                 new EntityValue(EmpBean.class, "empid", 4),
                                 new EntityValue(EmpBean.class, "empid", 7),
                                 new EntityValue(EmpBean.class, "empid", 9),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST140; select e, d from EmpBean e, DeptBean d
    public void testLoop140(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e, d from EmpBean e, DeptBean d ";
            Query q = em.createQuery(qStr);

//            TEST140; select e, d from EmpBean e, DeptBean d
//            EmpBean DeptBean
//            ~~~~~~~ ~~~~~~~~
//              [1]    [100]
//              [1]    [200]
//              [1]    [210]
//              [1]    [220]
//              [1]    [300]
//              [2]    [100]
//              [2]    [200]
//              [2]    [210]
//              [2]    [220]
//              [2]    [300]
//              [3]    [100]
//              [3]    [200]
//              [3]    [210]
//              [3]    [220]
//              [3]    [300]
//              [4]    [100]
//              [4]    [200]
//              [4]    [210]
//              [4]    [220]
//              [4]    [300]
//              [5]    [100]
//              [5]    [200]
//              [5]    [210]
//              [5]    [220]
//              [5]    [300]
//              [6]    [100]
//              [6]    [200]
//              [6]    [210]
//              [6]    [220]
//              [6]    [300]
//              [7]    [100]
//              [7]    [200]
//              [7]    [210]
//              [7]    [220]
//              [7]    [300]
//              [8]    [100]
//              [8]    [200]
//              [8]    [210]
//              [8]    [220]
//              [8]    [300]
//              [9]    [100]
//              [9]    [200]
//              [9]    [210]
//              [9]    [220]
//              [9]    [300]
//             [10]    [100]
//             [10]    [200]
//             [10]    [210]
//             [10]    [220]
//             [10]    [300]
//             TEST140; 50 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(50, rList.size());

            Object[] targets[] = { //  EmpBean DeptBean
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 300), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), new EntityValue(DeptBean.class, "no", 220), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), new EntityValue(DeptBean.class, "no", 300), },
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST141; select e,d from EmpBean e join e.dept d
    public void testLoop141(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e,d from EmpBean e join e.dept d ";
            Query q = em.createQuery(qStr);

//            TEST141; select e,d from EmpBean e join e.dept d
//            EmpBean DeptBean
//            ~~~~~~~ ~~~~~~~~
//              [1]    [210]
//              [2]    [210]
//              [3]    [200]
//              [4]    [200]
//              [5]    [220]
//              [6]    [100]
//              [7]    [210]
//              [8]    [100]
//              [9]    [210]
//             TEST141; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets[] = { //  EmpBean DeptBean
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 220), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 210), },

            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST142; select e,d from EmpBean e left join e.dept d
    public void testLoop142(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e,d from EmpBean e left join e.dept d ";
            Query q = em.createQuery(qStr);

//            TEST142; select e,d from EmpBean e left join e.dept d
//            EmpBean DeptBean
//            ~~~~~~~ ~~~~~~~~
//              [1]    [210]
//              [2]    [210]
//              [3]    [200]
//              [4]    [200]
//              [5]    [220]
//              [6]    [100]
//              [7]    [210]
//              [8]    [100]
//              [9]    [210]
//             [10]     null
//             TEST142; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets[] = { //  EmpBean DeptBean
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 1), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 2), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 3), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 4), new EntityValue(DeptBean.class, "no", 200), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 5), new EntityValue(DeptBean.class, "no", 220), },

                                   new Object[] { new EntityValue(EmpBean.class, "empid", 6), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 7), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 8), new EntityValue(DeptBean.class, "no", 100), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 9), new EntityValue(DeptBean.class, "no", 210), },
                                   new Object[] { new EntityValue(EmpBean.class, "empid", 10), null },

            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST143; select e,d from EmpBean e left join e.dept ed, DeptBean d where e.name='name1' and ed.name='dept1'
    public void testLoop143(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e,d from EmpBean e left join e.dept ed, DeptBean d where e.name='name1' and ed.name='dept1' ";
            Query q = em.createQuery(qStr);

//            TEST143; select e,d from EmpBean e left join e.dept ed, DeptBean d where e.name='name1' and ed.name='dept1'
//                            TEST143; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST144; select e.dept from  DeptBean d, in(d.emps) e
    public void testLoop144(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from  DeptBean d, in(d.emps) e ";
            Query q = em.createQuery(qStr);

//            TEST144; select e.dept from  DeptBean d, in(d.emps) e
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
//             TEST144; 9 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(9, rList.size());

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST145; select e.dept from EmpBean e
    public void testLoop145(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e";
            Query q = em.createQuery(qStr);

//            TEST145; select e.dept from EmpBean e
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
//             TEST145; 10 tuples

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(10, rList.size());

            Object[] targets = {
                                 null,
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST146; select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad')
    public void testLoop146(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad')";
            Query q = em.createQuery(qStr);

//            TEST146; select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad')
//            DeptBean
//            ~~~~~~~~
//             [100]
//             TEST146; 1 tuple

            List rList = q.getResultList();
            Assert.assertNotNull(rList);
            Assert.assertEquals(1, rList.size());

            Object[] targets = {
                                 new EntityValue(DeptBean.class, "no", 100)
            };

            validateQueryResult("testLoop146", qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST147; select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad') and e.dept.name is null
    public void testLoop147(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad') and e.dept.name is null ";
            Query q = em.createQuery(qStr);

//            TEST147; select e.dept from EmpBean e where (e.name = 'john' or e.name = 'ahmad') and e.dept.name is null
//            TEST147; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST148; select e.dept from EmpBean e where e.dept.mgr.name = 'Ahmad'
    public void testLoop148(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.dept.mgr.name = 'Ahmad'";
            Query q = em.createQuery(qStr);

//            TEST148; select e.dept from EmpBean e where e.dept.mgr.name = 'Ahmad'
//                            TEST148; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // TEST149; select e.dept from EmpBean e where e.dept.mgr.name = 'Dave' or e.empid > 0
    public void testLoop149(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.dept.mgr.name = 'Dave' or e.empid > 0";
            Query q = em.createQuery(qStr);

//            TEST149; select e.dept from EmpBean e where e.dept.mgr.name = 'Dave' or e.empid > 0
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
//                             TEST149; 10 tuples

            List rList = q.getResultList();

            Object[] targets = {
                                 null,
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 100),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 200),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 210),
                                 new EntityValue(DeptBean.class, "no", 220),
            };

            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    //  TEST150; select e.dept from EmpBean e where e.dept.name = 'Sales'
    public void testLoop150(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.dept.name = 'Sales' ";
            Query q = em.createQuery(qStr);

//            TEST150; select e.dept from EmpBean e where e.dept.name = 'Sales'
//                            TEST150; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST151; select e.dept from EmpBean e where e.dept.name is null
    public void testLoop151(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.dept.name is null";
            Query q = em.createQuery(qStr);

//            TEST151; select e.dept from EmpBean e where e.dept.name is null
//                            TEST151; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST152; select e.dept from EmpBean e where e.name = 'john' and  e.name = 'ahmad'
    public void testLoop152(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.name = 'john' and  e.name = 'ahmad' ";
            Query q = em.createQuery(qStr);

//            TEST152; select e.dept from EmpBean e where e.name = 'john' and  e.name = 'ahmad'
//                            TEST152; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST153; select e.dept from EmpBean e where e.name is null
    public void testLoop153(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.name is null";
            Query q = em.createQuery(qStr);

//            TEST153; select e.dept from EmpBean e where e.name is null
//            TEST153; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//   TEST154; select e.dept from EmpBean e where e.salary = 1000 and e.name = 'Bijan'
    public void testLoop154(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e where e.salary = 1000 and e.name = 'Bijan'";
            Query q = em.createQuery(qStr);

//            TEST154; select e.dept from EmpBean e where e.salary = 1000 and e.name = 'Bijan'
//                            TEST154; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST155; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget
    public void testLoop155(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget";
            Query q = em.createQuery(qStr);

//           TEST155; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget
//                           TEST155; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST156; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(e.dept.name is null)
    public void testLoop156(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(e.dept.name is null)";
            Query q = em.createQuery(qStr);

//            TEST156; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and NOT(e.dept.name is null)
//                            TEST156; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

//  TEST157; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and e.dept.name is not null
    public void testLoop157(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            String qStr = "select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and e.dept.name is not null ";
            Query q = em.createQuery(qStr);

//            TEST157; select e.dept from EmpBean e, ProjectBean p where e.name = 'john' and e.salary = p.budget and e.dept.name is not null
//                            TEST157; 0 tuples

            List rList = q.getResultList();

            Object[] targets = {};
            validateQueryResult(testName, qStr, rList, targets);

            em.clear();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /*
     * Utility Methods
     */

    private class EntityValue {
        private Class entityType;
        private Field pField;
        private Object pValue;

        public EntityValue(Class entityType, String pField, Object pValue) {
            super();

            if (entityType == null || pField == null || pValue == null)
                throw new IllegalArgumentException();

            this.entityType = entityType;

            try {
                this.pField = entityType.getDeclaredField(pField);
                this.pField.setAccessible(true);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            this.pValue = pValue;
        }

        public Class getEntityType() {
            return entityType;
        }

        public Field getpField() {
            return pField;
        }

        public Object getpValue() {
            return pValue;
        }

        @Override
        public String toString() {
            return "EntityValue [" + entityType + "/" + pField.getName() + "/" + pValue + "]";
        }

        public boolean isEqual(Object entity) {
            if (debug)
                System.out.println("isEqual: " + entity + " / " + ((entity != null) ? entity.getClass() : "null"));
            if (entity == null || entity.getClass() != entityType)
                return false;

            try {
                Object eVal = pField.get(entity);
                boolean evals = validateItem(pValue, eVal); // pValue.equals(eVal);

                if (debug)
                    System.out.println("Comparing \"" + eVal + "\" vs \"" + pValue + "\" = " + evals);
                return evals;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static boolean debug = false;

    private void validateQueryResult(String testName, String sql, List<Object[]> rList, Object[] targetVals) {
        validateQueryResult(testName, sql, rList, targetVals, false);
    }

    private void validateQueryResult(String testName, String sql, List<Object[]> rList, Object[] targetVals, boolean enforceOrder) {
        final StringBuilder sb = new StringBuilder();

        Assert.assertNotNull(rList);

        try {
            sb.append("\nValidate Output for test \"" + testName + "\":\n");
            sb.append("SQL: ").append(sql).append("\n");
            sb.append("Expected Output:\n");
            int tvIdx = 0;
            for (Object o : targetVals) {
                sb.append(String.format("%3s", tvIdx++)).append("  ");
                if (o == null) {
                    String output = String.format("%15s", "<null>");
                    sb.append(output).append("\n");
                } else if (o.getClass().isArray()) {
                    Object[] oArr = (Object[]) o;
                    for (int oArrIdx = 0; oArrIdx < oArr.length; oArrIdx++) {
                        String output = (oArr[oArrIdx] == null) ? String.format("%15s", null) : String.format("%15s", oArr[oArrIdx].toString());
                        sb.append(output).append("  ");
                    }
                    sb.append("\n");
                } else {
                    String output = (o == null) ? String.format("%15s", null) : String.format("%15s", o.toString());
                    sb.append(output).append("\n");
                }
            }
            sb.append("\n");

            sb.append("Generated Results:\n");
            tvIdx = 0;
            for (Object o : rList) {
                sb.append(String.format("%3s", tvIdx++)).append("  ");
                if (o == null) {
                    String output = String.format("%15s", "<null>");
                    sb.append(output).append("  ");
                } else if (o.getClass().isArray()) {
                    Object[] oArr = (Object[]) o;
                    for (int oArrIdx = 0; oArrIdx < oArr.length; oArrIdx++) {
                        String output = (oArr[oArrIdx] == null) ? String.format("%15s", null) : String.format("%15s", oArr[oArrIdx].toString());
                        sb.append(output).append("  ");
                    }
                    sb.append("\n");
                } else {
                    String output = (o == null) ? String.format("%15s", null) : String.format("%15s", o.toString());
                    sb.append(output).append("\n");
                }
            }
            sb.append("\n");
        } finally {
            System.out.println(sb.toString());

            sb.setLength(0);
        }

        Assert.assertEquals(targetVals.length, rList.size());

        int targetCount = targetVals.length;
        boolean[] found = new boolean[targetCount];
        Arrays.fill(found, false);

        if (enforceOrder) {
            for (int idx = 0; idx < targetCount; idx++) {
                final Object resultObj = rList.get(idx);
                final Object targetVal = targetVals[idx];

                if (targetVal == null) {
                    if (debug) {
                        System.out.println("EnforceOrder = true, targetVal = null resultObj = " + resultObj);
                    }
                    if (resultObj == null) {
                        found[idx] = true;
                        continue;
                    } else {
                        continue;
                    }
                }

                final boolean isComplexType = targetVal.getClass().isArray();

                if (isComplexType) {
                    final Object[] resultObjArr = (Object[]) resultObj;
                    final Object[] targetValArr = (Object[]) targetVal;

                    if (resultObjArr.length != targetValArr.length) {
                        if (debug) {
                            System.out.println("resultObjArr.length != targetValArr.length");
                        }
                        continue;
                    }

                    boolean failedCheck = false;
                    for (int idx2 = 0; idx2 < targetValArr.length; idx2++) {
                        if (failedCheck)
                            break;

                        final Object r1 = resultObjArr[idx2];
                        final Object v1 = targetValArr[idx2];

                        if (debug) {
                            System.out.println("r1 = " + r1);
                            System.out.println("v1 = " + v1);
                        }

                        if (r1 == null) {
                            if (v1 != null) {
                                failedCheck = true;
                                continue;
                            }
                        } else if (v1 == null) {
                            // We know that r1 isn't null at this point.
                            failedCheck = true;
                            continue;
                        } else {
                            Class rtype = r1.getClass();
                            Class vtype = v1.getClass();

                            if (vtype == EntityValue.class) {
                                EntityValue vEv = (EntityValue) v1;
                                if (!vEv.isEqual(r1)) {
                                    failedCheck = true;
                                    continue;
                                }
                            } else {
                                if (!rtype.equals(vtype)) {
                                    failedCheck = true;
                                    continue;
                                }

                                if (r1.equals(v1) == false) {
                                    failedCheck = true;
                                    continue;
                                }
                            }
                        }
                    }
                    if (failedCheck)
                        continue;
                } else {
                    if (debug) {
                        System.out.println("resultObj = " + resultObj);
                        System.out.println("targetVal = " + targetVal);
                    }
                    if (resultObj == null) {
                        if (targetVal != null) {
                            continue;
                        }
                    } else if (targetVal == null) {
                        // We know resultObj is not null at this point.
                        continue;
                    } else {
                        Class rtype = resultObj.getClass();
                        Class vtype = targetVal.getClass();

                        if (vtype == EntityValue.class) {
                            EntityValue vEv = (EntityValue) targetVal;
                            if (!vEv.isEqual(resultObj)) {
                                continue;
                            }
                        } else if (!resultObj.equals(targetVal)) {
                            continue;
                        }
                    }
                }

                found[idx] = true;
            }
        } else {
            // Compare, without enforcing query result order
            if (debug)
                System.out.println("Checking without order enforcement.");
            for (final Object resultObj : rList) {
                if (debug) {
                    System.out.println("**********");
                    System.out.println("Examining " + resultObj);
                }
                for (int idx = 0; idx < targetCount; idx++) {
                    if (found[idx])
                        continue;

                    final Object targetVal = targetVals[idx];

                    if (targetVal == null) {
                        if (resultObj == null) {
                            found[idx] = true;
                            break;
                        } else {
                            continue;
                        }
                    }

                    final boolean isComplexType = targetVal.getClass().isArray();
                    if (debug)
                        System.out.println("isComplexType = " + isComplexType);

                    if (isComplexType) {
                        final Object[] resultObjArr = (Object[]) resultObj;
                        final Object[] targetValArr = (Object[]) targetVal;

                        if (resultObjArr.length != targetValArr.length) {
                            continue;
                        }

                        boolean failedCheck = false;
                        for (int idx2 = 0; idx2 < targetValArr.length; idx2++) {
                            if (failedCheck)
                                break;

                            if (debug)
                                System.out.println("idx2 = " + idx2);

                            final Object r1 = resultObjArr[idx2];
                            final Object v1 = targetValArr[idx2];

                            if (!validateItem(v1, r1)) {
                                failedCheck = true;
                                continue;
                            }
                        }
                        if (failedCheck)
                            continue;
                    } else {
                        if (!validateItem(targetVal, resultObj)) {
                            continue;
                        }
                    }

                    found[idx] = true;
                    break;
                }
            }
        }

        sb.append(testName).append(" Found chain: ");
        for (boolean b : found) {
            sb.append(b).append(" ");
        }
        System.out.println(sb.toString());

        boolean allFound = true;
        for (boolean b : found) {
            allFound = allFound && b;
        }
        System.out.println("Final test result: " + allFound);

        Assert.assertTrue(sb.toString(), allFound);
    }

    private boolean validateItem(Object expect, Object actual) {
        if (debug)
            System.out.println("validateItem: " + expect + " / " + actual);

        // Handle Nulls First
        if (actual == null) {
            return expect == null;
        }

        if (expect == null) {
            return false;
        }

        final Class etype = expect.getClass();
        final Class atype = actual.getClass();

        if (debug) {
            System.out.println("etype = " + etype);
            System.out.println("atype = " + atype);
        }

        if (etype == EntityValue.class) {
            EntityValue vEv = (EntityValue) expect;
            boolean result = vEv.isEqual(actual);
            if (debug)
                System.out.println("vEv.isEqual(actual) == " + result);
            return result;
        } else if (etype == float.class || etype == Float.class || etype == double.class || etype == Double.class) {
            final double delta = 0.1;
            try {
                if (etype == float.class) {
                    Assert.assertEquals((float) expect, (float) actual, delta);
                }
                if (etype == double.class) {
                    Assert.assertEquals((double) expect, (double) actual, delta);
                }
                if (etype == Float.class) {
                    Assert.assertEquals((Float) expect, (Float) actual, delta);
                }
                if (etype == Double.class) {
                    Assert.assertEquals((Double) expect, (Double) actual, delta);
                }
            } catch (java.lang.AssertionError ae) {
                return false;
            }
            return true;
        } else {
            boolean result = expect.equals(actual);
            if (debug)
                System.out.println("expect.equals(actual) == " + result);
            return result;
        }
    }
}
