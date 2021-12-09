/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.commonentities.jpa10.employee;

import javax.persistence.Entity;

@Entity
public class Compensation_Salary extends Compensation {
    private int yearlySalary;

    public Compensation_Salary() {
        super();
    }

    public int getYearlySalary() {
        return yearlySalary;
    }

    public void setYearlySalary(int yearlySalary) {
        this.yearlySalary = yearlySalary;
    }

    @Override
    public String toString() {
        return "Salary [yearlySalary=" + yearlySalary + "]";
    }
}
