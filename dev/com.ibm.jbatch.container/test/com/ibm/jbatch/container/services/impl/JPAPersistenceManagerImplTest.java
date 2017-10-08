/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.services.impl;

import static org.mockito.Mockito.when;

import javax.batch.runtime.BatchStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.ws.InstanceState;

/*
 *JPAPersistenceManagerImplTest test
 *Test the batch table id validation routines.
 */
public class JPAPersistenceManagerImplTest {

    @Before
    public void mockitoSetup() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock
    private JobExecutionEntity mockExecution;

    @Mock
    private JobInstanceEntity mockJobInstance;

    @Mock
    private StepThreadExecutionEntity mockStepExecution;

    /*
     * Expecting this set to run with no exceptions thrown.
     */
    @Test
    public void testExecutionVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockExecution.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution, BatchStatus.STARTED);

    }

    @Test
    public void testInstanceVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance, BatchStatus.STARTED);

    }

    @Test
    public void testVerifyStateTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getInstanceState()).thenReturn(InstanceState.JMS_QUEUED);
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.verifyStateTransitionIsValid(mockJobInstance, InstanceState.DISPATCHED);

    }

    @Test
    public void testVerifyThreadStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 1);

        j.verifyThreadStatusTransitionIsValid(mockStepExecution, BatchStatus.FAILED);

    }

    /*
     * Expecting this set to throw exceptions.
     */

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testExecutionCompletedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockExecution.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceCompletedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testExecutionAbandonedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getBatchStatus()).thenReturn(BatchStatus.ABANDONED);
        when(mockExecution.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceAbandonedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getBatchStatus()).thenReturn(BatchStatus.ABANDONED);
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance, BatchStatus.STARTED);

    }

    //TCK tests allow this, so this is now a valid transition.
    //@Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceCompletedVerifyStateTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getInstanceState()).thenReturn(InstanceState.COMPLETED);
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.verifyStateTransitionIsValid(mockJobInstance, InstanceState.ABANDONED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testStepThreadExecutionCompletedVerifyThreadStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 1);

        j.verifyThreadStatusTransitionIsValid(mockStepExecution, BatchStatus.COMPLETED);

    }

}
