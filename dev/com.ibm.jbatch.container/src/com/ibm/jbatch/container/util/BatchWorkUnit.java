/**
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
package com.ibm.jbatch.container.util;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import com.ibm.jbatch.container.IThreadRootController;
import com.ibm.jbatch.container.callback.IJobExecutionEndCallbackService;
import com.ibm.jbatch.container.callback.IJobExecutionStartCallbackService;
import com.ibm.jbatch.container.controller.impl.WorkUnitThreadControllerImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

/**
 *
 * BatchWorkUnit is the thread that runs the job.
 *
 * BatchWorkUnits are created by BatchKernelImpl during pre-job setup, then they're
 * submitted to an ExecutorService and run from there.
 *
 * The high-level flow is:
 *
 * BatchWorkUnit.run
 * IJobExecutionStartCallbackService.jobStarted
 * WorkUnitThreadControllerImpl.originateExecutionOnThread
 * ExecutionTransitioner.doExecutionLoop // job steps are processed in this loop
 * BatchWorkUnit.markThreadCompleted
 * IJobExecutionEndCallbackService.jobEnded
 *
 *
 * Note: I took out the 'work type' constant since I don't see that we want to use
 * the same thread pool for start requests as we'd use for stop requests.
 * The stop seems like it should be synchronous from the JobOperator's
 * perspective, as it returns a 'success' boolean.
 */
public abstract class BatchWorkUnit implements Runnable {

    private final String CLASSNAME = BatchWorkUnit.class.getName();
    private final Logger logger = Logger.getLogger(BatchWorkUnit.class.getName());

    protected RuntimeWorkUnitExecution runtimeWorkUnitExecution = null;
    protected IBatchKernelService batchKernel = null;
    protected IThreadRootController controller;
    protected List<IJobExecutionStartCallbackService> beforeCallbacks;
    protected List<IJobExecutionEndCallbackService> afterCallbacks;
    protected TransactionManager tranMgr;

    protected boolean notifyCallbackWhenDone;

    public BatchWorkUnit(IBatchKernelService batchKernel,
                         RuntimeWorkUnitExecution runtimeExecution,
                         List<IJobExecutionStartCallbackService> beforeCallbacks,
                         List<IJobExecutionEndCallbackService> afterCallbacks,
                         TransactionManager tranMgr) {
        this(batchKernel, runtimeExecution, beforeCallbacks, afterCallbacks, tranMgr, true);
    }

    public BatchWorkUnit(IBatchKernelService batchKernel,
                         RuntimeWorkUnitExecution runtimeExecution,
                         List<IJobExecutionStartCallbackService> beforeCallbacks,
                         List<IJobExecutionEndCallbackService> afterCallbacks,
                         TransactionManager tranMgr,
                         boolean notifyCallbackWhenDone) {
        this.setBatchKernel(batchKernel);
        this.runtimeWorkUnitExecution = runtimeExecution;
        this.setNotifyCallbackWhenDone(notifyCallbackWhenDone);
        this.controller = new WorkUnitThreadControllerImpl(runtimeWorkUnitExecution);
        this.beforeCallbacks = beforeCallbacks;
        this.afterCallbacks = afterCallbacks;
        this.tranMgr = tranMgr;
    }

    public IThreadRootController getController() {
        return this.controller;
    }

    @Override
    public void run() {
        String method = "run";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, method);
        }

        try {

            threadBegin();

            runtimeWorkUnitExecution.logExecutionStartingMessage();

            //In case of top-level job, this will not throw an exception if the job fails.
            // hence we check and log the appropriate message.
            controller.runExecutionOnThread();

            if (runtimeWorkUnitExecution.getBatchStatus().equals(BatchStatus.COMPLETED)) {
                runtimeWorkUnitExecution.logExecutionCompletedMessage();
            } else if (runtimeWorkUnitExecution.getBatchStatus().equals(BatchStatus.STOPPED)) {
                runtimeWorkUnitExecution.logExecutionStoppedMessage();
            } else {
                runtimeWorkUnitExecution.logExecutionFailedMessage();
            }
        } catch (Throwable t) {
            runtimeWorkUnitExecution.logExecutionFailedMessage();
            throw new BatchContainerRuntimeException("The job failed unexpectedly.", t);
        } finally {
            threadEnd();
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(CLASSNAME, method);
        }
    }

    protected BatchStatus getBatchStatus() {
        return runtimeWorkUnitExecution.getWorkUnitJobContext().getBatchStatus();
    }

    protected String getExitStatus() {
        return runtimeWorkUnitExecution.getWorkUnitJobContext().getExitStatus();
    }

    public void setBatchKernel(IBatchKernelService batchKernel) {
        this.batchKernel = batchKernel;
    }

    public IBatchKernelService getBatchKernel() {
        return batchKernel;
    }

    public void setJobExecutionImpl(RuntimeWorkUnitExecution runtimeWorkUnitExecution) {
        this.runtimeWorkUnitExecution = runtimeWorkUnitExecution;
    }

    public RuntimeWorkUnitExecution getRuntimeWorkUnitExecution() {
        return runtimeWorkUnitExecution;
    }

    public void setNotifyCallbackWhenDone(boolean notifyCallbackWhenDone) {
        this.notifyCallbackWhenDone = notifyCallbackWhenDone;
    }

    public boolean isNotifyCallbackWhenDone() {
        return notifyCallbackWhenDone;
    }

    /**
     * All the beginning of thread processing.
     *
     * Note we are going to fail fast out of here and fail the execution upon experiencing any exception.
     */
    protected void threadBegin() {
        //
        // Note the 'beforeCallbacks' list is not going to have a well-defined order, so it is of limited use.
        // We probably want to revisit this with some kind of priority/order based on ranking to make
        // it more useful.
        //
        // At the moment we treat the setting of the tran timeout separately, rather than as a IJobExecutionStartCallbackService,
        // to reflect the fact that this is provided by a required dependency.  If we were to want this to function in SE mode,
        // say, we might want to factor this too as an "extension" that is provided by another layer, and so incorporate this into a
        // more generic, extensible "before" processing.
        try {
            tranMgr.setTransactionTimeout(0);
            if (beforeCallbacks != null) {
                for (IJobExecutionStartCallbackService callback : beforeCallbacks) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASSNAME, "threadBegin", "Calling before callback", callback);
                    }
                    callback.jobStarted(runtimeWorkUnitExecution);
                }
            }
        } catch (Throwable t) {
            // Fail the execution if any of our before-work-unit callbacks failed. E.g. the joblog context setup
            // was unsuccessful. We may want to make it an option whether or not to fail here in the future.
            runtimeWorkUnitExecution.logExecutionFailedMessage();
            throw new BatchContainerRuntimeException("An error occurred during thread initialization.", t);
        }
    }

    /**
     * All the end of thread processing.
     *
     * Note we are going to try to catch and eat any exceptions thrown within this method.
     * We really want to give as much chance as possible for each piece of cleanup to complete.
     */
    protected void threadEnd() {
        //
        // Note the 'afterCallbacks' list is not going to have a well-defined order, so it is of limited use.
        // We probably want to revisit this with some kind of priority/order based on ranking to make
        // it more useful.
        //
        // At the moment we treat the setting of the tran timeout separately, rather than as a IJobExecutionEndCallbackService,
        // to reflect the fact that this is provided by a required dependency.  If we were to want this to function in SE mode,
        // say, we might want to factor this too as an "extension" that is provided by another layer, and so incorporate this into a
        // more generic, extensible "after" processing.
        try {
            getBatchKernel().workUnitCompleted(this);
        } catch (Exception e) {
            // FFDC instrumentation
        }

        if (afterCallbacks != null) {
            for (IJobExecutionEndCallbackService callback : afterCallbacks) {
                try {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASSNAME, "threadEnd", "Calling after callback", callback);
                    }
                    callback.jobEnded(runtimeWorkUnitExecution);
                } catch (Throwable t) {
                    // FFDC instrumentation
                }
            }
        }

        try {
            tranMgr.setTransactionTimeout(0);
        } catch (SystemException e) {
            // FFDC instrumentation
        }
    }

}
