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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.ManyToMany;

import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;

public class XMLTaskBean implements ITaskBean, Serializable {
    private static final long serialVersionUID = -8768072785005801499L;
    Integer taskid;
    String name;
    private String description;
    public java.math.BigDecimal cost;
    XMLProjectBean project;

    @ManyToMany()
    List<XMLEmpBean> emps;

    public XMLTaskBean() {
    }

    public XMLTaskBean(int taskid, String name, String description, XMLProjectBean proj) {
        this.taskid = taskid;
        this.name = name;
        this.description = description;
        this.cost = new java.math.BigDecimal(123.45);
        setCost(this.cost.setScale(4, BigDecimal.ROUND_HALF_UP));
        this.project = proj;
        this.emps = new ArrayList<XMLEmpBean>();
        if (proj != null)
            proj.tasks.add(this);
    }

    public XMLTaskBean(int taskid, String name, String description, XMLProjectBean proj, List<XMLEmpBean> emps) {
        this.taskid = taskid;
        this.name = name;
        this.description = description;
        this.cost = new java.math.BigDecimal("123.45");
        this.project = proj;
        this.emps = emps;
        if (proj != null)
            proj.tasks.add(this);
    }

    @Override
    public Integer getKey() {
        return new Integer(taskid);
    }

    public void addEmp(XMLEmpBean e) {
        this.emps.add(e); //this updates database and emp's task collection
        e.getTasks().add(this); //this updates only task's emp collection
    }

    @Override
    public String toString() {
        return " TaskBean taskid=" + taskid;
    }

    @Override
    public int getTaskid() {
        return taskid;
    }

    @Override
    public void setTaskid(Integer newTaskid) {
        taskid = newTaskid;
    }

    @Override
    public java.lang.String getName() {
        return name;
    }

    @Override
    public void setName(java.lang.String newName) {
        name = newName;
    }

    @Override
    public java.math.BigDecimal getcost() {
        return cost;
    }

    @Override
    public void setCost(java.math.BigDecimal cost) {
        this.cost = cost;
    }

    @Override
    public java.lang.String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public XMLProjectBean getProject() {
        return project;
    }

    @Override
    public void setProject(IProjectBean project2) {
        project = (XMLProjectBean) project2;
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
    public void addEmp(IEmpBean e) {
        this.emps.add((XMLEmpBean) e);
    }

}
