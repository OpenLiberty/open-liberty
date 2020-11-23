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

/**
 *
 */
public interface BonusPayoutConstants{

    public static final String BONUSPAYOUT_LOGGER = "BonusPayout";
	public static final String FILE_ENCODING_PROP = "fileEncoding";
    public static final String NUM_RECORDS = "numRecords";
	public static final String USE_GLOBAL_JNDI = "useGlobalJNDI";
    public static final String GENERATE_FILE_NAME_ROOT_PROPNAME = "generateFileNameRoot";
    public static final String DFLT_GEN_FILE_PREFIX = "bonuspayout.outfile";

    // Could make these configurable
    public static final String ACCOUNT_NUM_COLUMN = "ACCTNUM";
    public static final String ACCOUNT_CODE_COLUMN = "ACCTCODE";
    public static final String BALANCE_COLUMN = "BALANCE";
    public static final String INSTANCE_ID_COLUMN = "INSTANCEID";
    
    public static final int MAX_ACCOUNT_VALUE = 1000; 
}
