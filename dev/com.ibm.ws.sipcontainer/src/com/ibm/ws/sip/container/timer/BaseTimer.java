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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.failover.ReplicatableImpl;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.util.Queueable;

/**
 * Definition for a general purpose timer. Specific timer types should extend
 * and provide concrete implementation for the invoke method.
 * 
 * @author Amir Perlman Sep 23, 2003
 * @updated Mordechai Dec 9th - added custom serialization method for better perf in ObjectGrid
 *  
 */
//Moti: moved to be externalizable
public abstract class BaseTimer extends ReplicatableImpl implements Runnable, Queueable, Comparable{
	
    
    /** Serialization UID (do not change) */
    static final long serialVersionUID = 3413470086880970783L;

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(BaseTimer.class);

    /**
     * Is Persistent flag.
     */
    protected boolean m_isPersistent;

    /**
     * Is Fixed Delay flag.
     */
    protected boolean m_isFixedDelay;

    /**
     * Flag indicating whether this timer was cancelled.
     */
    protected boolean m_isCancelled;

    /**
     * The time when the Timer was created
     */
    protected long m_startTime;
    
    /**
     * Timer's delay
     */
    protected long m_delay;

    /**
     * Time between successive timer executions.
     */
    protected long m_period;

    /**
     * The last scheduled execution time of this timer,
     * presented to the application by call to {@link #scheduledExecutionTime()}.
     * For repeated timers, this gets updated every time it goes off.
     * -1 before the first execution.
     */
    protected long m_scheduledExecution;

    /**
     * The expected time, in the future, for this timer to go off.
     * Needed for calculating {@link javax.servlet.sip.ServletTimer#getTimeRemaining()}.
     * For repeated timers, this gets updated every time it goes off.
     */
    transient protected long m_nextExecution;

    /**
     * Used to unambiguous identify timer in the Failover table for each
     * SipApplicationSession
     */
    protected int m_timerId;
    
    /**
     * Holds queue index which is used for the invocation on the 
     * application thread.
     */
    protected int m_queueIndex = 0;
    
    /**
     * Used to schedule timer actions
     */
    //Moti: OG: moved to be transient
    private transient SipTimerTask m_timerTask = null;
 
    
    /**
	 * Object that measures the duration of the task in the container queue
	 */
	private TaskDurationMeasurer _sipContainerQueueDuration= null;
	
	/**
	 * Object that measures the duration of the task in the application code
	 */
	private TaskDurationMeasurer _sipContainerApplicationCodeDuration= null;
	
    /**
     * Constructs a new Timer.
     * 
     * @param appSession
     */
    public BaseTimer() {
        m_timerId = -1;
    }
    
    /**
     * Set queue index for this timer
     * @param timerId
     */
    public void setQueueIndex(int queueIndex) {
    	m_queueIndex = queueIndex;
    }
      
    /**
     * This method must be implemented by all the extended classes.
     * It allows the 
     * @return the appropriate queue index where this timer should be run.
     */
    abstract protected int extractQueueIndex() ;
    
    /**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getQueueIndex()
	 */
	public int getQueueIndex() {
		return m_queueIndex;
	}
    
    /**
     * Set Id for this timer
     * @param timerId
     */
    public void setTimerId(int timerId) {
        m_timerId = timerId;
    }
    
    /**
     * @return Returns the m_timerId.
     */
    public int getTimerId() {
        return m_timerId;
    }

    /**
     * @see javax.servlet.sip.ServletTimer#isPersistent()
     */
    public boolean isPersistent() {
        return m_isPersistent;
    }

    /**
     * @see javax.servlet.sip.ServletTimer#scheduledExecutionTime()
     */
    public long scheduledExecutionTime() {
        return m_scheduledExecution;
    }

    /**
     * @see javax.servlet.sip.ServletTimer#isFixedDelay()
     */
    public boolean isFixedDelay() {
        return m_isFixedDelay;
    }

    /**
     * @see javax.servlet.sip.ServletTimer#cancel()
     */
    public void cancel() {
        m_isCancelled = true;
        if(m_timerTask != null){
            m_timerTask.cancel();
        }
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        int rc;
        BaseTimer otherTimer = (BaseTimer) o;
        
        if(o == this)
        {
            rc = 0;
        }
        else if (otherTimer.m_nextExecution < m_nextExecution) {
            rc = 1;
        }
        else if (otherTimer.m_nextExecution > m_nextExecution) {
            rc = -1;
        }
        else if(o.hashCode() < hashCode())
        {
            rc = 1;
        }
        else
        {
            rc = -1;
        }

        return rc;
    }

    /**
     * Initializes a one-time timer
     * 
     * @param isPersistent
     *            if true, the Timer will be reinstated after a shutdown be it
     *            due to complete failure or operator shutdown
     * @param delay
     *            delay in milliseconds before task is to be executed
     */
    public void initTimer(boolean isPersistent, long delay) {
    	initTimer(isPersistent, delay, 0, false);
    }
    
    /**
     * Initializes a repeated timer
     * 
     * @param isPersistent
     *            if true, the Timer will be reinstated after a shutdown be it
     *            due to complete failure or operator shutdown
     * @param delay
     *            delay in milliseconds before task is to fire
     * @param period
     *            time in milliseconds between successive timer
     * @param fixedDelay
     *            if true, the recurring timer is scheduled in a fixed-delay
     *            mode, otherwise in a fixed-rate mode
     */
    public void initTimer(boolean isPersistent, long delay, long period,
                          boolean fixedDelay) {
        m_isPersistent = isPersistent;
        m_startTime = System.currentTimeMillis();
        m_delay = delay;
        m_period = period;
        m_isFixedDelay = fixedDelay;
        m_nextExecution = m_startTime + delay;
        m_scheduledExecution = -1; // undefined before the first expiration
        createTimerTask();

        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "schedule", "Timer to be invoked in "
                                                   + Long.toString(delay)
                                                   + " ms");
        }
    }

    /**
     * Used to create a new TimerTask.
     */
    protected final void createTimerTask(){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "createTimerTask",m_timerTask);
		}

        if(m_timerTask != null){
           //If the BaseTimer was already scheduled and now it is going to 
           //be rescheduled - we should to cancel the previous timer and to create 
           //a new one
           m_timerTask.cancel();
        }
        m_timerTask = new SipTimerTask(this);
    }

    /**
     * Indicate whether the timer has been cancelled.
     * 
     * @return boolean
     */
    public boolean isCancelled() {
        return m_isCancelled;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer(96);
        buffer.append("Timer ,Delay:");
        buffer.append(m_delay);
        buffer.append(" ,Period:");
        buffer.append(m_period);
        buffer.append(" ,Fixed Delay:");
        buffer.append(m_isFixedDelay);
        buffer.append(" ,Cancelled:");
        buffer.append(m_isCancelled);

        return buffer.toString();
    }

    /**
     * called from the java.util.Timer thread when this timer fires
     */
    final void fire() {
    	// 1. make the "originally planned" execution time available to the
    	// application, in case it calls scheduledExecutionTime() from within
    	// the notification event
    	m_scheduledExecution = m_nextExecution;

        // 2. if this is a repeated timer, plan for the next execution time.
    	// this is needed in case the application calls getTimeRemaining().
        if (m_period > 0) {
        	// fixed-delay timer is based on the previous execution.
        	// fixed-rate timer is based on the current system clock.
        	long baseTime = isFixedDelay()
        		? m_nextExecution
        		: System.currentTimeMillis();
           	m_nextExecution = baseTime + m_period;
        }

    	// 3. notify the application
        invoke();
    }

    /**
	 *  @see com.ibm.ws.sip.container.timer.BaseTimer#invoke()
	 */
	protected void invoke() {
		
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "invoke");
		}
		
        if ( !SipContainer.getTasksInvoker().invokeTask( this)){
        	throw new IllegalArgumentException("Failed to dispatch task to SipContainer queue");
        }
		
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "invoke");
		}
	}

    /**
     * Timer's delay.
     * 
     * @return
     */
    public long getDelay() {
        return m_delay;
    }

    /**
     * Time between successive timer executions.
     * 
     * @return
     */
    public long getPeriod() {
        return m_period;
    }

    /**
     * @return Returns the m_startTime.
     */
    public long getStartTime() {
        return m_startTime;
    }

    /**
     * @return the m_timerTask
     */
    public SipTimerTask getTimerTask() {
        return m_timerTask;
    }
    
    /************************************************************************************
    *
    *                 Custom serialization to be used with ObjectGrid
    *
    ************************************************************************************/
    /**
     * Help customize serialization
     * @author mordechai
     * */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "writeObject");
    	}
    	out.writeBoolean(m_isPersistent);
    	out.writeBoolean(m_isFixedDelay);
    	out.writeBoolean(m_isCancelled);
    	out.writeLong(m_startTime);
    	out.writeLong(m_delay);
    	out.writeLong(m_period);
    	out.writeLong(m_scheduledExecution);
    	out.writeInt(m_timerId);
    }

    /**
     * Help customize de-serialization
     * @author mordechai
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "readObject");
    	}
    	m_isPersistent = in.readBoolean();
    	m_isFixedDelay = in.readBoolean();
    	m_isCancelled = 	in.readBoolean();
    	m_startTime =  	in.readLong();
    	m_delay = in.readLong();
    	m_period = 	in.readLong();
    	m_scheduledExecution =	in.readLong();
    	m_timerId = in.readInt();
    	if (!m_isCancelled) {
    		createTimerTask();
    	}
    }
    
    /**
     * Help customize serialization
     * @author mordechai
     * */
    public void writeExternal(ObjectOutput out) throws IOException     
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "writeExternal");
    	}
		//Moti: disabled serialization to see if its our bottleneck.
    	out.writeBoolean(m_isPersistent);
    	out.writeBoolean(m_isFixedDelay);
    	out.writeBoolean(m_isCancelled);
    	out.writeLong(m_startTime);
    	out.writeLong(m_delay);
    	out.writeLong(m_period);
    	out.writeLong(m_scheduledExecution);
    	out.writeInt(m_timerId);
    }

    /**
	 * An expiration timer task is critical to prevent memory leaks of unexpired sessions.
	 * @see com.ibm.ws.sip.container.util.Queueable#priority()
	 */
	public int priority(){
		return PRIORITY_CRITICAL;
	}
    /**
     * Help customize de-serialization
     * @author mordechai
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "readExternal");
    	}
		//Moti: disabled serialization to see if its our bottleneck.
    	m_isPersistent = in.readBoolean();
    	m_isFixedDelay = in.readBoolean();
    	m_isCancelled = 	in.readBoolean();
    	m_startTime =  	in.readLong();
    	m_delay = in.readLong();
    	m_period = 	in.readLong();
    	m_scheduledExecution = in.readLong();
    	m_timerId = in.readInt();
    	if (!m_isCancelled) {
    		createTimerTask();
    	}
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

	
	@Override
	public Object getServiceSynchronizer() {
		return null;
	}
	
	@Override
	public void store() {
		// Only relevant for application timer
		
	}

	@Override
	public void removeFromStorage() {
		// Only relevant for application timer
		
	}

}