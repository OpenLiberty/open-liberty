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

public interface ITaskBean {
    public Integer getKey();

    public void addEmp(IEmpBean e);

    @Override
    public String toString();

    public int getTaskid();

    public void setTaskid(Integer newTaskid);

    public java.lang.String getName();

    public void setName(java.lang.String newName);

    public java.math.BigDecimal getcost();

    public void setCost(java.math.BigDecimal cost);

    public java.lang.String getDescription();

    public void setDescription(String description);

    public IProjectBean getProject();

    public void setProject(IProjectBean project2);

    public List<? extends IEmpBean> getEmps();

    public void setEmps(List<? extends IEmpBean> emps);
}
