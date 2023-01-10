/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

/**
 *
 */
@Entity
public class Employee {

    @Column(name = "LNAME")
    public String firstName;

    @Column(name = "FNAME")
    public String lastName;

    @Embedded
    public Badge badge;

    public Employee() {
    }

    Employee(String firstName, String lastName, short badgeNum, char accessLevel) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.badge = new Badge(badgeNum, accessLevel);
    }

    @Override
    public String toString() {
        return "Employee " + firstName + " " + lastName + " " + badge;
    }
}
