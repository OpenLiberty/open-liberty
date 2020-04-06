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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.ws.PartitionReplyQueue;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;

public class ExecutionElementControllerFactory {

    private final static String CLASSNAME = ExecutionElementControllerFactory.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    public static BaseStepControllerImpl getStepController(RuntimeWorkUnitExecution jobExecutionImpl,
                                                           Step step,
                                                           long rootJobExecutionId,
                                                           PartitionReplyQueue partitionReplyQueue) {

        String methodName = "getStepController";
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASSNAME, methodName, "Get StepController for", step.getId());
        }

        if (step.getPartition() != null) {
            // We may have a partition in the cloned model at the partition level for which we don't want to use
            // the top-level controller
            if (!(jobExecutionImpl instanceof RuntimePartitionExecution)) {
                return new PartitionedStepControllerImpl(jobExecutionImpl, step);
                // TODO: check if multi-jvm enabled (non-null BatchJmsDispatcher), new controller type?
                // runtimeWorkUnitExecution.getTopLevelJobProperties("multiJvm")
            }
        }

        Batchlet batchlet = step.getBatchlet();
        if (batchlet != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Found batchlet: " + batchlet + ", with ref= " + batchlet.getRef());
            }
            if (step.getChunk() != null) {
                throw new IllegalArgumentException("Step contains both a batchlet and a chunk.  Aborting.");
            }
            return new BatchletStepControllerImpl(jobExecutionImpl, step).setPartitionReplyQueue(partitionReplyQueue);

        } else {
            Chunk chunk = step.getChunk();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Found chunk: " + chunk);
            }
            if (chunk == null) {
                throw new IllegalArgumentException("Step does not contain either a batchlet or a chunk.  Aborting.");
            }
            return new ChunkStepControllerImpl(jobExecutionImpl, step).setPartitionReplyQueue(partitionReplyQueue);
        }
    }

    public static DecisionControllerImpl getDecisionController(RuntimeWorkUnitExecution jobExecutionImpl, Decision decision) {
        return new DecisionControllerImpl(jobExecutionImpl, decision);
    }

    public static FlowControllerImpl getFlowController(RuntimeWorkUnitExecution jobExecutionImpl, Flow flow, long rootJobExecutionId) {
        return new FlowControllerImpl(jobExecutionImpl, flow, rootJobExecutionId);
    }

    public static SplitControllerImpl getSplitController(RuntimeWorkUnitExecution jobExecutionImpl, Split split, long rootJobExecutionId) {
        return new SplitControllerImpl(jobExecutionImpl, split, rootJobExecutionId);
    }

}
