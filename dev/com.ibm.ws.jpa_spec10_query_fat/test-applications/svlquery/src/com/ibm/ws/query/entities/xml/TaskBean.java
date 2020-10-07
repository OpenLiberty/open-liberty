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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.ManyToMany;

import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;

public class TaskBean implements ITaskBean, Serializable {
    private static final long serialVersionUID = -8768072785005801499L;
    Integer taskid;
    String name;
    private String description;
    public java.math.BigDecimal cost;
    ProjectBean project;

    @ManyToMany()
    List<EmpBean> emps;

    public TaskBean() {
    }

    public TaskBean(int taskid, String name, String description, ProjectBean proj) {
        this.taskid = taskid;
        this.name = name;
        this.description = description;
        this.cost = new java.math.BigDecimal(123.45);
        setCost(this.cost.setScale(4, BigDecimal.ROUND_HALF_UP));
        this.project = proj;
        this.emps = new ArrayList<EmpBean>();
        if (proj != null)
            proj.tasks.add(this);
    }

    public TaskBean(int taskid, String name, String description, ProjectBean proj, List<EmpBean> emps) {
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

    public void addEmp(EmpBean e) {
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
    public ProjectBean getProject() {
        return project;
    }

    @Override
    public void setProject(IProjectBean project2) {
        project = (ProjectBean) project2;
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
    public void addEmp(IEmpBean e) {
        this.emps.add((EmpBean) e);
    }

}
