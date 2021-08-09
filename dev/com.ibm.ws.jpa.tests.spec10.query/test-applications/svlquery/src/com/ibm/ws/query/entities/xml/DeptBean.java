/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.xml;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import com.ibm.ws.query.entities.interfaces.ICharityFund;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;

/**
 * Remote interface for Enterprise Bean: DepartBean
 */

public class DeptBean implements IDeptBean, Serializable {
    private static final long serialVersionUID = 2095252939824314777L;
    private Integer no;
    private String name;
    private float budget;
    private List<EmpBean> emps;
    private List<ProjectBean> projects;
    private EmpBean mgr;
    private DeptBean reportsTo;//dw added
    private CharityFund charityFund;

    public DeptBean() {
    }

    public DeptBean(int no, String nam) {
        this.no = no;
        name = nam;
        budget = 2.1f;
        mgr = null;
        emps = new Vector();
        projects = new Vector();
        reportsTo = null;
    }

    public DeptBean(DeptBean dept) {
        this.no = dept.no;
        this.name = dept.name;
        this.budget = dept.budget;
        this.setMgr(dept.getMgr());
        this.setEmps(dept.getEmps());
        this.setProjects(dept.getProjects());
        this.setReportsTo(dept.getReportsTo());
    }

    @Override
    public String toString() {
        return "( DeptBean: no=" + getNo() + " name =" + getName() + ")";
    }

    @Override
    public Integer getNo() {
        return no;
    }

    @Override
    public void setNo(Integer no) {
        this.no = no;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public float getBudget() {
        return budget;
    }

    @Override
    public void setBudget(float budget) {
        this.budget = budget;
    }

    @Override
    public CharityFund getCharityFund() {
        return charityFund;
    }

    @Override
    public void setCharityFund(ICharityFund charityFund) {
        this.charityFund = (CharityFund) charityFund;
    }

    @Override
    public List<EmpBean> getEmps() {
        return emps;
    }

    @Override
    public void setEmps(List<? extends IEmpBean> emps) {
        this.emps = (List<EmpBean>) emps;
    }

    @Override
    public List<ProjectBean> getProjects() {
        return projects;
    }

    @Override
    public void setProjects(List<? extends IProjectBean> projects) {
        this.projects = (List<ProjectBean>) projects;
    }

    @Override
    public EmpBean getMgr() {
        return mgr;
    }

    @Override
    public void setMgr(IEmpBean mgr) {
        this.mgr = (EmpBean) mgr;
    }

    @Override
    public DeptBean getReportsTo() {
        return reportsTo;
    }

    @Override
    public void setReportsTo(IDeptBean reportsTo) {
        this.reportsTo = (DeptBean) reportsTo;
    }
}
