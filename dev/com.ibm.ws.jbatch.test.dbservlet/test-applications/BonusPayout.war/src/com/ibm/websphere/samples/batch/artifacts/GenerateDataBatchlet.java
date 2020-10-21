/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.websphere.samples.batch.artifacts;

import java.io.BufferedWriter;
import java.util.Random;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

//import batch.fat.junit.BonusPayoutViaJBatchUtilityTest;



import com.ibm.websphere.samples.batch.beans.AccountType;
import com.ibm.websphere.samples.batch.beans.CheckingAccountType;
import com.ibm.websphere.samples.batch.beans.PriorityAccount;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;

/**
 * Generate some random data, then write in CSV format into a text file.
 */
@Named("GenerateDataBatchlet")
public class GenerateDataBatchlet implements Batchlet, BonusPayoutConstants{

    /**
	 * 
	 */

	private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    /**
     * How many records to write.
     */
    @Inject
    @BatchProperty(name = "numRecords")
    private String numRecordsStr;

    /**
     * File to write generated data to.
     */
    @Inject
    @BatchProperty
    private String generateFileNameRoot;

    @Inject
    private JobContext jobCtx;

    /*
     * For CDI version of sample this will be injectable.
     */
    private AccountType acctType = new CheckingAccountType();

    /*
     * Included for CDI version of sample.
     */
    @Inject
    public void setAccountType(@PriorityAccount AccountType acctType) {
        this.acctType = acctType;
        logger.fine("USING CDI VERSION HERE");
    }
    
    private volatile boolean stopped = false;

    private BufferedWriter writer = null;
    
    private Random randomSeedGenerator = null; 

    @Override
    public String process() throws Exception {
    	
    	if(randomSeedGenerator == null){
    		randomSeedGenerator = new Random(jobCtx.getInstanceId());
    	}
        writer = new BonusPayoutUtils(jobCtx).openCurrentInstanceStreamWriter();

        int numRecords = Integer.parseInt(numRecordsStr);
        for (int i = 0; i < numRecords; i++) {

            StringBuilder line = new StringBuilder();

            // 1. Write record number
            line.append(i).append(',');

            // 2. Write random value
            line.append(randomSeedGenerator.nextInt(MAX_ACCOUNT_VALUE)).append(',');

            // 3. Write account code
            line.append(acctType.getAccountCode());

            writer.write(line.toString());
            writer.newLine();

            if (stopped) {
                logger.info("Aborting GenerateDataBatchlet since a stop was received");
                writer.close();
                return null;
            }
        }
        writer.close();

        return acctType.getAccountCode();
    }

    @Override
    public void stop() {
        stopped = true;
    }

}
