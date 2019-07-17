/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.services;

import com.ibm.jbatch.container.instance.WorkUnitDescriptor;
import com.ibm.jbatch.container.ws.BatchJobNotLocalException;
import com.ibm.jbatch.spi.services.IBatchServiceBase;
import com.ibm.ws.jbatch.joblog.JobExecutionLog;
import com.ibm.ws.jbatch.joblog.JobInstanceLog;

public interface IJobLogManagerService extends IBatchServiceBase {

    /**
     * Sets the job logging context for the current thread.
     */
    void workUnitStarted(WorkUnitDescriptor ctx);

    /**
     * Un-sets the job logging context for the current thread.
     */
    void workUnitEnded(WorkUnitDescriptor ctx);

    /**
     * @return the JobExecutionLog for the given job execution id.
     * @throws BatchJobNotLocalException
     */
    JobExecutionLog getJobExecutionLog(long jobExecutionId) throws BatchJobNotLocalException;

    /**
     * @return the JobInstanceLog for the given job instance.
     * @throws BatchJobNotLocalException
     */
    JobInstanceLog getJobInstanceLog(long jobInstanceId) throws BatchJobNotLocalException;

    /**
     * @return the JobInstanceLog for the given job instance. No local check!
     */
    JobInstanceLog getLocalJobInstanceLog(long jobInstanceId);

    /**
     * @param jobInstanceId
     * @return
     */
    JobInstanceLog getJobInstanceLogAllExecutions(long jobInstanceId);

}
