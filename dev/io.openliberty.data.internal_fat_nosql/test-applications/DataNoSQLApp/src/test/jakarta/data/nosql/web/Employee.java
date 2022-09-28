/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.nosql.web;

import jakarta.nosql.mapping.Entity;
import jakarta.nosql.mapping.Id;

@Entity
public class Employee {
    public short age;

    @Id
    public long empNum;

    public String firstName;

    public String lastName;

    public String location;

    public String position;

    public float wage;

    public int yearHired;

    public Employee() {
    }

    public Employee(long empNum, String firstName, String lastName,
                    String position, String location,
                    int yearHired, int age, float wage) {
        this.empNum = empNum;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.location = location;
        this.yearHired = yearHired;
        this.age = (short) age;
        this.wage = wage;
    }
}
