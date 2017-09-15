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

package com.ibm.wsspi.requestContext;

import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;

public class EventStackFormatter {

	// creating the TraceNLS Instance
	protected static final TraceNLS nls = TraceNLS.getTraceNLS(
			EventStackFormatter.class,
			"com.ibm.ws.request.probe.internal.resources.LoggingMessages");

	public static StringBuffer getStackFormat(Event event, Boolean includeContextInfo) {
		StringBuffer sb = new StringBuffer();
		int noOfTabs = 0;
		long currentNanoTime = System.nanoTime();
		getTreeFormat(sb, event, includeContextInfo, noOfTabs, currentNanoTime, 0);
		return sb;

	}

	/**
	 * Format the Request Context into a Tree/Stack format
	 * 
	 * @param event
	 */
	private static void getTreeFormat(StringBuffer sb, Event event, Boolean includeContextInfo,
			int noOfTabs, long currentNanoTime, int spacesRequiredForDuration) {
		//Defect 166820
		//Check for events that got added after we started dumping the tree.
		//We ignore these events.
		if(currentNanoTime < event.getStartTime()){
			return;
		}
		
		String executionTimeStr = getExecutionTimeStringFormat(currentNanoTime,
				event); // Convert the execution time into string with 3 decimal
						// places..
		try {
			/**
			 * If its root event add header to table and set the
			 * spacesRequiredForDuration field
			 */
			if (event.getParentEvent() == null) {
				if (spacesRequiredForDuration == 0)
					spacesRequiredForDuration = executionTimeStr.length() + 5;
				sb.append(appendHeader(event, executionTimeStr));
			}
			// Append the execution time
			if (event.getEndTime() == 0) {
				sb.append(String.format(
						"%n%" + spacesRequiredForDuration + "s",
						executionTimeStr + "ms + "));
			} else {
				sb.append(String.format(
						"%n%" + spacesRequiredForDuration + "s",
						executionTimeStr + "ms   "));
			}

			// Append tabs for Child Events
			if (event.getParentEvent() != null) {
				for (int i = 0; i < noOfTabs; i++) {
					sb.append("    ");
				}
			}

			// Append Type + Context information to string...
			if (includeContextInfo && event.getContextInfo() != null) {
				sb.append(event.getType()
						+ RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR
						+ event.getContextInfo());
			} else {
				sb.append(event.getType());
			}

			//Iterating using indices instead of the list iterator to avoid ConcurrentModificationException
			//It is safe to do it this way as we only add elements to the list
			List<Event> list = event.getChildEvents();
			for(int i = 0; i < list.size(); i++){
				//Defect 165134
				//for (Event eventChild : event.getChildEvents()) {
				// Call recursively for child events ..
				getTreeFormat(sb, list.get(i), includeContextInfo, noOfTabs + 1, currentNanoTime,
						spacesRequiredForDuration);
			}
		} catch (Exception e) {

			FFDCFilter.processException(e, EventStackFormatter.class.getName(),
					"94", event);
		}
	}

	private static String appendHeader(Event event, String executionTimeStr) {
		return String.format("%n%1$-" + (executionTimeStr.length() + 5) + "s",
				nls.getString("REQUEST_PROBE_FORMAT_HEADER_DURATION",
						"Duration"))
				+ nls.getString("REQUEST_PROBE_FORMAT_HEADER_OPERATION",
						"Operation");
	}

	private static String getExecutionTimeStringFormat(long currentNanoTime,
			Event event) {
		double executionStr = 0.0;
		if (event.getEndTime() == 0) {

			executionStr = (currentNanoTime - event.getStartTime()) / 1000000.0;
		} else {
			executionStr = (event.getEndTime() - event.getStartTime()) / 1000000.0;
		}

		return String.format("%.3f", executionStr);
	}
}
