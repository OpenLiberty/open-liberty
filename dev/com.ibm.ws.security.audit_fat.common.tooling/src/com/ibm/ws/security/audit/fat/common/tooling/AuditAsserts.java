/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.audit.fat.common.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;

/**
 * Provides assertions and audit data for test cases to use when verifying the existence of various searchPatterns in the
 * audit log.
 */
public class AuditAsserts {
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
		private List<String> results;
		private int sleepSecs; 

		
		/**
		 * Given an audit stream, remember the stream for later use.
		 * 	@param stream of audit data
		 *  @param sleepSecs - number of seconds to sleep before reading from the stream to ensure audit
		 *  data has been buffered and written.
		 */
		public AuditData(IAuditStream stream) {
			this.stream = stream;
			this.sleepSecs = 3;  //default sleep of 3 seconds
		}
		
		public AuditData(IAuditStream stream, int sleepSecs) {
			this.stream = stream;
			this.sleepSecs = sleepSecs;
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
				sleep(sleepSecs);
				
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
 *  Sleep for specified number of seconds for the audit data to be buffered before reading from
 *  the audit stream.
 *  @param sleepSecs - number of seconds to sleep 
 *  @throws Exception
 */
		private void sleep(int sleepSecs) {
			try {
				Thread.sleep(sleepSecs * 1000);
			} catch (InterruptedException e) {
			}
		}
	}
    
	private Class<?> logClass;

	
	public AuditAsserts (Class<?> logClass, IAuditStream stream, int sleepSecs) {
		this.logClass = logClass;
		auditResults = new AuditData(stream, sleepSecs);
	}
	public AuditAsserts (Class<?> logClass, IAuditStream stream) {
		this.logClass = logClass;
		auditResults = new AuditData(stream);
	}
	
	public AuditAsserts (Class<?> logClass, String fileName) {
		this.logClass = logClass;
		auditResults = new AuditData(new RecentAuditFileStream(fileName));
	}
	
    protected String getCurrentTestName() {
        return "Test name not set";
    }
    
    /**
     * Provide audit data as a List of JSON records.
     * @return
     * @throws Exception
     */
    public List<JSONObject> auditData () throws Exception{
    	List<JSONObject> results = new ArrayList<JSONObject> ();
   
		for (String auditEntry : auditResults.read()) {
            JSONObject jsonRecord = JSONObject.parse(auditEntry);
            results.add(jsonRecord);
		}
    	return results;
    	
    }
    
    /**
     * Determine whether a sequence of test searchPatterns is found or not within JSON audit data and log appropriate information in the test output log.
     * If this is a positive search, the search expects to find ALL patterns within the sequence of searchPatterns (keys and values) within the audit data.
     * If this is a negative search, the search does not expect to find the sequence of the searchPatterns within the audit data. If all patterns in the
     * sequence are found, then the search fails. If any one pattern in the sequence is NOT found then the negative search passes.
     * 
     * @param searchPatterns - test patterns to be checked for in audit data
     * @param positiveSearch - true indicates positive search, false indicates negative search.
     * @throws Exception
     */
    private void isFoundInDataOrNot (SearchPatternsSequential searchPatterns, boolean positiveSearch) throws  Exception{
    	String thisMethod = "isFoundInDataOrNot";
    	boolean searchedOnce = false;
    	String firstSearchedSequenceNum, lastSearchedSequenceNum = "noneSearched";
    	if (positiveSearch) {
    		Log.info(logClass, thisMethod, "====== Verifying that audit log contains all of the expected test patterns in this sequence of patterns:\n" + searchPatterns.toString());

    	} else {
    		Log.info(logClass, thisMethod, "====== Verifying that the audit log does NOT contain any pattern in this sequence of patterns:\n" + searchPatterns.toString());
            
    	}

		for (String auditEntry : auditResults.read()) {
            if (searchPatterns.allMatched()) 
            	return;
 
            JSONObject jsonRecord = JSONObject.parse(auditEntry);//
            lastSearchedSequenceNum = (jsonRecord.get("eventSequenceNumber") == null ? "none" : jsonRecord.get("eventSequenceNumber").toString());

            
			if (!searchedOnce) {
            	searchedOnce = true;
            	firstSearchedSequenceNum = (jsonRecord.get("eventSequenceNumber") == null ? "none" : jsonRecord.get("eventSequenceNumber").toString());
        		Log.info(logClass, thisMethod, "----- Started searching audit log at eventSequenceNumber: " + firstSearchedSequenceNum);

			}
            // check for specified searchPattern within JSON audit record
            if (searchPatterns.matchedCurrent(jsonRecord)) {
            	if (positiveSearch) {
            		Log.info(logClass, thisMethod, "\n   Audit Expected:  Match should be found for pattern:" + searchPatterns.mostRecentlyMatchedPattern() +
            		"\n   Audit Result:    Pattern matched in audit log at eventSequenceNumber "+ jsonRecord.get("eventSequenceNumber").toString()  );
            	} else {
            		Log.info(logClass, thisMethod, "\n   Audit Expected: No match should be found for pattern, but unexpected match found:" + searchPatterns.mostRecentlyMatchedPattern() +
                    "\n   Audit Result:  Unexpected pattern match found in audit log at eventSequenceNumber "+ jsonRecord.get("eventSequenceNumber").toString()  );
            	}
            }

        }
  		Log.info(logClass, thisMethod, "----- Stopped searching audit log at eventSequenceNumber: " + lastSearchedSequenceNum);
    }
    
    /**
     * Determine whether search patterns in a set of test patterns are found within a JSON audit data and log appropriate information in the test output log.
     * 
     * @param searchPatterns - test patterns to be checked for in audit data
     * @throws Exception
     */
    private boolean areSinglesFoundInData (AuditData auditResults, SearchPatternsSingleton searchPatterns) throws  Exception{
    	String thisMethod = "areSinglesFoundInData";
    	boolean searchedOnce = false;
    	String firstSearchedSequenceNum, lastSearchedSequenceNum = "notSet";
    
        for (String auditEntry : auditResults.read()) {
 
            JSONObject jsonRecord = JSONObject.parse(auditEntry);
             
			if (!searchedOnce) {
            	searchedOnce = true;
            	firstSearchedSequenceNum = jsonRecord.get("eventSequenceNumber").toString();
 
        		Log.info(logClass, thisMethod, "----- Started searching audit log at eventSequenceNumber: " + firstSearchedSequenceNum);

			}
           	lastSearchedSequenceNum = jsonRecord.get("eventSequenceNumber").toString();
            // check for any of the search patterns in the set of searchPatterns within JSON audit record
            if (searchPatterns.matchedAny(jsonRecord)) {
             		Log.info(logClass, thisMethod, "\n   Audit Expected: No match should be found for pattern:" + searchPatterns.mostRecentlyMatchedPattern() +
             				"\n   Audit Result:    Pattern matched in audit log at eventSequenceNumber "+ jsonRecord.get("eventSequenceNumber").toString()  );
             		Log.info(logClass, thisMethod, "----- Stopped searching audit log at eventSequenceNumber: " + lastSearchedSequenceNum);
             	return true;
            }

        }
  		Log.info(logClass, thisMethod, "----- Stopped searching audit log at eventSequenceNumber: " + lastSearchedSequenceNum);
  		return false;
    }
    
	 /**
	  * Asserts that all patterns in a set of search patterns are found in the audit data. The patterns must be found in the 
	  * order in which they are specified, but can have any number of intervening records that do not match.
	  * @param thePatterns - a sequence of search patterns
	  * @throws Exception when not ALL of the search patterns are found in the order specified
	  */
	    public void assertFoundInData(JSONObject... thePatterns) throws Exception {
	    	String thisMethod = "assertFoundInData";
	    	
	    	SearchPatternsSequential searchPatterns = new SearchPatternsSequential(thePatterns);
	    	isFoundInDataOrNot(searchPatterns, true);
	   		searchPatterns.assertAllMatched();
	    }
	  /**
	   * Note: This is not expected to be used because I don't know of a use case for this assertion.
	   * 
	   * Asserts that a sequence of test search patterns is not found within audit results. If ALL of the patterns are
	   * found, then the assertion fails. If any one of the patterns is NOT found, then the assertion passes. 
	   * @param thePatterns - a sequence of search patterns
	   * @throws Exception when ALL of the patterns in the sequence of search patterns are found in audit data.
	   */
       public void assertSequenceNotFoundInData(JSONObject... thePatterns) throws Exception {

    	SearchPatternsSequential searchPatterns = new SearchPatternsSequential(thePatterns);
    	
    	isFoundInDataOrNot(searchPatterns, false);
   		searchPatterns.assertNotAllMatched();
    }
	    
    /**
     * Asserts that none of the test search patterns are found within audit results. If any one of the patterns is
     * found, then the assertion fails.  
     * @param thePatterns - a set of test search patterns
     * @throws Exception when any of the search patterns in the set are found in the audit data
     */
      public void assertNotFoundInData(JSONObject... thePatterns) throws Exception {
      	String thisMethod = "assertNotFoundInData";
 	   
		Log.info(logClass, thisMethod, "===== Verifying the audit log does NOT contain any of the specified patterns.");
      	SearchPatternsSingleton searchPatterns = new SearchPatternsSingleton(thePatterns);
 		Log.info(logClass, thisMethod, "\n   Audit Expected: The audit log should NOT contain any of the following patterns:\n" + searchPatterns.toString());
      	boolean patternFound = areSinglesFoundInData(auditResults, searchPatterns);
      	if (patternFound) {
      		Log.info(logClass, thisMethod, "\n   Audit Result: A pattern was matched when it should not have been found in audit data.");
      	} else {
      		Log.info(logClass, thisMethod, "\n   Audit Result: No match was found for patterns.");

      	}
      	searchPatterns.assertNotFound();

      }
    /**
     * Determines whether all patterns in a set of search patterns are found in the audit results and returns true/false.
     * @param thePatterns - a sequence of search patterns
     * @return true if ALL of the patterns are found in the audit data. 
     * 		   false if any one of the patterns is not found in the audit data.
     */
    public boolean isFoundInData (JSONObject... thePatterns) throws Exception {
    	String thisMethod = "isFoundInData";
    	
    	SearchPatternsSequential searchPatterns = new SearchPatternsSequential(thePatterns);
    	
    	isFoundInDataOrNot(searchPatterns, true);
    	if (!searchPatterns.allMatched()) {
      		Log.info(logClass, thisMethod, "Not all search patterns were matched.");
    	}
   		return searchPatterns.allMatched();
    }
    /**
     * Convert test case map format representation of a JSON key-value pair into a JSON object which will
     * be used to search against audit data.
     * @param testPatterns - sequence of test patterns
     * @return JSONObject representation of the test patterns
     * @throws Exception
     */
    public JSONObject asJson(String... testPatterns) throws Exception {
        Pattern pattern = Pattern.compile("^\\s*(.*?)\\s*=\\s*(.*)\\s*$");
        JSONObject entry = new JSONObject();
        for (String testPattern : testPatterns) {
            Matcher matcher = pattern.matcher(testPattern);
            if (matcher.matches()) {
                String keys = matcher.group(1);
                String value = matcher.group(2);
                String[] subKeys = keys.split("\\.", -1);
                JSONObject current = entry;
                for (int i = 0; i < subKeys.length; i++) {
                    String subkey = subKeys[i];
                    boolean atEnd = (i == subKeys.length - 1);
                    if (!atEnd) {
                        if (!current.containsKey(subkey)) {
                            JSONObject no = new JSONObject();
                            current.put(subkey, no);
                            current = no;
                        } else {
                            current = (JSONObject) current.get(subkey);
                        }
                    } else {
                        current.put(subkey, value);
                    }
                }

            } else {

                throw new Exception("Bad test pattern: " + testPattern);
            }
        }
        return entry;
    }

}
