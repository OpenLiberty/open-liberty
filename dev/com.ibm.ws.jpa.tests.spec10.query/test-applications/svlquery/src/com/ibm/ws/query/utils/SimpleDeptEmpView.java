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

package com.ibm.ws.query.utils;

import com.ibm.ws.query.entities.ano.CharityFund;

/**
 *
 */
public class SimpleDeptEmpView {

    private Integer no;
    private String name;
    private float budget;
    private Integer empid;
    private CharityFund charityFund;

    public SimpleDeptEmpView() {
    }

    public SimpleDeptEmpView(Integer no, String nam) {
        this.no = no;
        this.name = nam;
        this.budget = 2.1f;
        this.empid = null;
    }

    public SimpleDeptEmpView(int no, String name, Float budget, Integer empid) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.empid = empid;
    }

    @Override
    public String toString() {
        return "( SimpleDeptEmpView: no=" + getNo() + " name =" + getName() + " budget =" + getBudget() + " empid =" + getEmpid() + ")";
    }

    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getBudget() {
        return budget;
    }

    public void setBudget(float budget) {
        this.budget = budget;
    }

    public Integer getEmpid() {
        return empid;
    }

    public void setEmpid(Integer empid) {
        this.empid = empid;
    }
}
