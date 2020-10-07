/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.sip.container.pmi.basic.SessionsCounter;

/**
 * @author Anat Fradin , Nov 17, 2004.
 */
public class SessionModule implements 	SessionInterface {

	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(SessionModule.class
		.getName());  
	
	/** Total sip sessions in this application */
    private long  _totalSipSessions = 0;

    /** Total sip application sessions in this application */
    private long _totalSipAppSessions = 0;
    
    /**
     * Handle SessionsCounter for this Application Module
     */
    private SessionsCounter _appCounter = null;
    
    /** sub instance name*/
    protected String _appName;
    
    /**
     * CTOR
     * 
     * @param appFullName 
     *            name of the application that module belongs to
     */
    public SessionModule(String appFullName, 
						   SessionsCounter appCounter) {
        
        _appName = appFullName;        
        //_moduleID = _appName + _moduleID;
        _appCounter = appCounter;
        
       
    }
    
    /**
     * Application destroyed, unregister PMI module associated with this
     * application.
     *  
     */
    public void destroy() {
        
    }
   
    /**
    * 
    * @see com.ibm.ws.sip.container.pmi.SessionInterface#incrementSipSessionCount()
    */
    public void incrementSipSessionCount() {
        _appCounter.sipSessionIncrement();
        if (s_logger.isLoggable(Level.FINEST)) {
            StringBuffer buf = new StringBuffer();
            buf.append("ADD SipSessions to applicationName <");
            buf.append(_appName);
            buf.append("> ");
            buf.append(" sipSessionCouner = ");
            buf.append(_appCounter.getSipSessions());
            s_logger.logp(Level.FINEST, SessionModule.class.getName(),
					"incrementSipSessionCount", buf.toString());
        }
    }

    
    /**
     * 
     * @see com.ibm.ws.sip.container.pmi.SessionInterface#decrementSipSessionCount()
     */
    public void decrementSipSessionCount() { 
        _appCounter.sipSessionDecrement();
       
        if (s_logger.isLoggable(Level.FINEST)) {
             StringBuffer buf = new StringBuffer();
             buf.append("REMOVE SipSessions  from applicationName <");
             buf.append(_appName);
             buf.append("> ");
             buf.append(" sipSessionCouner = ");
             buf.append(_appCounter.getSipSessions());
             s_logger.logp(Level.FINEST, SessionModule.class.getName(),
            		 "decrementSipSessionCount",buf.toString());
         }         
    }


    /**
     * 
     * @see com.ibm.ws.sip.container.pmi.SessionInterface#incrementSipAppSessionCount()
     */
    public void incrementSipAppSessionCount() {
        _appCounter.sipAppSessionIncrement();
        
        if (s_logger.isLoggable(Level.FINEST)) {
           StringBuffer buf = new StringBuffer();
            buf.append("ADD Sip Application Sessions to applicationName <");
            buf.append(_appName);
            buf.append("> ");
            buf.append(" sipSessionCouner = ");
            buf.append(_appCounter.getSipAppSessions());
            s_logger.logp(Level.FINEST, SessionModule.class.getName(),
            		"incrementSipAppSessionCount", buf.toString());
        }            
    }

    /**
     * 
     * @see com.ibm.ws.sip.container.pmi.SessionInterface#decrementSipAppSessionCount()
     */
    public void decrementSipAppSessionCount() {
        _appCounter.sipAppSessionDecrement();

        if (s_logger.isLoggable(Level.FINEST)) {
            StringBuffer buf = new StringBuffer();
            buf.append("REMOVE SipApplicationSession from app = ");
            buf.append(_appName);
            buf.append(" _sipAppSessions = ");
            buf.append(_appCounter.getSipAppSessions());
            s_logger.logp(Level.FINEST, SessionModule.class.getName(),
            		"decrementSipAppSessionCount", buf.toString());
        }   
    }

   	/**
     * This method will update PMI counters in WAS
     */
    public void updateCounters(){
        _totalSipAppSessions = _appCounter.getSipAppSessions();
        _totalSipSessions = _appCounter.getSipSessions();
    }
}
