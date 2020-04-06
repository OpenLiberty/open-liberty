/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.cache.model;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CEmployee")
@Cacheable(true)
public class Employee {
    @Id
    private int id;

    private String lastName;
    private String firstName;

    private int vacationDays;

    @Version
    private long version;

    private transient String str = null;

    public Employee() {
    }

    public Employee(int id, String lastName, String firstName) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        str = null;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        str = null;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        str = null;
    }

    public int getVacationDays() {
        return vacationDays;
    }

    public void setVacationDays(int vacationDays) {
        this.vacationDays = vacationDays;
        str = null;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        if (str == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Employee: ");
            sb.append(getLastName()).append(", ").append(getFirstName());
            sb.append(" Vacation Days: ").append(getVacationDays());
            str = new String(sb);
        }
        return str;
    }
}
