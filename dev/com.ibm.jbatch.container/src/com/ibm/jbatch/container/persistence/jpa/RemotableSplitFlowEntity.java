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

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * @author skurz
 *
 */
//@Entity - comment out so there's no confusion..we don't want this in the DB for 2Q15
@IdClass(RemotableSplitFlowKey.class)
public class RemotableSplitFlowEntity extends JobThreadExecutionBase {

    // Repeat everywhere we use so caller has to think through granting privilege
    protected static String eol = AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
            return System.getProperty("line.separator");
        }
    });

    @Id
    private String flowName;

    @Id
    @ManyToOne
    private JobExecutionEntity jobExec;

    private int internalStatus;

    @Trivial
    public RemotableSplitFlowEntity() {}

    /**
     * @return the flowName
     */
    public String getFlowName() {
        return flowName;
    }

    /**
     * @param flowName the flowName to set
     */
    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    /**
     * @return the jobExecution
     */
    public JobExecutionEntity getJobExecution() {
        return jobExec;
    }

    /**
     * @param jobExecution the jobExecution to set
     */
    public void setJobExecution(JobExecutionEntity jobExec) {
        this.jobExec = jobExec;
    }

    public int getInternalStatus() {
        return internalStatus;
    }

    public void setInternalStatus(int internalStatus) {
        this.internalStatus = internalStatus;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString() + eol);
        buf.append("For RemotableSplitFlowExecutionEntity:");
        buf.append(" flowName = " + flowName);
        buf.append(", internal status = " + internalStatus);
        return buf.toString();
    }

}
