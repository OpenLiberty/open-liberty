package com.ibm.websphere.samples.batch.artifacts;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionReducer;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

public class ValidationPartitionReducer implements PartitionReducer, BonusPayoutConstants {

	private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);	
    
	@Inject @BatchProperty(name="validateCollectorData")
	private String validateCollectorDataStr;
	private boolean validateCollectorData;
	
	@Inject
	private JobContext jobCtx;

	@Inject
	private StepContext stepCtx;

	@Override
	public void beginPartitionedStep() throws Exception {
		validateCollectorData = Boolean.parseBoolean(validateCollectorDataStr);
		initializePersistentUserData();
	}

	@Override
	public void beforePartitionedStepCompletion() throws Exception {

		// Some variants don't use a collector.
		if (validateCollectorData) {
			validateCollectorCount();
		} else {
			logger.finer("In reducer, don't validate collector data in this variant.");
		}
		validateExitStatusCount();
		validateCompletedPartitionCount();
	}

	private void validateCompletedPartitionCount() {
		int numPartitions = Integer.parseInt(jobCtx.getProperties().getProperty("numValidationPartitions"));

		UserData ud = (UserData)stepCtx.getPersistentUserData();
		if (ud.cumulativeCompletedPartitionCount.equals(numPartitions)) {
			logger.finer("Completed expected number of partitions = " + numPartitions);
		} else {
			BonusPayoutUtils.throwIllegalStateExc("Expected number of partitions = " + numPartitions + ", but completed number of partitions only = " + ud.cumulativeCompletedPartitionCount);
		}
	}

	// Should be equal on a restart too since persistent UD was initialized with previous count,
	// and we're just adding to it on the restart execution.
	private void validateExitStatusCount() {

		int numRecords = Integer.parseInt(jobCtx.getProperties().getProperty("numRecords"));

		UserData ud = (UserData)stepCtx.getPersistentUserData();
		if (ud.cumulativeRecordCountPerExitStatus.equals(numRecords)) {
			logger.finer("Per partition ExitStatus, completed expected number of records = " + numRecords);
		} else {
			BonusPayoutUtils.throwIllegalStateExc("Per partition ExitStatus, expected number of records = " + numRecords + 
					", but completed number of records only = " + ud.cumulativeRecordCountPerExitStatus);
		}
	}

	private void validateCollectorCount() {

		int numRecords = Integer.parseInt(jobCtx.getProperties().getProperty("numRecords"));

		UserData ud = (UserData)stepCtx.getPersistentUserData();
		if (ud.cumulativeRecordCountPerCollector.equals(numRecords)) {
			logger.finer("Per partition collector, completed expected number of records = " + numRecords);
		} else {
			BonusPayoutUtils.throwIllegalStateExc("Per partition collector, expected number of records = " + numRecords + 
					", but completed number of records only = " + ud.cumulativeRecordCountPerCollector);
		}
	}

	@Override
	public void rollbackPartitionedStep() throws Exception { }

	@Override
	public void afterPartitionedStepCompletion(PartitionStatus status) throws Exception { }


	private void initializePersistentUserData() {
		
		// If this is a restart we will resume with cumulative data. 
		if (stepCtx.getPersistentUserData() == null){ 
			stepCtx.setPersistentUserData(new UserData());
		}
	}
	
	public static class UserData implements Serializable {

		private static final long serialVersionUID = 1L;

		// This field currently not used, a stub to remind me to test userdata persistence in the mapper,
		// e.g. when exactly are you guaranteed that the UD will be written to DB?  After the maper?
		private Integer partitionPlanSize = 0;

		private Integer cumulativeCompletedPartitionCount = 0;
		private Integer cumulativeRecordCountPerExitStatus = 0;
		private Integer cumulativeRecordCountPerCollector = 0;

		public Integer getCumulativeRecordCountPerExitStatus() {
			return cumulativeRecordCountPerExitStatus;
		}
		public void setCumulativeRecordCountPerExitStatus(Integer processedRecordCount) {
			this.cumulativeRecordCountPerExitStatus = processedRecordCount;
		}
		public Integer getCumulativeCompletedPartitionCount() {
			return cumulativeCompletedPartitionCount;
		}

		public Integer getPartitionPlanSize() {
			return partitionPlanSize;
		}
		public void setPartitionPlanSize(Integer partitionPlanSize) {
			this.partitionPlanSize = partitionPlanSize;
		}
		public Integer getCumulativeRecordCountPerCollector() {
			return cumulativeRecordCountPerCollector;
		}

		public void addToCollectorCount(int recordsProcessedInChunk) {
			logger.fine("In addToCollectorCount, adding " + recordsProcessedInChunk + " records to previous user data = " + this);
			cumulativeRecordCountPerCollector += recordsProcessedInChunk;
		}
		public void addToExitStatusCount(int numRecordsPartitionCompletedThisExecution) {
			logger.fine("In addToExitStatusCount, adding " + numRecordsPartitionCompletedThisExecution + " records to previous user data = " + this);
			cumulativeRecordCountPerExitStatus += numRecordsPartitionCompletedThisExecution;
		}
		public void incrementCompletedPartitionCount() {
			logger.fine("In incrementCompletedPartitionCount, completing partition, previous user data = " + this);
			this.cumulativeCompletedPartitionCount++;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("\npartitionPlanSize = " + partitionPlanSize);
			sb.append("\ncumulativeCompletedPartitionCount = " + cumulativeCompletedPartitionCount);
			sb.append("\ncumulativeRecordCountPerExitStatus = " + cumulativeRecordCountPerExitStatus);
			sb.append("\ncumulativeRecordCountPerCollector = " + cumulativeRecordCountPerCollector);
			return sb.toString();
		}
	}
}

