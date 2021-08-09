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
package com.ibm.sip.util.log;

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class wraps the <code>TraceComponent</code> component for logging.
 * 
 * @author Galina R, Dec 22, 2013
 */

// General comment:
// We are using the TraceComponent methods without object_id 
// For example: 
//	- using Tr.debug(TraceComponent tc, String msg, Object... objs)
//	instead of :
//	- Tr.debug(Object id, TraceComponent tc, String msg, Object... objs)
// The printouts with object_id show the ID number that we don't need
// Example:
// [12/25/13 16:15:30:518 IST] 0000001b id=657c657c com.ibm.ws.sip.container.was.CommonWebsphereAppLoadListener  1 SIP container activated        	

public class LogMgr
{   
	private TraceComponent _tc;
	
	/**
	 * Ctor
	 * @param tc
	 */
	protected LogMgr(TraceComponent tc) {
		_tc = tc;
	}

	/**
    * Use to record when entering a method. Shown on level FINER. 
    * @param obj Calling object
    * @param method Method name
    * @param param Methods parameter
    */
    public void traceEntry(Object obj, String method, Object param) {
    	if (_tc.isEntryEnabled()) {
    		Tr.entry(_tc, method, param);
    	}
    }
    
    /**
     * Use to record when entering a method. Shown on level FINER. 
     * @param obj Calling object
     * @param method Method name
     * @param params Methods parameters
     */
     public void traceEntry(Object obj, String method, Object... params) {
    	 if (_tc.isEntryEnabled()) {
    		 Tr.entry(_tc, method, params);
    	 }
     }
     
    /**
     * Use to record when entering a method. Shown on level FINER.
     * @param obj Calling object
     * @param method Method name
     */
    public void traceEntry(Object obj, String method) {
    	if (_tc.isEntryEnabled()) {
    		Tr.entry(_tc, method);
    	}
    }

    /**
     * Use for debug level messages. Shown on level FINEST.
     * @param obj Calling object
     * @param method Method name
     * @param msg Message to write
     */
    public void traceDebug(Object obj, String method, String msg) {
        if (_tc.isDebugEnabled()) {
        	Tr.debug(_tc, method + "  " + msg);
        }
    }
    
    /**
     * Use for debug level messages. Shown on level FINEST.
     * @param msg Message to write
     * @param objs Parameters to print
     */
    public void traceDebug(String message, Object... objs) {
        if (_tc.isDebugEnabled()) {
        	Tr.debug(_tc, message, objs);
        }
    }
    
    /**
     * Use for event level messages. Shown on level FINE.
     * @param msg Message to write
     * @param params Parameters to print
     */
    public void event(String message, Object... objs) {
        if (_tc.isEventEnabled()) {
        	Tr.event(_tc, message, objs);
        }
    }
    
    /**
     * Use for debug level messages. Shown on level FINEST.
     * @param msg Message to write
     */
    public void traceDebug( String msg) {
        if (_tc.isDebugEnabled()) {
        	Tr.debug(_tc, msg);
        }
    }
    
    /**
     * Use for debug level messages, when exception is caught. Shown on level FINEST.
     * @param obj Calling object
     * @param method Method name
     * @param msg Message to write
     * @param e Throwable caught
     */
    public void traceDebug(Object obj, String method, String msg, Throwable e) {
        if (_tc.isDebugEnabled()) {
        	if (e != null) {
        		Tr.debug(_tc, method + "  " + msg, e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        	} else {
            	Tr.debug(_tc, method + "  " + msg, "null");        		
        	}
        }
    }

    /**
     * @see #traceFailure(Object, String, String)
     * @param msg The message to write, null allowed
     */
    public void traceFailure(String msg) {
        if (_tc.isEventEnabled()) {
        	Tr.event(_tc, msg);
        }
    }

    /**
     * @see #traceFailure(Object, String, String)
     * @param obj The calling object
     * @param method The method name
     * @param msg The message to write, null allowed
     */
    public void traceFailure(Object obj, String method, String msg) {
        if (_tc.isEventEnabled()) {
        	Tr.event(_tc, method + "  " + msg);
        }
    }

    /**
     * Used for capturing failure that is subject to end-user activity.
     * Such errors are typically originated by end-users.
     * Active on level FINE.
     * This log level is particularly useful when running under high load,
     * while the number of failures is relatively small.
     * @param obj The calling object
     * @param method The method name
     * @param msg The message to write, null allowed
     * @param e The exception caught
     */
    public void traceFailure(Object obj, String method, String msg, Throwable e) {
        if (_tc.isEventEnabled()) {
        	Tr.event(_tc, method + "  " + msg, e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Use to record when exiting a method. Shown on level FINER. 
     * @param obj Calling object
     * @param method Method name
     */
    public void traceExit(Object obj, String method) {
    	if (_tc.isEntryEnabled()) {
    		Tr.exit(_tc, method);
    	}
    }
    
    /**
     * Use to record when exiting a method. Shown on level FINER. 
     * @param obj Calling object
     * @param method Method name
     * @param param Methods parameter
     */
    public void traceExit(Object obj, String method, Object param) {
    	if (_tc.isEntryEnabled()) {
    		Tr.exit(_tc, method, param);
    	}
    }
    
    /**
     * Use to record when exiting a method. Shown on level FINER. 
     * @param obj Calling object
     * @param method Method name
     * @param params Methods parameters
     */
    public void traceExit(Object obj, String method, Object[] params) {
    	if (_tc.isEntryEnabled()) {
    		Tr.exit(_tc, method, params);
    	}
    }
    
    /**
     * 
     * @param obj
     * @param method
     * @param param
     */
    public void traceEntryExit(Object obj, String method, Object param) {
    	if (_tc.isEntryEnabled()) {
    		Tr.entry(_tc, method, param);
    		Tr.exit(_tc, method, param);
    	}
    }
    
    /**
     * 
     * @param obj
     * @param method
     * @param params
     */
    public void traceEntryExit(Object obj, String method, Object[] params) {
    	if (_tc.isEntryEnabled()) {
    		Tr.entry(_tc,  method, params);
    		Tr.exit(_tc, method, params);
    	}
    }
    
    /**
     * Info level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param param parameter for message string
     */
    public void info(String msg, Object situation_start, Object param) {
        Tr.info(_tc, msg, param);
    }
    
    /**
     * Info level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param params parameters for message string
     */
    public void info(String msg, Object situation_start, Object[] params) {
        Tr.info(_tc, msg, params);
    }
    
    /**
     * Info level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     */
    public void info(String msg, Object situation_start) {
        Tr.info(_tc, msg);
    }
    
    public void info(String msg){
    	info(msg, null);
    }
    
    /**
     * Error level message, when exception is caught
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param param parameter for message string
     * @param e Caught throwable
     */
    public void error(String msg, String situation_request, Object param, Throwable e) {
        if (_tc.isErrorEnabled()) {
        	Tr.error(_tc, msg, param, e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        }
    }
    
    /**
     * Error level message, when exception is caught
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param params parameters for message string
     * @param e Caught throwable
     */
    public void error(String msg, String situation_request, Object[] params, Throwable e) {
        if (_tc.isErrorEnabled()) {
        	Tr.error(_tc, msg, params, e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        }
    }
    
    /**
     * Error level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param param parameter for message string
     */
    public void error(String msg, String situation_request, Object param) {
        if (_tc.isErrorEnabled()) {
        	Tr.error(_tc, msg, param);
        }
    }
    
    /**
     * Error level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param params parameters for message string
     */
    public void error(String msg, String situation_request, Object[] params) {
        if (_tc.isErrorEnabled()) {
        	Tr.error(_tc, msg, params);
        }
    }
    
    /**
     * Error level message
     * @param msg Message to write
     */
    public void error(String msg) {
    	error(msg,null);
    }
    
    /**
     * Error level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     */
    public void error(String msg, String situation_request) {
        if (_tc.isErrorEnabled()) {
        	Tr.error(_tc, msg);
        }
    }
   
    /**
     * Warn level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     */
    public void warn(String msg, String situation_request) {
       if (_tc.isWarningEnabled()) {
        	Tr.warning(_tc, msg);
        }
    }
    
    public void warn(String msg){
    	warn(msg, null);
    }
    
    /**
     * Warn level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param param parameter for message string
     */
    public void warn(String msg, String situation_request, Object param) {
       if (_tc.isWarningEnabled()) {
        	Tr.warning(_tc, msg, param);
        }
    }
    
    /**
     * Warn level message
     * @param msg Message to write
     * @param situation_start For LWP compatibility
     * @param params parameters for message string
     */
    public void warn(String msg, String situation_request, Object[] params) {
        if (_tc.isWarningEnabled()) {
        	Tr.warning(_tc, msg, params);
        }
    }
    
    /**
     * True if logger level is INFO and above
     * @return
     */
    public boolean isInfoEnabled() {
    	return _tc.isInfoEnabled();
    }
    
    /**
     * True if logger level is EVENT and above
     * @return
     */
    public boolean isEventEnabled() {
    	return _tc.isEventEnabled();
    }
    
    /**
     * True if logger level is ERROR and above
     * @return
     */
    public boolean isErrorEnabled() {
    	return _tc.isErrorEnabled();
    }
    
    /**
     * True if logger level is FINER and above
     * @return
     */
    public boolean isTraceEntryExitEnabled() {
    	return _tc.isEntryEnabled();
    }
 
    /**
     * True if logger level is FINEST and above
     * @return
     */
    public boolean isTraceDebugEnabled(){
    	return _tc.isDebugEnabled();
    }
    
    /**
     * True if any logger level enabled
     * @return
     */
    public boolean isTraceFailureEnabled(){
    	return _tc.isEventEnabled();
    }
        
    /**
     * True if logger level is WARNING and above
     * @return
     */
    public boolean isWarnEnabled(){
    	return _tc.isWarningEnabled();
    }
}
