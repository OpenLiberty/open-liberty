/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.execution.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.StepContextDelegate;
import com.ibm.jbatch.container.annotation.TCKExperimentProperty;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;

import com.ibm.ws.serialization.DeserializationObjectInputStream;


public class RuntimeStepExecution implements StepContextDelegate {

    private final static String sourceClass = RuntimeStepExecution.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    @TCKExperimentProperty
    private final static boolean cloneContextProperties = Boolean.getBoolean("clone.context.properties");
    
    private final String stepName;
    private Object transientUserData = null;
    private Serializable persistentUserDataObject = null;
    private Exception exception = null;
    
    private Properties properties = new Properties(); 

    private String batchletProcessRetVal = null;
    	
	protected StepContextImpl stepCtx = null;
	
	private ConcurrentHashMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();
	private ConcurrentHashMap<String, Metric> committedMetrics = new ConcurrentHashMap<String, Metric>();
	private ArrayList<MetricImpl.MetricType> tranCoordinatedMetricTypes = new ArrayList<MetricImpl.MetricType>();

	
	private BatchStatus batchStatus;
	private String exitStatus;
	private Date startTime;
	private Date endTime;
	private Date lastUpdatedTime; // This one isn't defined by spec, but provides a convenient way to pass along last-updated JobExecution time
	private long internalStepThreadExecutionId;
	private long topLevelStepExecutionId;

	public RuntimeStepExecution(StepThreadExecutionEntity stepThreadExecution) {
		this.stepName = stepThreadExecution.getStepName();
		this.batchStatus = stepThreadExecution.getBatchStatus();
		this.exitStatus = stepThreadExecution.getExitStatus();
		this.stepCtx = new StepContextImpl(this);
		this.startTime = stepThreadExecution.getStartTime();
		this.endTime = stepThreadExecution.getEndTime();
		this.internalStepThreadExecutionId = stepThreadExecution.getStepExecutionId();
		this.topLevelStepExecutionId = stepThreadExecution.getTopLevelStepExecution().getStepExecutionId();
		this.persistentUserDataObject = initializePersistentUserDataObject(stepThreadExecution.getPersistentUserDataBytes());
    	
		//Adding the metrics which we want to be rolled back
		this.tranCoordinatedMetricTypes.add(MetricImpl.MetricType.COMMIT_COUNT);
		this.tranCoordinatedMetricTypes.add(MetricImpl.MetricType.READ_COUNT);
		this.tranCoordinatedMetricTypes.add(MetricImpl.MetricType.FILTER_COUNT);
		this.tranCoordinatedMetricTypes.add(MetricImpl.MetricType.WRITE_COUNT);
	}

	@Override
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    @Override
    public Exception getException() {
        return exception;
    }
    
    public void setException(Exception exception){
    	this.exception = exception;
    }

    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    @Override
    public List<Metric> getMetrics() {
        return new ArrayList<Metric>(metrics.values());
    }
    
    public MetricImpl getMetric(MetricImpl.MetricType metricType) {
        return (MetricImpl)metrics.get(metricType.name());
    }
    
    // Since this is a non-trivial behavior to support, let's keep it internal rather than exposing it.
    private MetricImpl getCommittedMetric(MetricImpl.MetricType metricType) {
        return (MetricImpl)committedMetrics.get(metricType.name());
    }
    
    public void addMetric(MetricImpl.MetricType metricType, long value) {
    	metrics.putIfAbsent(metricType.name(), new MetricImpl(metricType, value));
    }
    
    /**
     * Creates/Updates the committedMetrics variable using the passed in metric types
     */
    public void setCommittedMetrics() {
    	for (MetricImpl.MetricType metricType : this.tranCoordinatedMetricTypes) {
    		committedMetrics.put(metricType.name(), new MetricImpl(metricType, this.getMetric(metricType).getValue()));
    	}
    }
    
    public void rollBackMetrics(){
    	for (MetricImpl.MetricType metricType : this.tranCoordinatedMetricTypes) {
    		metrics.put(metricType.name(), new MetricImpl(metricType, this.getCommittedMetric(metricType).getValue()));
    	}
    }

    @Override
    public Serializable getPersistentUserDataObject() {
        return persistentUserDataObject;
    }

    @Override
    public void setPersistentUserDataObject(Serializable data) {
    	
    	persistentUserDataObject = data;
    }

    @Override
    public Properties getProperties() {
    	if (cloneContextProperties) {
    		logger.fine("Cloning step context properties");
    		return (Properties)properties.clone();
    	} else {
    		logger.fine("Returing ref (non-clone) to step context properties");
    		return properties;
    	}
    }
    
    // This cannot be a copy or clone since we rely on the caller of this 
    // actually setting the individual properties into the Properties object.
    public Properties getJSLProperties() {
    	return properties;
    }

    public Object getTransientUserData() {
        return transientUserData;
    }

    @Override
    public void setExitStatus(String status) {
        this.exitStatus = status;
    }

    public void setBatchStatus(BatchStatus status) {
        this.batchStatus = status;
    }

    public void setTransientUserData(Object data) {
        transientUserData = data;        
    }

    @Override 
    public String toString() {    
        StringBuilder buf = new StringBuilder();
        buf.append(" stepId: " + stepName);
        buf.append(", stepThreadExecutionId: " + getInternalStepThreadExecutionId());
        buf.append(", batchStatus: " + batchStatus);        
        buf.append(", exitStatus: " + exitStatus);
        buf.append(", batchletProcessRetVal: " + batchletProcessRetVal);
        buf.append(", transientUserData: " + transientUserData);
        buf.append(", persistentUserData: " +     persistentUserDataObject);
        return buf.toString();
    }    
    
	public long getInternalStepThreadExecutionId() {
		return internalStepThreadExecutionId;
	}

	public long getTopLevelStepExecutionId() {
		return topLevelStepExecutionId;
	}

	public Date getStartTime(){
		return startTime;
	}
	
	public void setStartTime(Date start) {
		this.startTime = start;
	}
	
	public Date getEndTime(){
		return endTime;
	}

	public void setEndTime(Date end) {
		this.endTime = end;
	}
	
    public Date getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setLastUpdatedTime(Date lastUpdatedTime) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public String getBatchletProcessRetVal() {
		return batchletProcessRetVal;
	}

	public void setBatchletProcessRetVal(String batchletProcessRetVal) {
		this.batchletProcessRetVal = batchletProcessRetVal;
	}

	public StepContext getStepContext() {
		return stepCtx;
	}

	public void setStepContext(StepContextImpl stepCtx) {
		this.stepCtx = stepCtx;		
	} 

	private Serializable initializePersistentUserDataObject(byte[] bytesFromDB) {

		Serializable retVal = null;

			if (bytesFromDB != null) {
				try {
					ByteArrayInputStream bais = new ByteArrayInputStream(bytesFromDB);
					DeserializationObjectInputStream ois = null;
					try {
						ois = new DeserializationObjectInputStream(bais, Thread.currentThread().getContextClassLoader());
						retVal = (Serializable) ois.readObject();
					} finally {
						ois.close();
					}
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException("Problem while trying to deserialize persistent user data");
				} catch (IOException e) {
					throw new IllegalStateException("Problem while trying to deserialize persistent user data");
				}
			}
		return retVal;
	}
}
