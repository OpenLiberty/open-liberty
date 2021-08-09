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

public interface IProjectBean {
    public Integer getKey();

    public Integer getProjid();

    public void setProjid(Integer newProjid);

    public java.lang.String getName();

    public void setName(java.lang.String newName);

    public String getDescription();

    public void setDescription(java.lang.String description);

    public byte getPersonMonths();

    public void setPersonMonths(byte personMonths);

    public short getDurationDays();

    public void setDurationDays(short newDurationDays);

    public long getStartTime();

    public void setStartTime(long startTime);

    public java.math.BigDecimal getcost();

    public void setCost(java.math.BigDecimal cost);

    public java.math.BigDecimal getBudget();

    public void setBudget(java.math.BigDecimal budget);

    public IDeptBean getDept();

    public void setDept(IDeptBean department);

    public List<? extends ITaskBean> getTasks();

    public void setTasks(List<? extends ITaskBean> tasks2);
}
