/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.services.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceKey;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceKey;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.WSPartitionStepAggregate;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;

/**
 *
 */
public class MemoryPersistenceManagerImplTest {

    @Before
    public void mockitoSetup() {
        MockitoAnnotations.initMocks(this);

        this.service = new MemoryPersistenceManagerImpl();

        service.activate(null, null);

        when(mockBatchLocationService.getBatchRestUrl()).thenReturn("mockRestUrl");
        when(mockBatchLocationService.getServerId()).thenReturn("mockServerId");

        service.setBatchLocationService(mockBatchLocationService);

        this.jobInstanceEntity = service.createJobInstance("mockApp", "mockXML", "mockUser", new Date());
        this.jobExecutionEntity = service.createJobExecution(jobInstanceEntity.getInstanceId(), new Properties(), new Date());
        RemotablePartitionKey remotablePartitionKey1 = new RemotablePartitionKey(jobExecutionEntity.getExecutionId(), "mockStep", 0);
        RemotablePartitionKey remotablePartitionKey2 = new RemotablePartitionKey(jobExecutionEntity.getExecutionId(), "mockStep", 1);
        service.createRemotablePartition(remotablePartitionKey1);
        service.createRemotablePartition(remotablePartitionKey2);

        this.topLevelStepExecution = service.createTopLevelStepExecutionAndNewThreadInstance(jobExecutionEntity.getExecutionId(),
                                                                                             new TopLevelStepInstanceKey(jobInstanceEntity.getInstanceId(), "mockStep"), true);

        this.partition1 = service.createPartitionStepExecutionAndNewThreadInstance(jobExecutionEntity.getExecutionId(),
                                                                                   new StepThreadInstanceKey(jobInstanceEntity.getInstanceId(), "mockStep", 1), true);
        this.partition0 = service.createPartitionStepExecutionAndNewThreadInstance(jobExecutionEntity.getExecutionId(),
                                                                                   new StepThreadInstanceKey(jobInstanceEntity.getInstanceId(), "mockStep", 0), true);
    }

    private MemoryPersistenceManagerImpl service;

    private TopLevelStepExecutionEntity topLevelStepExecution;
    private StepThreadExecutionEntity partition0;
    private StepThreadExecutionEntity partition1;
    private JobExecutionEntity jobExecutionEntity;
    private JobInstanceEntity jobInstanceEntity;

    @Mock
    private BatchLocationService mockBatchLocationService;

    @Test
    public void testGetStepExecutionAggregate() {

        WSStepThreadExecutionAggregate steps = service.getStepExecutionAggregate(topLevelStepExecution.getStepExecutionId());

        validateStepAggregate(steps);

    }

    @Test
    public void testGetStepExecutionAggregateFromJobExecutionNumberAndStepName() {

        WSStepThreadExecutionAggregate steps = service.getStepExecutionAggregateFromJobExecutionNumberAndStepName(jobInstanceEntity.getInstanceId(), 0, "mockStep");

        validateStepAggregate(steps);

    }

    @Test
    public void testGetStepExecutionAggregateFromJobExecutionId() {

        WSStepThreadExecutionAggregate steps = service.getStepExecutionAggregateFromJobExecutionId(jobExecutionEntity.getExecutionId(), "mockStep");

        validateStepAggregate(steps);

    }

    /**
     * A step execution exists in the store (with a null startTime) from the moment it is created until
     * the step controller calls startStep(). Requesting the top-level step executions of the job in that
     * window must not fail.
     */
    @Test
    public void testGetStepExecutionsTopLevelFromJobExecutionIdWithStepNotYetStarted() {

        // Earlier step of the job which has already started.
        topLevelStepExecution.setStartTime(new Date());

        // Next step: created, but startStep() has not yet set its startTime.
        TopLevelStepExecutionEntity notYetStarted = service.createTopLevelStepExecutionAndNewThreadInstance(jobExecutionEntity.getExecutionId(),
                                                                                                           new TopLevelStepInstanceKey(jobInstanceEntity.getInstanceId(), "mockStep2"),
                                                                                                           false);

        List<StepExecution> steps = service.getStepExecutionsTopLevelFromJobExecutionId(jobExecutionEntity.getExecutionId());

        // The step without a startTime sorts last.
        assertEquals(2, steps.size());
        assertEquals(topLevelStepExecution.getStepExecutionId(), steps.get(0).getStepExecutionId());
        assertEquals(notYetStarted.getStepExecutionId(), steps.get(1).getStepExecutionId());
    }

    private void validateStepAggregate(WSStepThreadExecutionAggregate steps) {
        assertTrue(steps.getTopLevelStepExecution() == topLevelStepExecution);
        assertEquals(topLevelStepExecution.getStepExecutionId(), steps.getTopLevelStepExecution().getStepExecutionId());

        assertEquals(2, steps.getPartitionAggregate().size());

        //Check the order
        assertEquals(0, steps.getPartitionAggregate().get(0).getPartitionStepThread().getPartitionNumber());
        assertEquals(1, steps.getPartitionAggregate().get(1).getPartitionStepThread().getPartitionNumber());

        assertEquals(partition0, steps.getPartitionAggregate().get(0).getPartitionStepThread());
        assertEquals(partition1, steps.getPartitionAggregate().get(1).getPartitionStepThread());

        for (WSPartitionStepAggregate partition : steps.getPartitionAggregate()) {
            assertEquals("mockRestUrl", partition.getRemotablePartition().getRestUrl());
            assertEquals("mockServerId", partition.getRemotablePartition().getServerId());
        }
    }
}
