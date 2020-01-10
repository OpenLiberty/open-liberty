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

import com.ibm.ws.sip.parser.util.ObjectPool;

/**
 * @author Amir Perlman, Jun 6, 2005
 *
 * A log event associated with additional info of primitive type int, boolean
 */
public class PrimitiveLogEvent extends LogEvent {
    
    /**
     * Optional value associated with this event - int value.  
     */
    protected int _infoInt;
    
    /**
     * Optional value associated with this event - boolean value.  
     */
    private boolean _infoBoolean;
    
    /**
     * Info type - either int or boolean. 
     */
    private int _type; 
    
    //
    //Constants for types 
    //
    private static final int INT = 1; 
    private static final int BOOLEAN = 2;
    
    /**
     * Pool of reusable log events. 
     */
    private static ObjectPool c_pool = new ObjectPool(PrimitiveLogEvent.class, null, -1);
    
    
    /**
     * @see com.ibm.sip.util.seqlog.LogEvent#dump(java.lang.StringBuffer, 
     * 			com.ibm.sip.util.seqlog.SequenceLogger)
     */
    protected void dump(StringBuffer buf, SequenceLogger contextLogger) {
        
        //Call the base class to dump its contents first
        super.dump(buf, contextLogger);
        
        buf.append(", ");
        
        switch(_type)
        {
        	case INT:
        	    buf.append(_infoInt);
        	    break;
        	case BOOLEAN:
        	    buf.append(_infoBoolean ? "true" : "false");
        	    break;
        }
    }
     
    /**
     * Return this instance to the pool of reusable Log Events. 
     */
    protected void returnToPool() {
        clear();
        c_pool.putBack(this);
    }

    /**
     * Clear the event's content before it is put back to the pool 
     */
    protected void clear() {
        super.clear();
        _infoInt = Integer.MIN_VALUE;
        _infoBoolean = false;
    }

    /**
     * Update the log event with the specified state and additional info. Will 
     * also set the time of the event. 
     * @param state
     * @param info
     */
    protected void update(int state, int info) {
        super.update(state);
        _infoInt = info;
        _type = INT;
    }
    
    /**
     * Update the log event with the specified state and additional info. Will 
     * also set the time of the event. 
     * @param state
     * @param info
     */
    protected void update(int state, boolean info) {
        super.update(state);
        _infoBoolean = info;
        _type = BOOLEAN;
    }
    /**
     * Get an instance of a log event from the pool of available instances
     * 
     * @return
     */
    protected static LogEvent getInstance() {
        return (PrimitiveLogEvent) c_pool.get();
    }

}
