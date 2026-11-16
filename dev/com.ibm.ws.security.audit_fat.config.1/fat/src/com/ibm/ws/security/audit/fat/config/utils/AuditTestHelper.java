package com.ibm.ws.security.audit.fat.config.utils;

import java.util.ArrayList;
import java.util.List;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
import com.ibm.ws.security.audit.fat.common.tooling.IAuditStream;

/**
 * Provides assertions and audit data for test cases to use when verifying the existence of various searchPatterns in the
 * audit log.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditTestHelper {
	private AuditData auditResults;

  /**
   *  This class provides a chunk of audit data (results from an audit stream) that will be used for test assertions.
   */
	public static class AuditData {
				
		/**
		 * Thrown when check is made and audit data read is empty
		 */
	    public static class EmptyAuditDataException extends RuntimeException {

			public EmptyAuditDataException() {
				super("No audit data was read from the audit log.");
	    	}
	    }
		private IAuditStream stream;
		private List<String> results = null; 

		
		/**
		 * Given an audit stream, remember the stream for later use.
		 * @param stream of audit data
		 */
		public AuditData(IAuditStream stream) {
			this.stream = stream;
		}
		
		/**
		 * If audit data has not been read previously, read audit data of one or more JSON records (compact or expanded format)
		 * from the audit log until the end of the log is reached.
		 * @return the chunk of audit records 
		 * @throws Exception
		 */
		public List<String> read() throws Exception {
			
			if (results == null)
			{
				// TODO: Remove temp 3 sec sleep once a test sentry is added for 
				//determining end of audit data.
				sleep();
				
				results = new ArrayList<String>();
				String line;
				StringBuilder buildLine = new StringBuilder();
				int depth = 0;

				while ((line = stream.readNext()) != null)			
				{
					int countLeft = line.length() - line.replaceAll("\\{", "").length();
					int countRight = line.length() - line.replaceAll("\\}", "").length();
					depth += (countLeft - countRight);
	
					boolean hasJsonStart = (countLeft > 0);
					boolean hasJsonEnd = (countRight > 0);
					boolean willBalance = (depth == 0);
					boolean willHaveDepth = (depth > 0);
	
					if (hasJsonStart || hasJsonEnd || willHaveDepth)
						buildLine.append(line);
	
					if (hasJsonEnd && willBalance)
					{
						results.add(buildLine.toString());
						buildLine.setLength(0);
					}
				}
				if (buildLine.length() > 0)
					results.add(buildLine.toString());
				
				if (results.isEmpty())
					throw new EmptyAuditDataException();
				
			}
			return results;
		}
		/**
		 *  Sleep for 3 seconds 
		 *  @throws Exception
		 */
				private void sleep() {
					try {
						Thread.sleep(3 * 1000);
					} catch (InterruptedException e) {
					}
				}
	}
    
	private Class<?> logClass;

	
	public AuditTestHelper (Class<?> logClass, AuditFileStream recent) {
		this.logClass = logClass;
		auditResults = new AuditData(recent);
	}

    /**
     * Returns the first audit record in an audit log.  auditResults will contain a list of Strings, where each entry is an audit record.   
     * Here we return the first entry in the auditResults list, corresponding to the first audit record in the log.
     * 
     * @throws Exception
     */
	
	public String firstRecord() throws Exception{
		try {
			auditResults.read();
		} catch (Exception e) {
			throw e;
		}
		return auditResults.results.get(0);
	}
	
    /**
     * Returns the last audit record in an audit log.  auditResults will contain a list of Strings, where each entry is an audit record.   
     * Here we return the last entry in the auditResults list, corresponding to the last audit record in the log.
     * 
     * @throws Exception
     */

	public String lastRecord() throws Exception {
		try {
			auditResults.read();
		} catch (Exception e) {
			throw e;
		}
		int size = auditResults.results.size();
		return auditResults.results.get(size-1);
	}
	
    /**
     * Returns the audit eventSequenceNumber for the first audit record in an audit log.  
     * 
     * @throws Exception
     */

	public String firstSequenceNumber() throws Exception {
		String eventRecord = firstRecord();
		int indexSeq = eventRecord.indexOf("\"eventSequenceNumber\":");
		int indexTime = eventRecord.indexOf("\"eventTime\"");
		return (String) eventRecord.subSequence((indexSeq + 23), (indexTime - 4));		
	}

    /**
     * Returns the audit eventSequenceNumber for the last audit record in an audit log.  
     * 
     * @throws Exception
     */

	public String lastSequenceNumber() throws Exception {
		String eventRecord = lastRecord();
		int indexSeq = eventRecord.indexOf("\"eventSequenceNumber\":");
		int indexTime = eventRecord.indexOf("\"eventTime\"");
		return (String) eventRecord.subSequence((indexSeq + 23), (indexTime - 4));		
	}
	
}
