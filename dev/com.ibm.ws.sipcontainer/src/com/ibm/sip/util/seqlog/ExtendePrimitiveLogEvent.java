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

import java.lang.ref.SoftReference;

import com.ibm.ws.sip.parser.util.ObjectPool;

/**
 * @author Amir Perlman, Jun 6, 2005
 * 
 * A log event with extended information that is kept with a Soft Reference so
 * it can be collected by the GC in case of memory shortage. 
 * 
 */
public class ExtendePrimitiveLogEvent extends PrimitiveLogEvent {
    
    /**
     * Extended information that will be kept with a soft reference to allow 
     * GC to collect it in case memory is required. 
     */
    protected SoftReference _extendedInfoRef;
    
    /**
     * Pool of reusable log events. 
     */
    private static ObjectPool c_pool = new ObjectPool(ExtendePrimitiveLogEvent.class, null, -1);
    
    /**
     * @see com.ibm.sip.util.seqlog.LogEvent#dump(java.lang.StringBuffer, 
     * 			com.ibm.sip.util.seqlog.SequenceLogger)
     */
    public void dump(StringBuffer buf, SequenceLogger contextLogger) {
        
        //Avoid calling the base class as we don't want to print the basic
        //info information if we have the extended one. 
        dumpTime(buf);
        buf.append("  ");
        dumpLevelAsString(buf, _state);
        
        buf.append(", State:");
        contextLogger.dumpStateDesc(_state, buf);
        
        buf.append(", ");
        Object extendedInfo = null;
        
        if(null != _extendedInfoRef)
        {
            //Use the extended info if available 
            extendedInfo = _extendedInfoRef.get(); 
        }
        
        if(null == extendedInfo)
        {
            buf.append(_infoInt);
        }
        else
        {
            buf.append(extendedInfo);
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
     * Clear the object's content before it is put back to the pool 
     */
    protected void clear() {
        super.clear();
        if(null != _extendedInfoRef)
        {
            _extendedInfoRef.clear();
            _extendedInfoRef = null;
        }
    }
    
    /**
     * Update the log event with the specified state and both additional and 
     * extended info.  
     * @param state
     * @param info
     */
    public void update(int state, int info, Object extendedInfo) {
        super.update(state, info);
        if(null != extendedInfo)
        {
            _extendedInfoRef = new SoftReference(extendedInfo);
        }
    }
    
    /**
     * Get an instance of a log event from the pool of available instances
     * 
     * @return
     */
    protected static LogEvent getInstance() {
        return (ExtendePrimitiveLogEvent) c_pool.get();
    }
}
