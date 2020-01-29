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

import java.util.Vector;

/**
 * @author Amir Perlman, Jun 3, 2005
 * 
 * Base class for logging a sequence of events in a context of a single scenario. 
 * Provides the utility functions required for concrete implementations of the 
 * derived classes
 */
public abstract class SequenceLogger {

    /**
     * List of all currentlly logged states
     */
    private Vector _log = new Vector();
    
    /**
     * Unique Identifier for this logging context
     */
    private Object _id; 
    
    /**
     * Accumulated level of serverity 
     */
    protected int _severity;
    
    /**
     * Logger's threshold for dumping logs to output, a logger with a severity 
     * level which equals or greater then this value will be printed to log.
     * Default value is WARNING.  
     */
    protected static int c_logThreshold = LogEvent.WARNING;
    
    /**
     * Indicates whether logger contents will be dumped to log immeadiatly when
     * the threshold level is exceeded or wait for the completed indication.
     */
    protected static boolean c_dumpCompleteLogsOnly = false;

    
    /**
     * Default constructor. 
     *
     */
    public SequenceLogger()
    {
    }
    
    /**
     * Set the Id of the object associated with this context
     * @param id
     */
    public void setId(Object id)
    {
        _id = id; 
    }
    
    /**
     * Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     * @param info Additional information that will kept with a strong reference
     * so be aware of memory usage implications. 
     * @param extendedInfo Extended information that will be softly referenced
     * so be aware that its contents might not be available when the log is dumped 
     */
    public void log(int state, Object info, Object extendedInfo)
    {
        ExtendedInfoLogEvent logE = 
            (ExtendedInfoLogEvent) ExtendedInfoLogEvent.getInstance();
        logE.update(state, info, extendedInfo);
        
        addToLog(logE);
    }
    
    /**
     * Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     * @param info Additional information - primitive type.
     * @param extendedInfo Extended information that will be softly referenced
     * so be aware that its contents might not be available when the log is dumped 
     */
    public void log(int state, int info, Object extendedInfo)
    {
        ExtendePrimitiveLogEvent logE = 
            (ExtendePrimitiveLogEvent) ExtendePrimitiveLogEvent.getInstance();
        logE.update(state, info, extendedInfo);
        
        addToLog(logE);
    }
    
    /** Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     * @param info Additional information that will kept with a strong reference
     * so be aware of memory usage implications.
     */  
    public void log(int state, Object info)
    {
        BasicInfoLogEvent logE = 
            (BasicInfoLogEvent) BasicInfoLogEvent.getInstance();
        logE.update(state, info);
        
        addToLog(logE);
    }
    
    /** Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     */  
    public void log(int state)
    {
        LogEvent logE = LogEvent.getInstance();
        logE.update(state);
        
        addToLog(logE);
    }
    
    /** Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     * @param info Additional information that will associated with the event
     */  
    public void log(int state, int info)
    {
        PrimitiveLogEvent logE = 
            (PrimitiveLogEvent) PrimitiveLogEvent.getInstance();
        logE.update(state, info);
        
        addToLog(logE);
    }
    
    
    /** Add a log message to context. 
     * @param state Should composed of one of the know states 
     * (e.g ERROR, WARNING, COMPLETED, NORMAL defined by Log Event) combined with 
     * a unique value within the scope of the scenario being logged. 
     * @param info Additional boolean information that will be associated with 
     * the event
     */  
    public void log(int state, boolean info)
    {
        PrimitiveLogEvent logE = 
            (PrimitiveLogEvent) PrimitiveLogEvent.getInstance();
        logE.update(state, info);
        
        addToLog(logE);
    }
    
    /**
     * Add the specified log event to log. Updates the log's severity and 
     * dumps content if needed. 
     * @param logE
     */
    private void addToLog(LogEvent logE) {
        _log.add(logE);
        _severity |= (logE._state & LogEvent.ALL_LEVELS);
    
        dumpIfNeeded();
    }
    
    /**
     * Clear all log associated with this context. 
     */
    public void clear()
    {
        for(int i=0; i < _log.size(); i++)
        {
            ((LogEvent)_log.elementAt(i)).returnToPool();
        }
        
        _log.clear();
        _id = null;
        _severity = 0;
    }
    
    /**
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        dump(buf);
        return buf.toString(); 
    }
    
    
    /**
     * Dump the entire log to the specified buffer. 
     * @param buf
     */
    public void dump(StringBuffer buf)
    {
        buf.append("\n$$$\t");
        buf.append(getDesc());
        buf.append('\t');
        
        if(_id != null)
        {
            buf.append("id: ");
            buf.append(_id);
            buf.append('\n');
            
            buf.append("Severity: ");
            LogEvent.dumpLevelAsString(buf, _severity);
            buf.append('\n');
            
            buf.append("Log length: ");
            buf.append(_log.size());
            buf.append('\t');
            buf.append("is complete: ");
            buf.append((_severity & LogEvent.COMPLETED) == LogEvent.COMPLETED ? "true" : "false");
            buf.append("\n\n");
            
        }
        
        for(int i=0; i<_log.size(); i++)
        {
            buf.append('(');
            buf.append(i);
            buf.append(") ");
            ((LogEvent)_log.elementAt(i)).dump(buf, this);
            buf.append('\n');
        }
        
        buf.append("\t~~~\n");
        
    }
    
    /**
     * Dump a description of the specified state into the given buffer. Derived
     * classes should overide and provide more specific description. 
     */
    public void dumpStateDesc(int state, StringBuffer buf)
    {
        buf.append(state);
    }
    
    /**
     * Dump context's content to log if needed. Log will be dumped when the 
     * following conditions are met: 
     * 1. Severity is equal to or more then the current threshold. 
     * 2. One of the following is true:  
     * 	a. The log's severity level has at least one event marked with a completed
     * 	   state.
     *  b. The global setting for dumping uncompleted logs is set to true.   
     */
    protected void dumpIfNeeded() {
        if( c_dumpCompleteLogsOnly && 
            (_severity & LogEvent.COMPLETED) != LogEvent.COMPLETED)
        {
            //We have not reached the completed state, dont dump yet
            return;
        }
        
        if(_severity >= c_logThreshold)
        {
            StringBuffer buf = new StringBuffer();
            dump(buf);
            System.out.println(buf.toString());
        }
    }

    /**
     * Get the Logger's description
     * @return
     */
    protected static String getDesc() {
        return  "Sequence Logger";
    }
}
