/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
/**
 * using a package-info file here is kinda silly, but using a packageinfo file doesn't seem to work.
 * 
 * @version 1.0
 */
@org.osgi.annotation.versioning.Version("1.0")
@TraceOptions(traceGroup = "Transaction", messageBundle = "com.ibm.ws.transaction.services.TransactionMessages")
package com.ibm.tx.jta.ut.util;

import com.ibm.websphere.ras.annotation.TraceOptions;
