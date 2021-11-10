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

import java.util.List;

/**
 * Remote interface for Enterprise Bean: DepartBean
 */
public interface IDeptBean {
    public Integer no = null;
    public String name = null;
    public float budget = 0;

    public List<IEmpBean> emps = null;
    public List<IProjectBean> projects = null;
    public IEmpBean mgr = null;
    public IDeptBean reportsTo = null;
    public ICharityFund charityFund = null;

    public Integer getNo();

    public void setNo(Integer no);

    public String getName();

    public void setName(String name);

    public float getBudget();

    public void setBudget(float budget);

    public ICharityFund getCharityFund();

    public void setCharityFund(ICharityFund charityFund);

    public List<? extends IEmpBean> getEmps();

    public void setEmps(List<? extends IEmpBean> emps);

    public List<? extends IProjectBean> getProjects();

    public void setProjects(List<? extends IProjectBean> projects);

    public IEmpBean getMgr();

    public void setMgr(IEmpBean mgr);

    public IDeptBean getReportsTo();

    public void setReportsTo(IDeptBean reportsTo);
}
