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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
public class StepThreadExecutionEntityV2 extends StepThreadExecutionEntity {

    @OneToOne(optional = true, mappedBy = "stepExecutionEntity", cascade = CascadeType.REMOVE)
    private RemotablePartitionEntity remotablePartition;

    // For JPA
    @Trivial
    public StepThreadExecutionEntityV2() {
        super();
    }

    // For in-memory
    public StepThreadExecutionEntityV2(JobExecutionEntity jobExecution, String stepName, int partitionNumber) {
        super(jobExecution, stepName, partitionNumber);
    }

    public StepThreadExecutionEntityV2(long stepExecutionId, JobExecutionEntity jobExecution, String stepName, int partitionNumber) {
        super(stepExecutionId, jobExecution, stepName, partitionNumber);
    }

    @Override
    public RemotablePartitionEntity getRemotablePartition() {
        return remotablePartition;
    }

    @Override
    public void setRemotablePartition(RemotablePartitionEntity remotablePartition) {
        this.remotablePartition = remotablePartition;
    }

}
