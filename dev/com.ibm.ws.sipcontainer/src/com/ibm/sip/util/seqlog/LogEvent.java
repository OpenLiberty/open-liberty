/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sip.util.seqlog;

import java.util.Calendar;

import com.ibm.ws.sip.parser.util.ObjectPool;

/**
 * @author Amir Perlman, Jun 3, 2005
 *
 * Abstract base class for all log events represented by state (int value) without
 * any additional data attached to the event. 
 */
public class LogEvent {
    
    //
    //Constants indicating log levels/severites
    //
    public static final int NORMAL  	 =     0x00000100;
    public static final int COMPLETED    =	   0x00001000;
    public static final int WARNING 	 =     0x00010000;
    public static final int ERROR   	 = 	   0x00100000;
    
    
    public static final int ALL_LEVELS = NORMAL | WARNING | ERROR | COMPLETED;
    
    /**
     * The state of the current event. Each state's value  is composed of one of 
     * the known severities (normal, completed, warning, error) and a unique 
     * value assigned to it.  
     */
    protected int _state = -1; 
    
    /**
     * Current time
     */
    protected long _time; 
    
    /**
     * Time of current date at 00:00 in miliseconds. 
     */
    private static long c_baseTime;
    
    /**
     * Pool of reusable log events. 
     */
    private static ObjectPool c_pool = new ObjectPool(LogEvent.class, null, -1);
    
    static 
    {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.MILLISECOND, 0);
        
       c_baseTime = c.getTimeInMillis();
    }
    
    /**
     * Default constructor.  
     */
    public LogEvent() {

    }
    
    /**
     * Dump the object's content into the specified buffer. 
     * @param buf
     */
    protected void dump(StringBuffer buf, SequenceLogger contextLogger) {
        dumpTime(buf);
        buf.append("  ");
        dumpLevelAsString(buf, _state);
        
        buf.append(", State:");
        contextLogger.dumpStateDesc(_state, buf);
    }

    /**
     * Helper function for dumping the time in a text format. 
     * @param buf buffer to dump the output
     */
    protected void dumpTime(StringBuffer buf) {
        int t = (int)(_time - c_baseTime);
        int ms = t % 1000;
        t = t /1000;
        int sec = t % 60;
        
        t = t / 60;
        int min = t % 60;
        
        t = t / 60;
        int hrs = t % 60;
        
        buf.append(hrs);
        buf.append(':');
        
        buf.append(min);
        buf.append(':');
        
        buf.append(sec);
        buf.append(':');
        
        buf.append(ms);
        
    }

    /**
     * Helper function. Dumps the level of this state context in a text form. 
     * @param buf
     */
    public static void dumpLevelAsString(StringBuffer buf, int state) {
        if ((state & ERROR) == ERROR) {
            buf.append("Error");
        }
        else if ((state & WARNING) == WARNING) {
            buf.append("Warning");
        }
        else if ((state & COMPLETED) == COMPLETED) {
            buf.append("Completed");
        }
        else if ((state & NORMAL) == NORMAL) {
            buf.append("Normal");
        }
        else {
            buf.append(Integer.toHexString(state & ALL_LEVELS));
        }
    }
    
    /**
     * Get an instance of a log event from the pool of available instances
     * 
     * @return
     */
    protected static LogEvent getInstance() {
        return (LogEvent) c_pool.get();
    }

    /**
     * Return this instance to the pool of reusable Log Events. 
     */
    protected void returnToPool() {
        clear();
        c_pool.putBack(this);
    }

    /**
     * Clear the event's content. Should be called before it is put back into
     * the pool. 
     */
    protected void clear() {
        _state = -1;
        _time = -1;
    }

    /**
     * Update the log event with the specified state and will 
     * also set the time of the event. 
     * @param state
     * @param info
     */
    protected void update(int state) {
        _state = state;
        _time = System.currentTimeMillis();
    }
}
