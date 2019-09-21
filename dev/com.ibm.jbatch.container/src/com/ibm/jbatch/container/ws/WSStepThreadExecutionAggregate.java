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
 package com.ibm.jbatch.container.ws;

import java.util.List;

import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;

/**
 * Collects the partition-level and top-level "step thread executions"
 *  (accessible via an enhanced StepExecution interface) into a single aggregate.
 */
public interface WSStepThreadExecutionAggregate {

	/**
	 * 
	 * @return top level StepExecution
	 */
	public WSTopLevelStepExecution getTopLevelStepExecution();

	/**
	 * @return a List of the partitions which happen to have executed, in order of ascending partition number.
	 * NOTE:  Don't assume that the index into the list equals the partition number.   For a given execution
	 * of the top-level step, some of the partitions may have previously completed, so will not re-execute.
	 * The partition numbers are embedded 
	 */
	// 222050 - Backout 205106
	// public List<WSPartitionStepAggregate> getPartitionAggregate();
	public List<WSPartitionStepThreadExecution> getPartitionLevelStepExecutions();

}
