package com.ibm.ws.security.audit.fat.config.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;

/**
 * Provides file based audit log for determining characteristics of the log.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TestableAuditLog {
	protected File logFile;

	/**
	 * Given an audit file log name, create TestableAuditLog to manage the
	 * processing of a single audit log.
	 * 
	 * @param logName
	 */
	public TestableAuditLog(String logName) {
		logFile = new File(logName);
	}

	public String getLogFileName() {
		return logFile.toString();
	}

	/**
	 * Assert that the audit log size is less than or equal to the expected size limit.
	 * 
	 * @param size - expected size limit
	 */
	public void verifyLogSizeLimit(Long size) throws Exception {
		assertTrue("Audit log size was greater than expected size of " + size.toString(),
				logFile.length() <= size );
	}

}