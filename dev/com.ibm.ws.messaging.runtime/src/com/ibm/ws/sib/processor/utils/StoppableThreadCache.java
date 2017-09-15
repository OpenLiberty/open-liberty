/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.utils;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.StoppableThread;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author gatfora
 * 
 * Caches the stoppable threads that have been started, for Message Processor 
 * to stop at terminate time.
 * 
 * The aim for this class is to make sure at stop time that all threads that
 * have requested to be stopped before the system exits are stopped, or at least 
 * have the stopThead method called on it.
 */
public final class StoppableThreadCache
{
  private static final TraceComponent tc =
      SibTr.register(
        StoppableThreadCache.class,
        SIMPConstants.MP_TRACE_GROUP,
        SIMPConstants.RESOURCE_BUNDLE); 

 
  private ArrayList _threadCache;
  
  public StoppableThreadCache()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "StoppableThreadCache");
      
    _threadCache = new ArrayList();
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "StoppableThreadCache", this);
  }
  
  /**
   * Registers a new thread for stopping   
   */
  public void registerThread(StoppableThread thread)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "registerThread", thread);
      
    synchronized (this)
    {
      _threadCache.add(thread);
    } 
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "registerThread");   
  }
  
  /**
   * Deregisters a thread for stopping   
   */
  public void deregisterThread(StoppableThread thread)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterThread", thread);
      
    synchronized (this)
    {
      _threadCache.remove(thread);
    } 
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterThread");
  }
  
  /**
   * Stops all the stoppable threads that haven't already been stopped   
   */
  public void stopAllThreads()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "stopAllThreads");
      
    synchronized (this)
    {
      Iterator iterator = ((ArrayList)_threadCache.clone()).iterator();
      while (iterator.hasNext())
      {        
        StoppableThread thread = (StoppableThread)iterator.next();
        
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Attempting to stop thread " + thread);
        
        // Stop the thread  
        thread.stopThread(this);
        
        // Remove from the iterator
        iterator.remove();
        // Remove from the cache
        _threadCache.remove(thread);          
      }
    }
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "stopAllThreads");
  }
  
  /**
   * Unit test hook to check on connected threads   
   */
  public ArrayList getThreads()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getThreads");
      SibTr.exit(tc, "getThreads", _threadCache);
    }
    return _threadCache;
  }
}
