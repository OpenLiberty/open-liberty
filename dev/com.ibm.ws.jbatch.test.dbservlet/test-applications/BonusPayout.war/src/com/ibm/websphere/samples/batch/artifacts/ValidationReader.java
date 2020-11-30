package com.ibm.websphere.samples.batch.artifacts;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;
import com.ibm.websphere.samples.batch.beans.AccountType;
import com.ibm.websphere.samples.batch.beans.CheckingAccountType;
import com.ibm.websphere.samples.batch.beans.PriorityAccount;
import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;
import com.ibm.websphere.samples.batch.util.BonusPayoutUtils;
import com.ibm.websphere.samples.batch.util.TransientDataHolder;

/*
 * The idea here is that we'll reverse the calculation adding the 
 * bonus to each account balance, and confirm that the result matches
 * what was generated in step 1.
 * 
 * We will use an aggregate readItem to do this, reading both from the 
 * text file generated in the first step and the DB table inserted into
 * in the second step.
 * 
 * Transient user data (for step ctx) is used like:
 * 
 * Reader.open()
 *    => set record number
 * ChunkListener.beforeChunk()
 *    <= get record number (for query), issue query
 *    => set ResultSet
 * 
 * (in loop), Reader.readItem()
 *    <= get ResultSet
 *    
 * Reader.checkpointInfo()
 *    => set record number (for next beforeChunk()) 
 * 
 */
@Named("ValidationReader")
public class ValidationReader implements ItemReader, BonusPayoutConstants {

    protected final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    @BatchProperty
    protected String dsJNDI;

    @Inject
    @BatchProperty(name = "startAtIndex")
    protected String startAtIndexStr;
    protected int startAtIndex;

    // For a partition we want to stop with one partition's worth, not the full set of records.
    @Inject
    @BatchProperty(name = "maxRecordsToValidate")
    protected String maxRecordsToValidateStr;
    protected int maxRecordsToValidate;

    @Inject
    protected JobContext jobCtx;

    @Inject
    protected StepContext stepCtx;

    // Keep a separate count for the purpose of double-checking the total number of 
    // entries validated.
    protected Integer recordsAlreadyRead = null;
    
    private Random randomSeedGenerator = null;

    protected BufferedReader reader = null;
    
    private int numRecords ;
    
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

    @Override
    public void open(Serializable checkpoint) throws Exception {

        if (checkpoint != null) {
            recordsAlreadyRead = (Integer) checkpoint;
        } else {
            recordsAlreadyRead = 0;
        }

        startAtIndex = getStartAtIndex();

        maxRecordsToValidate = getMaxRecordsToValidate();

        setupDBReader();

        try{
        	setupFileReader();
        } catch(FileNotFoundException e){
        	logger.info("FileNotFoundException caught : " + e.getMessage());
        	setupRandomSeedGenerator();
        }
    }

    /**
     * This value incorporates both the initial position plus
     * the number processed in both this and previous executions.
     */
    protected int getNextRecordIndex() {
        return startAtIndex + recordsAlreadyRead;
    }
    
    private int getNextBalanceFromRandomSeedGenerator(){
    	return randomSeedGenerator.nextInt(MAX_ACCOUNT_VALUE);
    }
    
    /*
     * If the csv file is not found(because of the partition not running on the same machine),
     * use randomSeedGenerator to generate the same random account balances that was used to create the csv file
     */
    private void setupRandomSeedGenerator(){
    	
    		numRecords = Integer.parseInt(jobCtx.getProperties().getProperty(NUM_RECORDS));
    	
    		randomSeedGenerator = new Random(jobCtx.getInstanceId());
    		
    		// Advance cursor (not worrying much about performance) 
            for (int i = 0; i < getNextRecordIndex(); i++) {
            	getNextBalanceFromRandomSeedGenerator();
            }
    }

    private void setupFileReader() throws Exception {
        BonusPayoutUtils helper = new BonusPayoutUtils(getJobContext());

        reader = helper.openCurrentInstanceStreamReader();

        // Advance cursor (not worrying much about performance) 
        for (int i = 0; i < getNextRecordIndex(); i++) {
            reader.readLine();
        }
    }

    protected void setupDBReader() throws Exception {
        TransientDataHolder data = new TransientDataHolder();
        data.setRecordNumber(getNextRecordIndex());
        getStepContext().setTransientUserData(data);
    }

    @Override
    public Object readItem() throws Exception {

        if (recordsAlreadyRead >= maxRecordsToValidate) {
            logger.fine("Reached the maximum number of records to validate without error.  Exiting chunk loop.");
            return null;
        }

        AccountDataObject fileDO;
        
        //If randomSeedGenerator is null, then there was no Exception in reading the file 
        if(randomSeedGenerator == null){
        	fileDO = readFromFile();
        }
        else {
        	fileDO = readFromRandomSeedGenerator(getNextRecordIndex());
        }
        AccountDataObject tableDO = readFromDB();

        if (fileDO == null && tableDO != null) {
            String errorMsg = "App failure.  Read record # " + (recordsAlreadyRead + 1) + "from DB table, but only read " + recordsAlreadyRead + " records from file.";
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        } else if (fileDO != null && tableDO == null) {
            String errorMsg = "App failure.  Read record # " + (recordsAlreadyRead + 1) + "from file, but only read " + recordsAlreadyRead + " records from DB table.";
            BonusPayoutUtils.throwIllegalStateExc(errorMsg);
        } else if (fileDO == null && tableDO == null) {
            // Some partitions might not have the full maximum number of data (for odd lots), so exit if both streams are exhausted at the same record count
            logger.fine("Input exhausted without error.  Exiting chunk loop.");
            return null;
        }

        recordsAlreadyRead++;

        // Convenient way for passing both to processor as a single item.
        tableDO.setCompareToDataObject(fileDO);

        return tableDO;
    }
    
    /*
     * read data from randomSeedGenerator because probably there is no csv file to read from
     * the randomSeedGenerator should generate the same random balance that it did while generating the csv file
     */
    private AccountDataObject readFromRandomSeedGenerator(int accountNumber) throws IOException {
        int nextBalance = getNextBalanceFromRandomSeedGenerator();
        if (accountNumber == numRecords) {
            logger.fine("End of stream reached in " + this.getClass());
            return null;
        } else {
            return new AccountDataObject(accountNumber, nextBalance, acctType.getAccountCode());
        }
    }

    private AccountDataObject readFromFile() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            logger.fine("End of stream reached in " + this.getClass());
            return null;
        } else {
            AccountDataObject acct = BonusPayoutUtils.parseLine(line);
            return acct;
        }
    }

    protected AccountDataObject readFromDB() throws SQLException {
        ResultSet rs = getCurrentResultSet();

        if (rs.next()) {
            int acctNum = rs.getInt(1);
            int balance = rs.getInt(2);
            String acctCode = rs.getString(3);
            return new AccountDataObject(acctNum, balance, acctCode);
        } else {
            logger.fine("End of JDBC input reached in " + this.getClass());
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {

        // Update for the next chunk's query
        TransientDataHolder data = (TransientDataHolder) getStepContext().getTransientUserData();
        if (data != null) {
            data.setRecordNumber(getNextRecordIndex());
        }

        return recordsAlreadyRead;
    }

    protected ResultSet getCurrentResultSet() {
        TransientDataHolder data = (TransientDataHolder) getStepContext().getTransientUserData();
        return data.getResultSet();
    }

    /*
     * Solves the fact that injection needs to be done on the superclass and subclass independently
     */
    protected int getStartAtIndex() {
        return Integer.parseInt(startAtIndexStr);
    }

    protected int getMaxRecordsToValidate() {
        return Integer.parseInt(maxRecordsToValidateStr);
    }

    protected JobContext getJobContext() {
        return jobCtx;
    }

    protected StepContext getStepContext() {
        return stepCtx;
    }
}
