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

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CMN10_Department")
public class Department {
    @Id
    private int id;

    private String departmentName;

    private String description;

    @ManyToOne
    private Manager departmentManager;

    @OneToMany(mappedBy = "department")
    private Collection<Employee> employeeMembership;

    @Version
    private int version;

    public Department() {
        employeeMembership = new ArrayList<Employee>();
    }

    public Department(int id) {
        this.id = id;
        employeeMembership = new ArrayList<Employee>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Manager getDepartmentManager() {
        return departmentManager;
    }

    public void setDepartmentManager(Manager departmentManager) {
        this.departmentManager = departmentManager;
    }

    public Collection<Employee> getEmployeeMembership() {
        return employeeMembership;
    }

    public void setEmployeeMembership(Collection<Employee> employeeMembership) {
        this.employeeMembership = employeeMembership;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Department [id=" + id + ", departmentName=" + departmentName
               + ", description=" + description + ", departmentManager="
               + departmentManager + ", version=" + version + "]";
    }

}
