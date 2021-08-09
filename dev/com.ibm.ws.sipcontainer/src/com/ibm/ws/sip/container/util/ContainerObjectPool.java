/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util;

import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;

/**
 * @author Amirk
 * 
 * An Object pool, grows as needed. Does not free resources.
 * TODO move to sip.utils package 
 */
public class ContainerObjectPool
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ContainerObjectPool.class);
    
    /**
     * type of object
     */
    private final Class m_class;

    /**
     * The listener to be called when objects are returned.
     */
    private ContainerObjectPoolListener m_listener;
    
    /**
     * Maximum pool size, above this size returned objects will not be put
     * into the pool and would be left for garbage collection. Any value less then 
     * 1 indicates unlimited size.  
     */
    private int m_maxPoolSize = 1000; 
    
    /**
	 * Flag indicating if we are currentlly in overloaded state and objects will
	 * not be returned to the pool. 
	 */
	private boolean _isOverloaded = false; 
	
	/**
     * Last time an overload message has been printed to log. We we are overloaded
     * we want to print a message to the log but we need to avoid filling the 
     * log with such message. So we will print the periodicly every 60 seconds
     * if needed. 
     */
    private long _lastOverLoadMessageTime;
    
    /**
     * objects in pool
     */
    private final Vector m_objects;
    
    /**
     * Construct a new object pool for the specified class type. 
     * @param type
     * @param propertiesFlag Flag that is used to read the max of the pool size 
     * that should be created
     */
    public ContainerObjectPool(Class type,String propertiesFlag)
    {
        m_class = type;
        m_objects = new Vector();
        
        int maxPoolSize = 
			PropertiesStore.getInstance().getProperties().getInt(propertiesFlag);
		if(maxPoolSize > 0)
		{
		    m_maxPoolSize = maxPoolSize;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "ContainerObjectPool", 
                				"Max Pool Size: " + m_maxPoolSize + 
                				" for: " + m_class.getName());
        }
    }
    
    /**
     * Construct a new object pool for the specified class type. Enables 
     * adding a listener for notifications on each object returned to the pool 
     * @param propertiesFlag Flag that is used to read the max of the pool size 
     * that shold be created
     */
    public ContainerObjectPool(Class type,String propertiesFlag, ContainerObjectPoolListener listener)
    {
        this(type,propertiesFlag);
        m_listener = listener;
    }
    
    /**
     * Construct a new object for the specified type with the specified max
     * pool size. 
     * @param type
     * @param listener
     * @param size Maximum pool size or -1 for unlimited size. 
     */
    public ContainerObjectPool(Class type, ContainerObjectPoolListener listener, int size)
    {
        m_class = type;
        m_objects = new Vector(size < 1 ? 10 : size);
        m_maxPoolSize = size; 
        m_listener = listener; 
    }

    public synchronized Object get()
    {
        Object obj = null; 
    	if (m_objects.isEmpty())
        {
            try
            {
                obj = m_class.newInstance();
            }
            catch (InstantiationException ex)
            {
                ex.printStackTrace();
            }
            catch (IllegalAccessException ex)
            {
                ex.printStackTrace();
            }

            if(null == obj)
            {
            	// Throw unchecked exception for error in pool configuration.
            	throw new RuntimeException("Cannot create Object in pool");
            }
        }
        else
        {
            // Remove object from end of free pool.
            obj = m_objects.remove( m_objects.size()-1 );
        }
    	
//    	System.err.println("Taken from pool: " + obj);
    	return obj;
    }

    public synchronized void putBack(Object obj)
    {
//        System.err.println("Returned to pool: " + obj);
    	if (m_class.isInstance(obj))
        {
        	if (m_listener != null)
        	{
        		m_listener.objectReturned(obj);
        	}
        	
        	if(m_maxPoolSize < 1 || m_objects.size() < m_maxPoolSize)
        	{
        	    m_objects.addElement(obj);
        	    _isOverloaded = false;
        	}
        	else
        	{
        	    if(!_isOverloaded)
    	        {
    	            _isOverloaded = true;
    	            long cTime = System.currentTimeMillis();
    		        
    		        //Print message to log every 60 seconds only
    		        if(_lastOverLoadMessageTime + 60000 < cTime  &&
    		           c_logger.isTraceDebugEnabled())
                    {			
    	                c_logger.traceDebug(this, "putBack", m_class.getName() +
    	                		" Object pool is full, returned objects are dropped");
    	                _lastOverLoadMessageTime = cTime;
                    }
    	        }
        	}
        }
        else
        {
            throw new IllegalArgumentException("argument type invalid for pool");
        }
    }
    
    /**
     * Sets the listener associated with this pool 
     * @param listener
     */
    public void setObjectPoolListener(ContainerObjectPoolListener listener)
    {
        m_listener = listener;
    }
}

