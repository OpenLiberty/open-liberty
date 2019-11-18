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
 
package com.ibm.ws.request.timing.config;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;
import com.ibm.wsspi.requestContext.ContextInfoArray;

public class TimingTest {

	@Test
	public void testContextInfoParsing() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"first", "second"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String contextInfoString = t.getContextInfoString();
		String expectedContextInfoString = "first" + RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR + "second";
		assertEquals(expectedContextInfoString, contextInfoString);
		
		assertFalse(t.isDefaultTiming());
	}
	
	@Test
	public void testContextInfoParsingNull() {
		final String pid = "pid", type = "type";
		Timing t = new Timing(pid, type, null, 10, false);
		
		assertNull(t.getContextInfoString());
		assertFalse(t.isDefaultTiming());
	}
	
	@Test
	public void testDefaultTimingSingle() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"*"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String contextInfoString = t.getContextInfoString();
		String expectedContextInfoString = "*";
		assertEquals(contextInfoString, expectedContextInfoString);
		
		assertTrue(t.isDefaultTiming());
	}
	
	@Test
	public void testDefaultTimingMultiple() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"*", "*", "*"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String contextInfoString = t.getContextInfoString();
		String expectedContextInfoString = "*" + RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR + "*" + RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR + "*";
		assertEquals(contextInfoString, expectedContextInfoString);
		
		assertTrue(t.isDefaultTiming());
	}
	
	@Test
	public void testUnmatchedScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"first", "second"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"second", "first"};
		assertEquals(Integer.MIN_VALUE, t.getContextInfoMatchScore(probeContextInfo));
	}
	
	@Test
	public void testWildcardMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"*", "*"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"second", "first"};
		assertEquals(0, t.getContextInfoMatchScore(probeContextInfo));
	}
	
	@Test
	public void testFirstWildcardMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"*", "second"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"first", "second"};
		assertEquals(60, t.getContextInfoMatchScore(probeContextInfo));
	}
	
	@Test
	public void testFirstWildcardNoMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"*", "second"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"first", "third"};
		assertEquals(Integer.MIN_VALUE, t.getContextInfoMatchScore(probeContextInfo));
	}

	@Test
	public void testSecondWildcardMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"first", "*"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"first", "second"};
		assertEquals(5, t.getContextInfoMatchScore(probeContextInfo));
	}

	@Test
	public void testSecondWildcardNoMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"first", "*"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"third", "second"};
		assertEquals(Integer.MIN_VALUE, t.getContextInfoMatchScore(probeContextInfo));
	}
	
	@Test
	public void testMultipleExactMatchScore() {
		final String pid = "pid", type = "type";
		String[] contextInfoArray = new String[] {"first", "second"};
		Timing t = new Timing(pid, type, contextInfoArray, 10, false);
		
		String[] probeContextInfo = new String[] {"first", "second"};
		assertEquals(65, t.getContextInfoMatchScore(probeContextInfo));
	}
}
