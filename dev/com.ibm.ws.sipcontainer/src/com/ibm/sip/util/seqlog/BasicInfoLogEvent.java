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
 * A log event associated with a additional data which can be any java object. 
 * All internal data is strongly referenced and will not be collected by the 
 * GC till the sequence logger reaches completed state. 
 */
public class BasicInfoLogEvent extends LogEvent {
    
    /**
     * Optional value associated with this event. This value will be hold a 
     * a strong reference to the object which will prevent it from being garbage
     * collected till the logger reaches its completed state.  
     */
    protected Object _info;
    
    /**
     * Pool of reusable log events. 
     */
    private static ObjectPool c_pool = new ObjectPool(BasicInfoLogEvent.class, null, -1);
    
    
    /**
     * @see com.ibm.sip.util.seqlog.LogEvent#dump(java.lang.StringBuffer, 
     * 			com.ibm.sip.util.seqlog.SequenceLogger)
     */
    protected void dump(StringBuffer buf, SequenceLogger contextLogger) {
        
        //Call the base class to dump its contents first
        super.dump(buf, contextLogger);
        
        buf.append(", ");
        buf.append(_info == null ? "None" : _info);

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
        _info = null;
    }

    /**
     * Update the log event with the specified state and additional info. Will 
     * also set the time of the event. 
     * @param state
     * @param info
     */
    protected void update(int state, Object info) {
        super.update(state);
        _info = info;
    }
    
    /**
     * Get an instance of a log event from the pool of available instances
     * 
     * @return
     */
    protected static LogEvent getInstance() {
        return (BasicInfoLogEvent) c_pool.get();
    }
}
