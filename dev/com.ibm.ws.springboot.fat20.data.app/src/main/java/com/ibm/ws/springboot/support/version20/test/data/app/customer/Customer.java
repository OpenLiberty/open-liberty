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
package com.ibm.ws.springboot.support.version20.test.data.app.customer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Customer {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	private String firstName;
	private String lastName;
	@Column(length = 1)
	private String middleInitial;

	protected Customer() {}

	public Customer(String firstName, String lastName, String middleInitial) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.middleInitial = middleInitial;
	}

	@Override
	public String toString() {
		return String.format(
				"Customer[id=%d, firstName='%s', lastName='%s', middleInitial='%s']",
				id, firstName, lastName, middleInitial);
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getMiddleInitial() {
		return middleInitial;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Customer)) {
			return false;
		}
		Customer c = (Customer) o;
		return firstName.equals(c.getFirstName()) && lastName.equals(c.getLastName()) && middleInitial.equals(c.getMiddleInitial());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + firstName.hashCode();
		result = prime * result + lastName.hashCode();
		result = prime * result + middleInitial.hashCode();
		return result;
	}
}
