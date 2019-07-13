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
package com.ibm.ws.sip.container.timer;

import java.io.Serializable;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.TimerService;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;

/**
 * An implementation of the SIP Servlets spec TimerService
 * 
 * This implementation is intended to be called by the external application code only.
 * 
 * @author Nitzan Nissim
 */
public class ExternTimerService implements TimerService {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ExternTimerService.class);
    
    /**
     * The container timer service
     */
	private BaseTimerService timerService = SipContainerComponent.getTimerService();

	/**
	 * Singleton
	 */
	private static ExternTimerService instance = new ExternTimerService();
	
	/**
	 * Singleton instance
	 * @return
	 */
	public static ExternTimerService getInstance() {
		return instance;
	}

	/**
     * @see javax.servlet.sip.TimerService#createTimer(javax.servlet.sip.SipApplicationSession, 
     * 								long, boolean, java.io.Serializable)
     */
    public ServletTimer createTimer(
        SipApplicationSession appSession,
        long delay,
        boolean isPersistent,
        Serializable info)
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            Object[] params =
                {
                    appSession,
                    new Long(delay),
                    new Boolean(isPersistent),
                    info };
            c_logger.traceEntry(this, "createTimer", params);
        }
        SipApplicationSessionImpl appSessionImpl = 
            					(SipApplicationSessionImpl) appSession;
        
        checkIfSessionValid(appSessionImpl);
        
        ServletTimerImpl timer = null;
        timer =
            new ServletTimerImpl((SipApplicationSessionImpl) appSession, info);
        timerService.schedule(timer, isPersistent, delay);
        appSessionImpl.addTimer(timer);
        
        return timer;
    }

    /**
     * @see javax.servlet.sip.TimerService#createTimer(
     * 						javax.servlet.sip.SipApplicationSession, 
     * 						long, long, boolean, boolean, java.io.Serializable)
     */
    public ServletTimer createTimer(
        SipApplicationSession appSession,
        long delay,
        long period,
        boolean fixedDelay,
        boolean isPersistent,
        Serializable info)
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            Object[] params =
                {
                    appSession,
                    new Long(delay),
                    new Long(period),
                    new Boolean(fixedDelay),
                    new Boolean(isPersistent),
                    info };
            c_logger.traceEntry(this, "createTimer", params);
        }
        
        SipApplicationSessionImpl appSessionImpl = 
								 (SipApplicationSessionImpl) appSession;

        checkIfSessionValid(appSessionImpl);
        
        ServletTimerImpl timer = null;
        timer =
            new ServletTimerImpl((SipApplicationSessionImpl) appSession, info);
        timerService.schedule(timer, isPersistent, delay, period, fixedDelay);
        appSessionImpl.addTimer(timer);
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "createTimer", timer);
        }
        return timer;
    }
    
    /**
     * Checks whether the Session is a valid which means it was not invalidated. 
     * @param appSessionImpl 
     * @throws IllegalStateException In case the session is either null or invalid
     */
    private void checkIfSessionValid(SipApplicationSessionImpl appSessionImpl) 
    											throws IllegalStateException
    {
        if(appSessionImpl == null || !appSessionImpl.isValid())
        {
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, 
                        	"createTimer", 
                        	"Null or Invalid App Session: " + appSessionImpl);
            }
            throw new IllegalStateException("Session is null or invalid " + 
					appSessionImpl);
        }
        else 
        {
            SipAppDesc appDesc = appSessionImpl.getAppDescriptor();
            if(appDesc == null || appDesc.getTimerListener() == null)
            {
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, 
                       "createTimer", 
                       "Timer listener not associated with Application Session  " 
                       + appDesc);
                }
                throw new IllegalStateException(
                    "Timer Listener not associated with Application Session " + 
                    appDesc);
            }
        }
    }

    
}
