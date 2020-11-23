/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.samples.batch.artifacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

@Named("SimpleIntegerPartitionMapper")
public class SimpleIntegerPartitionMapper implements PartitionMapper, BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    JobContext jobCtx;

    @Inject
    @BatchProperty(name = "numRecords")
    private String numRecordsStr;
    private int numRecords;

    @Inject
    @BatchProperty(name = "numPartitions")
    private String numPartitionsStr;
    private int numPartitions;

    @Override
    public PartitionPlan mapPartitions() throws Exception {
        numPartitions = Integer.parseInt(numPartitionsStr);
        logger.fine("In mapper, # of partitions = " + numPartitionsStr);
        numRecords = Integer.parseInt(numRecordsStr);
        logger.fine("In mapper, # of records = " + numRecordsStr);

        PartitionPlanImpl plan = new PartitionPlanImpl();
        plan.setPartitions(numPartitions);

        List<Properties> partitionProperties = new ArrayList<Properties>(numPartitions);

        // If the number of records is evenly divisible by the number of partitions, then
        // all partitions are the same size (so 'extraRecords' = 0).
        int minNumRecordsPerPartition = numRecords / numPartitions;
        int extraRecords = numRecords % numPartitions;

        logger.fine("In mapper, # each partition will have at least = " + minNumRecordsPerPartition + 
        		" # of records and " + extraRecords + " partitions will have " + minNumRecordsPerPartition + 1 + " records");

        int startAtIndex = 0;
        for (int i = 0;  i < numPartitions; i++) {
        	        	
        	Properties p = new Properties();
        	
        	// This ensures each extra record gets assigned to a partition, starting with the 0th partition.
        	Integer recordsInNextPartition =  i < extraRecords ?
        			minNumRecordsPerPartition + 1 : 
        				minNumRecordsPerPartition;
            
            logger.fine("In mapper, partition # " + i + " will start at next record index of: " + 
            		startAtIndex + " and contain " + recordsInNextPartition + " # of records.");
            
            p.setProperty("startAtIndex", Integer.toString(startAtIndex));
            p.setProperty("partitionSize", Integer.toString(recordsInNextPartition));
            
            startAtIndex += recordsInNextPartition;

            partitionProperties.add(p);
        }
        
        if (startAtIndex != numRecords) {
        	String errorMsg = "Messed up calculation in mapper.  Total # of records = " + numRecords + " but only assigned partitions # = " + startAtIndex;
        	BonusPayoutUtils.throwIllegalStateExc(errorMsg);        	
        }

        plan.setPartitionProperties(partitionProperties.toArray(new Properties[0]));

        return plan;
    }
}
