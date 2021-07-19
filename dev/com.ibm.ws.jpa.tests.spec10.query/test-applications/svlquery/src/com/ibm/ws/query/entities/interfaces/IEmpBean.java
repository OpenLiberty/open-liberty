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

package com.ibm.ws.query.entities.interfaces;

import java.sql.Timestamp;
import java.util.List;

public interface IEmpBean {

    Integer empid = null;
    public String name = null;
    public Double salary = null;
    public Boolean isManager = null;
    public char execLevel = ' ';
    public double bonus = 0;
    public java.sql.Date hireDate = null;
    public java.sql.Time hireTime = null;
    public Timestamp hireTimestamp = null;

    public Integer getEmpid();

    public void setEmpid(Integer newEmpid);

    public java.lang.String getName();

    public void setName(java.lang.String newName);

    public Double getSalary();

    public void setSalary(Double newSalary);

    public int getRating();

    public void setRating(int rating);

    public IDeptBean dept = null;
    public List<IDeptBean> manages = null;
    public List<ITaskBean> tasks = null;
    public IAddressBean home = null;
    public IAddressBean work = null;

    public void setHireDate(java.sql.Date newDate);

    public java.sql.Date getHireDate();

    public void setHireTime(java.sql.Time newTime);

    public java.sql.Time getHireTime();

    public void setHireTimestamp(java.sql.Timestamp newTimestamp);

    public java.sql.Timestamp getHireTimestamp();

    public char getExecLevel();

    public void setExecLevel(char newExecLevel);

    public double getBonus();

    public void setBonus(double newBonus);

    public Boolean isManager();

    public void setIsManager(Boolean newManager);

    public IDeptBean getDept();

    public void setDept(IDeptBean department);

    public List<? extends IDeptBean> getManages();

    public void setManages(List<? extends IDeptBean> departments);

    public IAddressBean getHome();

    public void setHome(IAddressBean home);

    public IAddressBean getWork();

    public void setWork(IAddressBean work);

    public List<? extends ITaskBean> getTasks();

    public void setTasks(List<? extends ITaskBean> tasks);

}
