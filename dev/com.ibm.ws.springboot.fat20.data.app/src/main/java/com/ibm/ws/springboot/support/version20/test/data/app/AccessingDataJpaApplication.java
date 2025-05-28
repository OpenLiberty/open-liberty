/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.version20.test.data.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import com.ibm.ws.springboot.support.version20.test.data.app.customer.Customer;
import com.ibm.ws.springboot.support.version20.test.data.app.employee.Employee;

@SpringBootApplication
public class AccessingDataJpaApplication extends SpringBootServletInitializer {

	private static final Logger log = LoggerFactory.getLogger(AccessingDataJpaApplication.class);

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(AccessingDataJpaApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(AccessingDataJpaApplication.class);
	}

	@Bean
	public CommandLineRunner runTest(TestPersistence testTransaction) {
		return (args)-> {
			doDataTest("COMMAND_LINE_RUNNER", testTransaction);
		};
	}

	static void doDataTest(String context, TestPersistence testTransaction) {

		testTransaction.clear();
		// save a few employees
		List<Employee> employees = new ArrayList<>();
		employees.add(new Employee("Sydney", "Bristow"));
		employees.add(new Employee("Michael", "Vahughn"));
		employees.add(new Employee("Jack", "Bristow"));
		employees.add(new Employee("Arvin", "Sloane"));
		employees.add(new Employee("Marshall", "Flinkman"));

		// save a few customers
		List<Customer> customers = new ArrayList<>();
		customers.add(new Customer("Jack", "Bauer", "T"));
		customers.add(new Customer("Chloe", "O'Brian", "S"));
		customers.add(new Customer("Kim", "Bauer", "R"));
		customers.add(new Customer("David", "Palmer", "Q"));
		customers.add(new Customer("Michelle", "Dessler", "P"));
		// should cause failure with middle initial being too long
		customers.add(new Customer("Thomas", "Watson", "IBM"));

		boolean gotException = false;
		try {
			testTransaction.addAll(employees, customers);
		} catch (Exception e) {
			gotException = true;
		}
		Assert.isTrue(gotException, "Expected to fail to add bad customer");

		Collection<Employee> shouldBeEmptyEmployees = testTransaction.getEmployees();
		Collection<Customer> shouldBeEmptyCustomers = testTransaction.getCustomers();
		Assert.isTrue(shouldBeEmptyEmployees.isEmpty(), "Employees should be empty: " + shouldBeEmptyEmployees);
		Assert.isTrue(shouldBeEmptyCustomers.isEmpty(), "Customers should be empty: " + shouldBeEmptyCustomers);

		// remove bad middle name customer
		customers.remove(5);

		testTransaction.addAll(employees, customers);

		Collection<Employee> persistedEmployees = testTransaction.getEmployees();
		Collection<Customer> persistedCustomers = testTransaction.getCustomers();

		Assert.isTrue(persistedEmployees.size() == employees.size(), "Wrong number of persisted employees: " + persistedEmployees);
		Assert.isTrue(persistedCustomers.size() == customers.size(), "Wrong number of persisted customers: " + persistedCustomers);


		// fetch an individual customer by ID
		Customer firstCustomerA = persistedCustomers.iterator().next();
		Customer firstCustomerB = testTransaction.getCustomerByID(firstCustomerA.getId());
		Assert.isTrue(firstCustomerA.equals(firstCustomerB), "Wrong customer with id: " + firstCustomerA + " != " + firstCustomerB);


		// fetch customers by last name
		Collection<Customer> bauers = testTransaction.getCustomersByLastName("Bauer");
		Assert.isTrue(bauers.size() == 2, "Wrong number of Bauers: " + bauers);

		// fetch an individual employee by ID
		Employee firstEmployeeA = persistedEmployees.iterator().next();
		Employee firstEmployeeB = testTransaction.getEmployeeByID(firstEmployeeA.getId());
		Assert.isTrue(firstEmployeeA.equals(firstEmployeeB), "Wrong employee with id: " + firstEmployeeA + " != " + firstEmployeeB);

		// fetch employees by last name
		Collection<Employee> bristows = testTransaction.getEmployeesByLastName("Bristow");
		Assert.isTrue(bauers.size() == 2, "Wrong number of Bristow: " + bristows);

		log.info(context + ": SPRING DATA TEST: PASSED");
	}
}
