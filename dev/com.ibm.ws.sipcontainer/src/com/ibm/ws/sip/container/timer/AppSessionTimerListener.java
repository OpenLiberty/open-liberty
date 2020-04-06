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
package com.ibm.ws.sip.container.timer;

import java.util.Iterator;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;

/**
 * @author Amir Perlman, Jul 20, 2003
 *
 * Implementation of the Servlet Timer API. 
 */
public class AppSessionTimerListener extends ExpirationTimerListener
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(AppSessionTimerListener.class);
    
    /**
     * Reference to the expiration Timer
     */
    private ExpirationTimer m_timer;

    /**
     * Application Session associated with this timer. 
     */
    private SipApplicationSessionImpl m_appSession;

    /**
     * Constructs a new Servlet Timer associated with the given application 
     * session. 
     * @param appSession
     */
    public AppSessionTimerListener(SipApplicationSessionImpl appSession,ExpirationTimer timer)
    {
        m_appSession = appSession;        
        if(timer == null){
        	m_timer = new ExpirationTimer(this, m_appSession.getSynchronizer());
        }
        else{
        	m_timer = timer;
        	m_timer.setExpInvoker(this);
        }
    }
 
    /**
     * schedule timer event according to the session's expiration time (ttl)
     */
    private void scheduleAppSessionTimer()
    {
    	synchronized (m_appSession.getSynchronizer()) {//This will synchronize with the m_timer.cancel() method
	        if(!m_timer.isCancelled()){// check To avoid the exception
	        	long expires = m_appSession.getExpires();
	            long delta = expires - System.currentTimeMillis();
	            if (delta < 0)
	            {
	                delta = 1;
	            }
		        //reschedule the time for next time the session will expire.
	            SipContainerComponent.getTimerService().schedule(m_timer, false, delta);
		
		        if (c_logger.isTraceDebugEnabled())
		        {
		            StringBuffer b = new StringBuffer(64);
		            b.append("App Session scheduled to expire in ");
		            b.append(delta);
		            b.append(" ms, App Session: ");
		            b.append(m_appSession);
		            c_logger.traceDebug(
		                this,
		                "scheduleAppSessionTimer",
		                b.toString());
		        }
	        }
	        else{
	        	c_logger.traceDebug(
		                this,
		                "scheduleAppSessionTimer",
		                "Cannot reschedule expiration timer. Timer already canceled. ApplicationSession might have been invalidated");
	        }
        }
    }

    /**
     * re-schedule existing timer event
     */
    public void rescheduleAppSessionTimer()
    {
        long expires = m_appSession.getExpires();
        long delta = expires - System.currentTimeMillis();
        if (delta < 0)
        {
            delta = 1;
        }
        //reschedule the time for next time the session will expire. 
        SipContainerComponent.getTimerService().schedule(m_timer, false, delta);

        if (c_logger.isTraceDebugEnabled())
        {
            StringBuffer b = new StringBuffer(64);
            b.append("App Session re-scheduled to expire in ");
            b.append(delta);
            b.append(" ms, App Session: ");
            b.append(m_appSession);
            c_logger.traceDebug(
                this,
                "reschedualeAppSessionTimer",
                b.toString());
        }
    }

    /**
     * Checks if the session expiration time has passed. 
     * 
     * @return true if expired otherwise false
     */
    private boolean isAppSessionExpired()
    {
        //Test if value is within 16 ms to avoid putting short timers back 
        //into the tree set for such a short time. 
    	if(m_appSession == null){
    		//Meaning that the expiration timer was canceled, and that happens only 
    		//on AppSesion invalidate.
    		return true;
    	}
        return (m_appSession.getExpires() < System.currentTimeMillis() + 16);
    }

    /**
     * Send Session Expired Event to Application Session Listeners.  
     */
    private void sendSessionExpiredEvt()
    {
        //Get the Application Session Listener from the application's descriptor
        SipAppDesc desc = m_appSession.getAppDescriptor();

        //We will not have a Sip Add Descriptor in case the app session 
        //was created from the factory. Seems like a hole in the Sip Servlets
        //API. 
        if (null != desc)
        {
        	Iterator iter = desc.getAppSessionListeners().iterator();
        	if(!iter.hasNext()){
        		return;
        	}
            SipApplicationSessionEvent evt =
                new SipApplicationSessionEvent(m_appSession);
            //Invoke listeners - a notification is sent to allow listeners
            //to extend the session's expiration time. 
            ContextEstablisher contextEstablisher = 
            	desc.getContextEstablisher();
            ClassLoader currentThreadClassLoader =null;
			try{
				if( contextEstablisher != null){
					currentThreadClassLoader = contextEstablisher.getThreadCurrentClassLoader();
					contextEstablisher.establishContext();
				}
				
	            while (iter.hasNext())
	            {
	                ((SipApplicationSessionListener) iter.next()).sessionExpired(
	                    evt);
	            }
			} finally{
				if( contextEstablisher != null){
					contextEstablisher.removeContext(currentThreadClassLoader);
				}
			}
        }
    }
      
    /**
     * @see com.ibm.ws.sip.container.timer.BaseTimer#cancel()
     */
    public void cancel() {
    	synchronized (m_appSession.getSynchronizer()) {
    		m_timer.cancel();
            m_appSession = null;
		}
    }
    
    /**
     * This method might be executed multi-threaded or single threaded,
     * depends on the TasksInvoker definition 
     * @see com.ibm.ws.sip.container.events.TasksInvoker
     * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
     * @see java.lang.Runnable#run()
     */
	protected void invokeExpiredTimer() {
		if (m_appSession.isValid())
        {
            //Check if expiration time has passed. We might had the time
            //extended after the time has already been set. 
            if (isAppSessionExpired())
            {
                //Notify listeners that the session expired. 
                sendSessionExpiredEvt();

                //Check if session life time has been extended by the listeners.  
                if (isAppSessionExpired())
                {
                    //Session expired - Invalidate. 
                    try {
                    	if( m_appSession != null){//if null means already invalidated on app listener
                    		m_appSession.invalidate();
                    	}
                    } catch (IllegalStateException e) {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger
                                .traceDebug(this, "invoke",
                                            "SipApplication session was already invalidated");
                        }
                    }
                }
                else
                {
                    //reschedule next time event according the session's ttl
                    scheduleAppSessionTimer();
                }
            }
            else
            {
                scheduleAppSessionTimer();
            }
        }
        else
        {
            //Session has already been invalidated. Just ignore. 
        }
	}

	/**
	 * Returns Expires of this object
	 */
	public long getExpires() {
		return m_appSession.getExpires();
	}

	/**
	 * Returns ID of this Application Session 
	 */
	public String getId() {
		return m_appSession.getSharedId();
	}


	/**
	 * Schedule this timer
	 * @param delay
	 */
	public void schedule(boolean isPersistent,long delay) {
        SipContainerComponent.getTimerService().schedule(m_timer,isPersistent,delay);
	}

	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationId()
	 */
	public String getApplicationId() {
		return getId();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationName()
	 */
	public String getApplicationName() {
		if(m_appSession != null && m_appSession.getAppDescriptor() != null) {
			return m_appSession.getAppDescriptor().getAppName();
		}
		return null;
	}
	
	
	
	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getApplicationIdForPMI()
	 */
	public Integer getApplicationIdForPMI() {
		if(m_appSession != null && m_appSession.getAppDescriptor() != null) {
			return m_appSession.getAppDescriptor().getAppIndexForPmi(); 
		}
		return null;
	}

	/**
	 * Calculate and returns expiration time
	 * @return The time is returned as the number of milliseconds since midnight 
     * January 1, 1970 GMT. 
	 */
	public long getExpirationTime(){
		return m_timer.getStartTime() + m_timer.getDelay();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.timer.ExpirationTimerListener#getServiceSynchronizer()
	 */
	public Object getServiceSynchronizer(){
		return m_appSession.getServiceSynchronizer();
	}
	
	/**
	 * @return the sip application session
	 */
	SipApplicationSession getApplicationSession() {
		return m_appSession;
	}
}
