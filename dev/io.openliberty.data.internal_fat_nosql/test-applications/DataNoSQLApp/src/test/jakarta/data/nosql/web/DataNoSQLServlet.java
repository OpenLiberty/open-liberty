/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package test.jakarta.data.nosql.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataNoSQLServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final long TIMEOUT_MINUTES = 2L;

    @Inject
    Employees employees;

    @Override
    public void init(ServletConfig config) throws ServletException {
        employees.save(new Employee(3456, "Joe", "Biden", "Developer", "New York", 2022, 23, 720300));
        employees.save(new Employee(7654, "Nicholas", "Jack", "Manager", "New York", 2023, 25, 800500));
        employees.save(new Employee(3526, "Jesse", "Pinkman", "Consultant", "New York", 2022, 31, 650000));
        employees.save(new Employee(9865, "Adam", "John", "Developer", "New York", 2024, 22, 934500));
        employees.save(new Employee(2644, "Jake", "Williams", "Developer", "New York", 2025, 43, 867200));
        employees.save(new Employee(8754, "William", "Roger", "Manager", "New York", 2019, 33, 904500));
        employees.save(new Employee(3466, "Ajay", "Barter", "Consultant", "New York", 2021, 29, 734500));
        employees.save(new Employee(7656, "Richard", "Dave", "Support", "New York", 2022, 40, 657570));
        employees.save(new Employee(3864, "Nete", "Paul", "Developer", "New York", 2024, 21, 700300));
    }

    /**
     * Verify that implementation of a repository class can be injected.
     */
    @Test
    public void testInjectRepository() {
        assertNotNull(employees);
    }

    /**
     * Load classes from the Jakarta NoSQL API layer.
     */
    @Test
    public void testMappingLayerAvailable() throws ClassNotFoundException {
        Class.forName("jakarta.nosql.Entity");

    }

    /**
     * Basic test for using jNoSQL as a Jakarta Data implementation.
     * Creates three entities, saves them, and tests a query method.
     *
     * @throws Exception
     */
    @Test
    public void testBasicQuery() throws Exception {
        Employee mark = new Employee(10L, "Mark", "BasicTest", "Engineer", "Rochester", 2010, 35, 60f);
        Employee dan = new Employee(11L, "Dan", "BasicTest", "Engineer", "Rochester", 2010, 35, 50f);
        Employee scott = new Employee(12L, "Scott", "BasicTest", "Engineer", "Rochester", 2010, 35, 80f);

        employees.save(mark);
        employees.save(dan);
        employees.save(scott);

        assertEquals(Stream.of("Mark", "Dan")
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList()),
                     employees.findByWageLessThanEqual(70f)
                                     .map(c -> c.firstName)
                                     .sorted(Comparator.naturalOrder())
                                     .collect(Collectors.toList()));
    }

    @Test
    public void testGeneratedMetaModelsAvailable() {
        //Check for _Employees is not needed as Jakarta process repositories in a way different than hibernate
        try {
            Class.forName("test.jakarta.data.nosql.web._Employee");
        } catch (ClassNotFoundException e) {
            fail("Static metamodel class _Employee (for Data) was not generated or available at runtime.");
        }
    }

    @Ignore
    @Test
    public void testEmployeeBasicRepositoryFindAllWithPages() {
        Queue<Integer> wageList = new LinkedList<>();
        wageList.offer(934500);
        wageList.offer(904500);
        wageList.offer(867200);
        wageList.offer(800500);
        wageList.offer(734500);
        wageList.offer(720300);
        wageList.offer(700300);
        wageList.offer(657570);
        wageList.offer(650000);
        
        //TO-DO 
        //Currently the _Employee showing as unresolved
        //That will be addressed in further release of NOSQL and more tests can be written further
        //Need additional tests to address and test those changes
        PageRequest request = PageRequest.ofSize(3);
        Order<Employee> order = Order.by(_Employee.wage.desc());

        Page<Employee> page = employees.findAll(request, order);

        //Page assertions
        assertEquals(9, page.totalElements());
        assertEquals(3, page.totalPages());

        //Order assertions
        do {
            page = employees.findAll(request, order);
            Iterator<Employee> it = page.iterator();
            while (it.hasNext()) {
                assertEquals("Incorrect order of results during pagination", wageList.poll().floatValue(), it.next().wage);
            }
        } while ((request = page.hasNext() ? page.nextPageRequest() : null) != null);
        //Complete Assertion
        assertEquals("InComplete set of results during pagination", 0, wageList.size());
    }
}
