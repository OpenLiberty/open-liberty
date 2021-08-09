package com.ibm.websphere.samples.batch.artifacts;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.ValidationPartitionCollectorDataObject;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;

@Named("ValidationPartitionCollector")
public class ValidationPartitionCollector implements PartitionCollector, BonusPayoutConstants {
		
	private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);
	
	private int previousExitStatus = 0;
	
	private int chunkNum = 0;

	@Inject
    private StepContext stepCtx;

	@Override
	public Serializable collectPartitionData() throws Exception {
		
		ValidationPartitionCollectorDataObject dataObject = new ValidationPartitionCollectorDataObject();
		dataObject.setPreviousExitStatus(previousExitStatus);
		
		int currentChunkExitStatus = Integer.parseInt(stepCtx.getExitStatus());
		dataObject.setCurrentChunkExitStatus(currentChunkExitStatus);
		dataObject.setChunkNum(chunkNum);
		
		previousExitStatus = currentChunkExitStatus; 
		chunkNum ++;
		
		logger.finer("[DEBUG] stepCtx.getExitStatus() " + stepCtx.getExitStatus());
		logger.finer("[DEBUG] previousExitStatus " + previousExitStatus);
		logger.finer("[DEBUG] currentChunkExitStatus " + currentChunkExitStatus);
		logger.finer("[DEBUG] dataObject " + dataObject);
		
		return dataObject;
	}
		
}
