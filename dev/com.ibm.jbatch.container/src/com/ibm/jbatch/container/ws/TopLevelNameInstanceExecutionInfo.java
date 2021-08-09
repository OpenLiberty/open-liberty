/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import java.io.Serializable;

/**
 * Properties are also propagatable from the top level, but they need to be treated differently,
 * since some will potentially need to be resolved (especially for partitions)
 * early, before the job model is fully constructed.
 */
public class TopLevelNameInstanceExecutionInfo implements Serializable {

	/**
     * default.
     */
    private static final long serialVersionUID = 1L;
    
    private String jobName;
	private long instanceId;
	private long executionId;
	
	public TopLevelNameInstanceExecutionInfo(String jobName, long instanceId, long executionId) {
		this.jobName = jobName;
		this.instanceId = instanceId;
		this.executionId = executionId;
	}	

    public String getJobName() {
		return jobName;
	}

	public long getInstanceId() {
		return instanceId;
	}

	public long getExecutionId() {
		return executionId;
	}
	
	public String toString() {
	    return "TopLevelNameIntanceExecutionInfo:jobName=" + jobName + ":instanceId=" + instanceId + ":executionId=" + executionId;
	}
		
}
