/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.display.work.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.timing.stats.RequestTiming;
import com.ibm.wsspi.zos.command.processing.CommandResponses;


/**
 * An implementation of display work command responses. It
 * produces the responses for the display work console display command.
 * This implementation is a generic 'display,work' command that shows
 * servlet work. Can be extended for other types of work.
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=displayWork" }, service = { CommandResponses.class })
public class DisplayWorkCommandResponseImpl implements CommandResponses {

	/** trace variable */
	private static final TraceComponent tc = Tr.register(DisplayWorkCommandResponseImpl.class);
	
	/**
	 * Passed into requestTiming to get counts for servlet work 
	 * Note: The workaround below that adds the counts associated with 
	 * more than one request event types for a servlet request is temporary.
	 * Root events can change for any request type can change at any time and 
	 * without notice. We cannot foresee what new event type might break request 
	 * timing stats in the future. Check comment on the background in RequestTimingEventsTest
	 * in com.ibm.ws.zos.display.work.console_zfat.
	 */
	private final String servletEventType1 = "websphere.servlet.service";
	private final String servletEventType2 = "websphere.http.wrapHandlerAndExecute";
	/** Gets counts from requestTiming */
	private RequestTiming requestTiming;
	/** Stores time stamp of when command was last issued */
	private String prevTimeUsed;
	/** Stores total request count from last command call */
	private long prevRequestCount;

	/**
	 * DS method to activate this component.
	 */
	@Activate
	protected void activate() {}

	/**
	 * DS method to deactivate this component.
	 */
	@Deactivate
	protected void deactivate() {}

	@Reference(service = RequestTiming.class, policyOption = ReferencePolicyOption.GREEDY)
	protected void setrequestTiming(RequestTiming requestTiming) {
		this.requestTiming = requestTiming;
	}

	protected void unsetrequestTiming(RequestTiming requestTiming) {
		this.requestTiming = null;
	}
	
	/** {@inheritDoc} */
	@Override
	public int getCommandResponses(String command, List<String> responses) {
		// Initialize return code
		int rc = 0;
		try {		
			// Builds response string for generic display,work command
			// Only handles servlet work for now
			addWorkResponses(responses, requestTiming);
		} catch (Exception e) {
			// Relying on the FFDC to debug the exception
			responses.add("Unable to process display command due to an error.");
			responses.add("Check logs for details");
			
			// RC of -1 to set modify results to ERROR in command handler
			rc = -1;	
		}
		return rc;
	}

	/**
	 * Gets responses for "display,work" command and adds them to responses list
	 * 
	 * @param responses
	 */
	private synchronized void addWorkResponses(List<String> responses, RequestTiming rtStats) {
		// Get servlet stats from requestTiming, requestTiming should be active at this point if this code runs
		String currentTimeUsed = getCurrentTimeStamp();    
		long requestCount = rtStats.getRequestCount(servletEventType1) + rtStats.getRequestCount(servletEventType2);
		long activeRequestCount = rtStats.getActiveRequestCount(servletEventType1) + rtStats.getActiveRequestCount(servletEventType2);
		
		// Note: Active count may update faster than Hung and Slow count causing
		// inconsistencies with the counts displayed. For example, the command can show an active
		// count of 0 but still have a value greater than 0 for a hung and slow request. 
		// Allow hung and slow requests a few seconds so that they update properly.
		long slowRequestCount = rtStats.getSlowRequestCount(servletEventType1) + rtStats.getSlowRequestCount(servletEventType2);
		long hungRequestCount = rtStats.getHungRequestCount(servletEventType1) + rtStats.getHungRequestCount(servletEventType2);
		
		// Use current time on first use of this command
		if (prevTimeUsed == null) prevTimeUsed = currentTimeUsed;		
		
		// Get delta for total requests
		long deltaTotalRequests = requestCount - prevRequestCount;

		// Add timeLastIssued and servlet stats to response list
		responses.add("TIME OF LAST WORK DISPLAY   " + prevTimeUsed);
		responses.add("TOTAL REQUESTS           " + requestCount + "  (DELTA " + deltaTotalRequests + ")" );
		responses.add("TOTAL ACTIVE REQUESTS    " + activeRequestCount);
		responses.add("TOTAL SLOW REQUESTS      " + slowRequestCount);
		responses.add("TOTAL HUNG REQUESTS      " + hungRequestCount);

		// Set current request counts to prev request counts for next display work call
		prevTimeUsed = currentTimeUsed;
		prevRequestCount = requestCount;
	}

	/**
	 * Helper function that gets UTC time and returns it formatted as 'yyyy-MM-dd hh:mm:ss.SS'
	 * @return String
	 */
	public String getCurrentTimeStamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SS");
		java.util.Date date = new java.util.Date();
		return dateFormat.format(date);
	}
}


