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
package com.ibm.ws.sip.container.pmi.taskduration;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.sip.container.pmi.ApplicationTaskDurationModuleInterface;

public class ApplicationTaskDurationModule implements ApplicationTaskDurationModuleInterface {

	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(ApplicationTaskDurationModule.class
		.getName());
	
	/**
	 * Task duration counter for the application 
	 */
    private TaskDurationCounter _applicationCodeTaskDurationCounter = new TaskDurationCounter();
     
    /** Avg task duration in sip container queue */
    private long _avgTaskDurationInApplicationCode = 0;
    
    /** Maximum task duration in sip container queue*/
    private long _maxTaskDurationInApplicationCode = 0;
    
    /** Minimum task duration in sip container queue*/
    private long _minTaskDurationInApplicationCode = 0;
      
    /**
     * CTOR
     * 
     * @param appFullName 
     *            name of the application that module belongs to
     */
    public ApplicationTaskDurationModule(String appFullName) {        
    }
    
    /**
     * Application destroyed, unregister PMI module associated with this
     * application.
     *  
     */
    public void destroy() {       
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.ApplicationTaskDurationModuleInterface#updateTaskDurationInApplication(long)
     */
    public synchronized void updateTaskDurationInApplication(long ms) {
    	_applicationCodeTaskDurationCounter.updateTaskDurationStatistics(ms);
        if (s_logger.isLoggable(Level.FINEST)) {
            StringBuilder buf = new StringBuilder();
            buf.append("New task duration in application code = " + ms);
            s_logger.logp(Level.FINEST, ApplicationTaskDurationModule.class.getName(),
					"updateTaskDurationInApplication", buf.toString());
        }
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.ApplicationTaskDurationModuleInterface#updatePMICounters()
     */
    public synchronized void updatePMICounters() {
    	_avgTaskDurationInApplicationCode = _applicationCodeTaskDurationCounter.getAvgTaskDurationOut();
		_maxTaskDurationInApplicationCode = _applicationCodeTaskDurationCounter.getMaxTaskDurationOut();
		_minTaskDurationInApplicationCode = _applicationCodeTaskDurationCounter.getMinTaskDurationOut();
		
		_applicationCodeTaskDurationCounter.init();
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.ApplicationTaskDurationModuleInterface#isApplicationDurationPMIEnabled()
     */
	public boolean isApplicationDurationPMIEnabled() {		
		return true;
	}

	@Override
	public long getAvgTaskDurationInApplication() {
		return _avgTaskDurationInApplicationCode;
	}

	@Override
	public long getMaxTaskDurationInApplication() {
		return _maxTaskDurationInApplicationCode;
	}

	@Override
	public long getMinTaskDurationInApplication() {
		return _minTaskDurationInApplicationCode;
	}
    
}
