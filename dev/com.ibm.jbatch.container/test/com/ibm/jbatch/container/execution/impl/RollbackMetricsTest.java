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
package com.ibm.jbatch.container.execution.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.junit.Test;

import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;

public class RollbackMetricsTest {
	//private final Logger logger = Logger.getLogger(RuntimeStepExecution.class.getName());
	private TopLevelStepExecutionEntity tlsee = new TopLevelStepExecutionEntity();
	private StepThreadExecutionEntity ste = new StepThreadExecutionEntity();
	private RuntimeStepExecution rse = null;
	/*
	 * Test to ensure that the metrics rolled back properly
	 */
	@Test
	public void testMetricsRollback(){
			
		ste.setTopLevelStepExecution(tlsee);
		rse = new RuntimeStepExecution(ste);

    	ArrayList<MetricImpl.MetricType> committedMetricTypes = new ArrayList<MetricImpl.MetricType>();
    	committedMetricTypes.add(MetricImpl.MetricType.COMMIT_COUNT);
    	committedMetricTypes.add(MetricImpl.MetricType.READ_COUNT);
    	committedMetricTypes.add(MetricImpl.MetricType.FILTER_COUNT);
    	committedMetricTypes.add(MetricImpl.MetricType.WRITE_COUNT);
    	
    	ArrayList<MetricImpl.MetricType> otherMetricTypes = new ArrayList<MetricImpl.MetricType>();
    	otherMetricTypes.add(MetricImpl.MetricType.READ_SKIP_COUNT);
    	otherMetricTypes.add(MetricImpl.MetricType.PROCESS_SKIP_COUNT);
    	otherMetricTypes.add(MetricImpl.MetricType.WRITE_SKIP_COUNT);
    	otherMetricTypes.add(MetricImpl.MetricType.ROLLBACK_COUNT);
    	
		initializeMetrics(committedMetricTypes);
		initializeMetrics(otherMetricTypes);


    	
		//This will store the last committed metrics
		rse.setCommittedMetrics();
		
		//increment all of the metrics by 1, 3 times to ensure multiple increments work properly before a roll back 
		incrementMetrics();
		incrementMetrics();
		incrementMetrics();
		
		//roll back the specified metrics and ensure two roll backs in a row work
		rse.rollBackMetrics();
		rse.rollBackMetrics();
		
		for (int i = 0; i < committedMetricTypes.size(); i++){
			assertEquals( 0, rse.getMetric(committedMetricTypes.get(i)).getValue());			
		}
		for (int i = 0; i < otherMetricTypes.size(); i++){
			assertEquals( 3, rse.getMetric(otherMetricTypes.get(i)).getValue());			
		}
		
		//Test that incrementing, committing and rolling back metrics work properly after a roll back
		//committedMetricTypes == 3 and otherMetricTypes == 0 before increment
		incrementMetrics();
		rse.setCommittedMetrics();
		rse.rollBackMetrics();		
		for (int i = 0; i < committedMetricTypes.size(); i++){
			assertEquals( 1, rse.getMetric(committedMetricTypes.get(i)).getValue());			
		}
		for (int i = 0; i < otherMetricTypes.size(); i++){
			assertEquals( 4, rse.getMetric(otherMetricTypes.get(i)).getValue());			
		}
	}
	
	/*
	 * Initialize the passed in metricTypes to a value of 0
	 */
	private void initializeMetrics(ArrayList<MetricImpl.MetricType> metricTypes){

    	for (int i = 0; i < metricTypes.size(); i++){
    		rse.addMetric(metricTypes.get(i), 0);
    	}
	}
	
	/*
	 * Increment all of the metrics by a value of 1
	 */
	private void incrementMetrics() {
		rse.getMetric(MetricImpl.MetricType.READ_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.WRITE_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.FILTER_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.COMMIT_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.READ_SKIP_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.WRITE_SKIP_COUNT).incValue();
		rse.getMetric(MetricImpl.MetricType.ROLLBACK_COUNT).incValue();
	}
	

}
