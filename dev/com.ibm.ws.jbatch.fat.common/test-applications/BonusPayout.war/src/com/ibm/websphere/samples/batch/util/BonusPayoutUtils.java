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
package com.ibm.websphere.samples.batch.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.batch.runtime.context.JobContext;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.websphere.samples.batch.beans.AccountDataObject;

@Named("BonusPayoutUtils")
public class BonusPayoutUtils implements BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    private final JobContext jobCtx;

    public BonusPayoutUtils(JobContext jobCtx) {
        this.jobCtx = jobCtx;
    }

    public BufferedWriter openCurrentInstanceStreamWriter() throws FileNotFoundException, UnsupportedEncodingException {
        BufferedWriter writer = null;

        String fileEncoding = jobCtx.getProperties().getProperty(FILE_ENCODING_PROP);

        // Append job instance to avoid collision across instances
        String fileName = getGenerateFileNameRoot() + "." + jobCtx.getInstanceId() + ".csv";

        FileOutputStream fos = new FileOutputStream(fileName);

        if (fileEncoding == null || fileEncoding.isEmpty()) {
            logger.fine("Opening file: " + fileName + "with default encoding");
            writer = new BufferedWriter(new OutputStreamWriter(fos));
        } else {
            logger.fine("Opening file: " + fileName + "with encoding: " + fileEncoding);
            writer = new BufferedWriter(new OutputStreamWriter(fos, fileEncoding));
        }
        return writer;
    }

    public BufferedReader openCurrentInstanceStreamReader() throws FileNotFoundException, UnsupportedEncodingException {

        String fileName = getGenerateFileNameRoot() + "." + jobCtx.getInstanceId() + ".csv";
        return openStreamReader(fileName);
    }

    private BufferedReader openStreamReader(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader reader = null;
        String fileEncoding = jobCtx.getProperties().getProperty(FILE_ENCODING_PROP);

        FileInputStream fis = new FileInputStream(fileName);

        if (fileEncoding == null || fileEncoding.isEmpty()) {
            logger.fine("Opening file: " + fileName + "with default encoding");
            reader = new BufferedReader(new InputStreamReader(fis));
        } else {
            logger.fine("Opening file: " + fileName + "with encoding: " + fileEncoding);
            reader = new BufferedReader(new InputStreamReader(fis, fileEncoding));
        }
        return reader;
    }

    private String getGenerateFileNameRoot() {

        String generateFileNameRoot = jobCtx.getProperties().getProperty(GENERATE_FILE_NAME_ROOT_PROPNAME);

        if (generateFileNameRoot == null || generateFileNameRoot.equals("")) {
            String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
            return tmpDir + java.io.File.separator + DFLT_GEN_FILE_PREFIX;
        } else {
            return generateFileNameRoot;
        }
    }

    public static AccountDataObject parseLine(String line) {

        String[] tokens = line.split(",");
        if (tokens.length != 3) {
            String errorMsg = "Expecting three fields separated by two commas but instead found line: " + line;
            throwIllegalStateExc(errorMsg);
        }
        int acctNumber = Integer.parseInt(tokens[0]);
        int balance = Integer.parseInt(tokens[1]);

        return new AccountDataObject(acctNumber, balance, tokens[2]);

    }

    public static DataSource lookupDataSource(String dsJNDI) throws NamingException {
        return DataSource.class.cast(new InitialContext().lookup(dsJNDI));
    }

    public static void validateTableName(String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$")) {
            String errorMsg = "Table name: " + tableName + " does not conform to white list of valid tablenames.";
            throwIllegalStateExc(errorMsg);
        }
    }

    public static String getAccountQuery(String tableName) {

        /*
         * E.g.
         * 
         * SELECT ACCTNUM,BALANCE, ACCTCODE FROM BONUSPAYOUT.ACCOUNT
         * WHERE ACCTNUM >= ? AND ACCTNUM <= ? AND INSTANCEID = ? ORDER BY ACCTNUM ASC
         */
        StringBuilder query = new StringBuilder();
        query.append("SELECT " + ACCOUNT_NUM_COLUMN + "," + BALANCE_COLUMN + "," + ACCOUNT_CODE_COLUMN);
        query.append(" FROM " + tableName);
        query.append(" WHERE " + ACCOUNT_NUM_COLUMN + " >= ?");
        query.append(" AND " + ACCOUNT_NUM_COLUMN + " < ?");
        query.append(" AND " + INSTANCE_ID_COLUMN + " = ?");
        query.append(" ORDER BY " + ACCOUNT_NUM_COLUMN + " ASC");

        return query.toString();
    }
    
    public static void throwIllegalStateExc(String errorMsg) {
    	logger.severe(errorMsg);
    	throw new IllegalStateException(errorMsg);
    }

}
