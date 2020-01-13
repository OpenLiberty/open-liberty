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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;

import com.ibm.websphere.ras.annotation.Trivial;

@Entity
public class JobExecutionEntityV2 extends JobExecutionEntity {

    //@Column(name = "VERSION")
    //private int version = 2;

    @ElementCollection
    @CollectionTable(name = "JOBPARAMETER", joinColumns = @JoinColumn(name = "FK_JOBEXECID"))
    private Set<JobParameter> jobParameterElements;

    // For JPA
    @Trivial
    public JobExecutionEntityV2() {
        super();
    }

    // For in-memory persistence
    public JobExecutionEntityV2(long jobExecId) {
        super(jobExecId);
    }

    @Override
    public void setJobParameters(Properties jobParameters) {
        this.jobParameters = trimJESParameters(jobParameters);

        if (this.jobParameters != null) {
            Set<JobParameter> params = new HashSet<JobParameter>();
            for (Map.Entry param : this.jobParameters.entrySet()) {
                JobParameter newParam = new JobParameter();
                newParam.setParameterName((String) param.getKey());
                newParam.setParameterValue((String) param.getValue());
                params.add(newParam);
            }
            jobParameterElements = params;
        }
    }

}
