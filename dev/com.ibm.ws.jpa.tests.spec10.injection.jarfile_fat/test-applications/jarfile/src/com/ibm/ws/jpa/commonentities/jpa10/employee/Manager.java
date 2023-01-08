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

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
public class Manager extends Employee {
    @OneToMany(mappedBy = "manager")
    private Collection<Employee> managedEmployeeList;

    @Override
    public String toString() {
        return "Manager [id=" + getId() + ", lastName=" + getLastName()
               + ", firstName=" + getFirstName() + ", position="
               + getPosition() + ", birthdate=" + getBirthdate()
               + ", dateOfHire=" + getDateOfHire() + ", manager="
               + getManager() + ", version=" + getVersion()
               + ", department=" +
               ((getDepartment() == null) ? "<null>" : getDepartment().getDepartmentName()) + "]";
    }

}
