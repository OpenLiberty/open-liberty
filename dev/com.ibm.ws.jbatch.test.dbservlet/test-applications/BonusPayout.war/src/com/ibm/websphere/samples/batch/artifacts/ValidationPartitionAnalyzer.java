package com.ibm.websphere.samples.batch.artifacts;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.ValidationPartitionCollectorDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

import static com.ibm.websphere.samples.batch.artifacts.ValidationPartitionReducer.UserData;

@Named("ValidationPartitionAnalyzer")
public class ValidationPartitionAnalyzer extends AbstractPartitionAnalyzer
		implements BonusPayoutConstants {

	private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);
	
	private int chunkSize;

	// Partitions may not all be the same size, and also the chunk size may not be a divisor of
	// an individual partition's size.
	private int maxNumberOfChunks;
	
	private boolean isInitialized = false;
	
	@Inject @BatchProperty(name="validateCollectorData")
	private String validateCollectorDataStr;
	private boolean validateCollectorData;
	
	@Inject
	private JobContext jobCtx;

	@Inject
	private StepContext stepCtx;
	
	private void initialize() {
		if (!isInitialized) {
			validateCollectorData = Boolean.parseBoolean(validateCollectorDataStr);
			Properties jobProperties = jobCtx.getProperties();
			chunkSize = Integer.parseInt(jobProperties.getProperty("chunkSize"));
			
			int numValidationPartitions = Integer.parseInt(jobProperties.getProperty("numValidationPartitions"));
			int numRecords = Integer.parseInt(jobProperties.getProperty("numRecords"));
			
			int numRecordsPerPartition =
					(int) Math.ceil((double)numRecords/numValidationPartitions);

			int maxNumberOfNonEmptyChunksAmongPartitions = 
					(int) Math.ceil((double)numRecordsPerPartition/chunkSize);
			
	        // The fact that there will be a zero-item chunk when the chunk size is a divisor of 
			// the partition dataset size means we have to add '1' here.
			this.maxNumberOfChunks = maxNumberOfNonEmptyChunksAmongPartitions + 1;
			
			isInitialized = true;
		}
	}
	
	/*
	 * On each COLLECTOR_DATA
	 */
	@Override
	public void analyzeCollectorData(Serializable data) throws Exception {

		initialize();

		ValidationPartitionCollectorDataObject dataObj = (ValidationPartitionCollectorDataObject) data;

		int recordsProcessedInChunk = dataObj.getCurrentChunkExitStatus()
				- dataObj.getPreviousExitStatus();

		// Some variants don't use a collector.
		if (validateCollectorData) {
			validateCollectorDataSize(recordsProcessedInChunk);
		} else {
			logger.finer("In analyzer, don't validate collector data in this variant.");
		}
		
		validateChunkNumber(dataObj.getChunkNum());

		UserData ud = (UserData)stepCtx.getPersistentUserData();
		ud.addToCollectorCount(recordsProcessedInChunk);
		
	}

	private void validateChunkNumber(int chunkNum) {
		if (chunkNum >=0 && chunkNum <= maxNumberOfChunks) {
			logger.finest("For maxNumberOfChunks: " + maxNumberOfChunks + ", partition collector sent back acceptable chunk number: " + chunkNum);
		} else {
			BonusPayoutUtils.throwIllegalStateExc("For maxNumberOfChunks: " + maxNumberOfChunks + ", partition collector sent back unacceptable chunk number: " + chunkNum);
		}
	}

	private void validateCollectorDataSize(int recordsProcessedInChunk) {
		if (recordsProcessedInChunk >=0 && recordsProcessedInChunk <= chunkSize) {
			logger.finest("For chunkSize: " + chunkSize + ", partition collector sent back acceptable # of records: " + recordsProcessedInChunk);
		} else {
			BonusPayoutUtils.throwIllegalStateExc("For chunkSize: " + chunkSize + ", partition collector sent back unacceptable # of records: " + recordsProcessedInChunk);
		}
	}

	/*
	 * Coalesce the counts coming in from each partition to the "top-level"
	 * count held by the exitStatus on the top-level thread, on which the
	 * analyzer always executes.
	 */
	@Override
	public void analyzeStatus(BatchStatus batchStatus,
			String nextPartitionExitStatus) throws Exception {
		
		// Possible analyzeCollectorData() was never called
		initialize();
		
		if (batchStatus.equals(BatchStatus.COMPLETED)) {
			UserData ud = (UserData)stepCtx.getPersistentUserData();
			ud.incrementCompletedPartitionCount();
			updateRecordCounts(nextPartitionExitStatus);
		} else {
			logger.fine("Noticing a non-completed partition exiting with batchStatus of: "	+ batchStatus);
		}		
	
	}

	// No need to synchronize, the spec says there is only one thread processing all analyzer calls.
	private void updateRecordCounts(String nextPartitionExitStatus) {

		int numRecordsCumulativelyCompletedThisPartition = Integer.parseInt(nextPartitionExitStatus);
		
		updateExitStatus(numRecordsCumulativelyCompletedThisPartition);
		updatePersistentUserData(numRecordsCumulativelyCompletedThisPartition);
	}
	
	private void updatePersistentUserData(int numRecordsPartitionCompletedThisExecution) {
		UserData ud = (UserData)stepCtx.getPersistentUserData();
		ud.addToExitStatusCount(numRecordsPartitionCompletedThisExecution);
	}

	private void updateExitStatus(int numRecordsPartitionCompletedThisExecution) {
		String overallStepExitStatus = stepCtx.getExitStatus();

		int currentCumulativeCount = overallStepExitStatus == null ? 0
				: Integer.parseInt(overallStepExitStatus);
						
		int newCumulativeCount = currentCumulativeCount	+ numRecordsPartitionCompletedThisExecution;

        logger.fine("Current cumulative record count = " + currentCumulativeCount + ", adding: " + numRecordsPartitionCompletedThisExecution + " more records to count for a total of: "
                + newCumulativeCount);
                
		stepCtx.setExitStatus(String.valueOf(newCumulativeCount));	
	}
	
	
}
