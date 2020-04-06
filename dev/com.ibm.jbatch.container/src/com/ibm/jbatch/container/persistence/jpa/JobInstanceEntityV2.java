/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
public class JobInstanceEntityV2 extends JobInstanceEntity {

    // JPA
    @Trivial
    public JobInstanceEntityV2() { super(); }

    // in-memory
    public JobInstanceEntityV2(long instanceId) {
        super(instanceId);
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "UPDATETIME")
    private Date lastUpdatedTime;
    
    @Override
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }
}
