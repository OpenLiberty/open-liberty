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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.ws.springboot.support.version20.test.data.app.customer.Customer;
import com.ibm.ws.springboot.support.version20.test.data.app.customer.CustomerRepository;
import com.ibm.ws.springboot.support.version20.test.data.app.employee.Employee;
import com.ibm.ws.springboot.support.version20.test.data.app.employee.EmployeeRepository;

@Component
public class TestPersistence {
	final CustomerRepository customers;
	final EmployeeRepository employees;
	public TestPersistence(CustomerRepository customers, EmployeeRepository employees) {
		this.customers = customers;
		this.employees = employees;
	}

	@Transactional
	public void addAll(List<Employee> newEmployees, List<Customer> newCustomers) {
		newEmployees.forEach(employees::save);
		newCustomers.forEach(customers::save);
	}

	public Collection<Customer> getCustomers() {
		ArrayList<Customer> found = new ArrayList<>();
		customers.findAll().forEach(found::add);
		return found;
	}

	public Customer getCustomerByID(long id) {
		return customers.findById(id);
	}

	public Collection<Customer> getCustomersByLastName(String lastName) {
		return customers.findByLastName(lastName);
	}

	public Collection<Employee> getEmployees() {
		ArrayList<Employee> found = new ArrayList<>();
		employees.findAll().forEach(found::add);
		return found;
	}

	public Employee getEmployeeByID(long id) {
		return employees.findById(id);
	}

	public Collection<Employee> getEmployeesByLastName(String lastName) {
		return employees.findByLastName(lastName);
	}

	public void clear() {
		customers.deleteAll();
		employees.deleteAll();
	}
}
