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

package com.ibm.ws.request.timing;

public class RequestTimingConstants {

	 /** Property name for the slowRequestThreshold of request timing **/
    public static final String RT_SLOW_REQUEST_THRESHOLD  = "slowRequestThreshold";
    
    /** Property name for the hungRequestThreshold  of request timing **/
    public static final String RT_HUNG_REQUEST_THRESHOLD = "hungRequestThreshold";
    
    /** Property name for samplingRate of Request Timing **/
    public static final String RT_SAMPLE_RATE = "sampleRate";
    
    /** Property name for includeContextInfo of Request Timing **/
    public static final String RT_INCLUDE_CONTEXT_INFO = "includeContextInfo";
    
    /** Property name for interruptHungRequests of Request Timing **/
    public static final String RT_INTERRUPT_HUNG_REQUEST = "interruptHungRequests";
    
    /** Name for timing element of Request Timing **/
    public static final String RT_TIMING = "timing";
    
    /** Property name for type of timing element **/
    public static final String RT_TIMING_EVENT_TYPE = "eventType";
    
    /** Property name for the context info pattern of timing element **/
    public static final String RT_TIMING_CONTEXT_INFO_PATTERN = "contextInfoPattern";
    
    /** Default values for configuration values **/
	public static final int SAMPLE_RATE = 1;

	public static final int SLOW_REQUEST_THRESHOLD_MS = 10000;

	public static final int HUNG_REQUEST_THRESHOLD_MS = 600000;

	public static final int SLOW_REQUEST_ITERATIONS_REQ = 3;
	
	public static final int THREAD_DUMPS_REQUIRED = 3;

	public static final int THREAD_DUMP_DURATION = 1; // In minutes, each new thread dump should be generated with time difference of 1 minute 

	/** String constant for all types **/
	public static final String ALL_TYPES = "all";
}
