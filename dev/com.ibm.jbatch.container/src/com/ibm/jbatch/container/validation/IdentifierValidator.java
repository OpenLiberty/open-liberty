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
package com.ibm.jbatch.container.validation;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.exception.BatchIllegalIDPersistedException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;

/**
 *
 */
public class IdentifierValidator {

    private final static Logger logger = Logger.getLogger(IdentifierValidator.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    /**
     * Check persisted job instance id and throw exception if any ids violate
     * batch id rule: Cannot be <= 0
     */
    public static void validatePersistedJobInstanceIds(JobInstanceEntity instance) throws PersistenceException {
        if (instance.getInstanceId() <= 0) {

            long id = instance.getInstanceId();

            PersistenceException e = new PersistenceException(new BatchIllegalIDPersistedException(Long.toString(id)));
            logger.log(Level.SEVERE, "error.invalid.persisted.job.id",
                       new Object[] { Long.toString(id), e });

            throw e;
        }

    }

    /**
     * Check persisted job execution id and throw exception if any ids violate
     * batch id rule: Cannot be <= 0
     */
    public static void validatePersistedJobExecution(JobExecutionEntity execution) throws PersistenceException {

        if (execution.getExecutionId() <= 0) {

            long exId = execution.getExecutionId();

            PersistenceException e = new PersistenceException(new BatchIllegalIDPersistedException(Long.toString(exId)));
            logger.log(Level.SEVERE, "error.invalid.persisted.exe.id",
                       new Object[] { Long.toString(exId), e });

            throw e;
        }

    }

    /**
     * Check persisted job step execution id and throw exception if any ids
     * violate batch id rule: Cannot be <= 0
     */
    public static void validatePersistedStepExecution(
                                                      StepThreadExecutionEntity stepExecution) throws PersistenceException {

        if (stepExecution.getStepExecutionId() <= 0) {

            long stepId = stepExecution.getStepExecutionId();

            PersistenceException e = new PersistenceException(new BatchIllegalIDPersistedException(Long.toString(stepId)));
            logger.log(Level.SEVERE, "error.invalid.persisted.step.id",
                       new Object[] { Long.toString(stepId), e });

            throw e;
        }

    }
}
