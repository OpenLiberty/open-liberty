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

package jpa22query.web;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import jpa22query.entity.Employee;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestJPA22Query")
public class JPAQueryTestServlet extends FATServlet {
    @PersistenceContext(unitName = "JPAPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    private static boolean tablesSetup = false; // Expecting non-parallel test execution

    private void setupTables() throws Exception {
        if (tablesSetup) {
            return;
        }

        tx.begin();

        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Doe");
        emp1.setSalary(10000);
        em.persist(emp1);

        Employee emp2 = new Employee();
        emp2.setFirstName("Jane");
        emp2.setLastName("Doe");
        emp2.setSalary(20000);
        em.persist(emp2);

        Employee emp3 = new Employee();
        emp3.setFirstName("Joe");
        emp3.setLastName("Cool");
        emp3.setSalary(5000);
        em.persist(emp3);

        tx.commit();

        tablesSetup = true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testJPA22QueryStream() throws Exception {
        setupTables();

        String qStr = "SELECT e FROM Employee e";
        Query q = em.createQuery(qStr);
        Stream empStream = q.getResultStream();

        final AtomicInteger ai = new AtomicInteger(0);
        empStream.forEach(t -> ai.set(ai.get() + ((Employee) t).getSalary()));

        Assert.assertEquals(ai.get(), 35000);
    }

    @Test
    public void testJPA22TypedQueryStream() throws Exception {
        setupTables();

        String qStr = "SELECT e FROM Employee e";
        TypedQuery<Employee> tQuery = em.createQuery(qStr, Employee.class);
        Stream<Employee> empStream = tQuery.getResultStream();

        final AtomicInteger ai = new AtomicInteger(0);
        empStream.forEach(t -> ai.set(ai.get() + t.getSalary()));

        Assert.assertEquals(ai.get(), 35000);
    }
}
