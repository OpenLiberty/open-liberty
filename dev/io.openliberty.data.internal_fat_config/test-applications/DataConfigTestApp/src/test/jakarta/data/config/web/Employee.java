/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.config.web;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 *
 */
@Entity
public class Employee extends Person {

    @Id
    public Integer employeeId;

    public Employee() {
    }

    Employee(Integer employeeId, String firstName, String lastName, int age) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    @Override
    public String toString() {
        return "Employee#" + employeeId + " " + firstName + " " + lastName + " age ";
    }
}
