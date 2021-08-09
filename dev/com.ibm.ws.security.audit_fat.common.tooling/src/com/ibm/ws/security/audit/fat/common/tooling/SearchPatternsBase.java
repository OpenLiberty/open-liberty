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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.json.java.JSONObject;
/**
 * Provides an ordered sequence of test searchPatterns and pattern matching capabilities.
 */
public class SearchPatternsBase {

	protected List<JSONObject> searchPatterns;
	protected JSONObject recentMatch;
	protected String originalString;
	
	
    /**
     * Thrown when check is made and an unexpected pattern match occurs.
     */ 
    public class UnexpectedPatternMatchException extends RuntimeException {

		public UnexpectedPatternMatchException(String msg1, String msg2) {
    		super (msg1 + msg2);
		}
    }
	/**
	 * Given an array of JSON test patterns, create a sequence of searchPatterns.
	 * 
	 * @param thePatterns - sequence of one or more test patterns to be used in search. 
	 */
	public SearchPatternsBase (JSONObject... thePatterns){
		searchPatterns = new ArrayList<JSONObject>();
		searchPatterns.addAll(Arrays.asList(thePatterns));
		originalString = this.toString();
		}
	
	/**
	 * Return a string representation of the most recently matched searchPattern in a sequence of searchPatterns.
	 * @return most recently matched search pattern
	 */
	public String mostRecentlyMatchedPattern () {
		return (recentMatch == null) ? null : this.recentMatch.toString();
	}


	/**
	 * Return a specified pattern within a sequence of searchPatterns. 
	 * Return null if searchPatterns is empty.
	 * @param i - index for element to return
	 * @return single pattern at specified index
	 */
	public JSONObject get(int i) {
		return (searchPatterns.size() == 0) ? null : searchPatterns.get(i);
	};
	
	/**
	 * Given a searchPattern, return a string representation of it's contents.
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
    	for (JSONObject pattern: searchPatterns){
    		if (sb.length() > 0) 
    			sb.append( " | ");
    		sb.append(pattern.toString()); 
    	}
    	return sb.toString();
	}

	
	/**
	 * Determines whether all keys and values in a little JSON object are contained within (are a subset of)
	 * a big JSON object.
	 * @param little - JSON subset to be checked for in larger JSON object
	 * @param big - JSON object against which check is performed.
	 * @return true if all keys and valued in the little object are found in the big object 
	 * 		   false if not all keys and values are found in the big object
	 */
    public boolean isSubsetOf(JSONObject little, JSONObject big) {
        for (Object key : little.keySet()) {
            Object littleValue = little.get(key);
            Object bigValue = big.get(key);
            
            // Key is not found in JSON log data
            if (littleValue != null && bigValue == null) {
                return false;
            }

            if ((bigValue instanceof Long) || (bigValue instanceof Integer) || (bigValue instanceof Float)) {
            	bigValue = bigValue.toString();
            }
            
            // Looking for leaf, but key in JSON data is not a leaf
            if (littleValue instanceof String && !(bigValue instanceof String)) {
                return false;
            }

            // Search pattern is incorrect -too many keys
            if (!(littleValue instanceof String) && bigValue instanceof String) {
                return false;
            }

            // Found two leaves in JSON data, compare values and return false if not matched
            if (littleValue instanceof String && bigValue instanceof String) {
                if (!valueIsEqual(littleValue, bigValue))
					return false;
            }
            // Not comparing leaves, so keep looking
            if (littleValue instanceof JSONObject && bigValue instanceof JSONObject) {
                boolean subresult = isSubsetOf((JSONObject) littleValue, (JSONObject) bigValue);
                if (!subresult)
                    return false; // no match
            }
        }
        // Return true when all traversed and match found
        return true;
    }

	/**
	 * Determines whether the littleValue is a regular expression (starting with
	 * anchor string  ^.*) or whether it is to be used as an exact string compare and then
	 * perform the compare.
	 * 
	 * @param littleValue - value to be searched for - either regEx or string
	 * @param bigValue -  value against which the compare is performed
	 * @return true if the littleValue is equal to the bigValue or if littleValue regEx matches bigValue 
	 * 		   false if not equal
	 */
    private boolean valueIsEqual(Object littleValue, Object bigValue) {

		if (littleValue.toString().startsWith("^.*")) {
			Pattern pattern = Pattern.compile(littleValue.toString());
			Matcher matcher = pattern.matcher(bigValue.toString());
			return matcher.matches();
		}
		else 
		{
			return littleValue.equals(bigValue);
		}
	}
	
 
}
