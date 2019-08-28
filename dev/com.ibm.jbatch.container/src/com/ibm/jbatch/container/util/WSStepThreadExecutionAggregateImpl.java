/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.util;

import java.util.List;

import com.ibm.jbatch.container.ws.WSPartitionStepAggregate;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;

/**
 * @author skurz
 *
 */
public class WSStepThreadExecutionAggregateImpl implements WSStepThreadExecutionAggregate {

    private WSTopLevelStepExecution topLevelStepExecution;

    private List<WSPartitionStepAggregate> partitionAggregate;

    private List<WSPartitionStepThreadExecution> partitionLevelStepExecutions;

    @Override
    public WSTopLevelStepExecution getTopLevelStepExecution() {
        return topLevelStepExecution;
    }

    public void setTopLevelStepExecution(WSTopLevelStepExecution topLevelStepExecution) {
        this.topLevelStepExecution = topLevelStepExecution;
    }

    public List<WSPartitionStepAggregate> getPartitionAggregate() {
        return partitionAggregate;
    }

    public void setPartitionAggregate(List<WSPartitionStepAggregate> partitionAggregate) {
        this.partitionAggregate = partitionAggregate;
    }

    @Override
    public List<WSPartitionStepThreadExecution> getPartitionLevelStepExecutions() {
        return partitionLevelStepExecutions;
    }

    public void setPartitionLevelStepExecutions(
                                                List<WSPartitionStepThreadExecution> partitionLevelStepExecutions) {
        this.partitionLevelStepExecutions = partitionLevelStepExecutions;
    }

}
