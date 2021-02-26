/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import javax.batch.runtime.JobExecution;

/**
 * Resolves the batch REST url based on endpoint, server, and/or system config.
 */
public interface BatchLocationService {

    /**
     * @return the batch REST url for this server: https://{host}:{port}/ibm/api/batch
     */
    public String getBatchRestUrl();

    /**
     * @return unique identity for this server: ${defaultHostName}/${wlp.user.dir}/serverName
     */
    public String getServerId();

    /**
     * @return true if the given jobexecution ran (or is running) on this server.
     */
    public boolean isLocalJobExecution(WSJobExecution jobExecution);

    /**
     * @return true if the given jobexecution ran (or is running) on this server.
     */
    public boolean isLocalJobExecution(long executionId);

    /**
     * @return the JobExecution instance.
     *
     * @throws BatchJobNotLocalException if the given execution did not execute here in this server.
     */
    public JobExecution assertIsLocalJobExecution(long executionId) throws BatchJobNotLocalException;

    /**
     * @param partition
     * @return true if the given remotable partition ran (or is running) on this server
     */
    boolean isLocalRemotablePartition(WSRemotablePartitionExecution partition);

}
