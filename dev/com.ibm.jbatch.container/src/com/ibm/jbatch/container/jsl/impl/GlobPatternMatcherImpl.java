/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.jsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobPatternMatcherImpl {
	
	public GlobPatternMatcherImpl() {}
	
	private final static Logger logger = Logger.getLogger(GlobPatternMatcherImpl.class.getName());

	public boolean matchWithoutBackslashEscape(String toMatch, String pattern) {

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(GlobPatternMatcherImpl.class.getName(), "match", "Trying to match string: " + toMatch + " against un-normalized pattern: " + pattern);
		}

		boolean isMatch = false;

		// Precondition: Blow up with null or empty s or pattern.
		//TODO - Spec ramifications or is this already covered by property and exit status defaulting?
		if ((pattern == null) || (pattern.length() == 0) ||
				(toMatch == null) || (toMatch.length() == 0)) {
			throw new IllegalArgumentException("Pattern = " + pattern + 
					"and string to match = " + toMatch + 
			", but both pattern and to-match String are required to be non-null Strings with length >=1 ");
		}

		// Normalize away consecutive '*' chars:
		String newPattern = pattern;
		while(true) {
			if (newPattern.indexOf("**") >= 0) {
				newPattern = newPattern.replace("**", "*");
			} else {
				break;
			}
		}

		isMatch = recursiveMatch(toMatch, newPattern);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(GlobPatternMatcherImpl.class.getName(), "match", "Returning boolean: " + isMatch); 
		}

		return isMatch;
	}

	//
	// TODO - confirm in spec that there is NO backslashing of '*', '?'. 
	//
	private boolean recursiveMatch(String toMatch, String subpattern) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Trying to do a recursive match of string: " + toMatch + " against subpattern: " + subpattern);
		}

		int firstAsterisk = subpattern.indexOf("*");
		int secondAsterisk = subpattern.indexOf("*", firstAsterisk +1);
		int lastAsterisk = subpattern.lastIndexOf("*");

		//
		//  A) If there are no '*'(s), do a non-wildcard (except for ?) match
		//
		if (firstAsterisk == -1) {        	
			return matchNoAsterisk(toMatch, subpattern);
		}

		//
		//  B) Match off any beginning or end
		//			 

		// Match any beginning BEFORE the first asterisk
		if (firstAsterisk > 0) {
			String beginPattern = subpattern.substring(0, firstAsterisk);	
			if (toMatch.length() < beginPattern.length()) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Returning false, not enough chars to match against beginning of pattern: " + beginPattern);
				}			
				return false;
			}
			String beginToMatch = toMatch.substring(0, firstAsterisk);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Matching against beginning of pattern (before first asterisk).");
			}			
			if  (!matchNoAsterisk(beginToMatch, beginPattern)) {
				return false;
			}					
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("No beginning of pattern (before first asterisk) to match against.");
			}						
		}

		// This will just be a straight copy if 'firstAsterisk' = 0.
		// Since we're using recursion, try to be a bit performance-sensitive and not do this until necessary.
		String remainingToMatch = toMatch.substring(firstAsterisk);

		//Match any end AFTER the first asterisk
		if (lastAsterisk < subpattern.length() - 1) {
			String endPattern = subpattern.substring(lastAsterisk + 1);
			if (remainingToMatch.length() < endPattern.length()) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Returning false, not enough chars in remaining string " + remainingToMatch + " to match against end of pattern: " + endPattern);
				}
				return false;
			}
			// Give me a substring the size of 'endPattern' at the end
			String endToMatch = remainingToMatch.substring(remainingToMatch.length() - endPattern.length(), remainingToMatch.length());
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Matching against end of pattern (after last asterisk).");
			}	
			if (!matchNoAsterisk(endToMatch, endPattern)) {
				return false;
			}
			// Already matched against char at index: 'remainingToMatch.length() - endPattern.length()', so truncate immediately before. 
			remainingToMatch = remainingToMatch.substring(0, remainingToMatch.length() - endPattern.length());
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("No end of pattern (after last asterisk) to match against.");
			}						
		}

		// C) Now I either have:
		//  1)   *
		//  2)   *-xxxxx-*  (All non-'*' chars in the middle
		//  3)   *-xy-*-x-* (At least one '*' in the middle)
		if (firstAsterisk == lastAsterisk) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Returning true, remaining string matches single '*'");
			}
			return true;
		} else if (secondAsterisk == lastAsterisk) {
			String middlePattern = subpattern.substring(firstAsterisk + 1, lastAsterisk );

			if (remainingToMatch.length() < middlePattern.length()) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Returning false, remaining string: " + remainingToMatch + " is shorter than non-asterisk middle pattern: " + middlePattern);
				}						
				return false;
			}  else {
				for (int i = 0; i <= remainingToMatch.length() - middlePattern.length() ; i++) {
					String matchCandidate = remainingToMatch.substring(i, i + middlePattern.length());
					if (matchNoAsterisk(matchCandidate, middlePattern)) {
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest("Returning true, matched candidate: " + matchCandidate + " against non-asterisk middle pattern: " + middlePattern);
						}									
						return true;
					}
				}
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Returning false, no match found within string: " + remainingToMatch + 
							" against non-asterisk middle pattern: " + middlePattern);
				}									
				return false;
			}
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Trying to match: " + remainingToMatch + " against pattern containing at least three asterisks.");
			}			
			//
			// Now I have to match against sub-sub-pattern *xxx*xy*.  The strategy here is to use recursion.   

			// 1. Isolate 'xxx' and store as 'patternBetweenAsterisk1and2'
			String patternBetweenAsterisk1and2 = subpattern.substring(firstAsterisk + 1, secondAsterisk);

			// 2. Find every place in the string matching 'xxx' and store the remainder as a candidate
			List<String> subMatchCandidates = new ArrayList<String>();

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Begin looping looking to match pattern between first and second asterisk; pattern = " + patternBetweenAsterisk1and2);
			}					
			for (int i = 0; i <= remainingToMatch.length() - patternBetweenAsterisk1and2.length() ; i++) {
				String matchCandidate = remainingToMatch.substring(i, i + patternBetweenAsterisk1and2.length());
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Next match candidate = " + matchCandidate);
				}	
				if (matchNoAsterisk(matchCandidate, patternBetweenAsterisk1and2)) {					
					// Grab the part of the string after the match.
					String subMatchCandidate = remainingToMatch.substring( i + patternBetweenAsterisk1and2.length());
					subMatchCandidates.add(subMatchCandidate);
				}
			}
			
			if (subMatchCandidates.size() == 0) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Returning false, no matches of pattern: " + patternBetweenAsterisk1and2 + 
							" in remainingToMatch = " + remainingToMatch);
				}				
				return false;
			} else {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Found " + subMatchCandidates.size() + " candidates to recursively match against.");
				}	
			}

			// 2. Calculate the new pattern to match against. 
			// For "*xxx*xy*" this would be "*xy*".
			// For "*xxx*xy*z*" this would be "*xy*z*"
			String nestedPattern = subpattern.substring(secondAsterisk, lastAsterisk + 1);  // We want to include the last asterisk.

			// 3. try matching each one
			for (String candidate : subMatchCandidates) {		

				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Do a recursive match of candidate: " + candidate + " against nested pattern: " + nestedPattern);
				}	
				boolean match = recursiveMatch(candidate, nestedPattern);

				if (match) {
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("Found that nested, recursive match=true so returning 'true'");
					}	
					return true;
				}				
			}

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("No recursive match candidates matched so returning 'false'");
			}	

			return false;			
		}
	}


	/**
	 * 1. Match (against regular char or ?)
	 *Default to 'true' which would apply to zero-length boundary condition
	 */
	private boolean matchNoAsterisk(String toMatch, String subpattern) {

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Entering matchNoAsterisk with  toMatch = " + toMatch + " and subpattern = " + subpattern);
		}			

		int toMatchLen = toMatch.length();
		int subpatternLen = subpattern.length();

		if (toMatchLen != subpatternLen) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("FAIL: in matchNoAsterisk:  toMatchLen = " + toMatchLen + " != subpatternLen = " + subpatternLen);
			}			
			return false;
		}

		for (int i = 0; i < toMatchLen; i++) {
			if (subpattern.substring(i,i+1).equals("*")) {
				logger.fine("FAIL: in matchNoAsterisk: Shouldn't have encountered a '*' in matchNoAsterisk, toMatch = " + toMatch + ", subPattern = " + subpattern);
				throw new IllegalStateException("Shouldn't have encountered a '*' in matchNoAsterisk, toMatch = " + toMatch + ", subPattern = " + subpattern);
			} else if (subpattern.substring(i,i+1).equals("?")) {
				// Nothing needed) 
			} else {
				if (subpattern.charAt(i) != toMatch.charAt(i)) {
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("FAIL: in matchNoAsterisk: mismatch at index: " + i + ". The char subpattern.charAt(i)= "  + subpattern.charAt(i) + " != toMatch.charAt(i) = " + toMatch.charAt(i));
					}			
					return false;
				}			
				// else continue
			}
		}

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("SUCCESS: in matchNoAsterisk");
		}			

		return true;
	}



}
