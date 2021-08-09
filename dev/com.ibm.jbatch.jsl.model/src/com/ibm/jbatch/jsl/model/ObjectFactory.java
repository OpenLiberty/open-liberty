/*
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
*/
package com.ibm.jbatch.jsl.model;

/**
 *
 */
public interface ObjectFactory {

    /**
     * Create an instance of {@link ExceptionClassFilter }
     *
     */
    ExceptionClassFilter createExceptionClassFilter();

    /**
     * Create an instance of {@link JSLJob }
     *
     */
    JSLJob createJSLJob();

    /**
     * Create an instance of {@link Batchlet }
     *
     */
    Batchlet createBatchlet();

    /**
     * Create an instance of {@link Analyzer }
     *
     */
    Analyzer createAnalyzer();

    /**
     * Create an instance of {@link ItemWriter }
     *
     */
    ItemWriter createItemWriter();

    /**
     * Create an instance of {@link Partition }
     *
     */
    Partition createPartition();

    /**
     * Create an instance of {@link Chunk }
     *
     */
    Chunk createChunk();

    /**
     * Create an instance of {@link Flow }
     *
     */
    Flow createFlow();

    /**
     * Create an instance of {@link Next }
     *
     */
    Next createNext();

    /**
     * Create an instance of {@link JSLProperties }
     *
     */
    JSLProperties createJSLProperties();

    /**
     * Create an instance of {@link PartitionPlan }
     *
     */
    PartitionPlan createPartitionPlan();

    /**
     * Create an instance of {@link Listener }
     *
     */
    Listener createListener();

    /**
     * Create an instance of {@link ItemReader }
     *
     */
    ItemReader createItemReader();

    /**
     * Create an instance of {@link ItemProcessor }
     *
     */
    ItemProcessor createItemProcessor();

    /**
     * Create an instance of {@link Listeners }
     *
     */
    Listeners createListeners();

    /**
     * Create an instance of {@link PartitionReducer }
     *
     */
    PartitionReducer createPartitionReducer();

    /**
     * Create an instance of {@link Stop }
     *
     */
    Stop createStop();

    /**
     * Create an instance of {@link CheckpointAlgorithm }
     *
     */
    CheckpointAlgorithm createCheckpointAlgorithm();

    /**
     * Create an instance of {@link PartitionMapper }
     *
     */
    PartitionMapper createPartitionMapper();

    /**
     * Create an instance of {@link Step }
     *
     */
    Step createStep();

    /**
     * Create an instance of {@link Collector }
     *
     */
    Collector createCollector();

    /**
     * Create an instance of {@link Property }
     *
     */
    <P extends Property> P createProperty();

    /**
     * Create an instance of {@link Split }
     *
     */
    Split createSplit();

    /**
     * Create an instance of {@link End }
     *
     */
    End createEnd();

    /**
     * Create an instance of {@link Decision }
     *
     */
    Decision createDecision();

    /**
     * Create an instance of {@link Fail }
     *
     */
    Fail createFail();

    /**
     * Create an instance of {@link ExceptionClassFilter.Include }
     *
     */
    <I extends ExceptionClassFilter.Include> I createExceptionClassFilterInclude();

    /**
     * Create an instance of {@link ExceptionClassFilter.Exclude }
     *
     */
    <E extends ExceptionClassFilter.Exclude> E createExceptionClassFilterExclude();
}