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

package com.ibm.ws.request.timing.internal.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;

public class RequestTimingConfig {
	
	/** Rate at which sampling should happen for request timing **/
	private final int sampleRate;
	
	/** Indicates if context information details will be included in output  **/
	private final int contextInfoRequirement;
	
	/** Min & max values for thresholds based on the current settings **/
	private final long requestThresholdMin;
	
	private final long requestThresholdMax;
	
	/** Map holding timing values for different types. The map is read-only. **/
	private final Map<String, List<Timing>> requestTiming;

	/** The default request timing for all types. **/
	private final Timing defaultRequestTiming;
	
	public RequestTimingConfig(int sampleRate, int contextInfoRequirement, Map<String, List<Timing>> requestTiming){
		this.sampleRate = sampleRate;
		this.contextInfoRequirement = contextInfoRequirement;
		this.requestTiming = requestTiming;
		requestThresholdMin = getReqThresholdMin();
		requestThresholdMax = getReqThresholdMax();

		// Cache the default request timing.
		if (this.requestTiming.containsKey(RequestTimingConstants.ALL_TYPES)) {
			Timing defaultRequestTiming = null;
			List<Timing> defaultTimingMap = this.requestTiming.get(RequestTimingConstants.ALL_TYPES);
			for (Timing t : defaultTimingMap) {
				if (t.isDefaultTiming()) {
					defaultRequestTiming = t;
					break;
				}
			}
			this.defaultRequestTiming = defaultRequestTiming;
		} else {
			this.defaultRequestTiming = null;
		}
	}
	
	public RequestTimingConfig(){
		this(RequestTimingConstants.SAMPLE_RATE, ContextInfoRequirement.NONE, new HashMap<String, List<Timing>>());
	}
	
	public int getSampleRate() {
		return sampleRate;
	}

	public int getContextInfoRequirement() {
		return contextInfoRequirement;
	}
	
	public Map<String, List<Timing>> getRequestTiming() {
		return requestTiming;
	}

	public long getRequestThresholdMin() {
		return requestThresholdMin;
	}

	public long getRequestThresholdMax() {
		return requestThresholdMax;
	}
	
	public long getRequestThreshold(String type, String[] contextInfo){
		return getTiming(type, contextInfo).getRequestThreshold();
	}
	
	public boolean getInterruptRequest(String type, String[] contextInfo) {
		return false;
	}

	protected Timing getTiming(String type, String contextInfo[]) {
		//Returns the threshold value for the type if present
		//else returns the global threshold value.
		if(type == null){
			throw new IllegalArgumentException("Parameter 'type' can not be null");
		}
		
		// If we've got some type-specific timings, look for a match here.
		if(requestTiming.containsKey(type)) {
			int bestMatchScore = Integer.MIN_VALUE;
			Timing bestMatch = null;
			
			// Start by looking for a match based on context info.
			List<Timing> timingMapForType = requestTiming.get(type);
			for (Timing t : timingMapForType) {
				if (t != null){
					int score = t.getContextInfoMatchScore(contextInfo);
					if (score > bestMatchScore) {
						bestMatchScore = score;
						bestMatch = t;
					}
				}
			}
			
			// If we found a match based on context info, return that.
			if (bestMatch != null) {
				return bestMatch;
			}
		}

		// No default setting for this type.
		return defaultRequestTiming;
	}
	
	private long getReqThresholdMin(){
		//Calculate minimum of threshold values
		//Minimum will be 0 if the global threshold and all type specific thresholds are either 0 or -1
		//else it will be minimum of valid threshold values, valid threshold value is some value which is > 0
		//Example 1) RT = 0, RT1 = -1, RT2 = 0. Minimum will be 0
		// 2) RT = 0, RT1 = 1000, RT2 = 2000. Minimum will be 1000
		long requestThreasholdMin = Long.MAX_VALUE;

		for(List<Timing> typeMap : requestTiming.values()) {
			for (Timing t : typeMap) {
				long curThreshold = t.getRequestThreshold();
				if (curThreshold > 0) {
					requestThreasholdMin = Math.min(requestThreasholdMin, curThreshold);
				}
			}
		}

		//We didn't find a valid minimum threshold value so assigning min value back to 0.
		if(requestThreasholdMin == Long.MAX_VALUE)
			requestThreasholdMin = 0;

		return requestThreasholdMin;
	}
	
	private long getReqThresholdMax(){
		long requestThreasholdMax = Long.MIN_VALUE;
		for (List<Timing> typeMap : requestTiming.values()) {
			for (Timing t : typeMap) {
				requestThreasholdMax = Math.max(requestThreasholdMax, t.getRequestThreshold());
			}
		}
		return requestThreasholdMax;
	}
}
