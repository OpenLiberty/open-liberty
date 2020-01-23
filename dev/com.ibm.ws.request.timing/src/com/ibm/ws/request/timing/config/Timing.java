/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.timing.config;

import java.io.PrintWriter;

import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;

/**
 * Class for storing timing values for a request type.
 * Instances of this class are immutable.
 */
public class Timing {
	
	/** Matches all context info. */
	public static final String[] ALL_CONTEXT_INFO = new String[] {"*"};
	
	/** Threshold for a request after which we will take some action **/
	private final long requestThreshold;
	
	/** 
     * Request type for which the above timing settings should be applied 
     * Could be "all" or a specific type
     * "all" implies, for everything except the types which have timing information
     * explicitly defined
     */
    private final String type;
    
    /**
     * Optional context info for which the above timing settings should be applied.
     * If no context info is supplied, then this Timing matches all requests for this
     * type, unless another Timing that matches type and context info exists.
     * 
     * A section of the context info may consist of an asterisk character, in which
     * case that section may match anything.
     */
    private final String[] contextInfo;
    
    /**
     * Stores whether each section of the context info contains only an asterisk.
     */
    private final boolean[] contextInfoAsterisk;

    /**
     * Stringified version of context info, for trace.
     */
    private final String contextInfoString;
    
    /**
     * Should we interrupt this request when it's hung (for hung request threshold
     * only).
     */
    private final boolean interruptHungRequest;
    
    /**
     * The PID which generated this timing, if generated from a sub-type.
     */
    private final String timingPid;
    
    private static final int HISTORICAL_REQUEST_LIMIT = 5;
    
    /**
     * Some statistics about what we've processed lately.  Serialized on the last
     * request array.
     */
    transient private long requestCount = 0L;
    
    /**
     * The context info for the last X requests that have matched this timing config.
     * This is dumped in the dump introspector to help determine what this timing config is matching.
     */
    transient private String[] historicalRequestData = new String[HISTORICAL_REQUEST_LIMIT];
    
    public Timing(String type, long requestThreshold){
    	this(type, null, requestThreshold, false);
	}
    
    public Timing(String type, String[] contextInfo, long requestThreshold, boolean interruptHungRequest){
		this(null, type, contextInfo, requestThreshold, interruptHungRequest);
	}

    public Timing(String timingPid, String type, String[] contextInfo, long requestThreshold, boolean interruptHungRequest){
    	this.timingPid = timingPid;
		this.type = type;
		this.requestThreshold = requestThreshold;
		this.interruptHungRequest = interruptHungRequest;

		if (contextInfo == null) {
			this.contextInfo = null;
			this.contextInfoAsterisk = null;
			this.contextInfoString = null;
		} else {
			this.contextInfo = new String[contextInfo.length];
			System.arraycopy(contextInfo, 0, this.contextInfo, 0, contextInfo.length);

			StringBuilder sb = new StringBuilder();
			this.contextInfoAsterisk = new boolean[this.contextInfo.length];
			for (int x = 0; x < this.contextInfoAsterisk.length; x++) {
				if (x != 0) {
					sb.append(RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR);
				}
				sb.append(this.contextInfo[x]);
				this.contextInfoAsterisk[x] = this.contextInfo[x].equals("*");
			}
			this.contextInfoString = sb.toString();
		}
	}

    public String getTimingPid() {
    	return timingPid;
    }
    
    public long getRequestThreshold() {
		return requestThreshold;
	}

	public String getType() {
		return type;
	}

	/**
	 * Determine if the input context info is a match for this timing element.  The
	 * match is returned as an integer reflecting the quality of the match.  Sections
	 * further into the context info array have a higher weight than the first
	 * sections.  Matching a string value has a higher weight than matching an asterisk.
	 * 
	 * @return A negative value if there is no match, and a zero or positive value if
	 *         there is a match.  Higher numbers indicate a better quality match.
	 */
	public int getContextInfoMatchScore(String[] contextInfo) {
		if (this.contextInfo == null) {
			return Integer.MIN_VALUE;
		}

		// The first section has a multiplier of one.  For each subsequent section,
		// increase the multiplier by a factor of 10.
		int curMultiplier = 1;
		int multFactor = 10;
		int curScore = 0;
		
		// We try to match on as many sections of the context info as we have in the
		// Timing element.  If the input context info has more sections, we ignore
		// those.
		for (int x = 0; x < this.contextInfo.length; x++) {
			if (this.contextInfoAsterisk[x] == false) {
				if (this.contextInfo[x].equals(contextInfo[x])) {
					curScore += (curMultiplier * this.contextInfo[x].length());
				} else {
					return Integer.MIN_VALUE;
				}
			}
			
			curMultiplier *= multFactor;
		}

		return curScore;
	}
	
	/**
	 * Tells us if this is a default timing (matches all requests).
	 */
	public boolean isDefaultTiming() {
		if ((contextInfoAsterisk == null) || (contextInfoAsterisk.length == 0)) {
			return false;
		}

		for (boolean b : contextInfoAsterisk) {
			if (b == false) {
				return false;
			}
		}
		
		return true;
	}
	
	public String getContextInfoString() {
		return contextInfoString;
	}
	
	public boolean interruptHungRequest() {
		return interruptHungRequest;
	}
	
	public void incrementCount(String requestContextInfo) {
		synchronized(historicalRequestData) {
			historicalRequestData[(int)(requestCount % HISTORICAL_REQUEST_LIMIT)] = requestContextInfo;
			requestCount++;
		}
	}
	
	public void writeIntrospectionData(PrintWriter pw) {
		synchronized(historicalRequestData) {
			pw.println(" Context info pattern: " + contextInfoString + "  Request count: " + requestCount);
			if (requestCount > 0L) {
				for (int x = 0; x < HISTORICAL_REQUEST_LIMIT; x++) {
					if (historicalRequestData[x] != null) {
						pw.println("   Sample context info:" + historicalRequestData[x]);
					}
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		return type.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Timing) {
			Timing that = (Timing)o;
			return ((this.requestThreshold == that.requestThreshold) &&
					(objectEquals(this.type, that.type)) &&
					(objectEquals(this.contextInfo, that.contextInfo)));
		} else {
			return false;
		}
	}
	
	private boolean objectEquals(Object o1, Object o2) {
		if (o1 == o2) 
			return true;
		else if (o1 == null) 
			return false;
		else if (o2 == null)
			return false;
		else 
			return o1.equals(o2);
	}
}
