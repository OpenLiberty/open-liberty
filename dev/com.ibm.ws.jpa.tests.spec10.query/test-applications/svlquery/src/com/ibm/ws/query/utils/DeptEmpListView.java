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
import java.util.Collections;
import java.util.List;

import com.ibm.ws.query.entities.ano.CharityFund;
import com.ibm.ws.query.entities.ano.DeptBean;
import com.ibm.ws.query.entities.ano.EmpBean;
import com.ibm.ws.query.entities.ano.ProjectBean;
import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;

public class DeptEmpListView {
    private Integer no;
    private String name;
    private float budget;//dw added
    private CharityFund charityFund;
    private List<EmpBean> emps;
    private List<ProjectBean> projects;
    private EmpBean mgr;
    private DeptBean reportsTo;

    public DeptEmpListView() {
    }

    public DeptEmpListView(Integer no, String nam) {
        this.no = no;
        name = nam;
        budget = 2.1f;
        mgr = null;
        emps = new ArrayList<EmpBean>();
        projects = new ArrayList<ProjectBean>();
        reportsTo = null;
    }

    public DeptEmpListView(Integer no, String name, float budget, List<EmpBean> emps) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.emps = emps;
        projects = new ArrayList<ProjectBean>();
        reportsTo = null;
    }

    public DeptEmpListView(Integer no, String name, float budget, List<EmpBean> emps, CharityFund charityFund, List<ProjectBean> projects) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.emps = emps;
        this.charityFund = charityFund;
        this.projects = projects;
        this.reportsTo = null;
    }

    public DeptEmpListView(Integer no, String name, float budget, EmpBean mgr, List<EmpBean> emps, CharityFund charityFund, List<ProjectBean> projects, DeptBean reportsTo) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.mgr = mgr;
        this.emps = emps;
        this.charityFund = charityFund;
        this.projects = projects;
        this.reportsTo = reportsTo;
    }

    public DeptEmpListView(Integer no, String name, float budget, DeptBean dept) {
        this.no = no;
        this.name = name;
        this.budget = budget;
        this.mgr = dept.getMgr();
        this.emps = dept.getEmps();
        this.charityFund = dept.getCharityFund();
        this.projects = dept.getProjects();
        this.reportsTo = dept.getReportsTo();
    }

    public DeptEmpListView(DeptBean dept) {
        this.no = dept.getNo();
        this.name = dept.getName();
        this.budget = dept.getBudget();
        this.mgr = dept.getMgr();
        this.emps = dept.getEmps();
        this.charityFund = dept.getCharityFund();
        this.projects = dept.getProjects();
        this.reportsTo = dept.getReportsTo();
    }

    @Override
    public String toString() {
        List<Integer> empids = new ArrayList<Integer>();
        List<EmpBean> elist = getEmps();
        for (EmpBean s : elist) {
            empids.add(s.getEmpid());
        }
        Collections.sort(empids);
        return "( DeptEmpListView: no=" + getNo() + " name =" + getName() + " budget =" + getBudget() + " empids =" + empids + ")";
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

    public List<EmpBean> getEmps() {
        return emps;
    }

    public void setEmps(List<? extends IEmpBean> emps) {
        this.emps = (List<EmpBean>) emps;
    }

    public List<ProjectBean> getProjects() {
        return projects;
    }

    public void setProjects(List<? extends IProjectBean> projects) {
        this.projects = (List<ProjectBean>) projects;
    }

    public EmpBean getMgr() {
        return mgr;
    }

    public void setMgr(IEmpBean mgr) {
        this.mgr = (EmpBean) mgr;
    }

    public DeptBean getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(IDeptBean reportsTo) {
        this.reportsTo = (DeptBean) reportsTo;
    }

}