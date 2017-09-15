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
/**
 * 
 */
package com.ibm.jbatch.container.util;

import java.util.List;

import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

/**
 * @author skurz
 *
 */
public class BatchJobWorkUnit extends BatchWorkUnit {

	public BatchJobWorkUnit(IBatchKernelService batchKernel, RuntimeWorkUnitExecution runtimeExecution,
			List<IJobExecutionStartCallbackService> beforeCallbacks, List<IJobExecutionEndCallbackService> afterCallbacks) {
		super(batchKernel, runtimeExecution, beforeCallbacks, afterCallbacks);
	}
}
