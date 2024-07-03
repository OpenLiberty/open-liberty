/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utilities for failover tests
 */
public class FailoverTestUtils {
	
	public int NORMAL_NUMBER_OF_STARTUP_QUERIES = 6; // Creating logs 

    public enum FailoverTestType {
        STARTUP, RUNTIME, DUPLICATE_RESTART, DUPLICATE_RUNTIME, HALT, CONNECT, LEASE
    };

	public static void setupTestParameters(Connection con, FailoverTestType testType, int thesqlcode, int operationToFail, int numberOfFailures) throws Exception {
        setupHATable(con, testType, thesqlcode, operationToFail, numberOfFailures);
	}

    public static void setupHATable(Connection con, FailoverTestType testType,
                              int thesqlcode, int operationToFail, int numberOfFailures) throws Exception {
        System.out.println("FailoverTestUtils: setupHATable");

        // Set up statement to use for table delete/recreate
        try (Statement stmt = con.createStatement()) {
        	int ret;
        	
        	try {
        		ret = stmt.executeUpdate("drop table hatable");
        		System.out.println("FailoverTestUtils: drop hatable returned " + ret);
        	} catch (SQLException x) {
        		// didn't exist
        	}
        	ret = stmt.executeUpdate(
        			"create table hatable (testtype int not null primary key, failingoperation int, numberoffailures int, simsqlcode int)");
        	System.out.println("FailoverTestUtils: create hatable returned "+ret);
        	// was col2 varchar(20)
        	stmt.executeUpdate("insert into hatable values (" + testType.ordinal() + ", " + operationToFail + ", " + numberOfFailures + ", "
        			+ thesqlcode + ")"); // was -4498
        	System.out.println("FailoverTestUtils: insert row into hatable - type" + testType.ordinal()
        	+ ", operationtofail: " + operationToFail + ", sqlcode: " + thesqlcode+ " returned " + ret);

        	// UserTransaction Commit
        	con.setAutoCommit(false);

        	System.out.println("FailoverTestUtils: commit changes to database");
        	con.commit();
        }
    }
    
    public static void setupForRecoverableFailover(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.RUNTIME, -4498, 12, 1);
    }

	public static void setupForHalt(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.HALT, 0, 12, 1); // set the ioperation to the duplicate test value + 2
	}

	public static void setupForDuplicationRestart(Connection con) throws Exception {
		setupTestParameters(con, FailoverTestType.DUPLICATE_RESTART, 0, 10, 1);
	}

	public static void setupForDuplicationRuntime(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.DUPLICATE_RUNTIME, 0, 10, 1);
	}

	public static void setupForEarlyNonRecoverableStartupFailover(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.STARTUP, -3, 0, 1);
	}

	public static void setupForNonRecoverableFailover(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.RUNTIME, -3, 12, 1);
	}

	public static void setupForNonRecoverableStartupFailover(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.STARTUP, -3, 6, 1);
	}

	public static void setupForRecoverableFailureMultipleRetries(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.RUNTIME, -4498, 12, 5); // Can fail up to 5 times
	}

	public static void setupForStartupFailover(Connection con) throws Exception {
        setupTestParameters(con, FailoverTestType.STARTUP, -4498, 6, 1);
	}

	public static void setupForConnectFailover(Connection con) throws Exception {
        FailoverTestUtils.setupTestParameters(con, FailoverTestType.CONNECT, 0, 0, 1);
	}

	public static void setupForMultiConnectFailover(Connection con) throws Exception {
        FailoverTestUtils.setupTestParameters(con, FailoverTestType.CONNECT, 0, 0, 3);
	}
}