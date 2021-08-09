/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * Class that encapsulates data for SIP Messages PMI Statistics.
 * @author Assya Azrieli
 */
public class SipMessagePMIEntry {
	
	/**
	 * Statistic ID according to PMI module XML file
	 */
	private Integer _statisticId;
	
	/**
	 * Counter for PMI entry. Hold the delta for the statisticData
	 * and update PMI counter at the next "Static Update Rate" interval.
	 */
	private long _counter = 0;
	
	/**
	 * Description for PMI entry
	 */
	private String _description = "";
	    
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipMessagePMIEntry.class);
    
	
	/**
	 * Setup an PMI entry with a specified id.  
	 */
	public SipMessagePMIEntry(Integer statisticId, String description) {
//		if(c_logger.isTraceDebugEnabled()){
//			c_logger.traceDebug("new SipMessagePMIEntry created, id: "
//			+ statisticId + " description: " + description);
//		}
		_statisticId = statisticId;
		_description = description;
	}

	public long getCounter() {
		return _counter;
	}

	/**
	 * Increment counter. 
	 */
	public void increment() {		
		_counter++;
	}
	
	/**
	 * Update Statistic data. 
	 */
	public void update() {
		// in tWAS we reset this counter because we had another statistic data object
		// in Liberty this counter represents a real number of the application messages
//		_counter = 0;
	}

	/**
	 * Get the statistic's description. 
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * Get the statistic's ID. 
	 */
	public Integer getStatisticId() {
		return _statisticId;
	}
}