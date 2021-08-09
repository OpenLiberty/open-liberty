/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.controller.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.navigator.NavigatorFactory;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.jbatch.jsl.model.Flow;

public class FlowControllerImpl implements IExecutionElementController {

	private final static String CLASSNAME = FlowControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);
	

	private final RuntimeWorkUnitExecution flowExecution;

	protected ModelNavigator<Flow> flowNavigator;

	protected Flow flow;

	private ExecutionTransitioner transitioner;

	public FlowControllerImpl(RuntimeWorkUnitExecution flowExecution, Flow flow, long rootJobExecutionId) {
		this.flowExecution = flowExecution;
		this.flowNavigator = NavigatorFactory.createFlowNavigator(flow);
		this.flow = flow;
	}

	@Override
	public ExecutionStatus execute() {
		if (!flowExecution.getBatchStatus().equals(BatchStatus.STOPPING)) { 
			transitioner = new ExecutionTransitioner(flowExecution, flowNavigator);
			
			JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "flow.started", new Object[]{flow.getId(), 
					flowExecution.getTopLevelInstanceId(), 
					flowExecution.getTopLevelExecutionId()},
					logger);
			
			ExecutionStatus flowStatus =  transitioner.doExecutionLoop();
			
			if (flowStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
				JoblogUtil.logToJobLogAndTraceOnly(Level.WARNING, "flow.failed", new Object[]{flowExecution.getFlowName(), 
						flowExecution.getTopLevelInstanceId(), 
						flowExecution.getTopLevelExecutionId()},
						logger);
			}
			
			return flowStatus;
			
		} else {
			return new ExecutionStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
		}
	}

	@Override
	public void stop() { 
		// Since this is not a top-level controller, don't try to filter based on existing status.. just pass
		// along the stop().
		IController stoppableElementController = transitioner.getCurrentStoppableElementController();
		if (stoppableElementController != null) {
			stoppableElementController.stop();
		}
	}

    @Override
    public List<Long> getLastRunStepExecutions() {
        return this.transitioner.getStepExecIds();
    }

}
