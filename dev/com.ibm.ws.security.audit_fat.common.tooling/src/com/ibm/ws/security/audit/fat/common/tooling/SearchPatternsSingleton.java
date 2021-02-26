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
 * Provides capability to determine if any one of a set of test searchPatterns is found within a JSON record.
 */
public class SearchPatternsSingleton extends SearchPatternsBase {



	/**
	 * Given an array of JSON test patterns, create a set of searchPatterns.
	 * 
	 * @param thePatterns - set of one or more test patterns to be used in search. 
	 */
	public SearchPatternsSingleton (JSONObject... thePatterns){
		super (thePatterns);
	}
	
	/**
	 * Given a JSON object, determine if a searchPattern is a subset of that object (i.e. all keys and values are
	 * contained within the JSON record.
	 * @param jsonRecord JSON against which searchPattern is checked
	 * @return true if the searchPattern was matched. Otherwise, return false.
	 */
	public boolean matchedAny(JSONObject jsonRecord) {
		for (JSONObject searchPattern : searchPatterns) {
			if (isSubsetOf(searchPattern, jsonRecord))  {
				recentMatch = searchPattern;
				return true;
			}
		}
		return false;
	}

	/**
	 * Throws an exception if any of the patterns matched.
	 */
	public void assertNotFound() {
		if (recentMatch != null) 
			throw new UnexpectedPatternMatchException("A test pattern was unexpectedly matched in the audit log: ", mostRecentlyMatchedPattern());
	}
	
}
