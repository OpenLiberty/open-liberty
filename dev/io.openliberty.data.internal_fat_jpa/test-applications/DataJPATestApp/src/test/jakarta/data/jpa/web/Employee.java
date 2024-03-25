/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Employee entity for tests.
 */
@Entity
public class Employee {
    @Embedded
    public Badge badge;

    @Id
    public int empNum;

    @Column(name = "LNAME")
    public String firstName;

    public String id;

    @Column(name = "FNAME")
    public String lastName;

    public Employee() {
    }

    Employee(int empNum, String firstName, String lastName, short badgeNum, char accessLevel) {
        this.empNum = empNum;
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = firstName + " " + lastName;
        this.badge = new Badge(badgeNum, accessLevel);
    }

    @Override
    public String toString() {
        return "Employee #" + empNum + " " + firstName + " " + lastName + " " + badge;
    }
}
