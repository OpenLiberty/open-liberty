/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

@Entity
public class Employee {
    public short age;

    @Id
    public long empNum;

    @Column
    public String firstName;

    @Column
    public String lastName;

    @Column
    public String location;

    @Column
    public String position;

    @Column
    public float wage;

    @Column
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
