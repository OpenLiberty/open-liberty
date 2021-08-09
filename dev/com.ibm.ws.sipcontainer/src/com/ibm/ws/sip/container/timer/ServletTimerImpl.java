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

import java.io.Serializable;

import javax.servlet.sip.ServletTimer;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.TimerListener;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.failover.Replicatable;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.Queueable;
//TODO Liberty import com.ibm.wsspi.sip.hamanagment.logicalname.ILogicalName;

/**
 * @author Amir Perlman, Jul 20, 2003
 *
 * Implementation of the Servlet Timer API. 
 */
public class ServletTimerImpl extends BaseTimer implements ServletTimer,
    Runnable, Replicatable, Queueable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	/**     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ServletTimerImpl.class);

    /**
     * Application Session associated with this timer. 
     */
    private transient SipApplicationSessionImpl m_appSession;
    
    /**
     * Info associated with the event. 
     * NOTICE: this object is deserialized when called, it shouldn't be accessed
     * directly but just through the proper getter 
     */
    private Serializable m_info;
    
	private transient boolean _isRunning = false;
    
	/**
	 * Object that measures the duration of the task in the container queue
	 */
	private TaskDurationMeasurer _sipContainerQueueDuration= null;
	
	/**
	 * Object that measures the duration of the task in the application code
	 */
	private TaskDurationMeasurer _sipContainerApplicationCodeDuration= null;
	
    /**
     *  @see javax.servlet.sip.ServletTimer#getId()
     */
    public String getId(){
    	return Integer.toString(getTimerId());
    }
    
    /**
     * Constructs a new Servlet Timer associated with the given application 
     * session. 
     * @param appSession
     */
    public ServletTimerImpl(SipApplicationSessionImpl appSession,
            Serializable info) {
        m_appSession = appSession;
        m_info = info;
        setTimerId(m_appSession.getNextTimerId());
        setSharedId(m_appSession.getSharedId());
    	setQueueIndex(extractQueueIndex());
    }

    /**
     * @see javax.servlet.sip.ServletTimer#getApplicationSession()
     */
    public SipApplicationSession getApplicationSession() {
        return m_appSession;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(96);
        buffer.append("Timer, App Session:")
        			.append(m_appSession)
        			.append(" ,Delay:")
			        .append(getDelay())
			        .append(" ,Period:")
			        .append(getPeriod())
			        .append(" ,Fixed Delay:")
			        .append(isFixedDelay())
			        .append(" ,Cancelled:")
			        .append(isCancelled())
        			.append(" ,info:").append(getInfo());

        return buffer.toString();
    }

       
    /**
     * @see javax.servlet.sip.ServletTimer#getInfo()
     */
    public Serializable getInfo() {
        return m_info;
    }

    /**
     * @see javax.servlet.sip.ServletTimer#cancel()
     */
    @Override
    public void cancel() {
    	if (isCancelled()) {
    		return;
    	}

    	if (!_isRunning) {
    		super.cancel();
    		m_appSession.removeTimer(this);
    	} else {
    		cancelWithoutRemove();
    	}

    }
    
    /**
     * Cancel the timer without remove it from the AppSession
     *  
     */
    public void cancelWithoutRemove() {
        super.cancel();
        setDirty();
        store();
    }

    /**
     * This method might be executed multi-threaded or single threaded,
     * depends on the TasksInvoker definition 
     * @see com.ibm.ws.sip.container.events.TasksInvoker
     * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
     * @see java.lang.Runnable#run()
     */
	public void run() {
//		Get the Timer Listener from the application's descriptor
        SipAppDesc desc = m_appSession.getAppDescriptor();

        //Invoke listener
        TimerListener timerL = desc.getTimerListener();
        if(c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug("ServletTimerImpl.run(): timerListener=" + timerL);
        }
        
        if (null != timerL) {
        	
        	ClassLoader cl = Thread.currentThread().getContextClassLoader();
        	ContextEstablisher contextEstablisher = null;
        	
        	try {
            	//We are now establishing the application context for the thread that will invoke the 
        		//listener. Note that the context establisher may be null on a standalone environment.
            	contextEstablisher = desc.getContextEstablisher();
            	if (contextEstablisher != null) {
            		contextEstablisher.establishContext(timerL.getClass().getClassLoader());
            	}
        		
            	//Call listener event
            	if(c_logger.isTraceDebugEnabled()){
                	c_logger.traceDebug("ServletTimerImpl.run(): calling timeout timerListener="
                			+ timerL + "ServletTimer=" + this);
                }
            	
            	_isRunning = true;

                timerL.timeout(this);
        	} catch (Throwable t) {
				if (c_logger.isErrorEnabled()) {
		    		c_logger.error("Fail to run timeout method", null, null, t);
		    	}				
        	} finally {
            	_isRunning = false;      

            	if (getPeriod() <= 0 && !isCancelled()) {
            		//The one-time timer task was executed, so this timer 
            		//must be removed from appSession and from replication
            		m_appSession.removeTimer(this);
            	}
        		
            	if (isCancelled() && m_appSession.isValid()){
            		m_appSession.removeTimer(this);        	
            	}else if (isCancelled()){
            		//the timer was canceled and the app session is not valid, we need 
            		//to remove the timer from the session repository map directly
            		if(c_logger.isTraceDebugEnabled()){
                    	c_logger.traceDebug("appsession already invalidated, removing timer directly from the session repository, timerId: " + getId() + ", appSession: " + m_appSession.getId());
                    }
            		removeFromStorage();
            	}
        		
                //Remove the application specific context from thread
                if (contextEstablisher != null) {
                	contextEstablisher.removeContext( cl);
                }        		
        	}
        }
        else {
            if (c_logger.isWarnEnabled()) {
                Object[] args = { desc.getApplicationName() };
                c_logger.warn("warning.timer.listener.unavailable",
                              Situation.SITUATION_CREATE, args);
            }
        }
	}
	
	/**
	 * Setting the ApplicationSession
	 * @param appSession
	 */
	public void setAppSession( SipApplicationSessionImpl appSession){
		m_appSession = appSession;
	}
	
    
    /**
     * @see com.ibm.ws.sip.container.failover.Replicatable#store()
     */
	public void store() {
		SessionRepository.getInstance().put(m_appSession, this);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#notifyOnActivation()
	 */
	public void notifyOnActivation() {
		//no notification needed
	}
	
    /**
     * Returns the hash code of the applicationId 
     */
    protected int extractQueueIndex() {
		if(m_appSession != null){
			//Moti: fix for defect 487485
			return m_appSession.extractAppSessionCounter();
		}
		return 0;
	}
    
    /**
     * @see com.ibm.ws.sip.container.util.Queueable#priority()
     */
	public int priority() {
		return PRIORITY_NORMAL;
	}

	/**
	 * @see javax.servlet.sip.ServletTimer#getTimeRemaining()
	 */
	public long getTimeRemaining() {
		long scheduled = m_nextExecution;
		long now = System.currentTimeMillis();
		long remaining = scheduled - now;

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getTimeRemaining",
				"scheduled [" + scheduled +
				"] now [" + now +
				"] remaining [" + remaining + ']');
		}
		return remaining;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getServiceSynchronizer()
	 */
	public Object getServiceSynchronizer() {
		return m_appSession.getServiceSynchronizer();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getSipContainerQueueDuration()
	 */
	public TaskDurationMeasurer getSipContainerQueueDuration() {
		return _sipContainerQueueDuration;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getApplicationCodeDuration()
     */
	public TaskDurationMeasurer getApplicationCodeDuration() {
		return _sipContainerApplicationCodeDuration;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
	 */
	public String getAppName() {
		if(m_appSession != null && m_appSession.getAppDescriptor() != null) {
			return m_appSession.getAppDescriptor().getApplicationName();
		}
		else return null;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.getAppIndexForPMI()
	 */
	public Integer getAppIndexForPMI() {
		if(m_appSession != null && m_appSession.getAppDescriptor() != null) {
			return m_appSession.getAppDescriptor().getAppIndexForPmi();
		}
		else return null;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setSipContainerQueueDuration(TaskDurationMeasurer)
     */
	public void setSipContainerQueueDuration(TaskDurationMeasurer tm) {
		_sipContainerQueueDuration = tm;
		
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setApplicationCodeDuration(TaskDurationMeasurer)
     */
	public void setApplicationCodeDuration(TaskDurationMeasurer tm) {
		_sipContainerApplicationCodeDuration = tm;
		
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		return null;
	}

	@Override
	public void removeFromStorage() {
		SessionRepository.getInstance().removeTimer(m_appSession.getId(), this);
		
	}
}
