/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

public class XMLDeptBean implements IDeptBean, Serializable {
    private static final long serialVersionUID = 2095252939824314777L;
    private Integer no;
    private String name;
    private float budget;
    private List<XMLEmpBean> emps;
    private List<XMLProjectBean> projects;
    private XMLEmpBean mgr;
    private XMLDeptBean reportsTo;//dw added
    private XMLCharityFund charityFund;

    public XMLDeptBean() {
    }

    public XMLDeptBean(int no, String nam) {
        this.no = no;
        name = nam;
        budget = 2.1f;
        mgr = null;
        emps = new Vector();
        projects = new Vector();
        reportsTo = null;
    }

    public XMLDeptBean(XMLDeptBean dept) {
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
    public XMLCharityFund getCharityFund() {
        return charityFund;
    }

    @Override
    public void setCharityFund(ICharityFund charityFund) {
        this.charityFund = (XMLCharityFund) charityFund;
    }

    @Override
    public List<XMLEmpBean> getEmps() {
        return emps;
    }

    @Override
    public void setEmps(List<? extends IEmpBean> emps) {
        this.emps = (List<XMLEmpBean>) emps;
    }

    @Override
    public List<XMLProjectBean> getProjects() {
        return projects;
    }

    @Override
    public void setProjects(List<? extends IProjectBean> projects) {
        this.projects = (List<XMLProjectBean>) projects;
    }

    @Override
    public XMLEmpBean getMgr() {
        return mgr;
    }

    @Override
    public void setMgr(IEmpBean mgr) {
        this.mgr = (XMLEmpBean) mgr;
    }

    @Override
    public XMLDeptBean getReportsTo() {
        return reportsTo;
    }

    @Override
    public void setReportsTo(IDeptBean reportsTo) {
        this.reportsTo = (XMLDeptBean) reportsTo;
    }
}
