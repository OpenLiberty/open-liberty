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

import java.io.Serializable;

/**
 * @author skurz
 *
 */
public class RemotableSplitFlowKey implements Serializable {

	private static final long serialVersionUID = 1L;

	public RemotableSplitFlowKey() { }
	
	public RemotableSplitFlowKey(long jobExecutionId, String flowName) {
		this.jobExec = jobExecutionId;
		this.flowName = flowName;
	}

	private long jobExec;
	
	private String flowName;

    public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public int hashCode() {
        return (new Long(jobExec).intValue() + flowName.hashCode()) / 37;
    }
    
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RemotableSplitFlowKey)) return false;
        RemotableSplitFlowKey pk = (RemotableSplitFlowKey) obj;
        return (pk.flowName.equals(this.flowName) && pk.jobExec == this.jobExec);
    }

	public long getJobExec() {
		return jobExec;
	}

	public void setJobExec(long jobExecutionId) {
		this.jobExec = jobExecutionId;
	}

	@Override
	public String toString() {
		return "Type: RemotableSplitFlowKey, fields:  jobExecutionId = " + jobExec + ", flowName = " + flowName;
	}
}
