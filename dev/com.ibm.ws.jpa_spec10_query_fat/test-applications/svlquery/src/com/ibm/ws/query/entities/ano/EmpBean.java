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

package com.ibm.ws.query.entities.ano;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ibm.ws.query.entities.interfaces.IAddressBean;
import com.ibm.ws.query.entities.interfaces.IDeptBean;
import com.ibm.ws.query.entities.interfaces.IEmpBean;
import com.ibm.ws.query.entities.interfaces.ITaskBean;

@Entity
@Table(name = "JPAEmpBean")
public class EmpBean implements IEmpBean, Serializable {
    private static final long serialVersionUID = -3953529807242594023L;

    @Id
    private Integer empid;

    @Column(length = 40)
    public String name;
    public Double salary;
    public int rating;
    public Boolean isManager;

    public char execLevel;
    public double bonus;
//    @Temporal(TemporalType.DATE)
    public java.sql.Date hireDate;
//    @Temporal(TemporalType.TIME)
    public java.sql.Time hireTime;
//    @Temporal(TemporalType.TIMESTAMP)
    public Timestamp hireTimestamp;
    @Transient
    private Calendar calendar = new GregorianCalendar();
    @Transient
    private long pdtOffset = -(calendar.get(calendar.ZONE_OFFSET)) - 8 * (60 * 60 * 1000);

    @ManyToOne()
    public DeptBean dept;

    @OneToMany(mappedBy = "mgr", cascade = CascadeType.REMOVE) //if fk creates table need cascadeRemove on inverse
    public List<DeptBean> manages;

    @ManyToMany(mappedBy = "emps")
    public List<TaskBean> tasks;

    @OneToOne(cascade = CascadeType.REMOVE) //gfh 7/17
    @JoinColumns({
                   @JoinColumn(name = "HOME_STREET", referencedColumnName = "street")
    })
    public AddressBean home;

    @OneToOne(cascade = CascadeType.REMOVE) //gfh 7/17
    @JoinColumns({
                   @JoinColumn(name = "WORK_STREET", referencedColumnName = "street")
    })
    public AddressBean work;

    public EmpBean() {
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
        tasks = new ArrayList<TaskBean>();
        home = null;
        work = null;
    }

    public EmpBean(int id, String nam, double sal) {
        empid = id;
        name = nam;
        salary = sal;
        bonus = 0.0;
        hireDate = new java.sql.Date(0 + pdtOffset);
        hireTime = new Time(0 + pdtOffset);
        hireTimestamp = new Timestamp(0 + pdtOffset);
        execLevel = 'A';
        isManager = true;
        tasks = new ArrayList<TaskBean>();
        home = null;
        work = null;
    }

    public EmpBean(int id, String nam, double sal, DeptBean dept) {
        empid = id;
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
        tasks = new ArrayList<TaskBean>();
        home = null;
        work = null;
    }

    public EmpBean(String nam, double sal, DeptBean dept) {
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
        tasks = new ArrayList<TaskBean>();
        home = null;
        work = null;
    }

    public Double pension(Double in) {
        return new Double(in.doubleValue() * 2.0);
    }

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
    public IDeptBean getDept() {
        return dept;
    }

    @Override
    public void setDept(IDeptBean department) {
        dept = (DeptBean) department;
    }

    @Override
    public List<DeptBean> getManages() {
        return manages;
    }

    @Override
    public void setManages(List<? extends IDeptBean> departments) {
        manages = (List<DeptBean>) departments;
    }

    @Override
    public AddressBean getHome() {
        return home;
    }

    @Override
    public void setHome(IAddressBean home) {
        this.home = (AddressBean) home;
    }

    @Override
    public AddressBean getWork() {
        return work;
    }

    @Override
    public void setWork(IAddressBean work) {
        this.work = (AddressBean) work;
    }

    @Override
    public List<TaskBean> getTasks() {
        return tasks;
    }

    @Override
    public void setTasks(List<? extends ITaskBean> tasks) {
        this.tasks = (List<TaskBean>) tasks;
    }

    @Override
    public String toString() {
        return "EmpBean [empid=" + empid + ", name=" + name + "]";
    }

}
