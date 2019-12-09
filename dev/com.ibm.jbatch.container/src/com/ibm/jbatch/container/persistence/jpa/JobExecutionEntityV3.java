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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
public class JobExecutionEntityV3 extends JobExecutionEntityV2 {

    @OneToMany(mappedBy = "jobExec", cascade = CascadeType.REMOVE)
    private Collection<RemotablePartitionEntity> remotablePartitions;

    // For JPA
    @Trivial
    public JobExecutionEntityV3() {
        super();
    }

    // For in-memory persistence
    public JobExecutionEntityV3(long jobExecId) {
        super(jobExecId);
        this.remotablePartitions = Collections.synchronizedList(new ArrayList<RemotablePartitionEntity>());
    }

    @Override
    public Collection<RemotablePartitionEntity> getRemotablePartitions() {
        return remotablePartitions;
    }

    @Override
    public void setRemotablePartitions(Collection<RemotablePartitionEntity> remotablePartitions) {
        this.remotablePartitions = remotablePartitions;
    }

}
