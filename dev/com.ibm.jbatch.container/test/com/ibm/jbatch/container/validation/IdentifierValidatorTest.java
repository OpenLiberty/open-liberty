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

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.services.impl.JPAPersistenceManagerImpl;

/**
 *
 */
public class IdentifierValidatorTest {

    @Before
    public void mockitoSetup() {
        MockitoAnnotations.initMocks(this);
    }

    /*
     * JobInstanceId test
     * Test for null list, empty list, non-empty list scenarios
     */
    @Mock
    private JobInstanceEntity mockJobInstance;

    /*
     * JobExecution test
     * Test for null list, empty list, non-empty list scenarios
     */
    @Mock
    private JobExecutionEntity mockExecution;

    /*
     * StepExecution test
     * Test for null list, empty list, non-empty list scenarios
     */
    @Mock
    private StepThreadExecutionEntity mockStepExecution;

    @Test
    public void testValidatePersistedJobInstanceIdsValid() throws PersistenceException {
        when(mockJobInstance.getInstanceId()).thenReturn((long) 1);

        IdentifierValidator.validatePersistedJobInstanceIds(mockJobInstance);

    }

    @Test(expected = PersistenceException.class)
    public void testValidatePersistedJobInstanceIdsException() throws PersistenceException {
        when(mockJobInstance.getInstanceId()).thenReturn((long) 0);
        when(mockJobInstance.getAmcName()).thenReturn("TestAmcName");
        when(mockJobInstance.getSubmitter()).thenReturn("TestSubmitterName");

        //Should throw exception.
        IdentifierValidator.validatePersistedJobInstanceIds(mockJobInstance);
    }

    @Test(expected = PersistenceException.class)
    public void testNegativeValidatePersistedJobInstanceIdsException() throws PersistenceException {
        when(mockJobInstance.getInstanceId()).thenReturn((long) -1);
        when(mockJobInstance.getAmcName()).thenReturn("TestAmcName");
        when(mockJobInstance.getSubmitter()).thenReturn("TestSubmitterName");

        //Should throw exception.
        IdentifierValidator.validatePersistedJobInstanceIds(mockJobInstance);
    }

    @Test
    public void testValidatePersistedJobExecutionIsValid() throws PersistenceException {
        when(mockExecution.getInstanceId()).thenReturn((long) 1);
        when(mockExecution.getExecutionId()).thenReturn((long) 1);

        IdentifierValidator.validatePersistedJobExecution(mockExecution);
    }

    @Test(expected = PersistenceException.class)
    public void testValidatePersistedJobExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getInstanceId()).thenReturn((long) 2);
        when(mockExecution.getExecutionId()).thenReturn((long) 0);

        //Should throw exception.
        IdentifierValidator.validatePersistedJobExecution(mockExecution);
    }

    @Test(expected = PersistenceException.class)
    public void testNegativeValidatePersistedJobExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockExecution.getInstanceId()).thenReturn((long) 2);
        when(mockExecution.getExecutionId()).thenReturn((long) -1);

        //Should throw exception.
        IdentifierValidator.validatePersistedJobExecution(mockExecution);
    }

    @Test
    public void testValidatePersistedStepExecutionIsValid() throws PersistenceException {
        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 1);

        IdentifierValidator.validatePersistedStepExecution(mockStepExecution);

    }

    @Test(expected = PersistenceException.class)
    public void testValidatePersistedStepExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getStepExecutionId()).thenReturn((long) 0);

        when(mockStepExecution.getJobExecution()).thenReturn(mockExecution);
        when(mockExecution.getExecutionId()).thenReturn((long) 5);
        when(mockExecution.getInstanceId()).thenReturn((long) 3);

        //Should throw exception.
        IdentifierValidator.validatePersistedStepExecution(mockStepExecution);
    }

    @Test(expected = PersistenceException.class)
    public void testNegativeValidatePersistedStepExecutionException() throws PersistenceException {
        JPAPersistenceManagerImpl j = new JPAPersistenceManagerImpl();

        when(mockStepExecution.getStepExecutionId()).thenReturn((long) -1);

        when(mockStepExecution.getJobExecution()).thenReturn(mockExecution);
        when(mockExecution.getExecutionId()).thenReturn((long) 5);
        when(mockExecution.getInstanceId()).thenReturn((long) 3);

        //Should throw exception.
        IdentifierValidator.validatePersistedStepExecution(mockStepExecution);
    }
}
