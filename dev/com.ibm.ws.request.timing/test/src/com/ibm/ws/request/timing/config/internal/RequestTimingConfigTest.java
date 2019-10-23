/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package com.ibm.ws.request.timing.config.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.ws.request.timing.internal.config.RequestTimingConfig;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;

public class RequestTimingConfigTest {
	
	private static final String TIMING_TYPE_A = "typea";
	private static final String TIMING_TYPE_B = "typeb";
	
	private static final List<Timing> timingListA = Arrays.asList(new Timing[] {
			new Timing(TIMING_TYPE_A, new String[] {"first", "second"}, 10, false),
			new Timing(TIMING_TYPE_A, new String[] {"first", "third"}, 20, false),
			new Timing(TIMING_TYPE_A, new String[] {"first", "*"}, 30, false),
			new Timing(TIMING_TYPE_A, new String[] {"*", "second"}, 40, false)});
	
	private static final List<Timing> timingListB = Arrays.asList(new Timing[] {
			new Timing(TIMING_TYPE_B, new String[] {"*", "*"}, 50, false)});
	
	private static final List<Timing> timingListB2 = Arrays.asList(new Timing[] {
			new Timing(TIMING_TYPE_B, new String[] {"*", "*"}, 50, false),
			new Timing(TIMING_TYPE_B, new String[] {"fourth", "fifth"}, 55, false),
			new Timing(TIMING_TYPE_B, new String[] {"fourth", "*"}, 56, false),
			new Timing(TIMING_TYPE_B, new String[] {"*", "fifth"}, 57, false)});

	private static final List<Timing> defaultList = Arrays.asList(new Timing[] {
			new Timing(RequestTimingConstants.ALL_TYPES, Timing.ALL_CONTEXT_INFO, 60, false)});

	
	@Test
	public void testBestScoreDefaultMatch() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);
		
		String[] probeContextInfo = new String[] {"no", "match"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 60);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 50);
	}
	
	@Test
	public void testBestScoreDefaultMatchNoTypeMap() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);
		
		String[] probeContextInfo = new String[] {"no", "match"};
		long threshold = config.getRequestThreshold("NOTYPE", probeContextInfo);
		assertEquals(threshold, 60);
	}

	@Test
	public void testBestScoreExactMatch() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);

		String[] probeContextInfo = new String[] {"first", "third"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 20);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 50);
	}

	@Test
	public void testBestScoreExactMatchFirstSection() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);

		String[] probeContextInfo = new String[] {"first", "period"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 30);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 50);
	}

	@Test
	public void testBestScoreExactMatchSecondSection() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);

		String[] probeContextInfo = new String[] {"milli", "second"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 40);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 50);
	}
	
	@Test
	public void testBestScoreExactMatchOverWildcard() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB2);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);

		String[] probeContextInfo = new String[] {"fourth", "fifth"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 60);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 55);
	}

	@Test
	public void testBestScorePartialMatchOverWildcard() {
		HashMap<String, List<Timing>> timingMap = new HashMap<String, List<Timing>>();
		timingMap.put(TIMING_TYPE_A, timingListA);
		timingMap.put(TIMING_TYPE_B, timingListB2);
		timingMap.put(RequestTimingConstants.ALL_TYPES, defaultList);
		RequestTimingConfig config = new RequestTimingConfig(1, ContextInfoRequirement.ALL_EVENTS, timingMap);

		String[] probeContextInfo = new String[] {"fourth", "quarter"};
		long threshold = config.getRequestThreshold(TIMING_TYPE_A, probeContextInfo);
		assertEquals(threshold, 60);
		
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 56);
		
		probeContextInfo = new String[] {"mahler", "fifth"};
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 57);
		
		probeContextInfo = new String[] {"total", "nonsense"};
		threshold = config.getRequestThreshold(TIMING_TYPE_B, probeContextInfo);
		assertEquals(threshold, 50);
	}
}
