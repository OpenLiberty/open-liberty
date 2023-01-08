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
import java.util.List;
import java.util.Vector;

import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IProjectBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;

public class XMLProjectBean implements IProjectBean, Serializable {

    private static final long serialVersionUID = -9158952547600969949L;
    Integer projid;
    String name;

    public String description;
    public byte personMonths;
    public short durationDays;
    public long startTime;
    public java.math.BigDecimal cost;
    public java.math.BigDecimal budget;

    public XMLProjectBean() {
    }

    public XMLProjectBean(int id, String description, byte pm, short dur, long start, XMLDeptBean dept) {
        this.projid = id;
        this.name = "Project:" + String.valueOf(id);
        this.description = description;
        this.personMonths = pm;
        this.durationDays = dur;
        this.startTime = start;
        this.dept = dept;
        if (dept != null)
            dept.getProjects().add(this);
        tasks = new Vector();
        cost = new BigDecimal(123.45);
        cost = cost.setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public Integer getKey() {
        return new Integer(projid);
    }

    XMLDeptBean dept;
    List<XMLTaskBean> tasks;

    @Override
    public Integer getProjid() {
        return projid;
    }

    @Override
    public void setProjid(Integer newProjid) {
        projid = newProjid;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(java.lang.String description) {
        this.description = description;
    }

    @Override
    public byte getPersonMonths() {
        return personMonths;
    }

    @Override
    public void setPersonMonths(byte personMonths) {
        this.personMonths = personMonths;
    }

    @Override
    public short getDurationDays() {
        return durationDays;
    }

    @Override
    public void setDurationDays(short newDurationDays) {
        durationDays = newDurationDays;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
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
    public java.math.BigDecimal getBudget() {
        return budget;
    }

    @Override
    public void setBudget(java.math.BigDecimal budget) {
        this.budget = budget;
    }

    @Override
    public XMLDeptBean getDept() {
        return dept;
    }

    @Override
    public void setDept(IDeptBean department) {
        dept = (XMLDeptBean) department;
    }

    @Override
    public List<XMLTaskBean> getTasks() {
        return tasks;
    }

    @Override
    public void setTasks(List<? extends ITaskBean> tasks2) {
        tasks = (List<XMLTaskBean>) tasks2;
    }

}
