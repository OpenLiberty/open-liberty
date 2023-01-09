/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.commonentities.jpa10.employee;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "EMP_TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
@Table(name = "CMN10_Employee")
public class Employee {
    @Id
    private int id;

    private String lastName;
    private String firstName;

    private String position;

    private java.sql.Date birthdate;
    private java.sql.Date dateOfHire;

    @ManyToOne
    private Manager manager;

    @ManyToOne
    private Department department;

    @Version
    private int version;

    public Employee() {

    }

    public Employee(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public java.sql.Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(java.sql.Date birthdate) {
        this.birthdate = birthdate;
    }

    public java.sql.Date getDateOfHire() {
        return dateOfHire;
    }

    public void setDateOfHire(java.sql.Date dateOfHire) {
        this.dateOfHire = dateOfHire;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public int getVersion() {
        return version;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "Employee [id=" + id + ", lastName=" + lastName + ", firstName="
               + firstName + ", position=" + position + ", birthdate="
               + birthdate + ", dateOfHire=" + dateOfHire + ", manager="
               + manager + ", department=" +
               ((getDepartment() == null) ? "<null>" : getDepartment().getDepartmentName()) +
               ", version=" + version + "]";
    }
}
