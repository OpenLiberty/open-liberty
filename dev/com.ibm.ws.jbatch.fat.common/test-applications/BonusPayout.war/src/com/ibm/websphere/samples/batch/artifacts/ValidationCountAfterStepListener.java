package com.ibm.websphere.samples.batch.artifacts;

import java.util.logging.Logger;

import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

@Named("ValidationCountAfterStepListener")
public class ValidationCountAfterStepListener extends AbstractStepListener implements BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    private JobContext jobCtx;

    @Inject
    private StepContext stepCtx;

    @Override
    public void afterStep() throws Exception {

    	BatchStatus bs = stepCtx.getBatchStatus();
    			
    	// Prevents us FAILING a STOPPING job.
        if (bs.equals(BatchStatus.FAILED) || bs.equals(BatchStatus.STOPPING)) {
            logger.info("Don't bother parsing exit status since step status is already: " + bs);
            return;
        }

        int numRecordsProcessed = (Integer)stepCtx.getPersistentUserData();
        
        int originalNumRecords = Integer.parseInt(jobCtx.getProperties().getProperty(NUM_RECORDS));

        if (numRecordsProcessed != originalNumRecords) {
            String errorMsg = "App failure.  Didn't read the same number of records as we originally wrote.  Originally wrote " + originalNumRecords + " # of records but instead read "
                              + numRecordsProcessed + " # of records during validation step.";
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        }
    }

}
