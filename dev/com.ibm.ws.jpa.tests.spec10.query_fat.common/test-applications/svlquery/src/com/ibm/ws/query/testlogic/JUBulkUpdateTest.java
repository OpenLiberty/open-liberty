/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.testlogic;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.query.entities.ano.DeptBean;
import com.ibm.ws.query.entities.interfaces.IAddressBean;
import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;
import com.ibm.ws.query.testlogic.enums.TestEntityTypeEnum;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

@SuppressWarnings("unchecked")
public class JUBulkUpdateTest extends AbstractTestLogic {

    private static IAddressBean[] ADDRESS;
    private static ICharityFund[] CHARITY;
    private static IDeptBean[] DEPT;
    private static IEmpBean[] EMP;
    private static IProjectBean[] PROJ;
    private static ITaskBean[] TASK;

    private static boolean POPULATED = false;

    private static void populate(JPAResource jpaResource, TestEntityTypeEnum testEntityType) {
        ADDRESS = new IAddressBean[9];
        CHARITY = new ICharityFund[2];
        DEPT = new IDeptBean[5];
        EMP = new IEmpBean[10];
        PROJ = new IProjectBean[3];
        TASK = new ITaskBean[5];

        // Populate
        try {
            System.out.println("JUBulkUpdateTest.populate(): Begin");

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

            if (TestEntityTypeEnum.ANNO.equals(testEntityType)) {
                DEPT[0] = new com.ibm.ws.query.entities.ano.DeptBean(210, "Development");
                DEPT[1] = new com.ibm.ws.query.entities.ano.DeptBean(220, "Service");
                DEPT[2] = new com.ibm.ws.query.entities.ano.DeptBean(300, "Sales");
                DEPT[3] = new com.ibm.ws.query.entities.ano.DeptBean(200, "Admin");
                DEPT[4] = new com.ibm.ws.query.entities.ano.DeptBean(100, "CEO");

                ADDRESS[0] = new com.ibm.ws.query.entities.ano.AddressBean("1780 Mercury Way", "Morgan Hill", "CA", "95037");
                ADDRESS[1] = new com.ibm.ws.query.entities.ano.AddressBean("512 Venus Drive", "Morgan Hill", "CA", "95037");
                ADDRESS[2] = new com.ibm.ws.query.entities.ano.AddressBean("12440 Vulcan Avenue", "Los Altos", "CA", "95130");
                ADDRESS[3] = new com.ibm.ws.query.entities.ano.AddressBean("4983 Plutonium Avenue", "Palo Alto", "CA", "95140");
                ADDRESS[4] = new com.ibm.ws.query.entities.ano.AddressBean("182 Martian Street", "San Jose", "CA", "95120");
                ADDRESS[5] = new com.ibm.ws.query.entities.ano.AddressBean("555 Silicon Valley Drive", "San Jose", "CA", "94120");
                ADDRESS[6] = new com.ibm.ws.query.entities.ano.AddressBean("6200 Vegas Drive", "San Jose", "CA", "95120");
                ADDRESS[7] = new com.ibm.ws.query.entities.ano.AddressBean("150 North First Apt E1", "San Jose", "CA", "94003");
                ADDRESS[8] = new com.ibm.ws.query.entities.ano.AddressBean("8900 Jupiter Park", "San Jose", "CA", "94005");

                EMP[0] = new com.ibm.ws.query.entities.ano.EmpBean(1, "david", 12.1, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[0], 1, false, ADDRESS[0], ADDRESS[5]);
                EMP[1] = new com.ibm.ws.query.entities.ano.EmpBean(2, "andrew", 13.1, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[0], 1, false, ADDRESS[0], ADDRESS[5]);
                EMP[2] = new com.ibm.ws.query.entities.ano.EmpBean(3, "minmei", 15.5, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[3], 2, true, ADDRESS[0], ADDRESS[5]);
                EMP[3] = new com.ibm.ws.query.entities.ano.EmpBean(4, "george", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[3], 2, true, ADDRESS[1], ADDRESS[5]);
                EMP[4] = new com.ibm.ws.query.entities.ano.EmpBean(5, "ritika", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[1], 3, true, ADDRESS[2], ADDRESS[5]);
                EMP[5] = new com.ibm.ws.query.entities.ano.EmpBean(6, "ahmad", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[4], 3, true, ADDRESS[3], ADDRESS[3]);
                EMP[6] = new com.ibm.ws.query.entities.ano.EmpBean(7, "charlene", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[0], 4, true, ADDRESS[4], ADDRESS[5]);
                EMP[7] = new com.ibm.ws.query.entities.ano.EmpBean(8, "Tom Rayburn", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[4], 4, true, ADDRESS[6], ADDRESS[5]);
                EMP[8] = new com.ibm.ws.query.entities.ano.EmpBean(9, "harry", 0, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[0], 5, true, ADDRESS[7], ADDRESS[8]);
                EMP[9] = new com.ibm.ws.query.entities.ano.EmpBean(10, "Catalina Wei", 0, null, 5, true, null, ADDRESS[5]);

                PROJ[0] = new com.ibm.ws.query.entities.ano.ProjectBean(1000, "WebSphere Version 1", (byte) 10, (short) 20, 50, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[0]);
                PROJ[1] = new com.ibm.ws.query.entities.ano.ProjectBean(2000, "WebSphere Feature Pack", (byte) 40, (short) 10, 100, (com.ibm.ws.query.entities.ano.DeptBean) DEPT[1]);
                PROJ[2] = new com.ibm.ws.query.entities.ano.ProjectBean(3000, "WebSphere Community Edition", (byte) 45, (short) 10, 100, null);

                TASK[0] = new com.ibm.ws.query.entities.ano.TaskBean(1010, "Design ", "Design ", (com.ibm.ws.query.entities.ano.ProjectBean) PROJ[0]);
                TASK[1] = new com.ibm.ws.query.entities.ano.TaskBean(1020, "Code", "Code", (com.ibm.ws.query.entities.ano.ProjectBean) PROJ[0]);
                TASK[2] = new com.ibm.ws.query.entities.ano.TaskBean(1030, "Test", "Test", (com.ibm.ws.query.entities.ano.ProjectBean) PROJ[0]);
                TASK[3] = new com.ibm.ws.query.entities.ano.TaskBean(2010, "Design", "Design", (com.ibm.ws.query.entities.ano.ProjectBean) PROJ[1]);
                TASK[4] = new com.ibm.ws.query.entities.ano.TaskBean(2020, "Code, Test", "Code, Test", (com.ibm.ws.query.entities.ano.ProjectBean) PROJ[1]);

                CHARITY[0] = new com.ibm.ws.query.entities.ano.CharityFund("WorldWildlifeFund", 1000.);
                CHARITY[1] = new com.ibm.ws.query.entities.ano.CharityFund("GlobalWarmingFund", 2000.);
            } else {
                DEPT[0] = new com.ibm.ws.query.entities.xml.XMLDeptBean(210, "Development");
                DEPT[1] = new com.ibm.ws.query.entities.xml.XMLDeptBean(220, "Service");
                DEPT[2] = new com.ibm.ws.query.entities.xml.XMLDeptBean(300, "Sales");
                DEPT[3] = new com.ibm.ws.query.entities.xml.XMLDeptBean(200, "Admin");
                DEPT[4] = new com.ibm.ws.query.entities.xml.XMLDeptBean(100, "CEO");

                ADDRESS[0] = new com.ibm.ws.query.entities.xml.XMLAddressBean("1780 Mercury Way", "Morgan Hill", "CA", "95037");
                ADDRESS[1] = new com.ibm.ws.query.entities.xml.XMLAddressBean("512 Venus Drive", "Morgan Hill", "CA", "95037");
                ADDRESS[2] = new com.ibm.ws.query.entities.xml.XMLAddressBean("12440 Vulcan Avenue", "Los Altos", "CA", "95130");
                ADDRESS[3] = new com.ibm.ws.query.entities.xml.XMLAddressBean("4983 Plutonium Avenue", "Palo Alto", "CA", "95140");
                ADDRESS[4] = new com.ibm.ws.query.entities.xml.XMLAddressBean("182 Martian Street", "San Jose", "CA", "95120");
                ADDRESS[5] = new com.ibm.ws.query.entities.xml.XMLAddressBean("555 Silicon Valley Drive", "San Jose", "CA", "94120");
                ADDRESS[6] = new com.ibm.ws.query.entities.xml.XMLAddressBean("6200 Vegas Drive", "San Jose", "CA", "95120");
                ADDRESS[7] = new com.ibm.ws.query.entities.xml.XMLAddressBean("150 North First Apt E1", "San Jose", "CA", "94003");
                ADDRESS[8] = new com.ibm.ws.query.entities.xml.XMLAddressBean("8900 Jupiter Park", "San Jose", "CA", "94005");

                EMP[0] = new com.ibm.ws.query.entities.xml.XMLEmpBean(1, "david", 12.1, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[0], 1, false, ADDRESS[0], ADDRESS[5]);
                EMP[1] = new com.ibm.ws.query.entities.xml.XMLEmpBean(2, "andrew", 13.1, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[0], 1, false, ADDRESS[0], ADDRESS[5]);
                EMP[2] = new com.ibm.ws.query.entities.xml.XMLEmpBean(3, "minmei", 15.5, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[3], 2, true, ADDRESS[0], ADDRESS[5]);
                EMP[3] = new com.ibm.ws.query.entities.xml.XMLEmpBean(4, "george", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[3], 2, true, ADDRESS[1], ADDRESS[5]);
                EMP[4] = new com.ibm.ws.query.entities.xml.XMLEmpBean(5, "ritika", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[1], 3, true, ADDRESS[2], ADDRESS[5]);
                EMP[5] = new com.ibm.ws.query.entities.xml.XMLEmpBean(6, "ahmad", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[4], 3, true, ADDRESS[3], ADDRESS[3]);
                EMP[6] = new com.ibm.ws.query.entities.xml.XMLEmpBean(7, "charlene", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[0], 4, true, ADDRESS[4], ADDRESS[5]);
                EMP[7] = new com.ibm.ws.query.entities.xml.XMLEmpBean(8, "Tom Rayburn", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[4], 4, true, ADDRESS[6], ADDRESS[5]);
                EMP[8] = new com.ibm.ws.query.entities.xml.XMLEmpBean(9, "harry", 0, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[0], 5, true, ADDRESS[7], ADDRESS[8]);
                EMP[9] = new com.ibm.ws.query.entities.xml.XMLEmpBean(10, "Catalina Wei", 0, null, 5, true, null, ADDRESS[5]);

                PROJ[0] = new com.ibm.ws.query.entities.xml.XMLProjectBean(1000, "WebSphere Version 1", (byte) 10, (short) 20, 50, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[0]);
                PROJ[1] = new com.ibm.ws.query.entities.xml.XMLProjectBean(2000, "WebSphere Feature Pack", (byte) 40, (short) 10, 100, (com.ibm.ws.query.entities.xml.XMLDeptBean) DEPT[1]);
                PROJ[2] = new com.ibm.ws.query.entities.xml.XMLProjectBean(3000, "WebSphere Community Edition", (byte) 45, (short) 10, 100, null);

                TASK[0] = new com.ibm.ws.query.entities.xml.XMLTaskBean(1010, "Design ", "Design ", (com.ibm.ws.query.entities.xml.XMLProjectBean) PROJ[0]);
                TASK[1] = new com.ibm.ws.query.entities.xml.XMLTaskBean(1020, "Code", "Code", (com.ibm.ws.query.entities.xml.XMLProjectBean) PROJ[0]);
                TASK[2] = new com.ibm.ws.query.entities.xml.XMLTaskBean(1030, "Test", "Test", (com.ibm.ws.query.entities.xml.XMLProjectBean) PROJ[0]);
                TASK[3] = new com.ibm.ws.query.entities.xml.XMLTaskBean(2010, "Design", "Design", (com.ibm.ws.query.entities.xml.XMLProjectBean) PROJ[1]);
                TASK[4] = new com.ibm.ws.query.entities.xml.XMLTaskBean(2020, "Code, Test", "Code, Test", (com.ibm.ws.query.entities.xml.XMLProjectBean) PROJ[1]);

                CHARITY[0] = new com.ibm.ws.query.entities.xml.XMLCharityFund("WorldWildlifeFund", 1000.);
                CHARITY[1] = new com.ibm.ws.query.entities.xml.XMLCharityFund("GlobalWarmingFund", 2000.);
            }

            TASK[0].addEmp(EMP[0]);
            TASK[1].addEmp(EMP[0]);
            TASK[1].addEmp(EMP[1]);
            TASK[1].addEmp(EMP[8]);
            TASK[2].addEmp(EMP[8]);
            TASK[2].addEmp(EMP[4]);
            TASK[3].addEmp(EMP[0]);

            // Persist new entities
            for (IAddressBean a : ADDRESS) {
                IAddressBean a2 = em.find(a.getClass(), a.getStreet());
                if (a2 == null)
                    em.persist(a);
            }
            for (IEmpBean e : EMP) {
                IEmpBean e2 = em.find(e.getClass(), e.getEmpid());
                if (e2 == null)
                    em.persist(e);
            }
            for (ITaskBean t : TASK) {
                ITaskBean t2 = em.find(t.getClass(), t.getTaskid());
                if (t2 == null)
                    em.persist(t);
            }
            for (IProjectBean p : PROJ) {
                IProjectBean p2 = em.find(p.getClass(), p.getProjid());
                if (p2 == null)
                    em.persist(p);
            }
            for (IDeptBean d : DEPT) {
                IDeptBean d2 = em.find(d.getClass(), d.getNo());
                if (d2 == null)
                    em.persist(d);
            }

            DEPT[0].setReportsTo(DEPT[3]);
            DEPT[1].setReportsTo(DEPT[3]);
            DEPT[2].setReportsTo(DEPT[4]);
            DEPT[3].setReportsTo(DEPT[4]);
            DEPT[4].setReportsTo(DEPT[4]);

            DEPT[0].setMgr(EMP[2]);
            DEPT[1].setMgr(EMP[3]);
            DEPT[2].setMgr(EMP[5]);
            DEPT[3].setMgr(EMP[7]);
            DEPT[4].setMgr(EMP[9]);

            DEPT[0].setCharityFund(CHARITY[0]);
            DEPT[1].setCharityFund(CHARITY[1]);

            System.out.println("Committing transaction...");
            tj.commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            JUBulkUpdateTest.POPULATED = true;
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.populate(): End");
        }
    }

    private static void cleanup(JPAResource jpaResource, TestEntityTypeEnum testEntityType) {
        if (JUBulkUpdateTest.POPULATED) {
            try {
                System.out.println("JUBulkUpdateTest.cleanup(): Begin");

                EntityManager em = jpaResource.getEm();
                TransactionJacket tj = jpaResource.getTj();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Cleaning up EmpBean entities...");
                for (IEmpBean e : EMP) {
                    Integer id = e.getEmpid();
                    System.out.println("Cleaning up EmpBean [" + id + "]...");
                    e = em.find(e.getClass(), id);
                    System.out.println("Found [" + id + "] = " + e + "...");
                    if (e != null)
                        em.remove(e);
                }

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Cleaning up DeptBean entities...");
                for (IDeptBean d : DEPT) {
                    d = em.find(d.getClass(), d.getNo());
                    if (d != null)
                        em.remove(d);
                }

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Cleaning up AddressBean entities...");
                for (IAddressBean a : ADDRESS) {
                    a = em.find(a.getClass(), a.getStreet());
                    if (a != null)
                        em.remove(a);
                }

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Cleaning up ProjectBean entities...");
                for (IProjectBean p : PROJ) {
                    p = em.find(p.getClass(), p.getProjid());
                    if (p != null)
                        em.remove(p);
                }

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Cleaning up TaskBean entities...");
                for (ITaskBean t : TASK) {
                    t = em.find(t.getClass(), t.getTaskid());
                    if (t != null)
                        em.remove(t);
                }

                System.out.println("Committing transaction...");
                tj.commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                em.clear();

                JUBulkUpdateTest.POPULATED = false;
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            } finally {
                System.out.println("JUBulkUpdateTest.cleanup(): End");
            }
        }
    }

    public void testNamedDeleteQuery(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        /*
         * TODO: Hibernate fails populate/cleanup steps of testing due to a possible bug or behavior difference.
         * Hibernate cleanup fails to find() some entity id values due to relationship issues. This causes the next
         * test to fail populate due to duplicateconstrantviolations.
         * Hibernate population fails to find() some entity id values for an unknown reason, but then still throws
         * EntityExistsExceptions
         */
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testNamedDeleteQuery(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // query = "delete FROM DeptBean d WHERE d.no > :deptNo"
                String queryName = "deleteDepartmentsGreaterThan";
                int deptNo = 200;

                System.out.println("Creating named query " + queryName + ")...");
                Query q1 = jpaResource.getEm().createNamedQuery(queryName);
                q1.setParameter("deptNo", deptNo);

                System.out.println("Executing named query " + queryName + " (deptNo=" + deptNo + ")...");
                int deleted = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Delete query did not run successfully ", 3, deleted);

                System.out.println("Executing query...");
                List<Long> rl = (jpaResource.getEm().createQuery("select count(d) from DeptBean d").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // TODO: Need to figure out what exactly George was testing here:
                for (Long l : rl) {
                    System.out.println("JAG: l : " + l);
                }
//                QueryResultFormatter qrf = new QueryResultFormatter(em);
//                System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));
                //
//                Long o = rl.get(0);
//                Assert.assertTrue("assert Dept count == 2 ", o == 2L);

            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testNamedDeleteQuery(): End");
        }
    }

    public void testNamedUpdateDeptBudget(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testNamedUpdateDeptBudget(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // query = "update DeptBean d set d.budget = (d.budget + ?1)"
                String queryName = "updateDeptBudget";
                float budget = 10000;

                System.out.println("Creating named query " + queryName + ")...");
                Query q1 = jpaResource.getEm().createNamedQuery(queryName);
                q1.setParameter(1, budget);

                System.out.println("Executing named query " + queryName + " (budget=" + budget + ")...");
                int updated = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Update query did not run successfully ", 5, updated);

                System.out.println("Executing query...");
                List<Float> rl = (jpaResource.getEm().createQuery("select min(d.budget) from DeptBean d").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                Float o = rl.get(0);
                Assert.assertTrue("assert Dept min budget > 10000 ", o > 10000);
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testNamedUpdateDeptBudget(): End");
        }
    }

    public void testNamedUpdateDeptBudgetForParent(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testNamedUpdateDeptBudgetForParent(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // query = "update DeptBean d set d.budget = (d.budget * ?1) where d.reportsTo.no = ?2"
                String queryName = "updateDeptBudgetForParent";
                float budget = 10;
                int reportsTo = 200;

                System.out.println("Creating named query " + queryName + ")...");
                Query q1 = jpaResource.getEm().createNamedQuery(queryName);
                q1.setParameter(1, budget);
                q1.setParameter(2, reportsTo);

                System.out.println("Executing named query " + queryName + " (budget=" + budget + ")...");
                int updated = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Update query did not run successfully ", 2, updated);

                System.out.println("Executing query...");
                Long l = (Long) (jpaResource.getEm().createQuery("select count(d) from DeptBean d where d.budget > 20").getSingleResult());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Executing query...");
                List<DeptBean> rl = (jpaResource.getEm().createQuery("select d from DeptBean d where d.budget > 20").getResultList());

                // TODO
//                QueryResultFormatter qrf = new QueryResultFormatter();
//                System.out.println(qrf.prtAryTuple(rl, "select d, d.budget from DeptBean d where d.budget > 20"));

                Assert.assertEquals("assert 2 Depts have budget > 20 ", 2, l.longValue());

                Iterator<DeptBean> it = rl.iterator();
                while (it.hasNext()) {
                    DeptBean checkdept = it.next();
                    System.out.println("assert Dept " + checkdept.getNo() + " has budget > 20 " + checkdept.getBudget());
                    Assert.assertTrue("assert Dept " + checkdept.getNo() + " has budget > 20 ", checkdept.getBudget() > 20);
                }
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testNamedUpdateDeptBudgetForParent(): End");
        }
    }

    public void testDeleteQuery(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        /*
         * TODO: Hibernate fails populate/cleanup steps of testing due to a possible bug or behavior difference.
         * Hibernate cleanup fails to find() some entity id values due to relationship issues. This causes the next
         * test to fail populate due to duplicateconstrantviolations.
         * Hibernate population fails to find() some entity id values for an unknown reason, but then still throws
         * EntityExistsExceptions
         */
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testDeleteQuery(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                String query = "delete FROM DeptBean d WHERE d.no > ?1";
                int deptNo = 200;

                System.out.println("Creating query '" + query + "' ...");
                Query q1 = jpaResource.getEm().createQuery(query);
                q1.setParameter(1, deptNo);

                System.out.println("Executing query '" + query + "' (deptNo=" + deptNo + ")...");
                int deleted = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Delete query ran unsuccessfully ", 3, deleted);

                System.out.println("Executing query...");
                List<Long> rl = (jpaResource.getEm().createQuery("select count(d) from DeptBean d").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // TODO
//                QueryResultFormatter qrf = new QueryResultFormatter();
//                System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));

                int cnt = rl.get(0).intValue();
                Assert.assertEquals("assert Dept count == 2 ", 2, cnt);
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testDeleteQuery(): End");
        }
    }

    public void testDeleteQuery2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        /*
         * TODO: Hibernate fails populate/cleanup steps of testing due to a possible bug or behavior difference.
         * Hibernate cleanup fails to find() some entity id values due to relationship issues. This causes the next
         * test to fail populate due to duplicateconstrantviolations.
         * Hibernate population fails to find() some entity id values for an unknown reason, but then still throws
         * EntityExistsExceptions
         */
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testDeleteQuery2(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                String query = "delete FROM DeptBean d WHERE d.no > :deptNo";
                int deptNo = 200;

                System.out.println("Creating query '" + query + "' ...");
                Query q1 = jpaResource.getEm().createQuery(query);
                q1.setParameter("deptNo", deptNo);

                System.out.println("Executing query '" + query + "' (deptNo=" + deptNo + ")...");
                int deleted = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Delete query ran unsuccessfully ", 3, deleted);

                System.out.println("Executing query...");
                List<Long> rl = (jpaResource.getEm().createQuery("select count(d) from DeptBean d").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // TODO
//                QueryResultFormatter qrf = new QueryResultFormatter();
//                System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));

                int cnt = rl.get(0).intValue();
                Assert.assertEquals("assert Dept count == 2 ", 2, cnt);
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testDeleteQuery2(): End");
        }
    }

    public void testUpdateDeptBudget(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testUpdateDeptBudget(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                String query = "update DeptBean d set d.budget = (d.budget + ?1)";
                float budget = 10000;

                System.out.println("Creating query '" + query + "' ...");
                Query q1 = jpaResource.getEm().createQuery(query);
                q1.setParameter(1, budget);

                System.out.println("Executing query '" + query + "' (budget=" + budget + ")...");
                int updated = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Delete query ran unsuccessfully ", 5, updated);

                System.out.println("Executing query...");
                List<Float> rl = (jpaResource.getEm().createQuery("select min(d.budget) from DeptBean d").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // TODO
//                QueryResultFormatter qrf = new QueryResultFormatter();
//                System.out.println(qrf.prtAryTuple(rl, "SELECT min(d.budget) FROM DeptBean d"));

                Float o = rl.get(0);
                Assert.assertTrue("assert Dept min budget > 10000 ", o > 10000);
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testUpdateDeptBudget(): End");
        }
    }

    public void testUpdateTwoTable(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Fetch target entity type from test parameters
        TestEntityTypeEnum testEntityType = (TestEntityTypeEnum) testExecCtx.getProperties().get("TestEntityType");
        if (testEntityType == null) {
            // Oops, unknown type
            Assert.fail("TestEntityType not set. Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("JUBulkUpdateTest.testUpdateTwoTable(): Begin");

            try {
                JUBulkUpdateTest.populate(jpaResource, testEntityType);

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                String query = "update EmpBean e set e.salary = ?1*e.empid";
                int salary = 1000;

                System.out.println("Creating query '" + query + "' ...");
                Query q1 = jpaResource.getEm().createQuery(query);
                q1.setParameter(1, salary);

                System.out.println("Executing query '" + query + "' (salary=" + salary + ")...");
                int updated1 = q1.executeUpdate();

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Update query ran unsuccessfully ", 10, updated1);

                // updateOneFromTwo
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                String query2 = "update EmpBean e set e.bonus = (e.salary / ?1)";
                double bonus = 10.0;

                System.out.println("Creating query '" + query + "' ...");
                Query q2 = jpaResource.getEm().createQuery(query2);
                q2.setParameter(1, bonus);

                System.out.println("Executing query '" + query + "' (bonus=" + bonus + ")...");
                int updated2 = q2.executeUpdate();

                jpaResource.getTj().commitTransaction();

                Assert.assertEquals("Update query ran successfully ", 10, updated2);

                System.out.println("Executing query...");
                List<Double> rl = (jpaResource.getEm().createQuery("select min(e.bonus) from EmpBean e").getResultList());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // TODO
//                QueryResultFormatter qrf = new QueryResultFormatter();
//                System.out.println(qrf.prtAryTuple(rl, "SELECT min(e.bonus) FROM EmpBean e"));

                Double o = rl.get(0);
                Assert.assertEquals("assert EmpBean  min bonus == 100 ", 100.0, o.doubleValue(), 0.2);
            } finally {
                JUBulkUpdateTest.cleanup(jpaResource, testEntityType);
            }
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("JUBulkUpdateTest.testUpdateTwoTable(): End");
        }
    }
}
