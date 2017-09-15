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
import com.ibm.jbatch.container.exception.PersistenceException;
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
    private JobInstanceEntity mockJobInstance;

    /*
     * JobInstanceId test
     * Test for null list, empty list, non-empty list scenarios
     */
    @Test
    public void testValidatePersistedJobInstanceIdsValid() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();
        //JPAPersistenceManagerImpl spyj = spy(j);

        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        j.validatePersistedJobInstanceIds(mockJobInstance);

    }

    @Test(expected = PersistenceException.class)
    public void testvalidatePersistedJobInstanceIdsException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance.getInstanceId()).thenReturn((long) 0);
        when(mockJobInstance.getAmcName()).thenReturn("TestAmcName");
        when(mockJobInstance.getSubmitter()).thenReturn("TestSubmitterName");

        //Should throw exception.
        j.validatePersistedJobInstanceIds(mockJobInstance);

    }

    /*
     * JobExecution test
     * Test for null list, empty list, non-empty list scenarios
     */

    @Mock
    private JobExecutionEntity mockExecution;

    @Test
    public void testValidatePersistedJobExecutionIsValid() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getInstanceId()).thenReturn((long) 1);
        when(mockExecution.getExecutionId()).thenReturn((long) 1);

        j.validatePersistedJobExecution(mockExecution);

    }

    @Test(expected = PersistenceException.class)
    public void testValidatePersistedJobExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getInstanceId()).thenReturn((long) 0);
        when(mockExecution.getExecutionId()).thenReturn((long) -1);

        //Should throw exception.
        j.validatePersistedJobExecution(mockExecution);
    }

    /*
     * StepExecution test
     * Test for null list, empty list, non-empty list scenarios
     */

    @Mock
    private StepThreadExecutionEntity mockStepExecution;

    @Test
    public void testValidatePersistedStepExecutionIsValid() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 1);

        j.validatePersistedStepExecution(mockStepExecution);

    }

    @Test(expected = PersistenceException.class)
    public void testValidatePersistedStepExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 0);

        when(mockStepExecution.getJobExecution()).thenReturn(mockExecution);
        when(mockExecution.getExecutionId()).thenReturn((long) 0);
        when(mockExecution.getInstanceId()).thenReturn((long) 0);

        //Should throw exception.
        j.validatePersistedStepExecution(mockStepExecution);
    }

    @Mock
    private JobExecutionEntity mockExecution2;

    @Mock
    private JobInstanceEntity mockJobInstance2;

    @Mock
    private StepThreadExecutionEntity mockStepExecution2;

    /*
     * Expecting this set to run with no exceptions thrown.
     */
    @Test
    public void testExecutionVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution2.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockExecution2.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution2, BatchStatus.STARTED);

    }

    @Test
    public void testInstanceVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance2.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockJobInstance2.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance2, BatchStatus.STARTED);

    }

    @Test
    public void testVerifyStateTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance2.getInstanceState()).thenReturn(InstanceState.JMS_QUEUED);
        when(mockJobInstance2.getInstanceId()).thenReturn((long) 1);

        j.verifyStateTransitionIsValid(mockJobInstance2, InstanceState.DISPATCHED);

    }

    @Test
    public void testVerifyThreadStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution2.getBatchStatus()).thenReturn(BatchStatus.STARTING);
        when(mockStepExecution2.getStepExecutionId()).thenReturn((long) 1);

        j.verifyThreadStatusTransitionIsValid(mockStepExecution2, BatchStatus.FAILED);

    }

    /*
     * Expecting this set to throw exceptions.
     */

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testExecutionCompletedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution2.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockExecution2.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution2, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceCompletedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance2.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockJobInstance2.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance2, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testExecutionAbandonedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution2.getBatchStatus()).thenReturn(BatchStatus.ABANDONED);
        when(mockExecution2.getExecutionId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockExecution2, BatchStatus.STARTED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceAbandonedVerifyStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance2.getBatchStatus()).thenReturn(BatchStatus.ABANDONED);
        when(mockJobInstance2.getInstanceId()).thenReturn((long) 1);

        j.verifyStatusTransitionIsValid(mockJobInstance2, BatchStatus.STARTED);

    }

    //TCK tests allow this, so this is now a valid transition.
    //@Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testInstanceCompletedVerifyStateTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockJobInstance2.getInstanceState()).thenReturn(InstanceState.COMPLETED);
        when(mockJobInstance2.getInstanceId()).thenReturn((long) 1);

        j.verifyStateTransitionIsValid(mockJobInstance2, InstanceState.ABANDONED);

    }

    @Test(expected = BatchIllegalJobStatusTransitionException.class)
    public void testStepThreadExecutionCompletedVerifyThreadStatusTransitionIsValid() throws BatchIllegalJobStatusTransitionException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution2.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockStepExecution2.getStepExecutionId()).thenReturn((long) 1);

        j.verifyThreadStatusTransitionIsValid(mockStepExecution2, BatchStatus.COMPLETED);

    }

}
