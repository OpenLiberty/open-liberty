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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.query.entities.ano.CharityFund;
import com.ibm.ws.query.entities.ano.DeptBean;
import com.ibm.ws.query.entities.ano.EmpBean;
import com.ibm.ws.query.entities.ano.ProjectBean;
import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.IProjectBean;

public class DeptEmpView {
    private Integer no;
    private String name;
    private float budget;//dw added
    private Integer empid;
    private CharityFund charityFund;
    private List<ProjectBean> projects;
    private EmpBean mgr;
    private DeptBean reportsTo;

    public DeptEmpView() {
    }

    public DeptEmpView(Integer no, String nam) {
        this.no = no;
        name = nam;
        budget = 2.1f;
        mgr = null;
        empid = null;
        projects = new ArrayList<ProjectBean>();
        reportsTo = null;
    }

    public DeptEmpView(Integer no, String name, float budget, Integer empid) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.empid = empid;
        projects = new ArrayList<ProjectBean>();
        reportsTo = null;
    }

    public DeptEmpView(Integer no, String name, float budget, Integer empid, CharityFund charityFund, List<ProjectBean> projects) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.empid = empid;
        this.charityFund = charityFund;
        this.projects = projects;
        this.reportsTo = null;
    }

    @Override
    public String toString() {
        return "( DeptEmpView: no=" + getNo() + " name =" + getName() + " budget =" + getBudget() + " empid =" + getEmpid() + ")";
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

    public CharityFund getCharityFund() {
        return charityFund;
    }

    public void setCharityFund(ICharityFund charityFund) {
        this.charityFund = (CharityFund) charityFund;
    }

    public Integer getEmpid() {
        return empid;
    }

    public void setEmpid(Integer empid) {
        this.empid = empid;
    }

    public List<ProjectBean> getProjects() {
        return projects;
    }

    public void setProjects(List<? extends IProjectBean> projects) {
        this.projects = (List<ProjectBean>) projects;
    }

}
