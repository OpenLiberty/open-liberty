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

import com.ibm.json.java.JSONObject;
/**
 * Provides an ordered sequence of test searchPatterns and pattern matching capabilities.
 */
public class SearchPatternsSequential extends SearchPatternsBase {
	/**
	 * Thrown when check is made and not all patterns match.
	 */
    public class IncompletePatternMatchException extends RuntimeException {

		public IncompletePatternMatchException(String msg1, String msg2) {
    		super (msg1 + msg2);
		}
    }

    
	/**
	 * Given an array of JSON test patterns, create a sequence of searchPatterns.
	 * 
	 * @param thePatterns - sequence of one or more test patterns to be used in search. 
	 */
	public SearchPatternsSequential (JSONObject... thePatterns){
		super (thePatterns);
	}
	

	/**
	 * Given a JSON object, determine if a searchPattern is a subset of that object (i.e. all keys and values are
	 * contained within the JSON record.
	 * @param jsonRecord JSON against which searchPattern is checked
	 * @return true if the searchPattern was matched. When true, remove the pattern from the list of searchPatterns.
	 */
	public boolean matchedCurrent (JSONObject jsonRecord) {
		boolean result = isSubsetOf(searchPatterns.get(0), jsonRecord);
		if (result)  {
			recentMatch = searchPatterns.get(0);
			searchPatterns.remove(0);
		}
		return result;
	}
	

	/**
	 * Throws an exception if not all of the expected patterns were matched.
	 */	
	public void assertAllMatched() {
		if (!searchPatterns.isEmpty()) {
            	throw new IncompletePatternMatchException("The audit log did not contain all of the expected test patterns: " , this.toString());
        }
	}
	/**
	 * Throws an exception if all patterns were matched unexpectedly.
	 */
	public void assertNotAllMatched() {
		if (searchPatterns.isEmpty()) 
			throw new UnexpectedPatternMatchException("All test patterns were unexpectedly matched in the audit log: ", originalString);
	
	}
	
	/**
	 * Determine if all searchPatterns have been matched.
	 * @return true if all matched; false if not all matched.
	 */
	public boolean allMatched() {
		return searchPatterns.isEmpty();
	}


	
}
