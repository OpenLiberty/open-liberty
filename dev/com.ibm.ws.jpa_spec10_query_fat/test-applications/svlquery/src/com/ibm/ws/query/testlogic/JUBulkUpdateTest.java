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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.query.entities.ano.DeptBean;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

@SuppressWarnings("unchecked")
public class JUBulkUpdateTest extends AbstractTestLogic {
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();
            String name = "deleteDepartmentsGreaterThan";
            Query q1 = em.createNamedQuery(name);
            q1.setParameter("deptNo", 200);
            int deleted = q1.executeUpdate();
            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Delete query did not run successfully ", 3, deleted);

            List<Long> rl = (em.createQuery("select count(d) from DeptBean d").getResultList());
            em.clear();

            // TODO: Need to figure out what exactly George was testing here:
            for (Long l : rl) {
                System.out.println("JAG: l : " + l);
            }
//            QueryResultFormatter qrf = new QueryResultFormatter(em);
//            System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));
//
//            Long o = rl.get(0);
//            Assert.assertTrue("assert Dept count == 2 ", o == 2L);

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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();
            String name = "updateDeptBudget";
            Query q1 = em.createNamedQuery(name);
            q1.setParameter(1, 10000);
            int updated = q1.executeUpdate();
            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Update query did not run successfully ", 5, updated);

            List<Float> rl = (em.createQuery("select min(d.budget) from DeptBean d").getResultList());
            em.clear();

            Float o = rl.get(0);
            Assert.assertTrue("assert Dept min budget > 10000 ", o > 10000);

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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();

            String name = "updateDeptBudgetForParent";
            Query q1 = em.createNamedQuery(name);
            q1.setParameter(1, 10);
            q1.setParameter(2, 200);

            int updated = q1.executeUpdate();
            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Update query did not run successfully ", 2, updated);

            Long l = (Long) (em.createQuery("select count(d) from DeptBean d where d.budget > 20").getSingleResult());
            em.clear();

            List<DeptBean> rl = (em.createQuery("select d from DeptBean d where d.budget > 20").getResultList());

            // TODO
//            QueryResultFormatter qrf = new QueryResultFormatter();
//            System.out.println(qrf.prtAryTuple(rl, "select d, d.budget from DeptBean d where d.budget > 20"));

            Assert.assertEquals("assert 2 Depts have budget > 20 ", 2, l.longValue());

            Iterator<DeptBean> it = rl.iterator();
            while (it.hasNext()) {
                DeptBean d3 = it.next();
                System.out.println("assert Dept " + d3.getNo() + " has budget > 20 " + d3.getBudget());
                Assert.assertTrue("assert Dept " + d3.getNo() + " has budget > 20 ", d3.getBudget() > 20);
            }
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();

            String name = "delete FROM DeptBean d WHERE d.no > ?1";
            Query q1 = em.createQuery(name);
            q1.setParameter(1, 200);
            int deleted = q1.executeUpdate();

            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Delete query ran successfully ", 3, deleted);

            List<Long> rl = (em.createQuery("select count(d) from DeptBean d").getResultList());
            em.clear();

            // TODO
//            QueryResultFormatter qrf = new QueryResultFormatter();
//            System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));

            int cnt = rl.get(0).intValue();
            Assert.assertEquals("assert Dept count == 2 ", 2, cnt);
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();

            String name = "delete FROM DeptBean d WHERE d.no > :deptNo";
            Query q1 = em.createQuery(name);
            q1.setParameter("deptNo", 200);
            int deleted = q1.executeUpdate();

            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Delete query ran successfully ", 3, deleted);

            List<Long> rl = (em.createQuery("select count(d) from DeptBean d").getResultList());
            em.clear();

            // TODO
//            QueryResultFormatter qrf = new QueryResultFormatter();
//            System.out.println(qrf.prtAryTuple(rl, "SELECT count(d) FROM DeptBean d"));

            int cnt = rl.get(0).intValue();
            Assert.assertEquals("assert Dept count == 2 ", 2, cnt);
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            jpaResource.getTj().beginTransaction();

            String query = "update DeptBean d set d.budget = (d.budget + ?1)";
            Query q1 = em.createQuery(query);
            q1.setParameter(1, 10000);
            int updated = q1.executeUpdate();

            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Delete query ran successfully ", 5, updated);

            List<Float> rl = (em.createQuery("select min(d.budget) from DeptBean d").getResultList());
            em.clear();

            // TODO
//            QueryResultFormatter qrf = new QueryResultFormatter();
//            System.out.println(qrf.prtAryTuple(rl, "SELECT min(d.budget) FROM DeptBean d"));

            Float o = rl.get(0);
            Assert.assertTrue("assert Dept min budget > 10000 ", o > 10000);
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // updateTwoFromOne
            jpaResource.getTj().beginTransaction();

            String query1 = "update EmpBean e set e.salary = ?1*e.empid";
            Query q1 = em.createQuery(query1);
            q1.setParameter(1, 1000);
            int updated1 = q1.executeUpdate();

            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Update query ran successfully ", 10, updated1);

            // updateOneFromTwo
            jpaResource.getTj().beginTransaction();

            String query2 = "update EmpBean e set e.bonus = (e.salary / ?1)";
            Query q2 = em.createQuery(query2);
            q2.setParameter(1, 10.0);
            int updated2 = q2.executeUpdate();

            jpaResource.getTj().commitTransaction();

            Assert.assertEquals("Update query ran successfully ", 10, updated2);

            List<Double> rl = (em.createQuery("select min(e.bonus) from EmpBean e").getResultList());
            em.clear();

            // TODO
//            QueryResultFormatter qrf = new QueryResultFormatter();
//            System.out.println(qrf.prtAryTuple(rl, "SELECT min(e.bonus) FROM EmpBean e"));

            Double o = rl.get(0);
            Assert.assertEquals("assert EmpBean  min bonus == 100 ", 100.0, o.doubleValue(), 0.2);
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
