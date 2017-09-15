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
package com.ibm.jbatch.container.ws;

/**
 * For a given logical partition, this object aggregates related data
 * from each of the STEPTHREADEXECUTION and REMOTABLEPARTITION tables.
 */
public interface WSPartitionStepAggregate {

    /**
     * @return STEPTHREADEXECUTION data
     */
    public WSPartitionStepThreadExecution getPartitionStepThread();

    /**
     * @return REMOTABLEPARTITION data
     */
    public WSRemotablePartitionExecution getRemotablePartition();

}