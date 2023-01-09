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
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Transient;

import com.ibm.ws.query.entities.interfaces.IAddressBean;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;

public class XMLEmpBean implements IEmpBean, Serializable {
    private static final long serialVersionUID = 7631622413718953273L;
    protected Integer empid;

    public String name;
    public Double salary;
    public int rating;
    public Boolean isManager;

    public char execLevel;
    public double bonus;
    public java.sql.Date hireDate;
    public java.sql.Time hireTime;
    public Timestamp hireTimestamp;
    @Transient
    private Calendar calendar = new GregorianCalendar();
    @Transient
    private long pdtOffset = -(calendar.get(calendar.ZONE_OFFSET)) - 8 * (60 * 60 * 1000);

    public XMLEmpBean() {
        empid = 0;
        name = "none";
        salary = 0.0;
        bonus = 0.0;
        hireDate = new java.sql.Date(0 + pdtOffset);
        hireTime = new Time(0 + pdtOffset);
        hireTimestamp = new Timestamp(0 + pdtOffset);
        dept = null;
        execLevel = 'A';
        isManager = true;
        tasks = new ArrayList<XMLTaskBean>();
        home = null;
        work = null;
    }

    public XMLEmpBean(int id, String nam, double sal) {
        empid = id;
        name = nam;
        salary = sal;
        bonus = 0.0;
        hireDate = new java.sql.Date(0 + pdtOffset);
        hireTime = new Time(0 + pdtOffset);
        hireTimestamp = new Timestamp(0 + pdtOffset);
        execLevel = 'A';
        isManager = true;
        tasks = new ArrayList<XMLTaskBean>();
        home = null;
        work = null;
    }

    public XMLEmpBean(int id, String nam, double sal, XMLDeptBean dept, int rating, boolean isManager, IAddressBean home, IAddressBean work) {
        empid = id;
        name = nam;
        salary = sal;
        this.rating = rating;
        bonus = 0.0;
        hireDate = new java.sql.Date(0 + pdtOffset);
        hireTime = new Time(0 + pdtOffset);
        hireTimestamp = new Timestamp(0 + pdtOffset);
        this.dept = dept;
        if (dept != null)
            dept.getEmps().add(this);
        execLevel = 'A';
        isManager = true;
        tasks = new ArrayList<XMLTaskBean>();
        this.home = (XMLAddressBean) home;
        this.work = (XMLAddressBean) work;
    }

    public XMLEmpBean(String nam, double sal, XMLDeptBean dept) {
        name = nam;
        salary = sal;
        bonus = 0.0;
        hireDate = new java.sql.Date(0 + pdtOffset);
        hireTime = new Time(0 + pdtOffset);
        hireTimestamp = new Timestamp(0 + pdtOffset);
        this.dept = dept;
        if (dept != null)
            dept.getEmps().add(this);
        execLevel = 'A';
        isManager = true;
        tasks = new ArrayList<XMLTaskBean>();
        home = null;
        work = null;
    }

    public Double pension(Double in) {
        return new Double(in.doubleValue() * 2.0);
    }

    XMLDeptBean dept;

    List<XMLDeptBean> manages;

    public List<XMLTaskBean> tasks;
    public XMLAddressBean home;
    public XMLAddressBean work;

    @Override
    public Integer getEmpid() {
        return empid;
    }

    @Override
    public void setEmpid(Integer newEmpid) {
        empid = newEmpid;
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
    public Double getSalary() {
        return salary;
    }

    @Override
    public void setSalary(Double newSalary) {
        salary = newSalary;
    }

    @Override
    public int getRating() {
        return rating;
    }

    @Override
    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public void setHireDate(java.sql.Date newDate) {
        hireDate = newDate;
    }

    @Override
    public java.sql.Date getHireDate() {
        return hireDate;
    }

    @Override
    public void setHireTime(java.sql.Time newTime) {
        hireTime = newTime;
    }

    @Override
    public java.sql.Time getHireTime() {
        return hireTime;
    }

    @Override
    public void setHireTimestamp(java.sql.Timestamp newTimestamp) {
        hireTimestamp = newTimestamp;
    }

    @Override
    public java.sql.Timestamp getHireTimestamp() {
        return hireTimestamp;
    }

    @Override
    public char getExecLevel() {
        return execLevel;
    }

    @Override
    public void setExecLevel(char newExecLevel) {
        execLevel = newExecLevel;
    }

    @Override
    public double getBonus() {
        return bonus;
    }

    @Override
    public void setBonus(double newBonus) {
        bonus = newBonus;
    }

    @Override
    public Boolean isManager() {
        return isManager;
    }

    @Override
    public void setIsManager(Boolean newManager) {
        isManager = newManager;
    }

    @Override
    public List<XMLDeptBean> getManages() {
        return manages;
    }

    @Override
    public void setManages(List<? extends IDeptBean> departments) {
        manages = (List<XMLDeptBean>) departments;
    }

    @Override
    public XMLAddressBean getHome() {
        return home;
    }

    @Override
    public void setHome(IAddressBean home) {
        this.home = (XMLAddressBean) home;
    }

    @Override
    public XMLAddressBean getWork() {
        return work;
    }

    @Override
    public void setWork(IAddressBean work) {
        this.work = (XMLAddressBean) work;
    }

    @Override
    public List<XMLTaskBean> getTasks() {
        return tasks;
    }

    @Override
    public void setTasks(List<? extends ITaskBean> tasks) {
        this.tasks = (List<XMLTaskBean>) tasks;
    }

    @Override
    public XMLDeptBean getDept() {
        return dept;
    }

    @Override
    public void setDept(IDeptBean department) {
        dept = (XMLDeptBean) department;
    }

}
