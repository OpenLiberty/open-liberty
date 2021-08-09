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
package com.ibm.ws.sib.processor.impl;

// Import required classes.
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.indexes.DestinationIndex;
import com.ibm.ws.sib.processor.impl.indexes.DestinationTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.LinkIndex;
import com.ibm.ws.sib.processor.impl.indexes.LinkTypeFilter;
import com.ibm.ws.sib.processor.impl.indexes.statemodel.State;
import com.ibm.ws.sib.processor.impl.interfaces.MPCallsToUnitTestHandler;
import com.ibm.ws.sib.processor.impl.interfaces.StoppableThread;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.utils.StoppableThreadCache;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The AsynchDeletionThread class is responsible for removing destination localizations, 
 * neighbouring ME's which no longer exist and durable subscriptions which may have in doubt transactions
 * on them. 
 */
public class AsynchDeletionThread implements Runnable, StoppableThread
{
  //NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
      SibTr.register(
      AsynchDeletionThread.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);	
 
  private MessageProcessor _messageProcessor;
  private volatile boolean _isRunning = false;
  private volatile boolean _isStopping = false;
  private volatile boolean _rerunRequested = false;

  private Object asynchLock = new Object();
  
 /**
  * Create a new Thread to perform asuynch deletion
  * 
  * @param mp The 'owning' MP object
  */
  AsynchDeletionThread(MessageProcessor mp)
  {
    if (tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "AsynchDeletionThread",
          new Object[] { mp});

    //store the variables
    _messageProcessor = mp;
    
    _messageProcessor.getStoppableThreadCache().registerThread(this);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "AsynchDeletionThread", this);
  }
          

 /** 
  * Main run method for class.
  * 
  * There are 3 things that deletion thread handles.
  * 
  * 1) Removal of neighbouring ME's which no longer exist.
  * 2) Removal of deleted durable subscriptions
  * 3) Removal of destination/link localizations.
  * 
  * Removal of Neighbouring ME's is handled by the call removeUnusedNeighbours in the Proxy handling code.
  * 
  * Removal of durable subscriptions is handled by looping through the list of subscriptions that failed
  * to be deleted last time and attempting the deleteIfPossible call.  There is a possibility that the
  * subscriptions item stream was deleted if the destination was removed.  We handle this by catching the
  * NotInStoreException.
  * 
  * Removal of the destinations/link localizations is handled by looping through an iterator for 
  * destinations which are in the state CLEANUP_PENDING = Boolean.TRUE and DELETE_PENDING = Boolean.TRUE
  * The AsynchDeletion thread then asks each destination to clean up any localizations.
  * 
  * 
  * There is code here which links into the unit test framework.  During the unit test runs the framework 
  * checks that destinations are deleted with no errors.  Any problems deleting destinations are reported 
  * as junit failures.
  * 
  */
  public void run()
  {      
    if (tc.isEntryEnabled())
        SibTr.entry(tc, "run");
            
    // Make sure that the unit test is happy for this tread to continue with 
    // an attempted pass at deletions.
    // The unit tests may block this thread at this point, to make sure 
    // we don't delete things immediately. Leads to more determinate behaviour 
    // for the unit tests.
    MPCallsToUnitTestHandler unitTestCallback = MessageProcessor.getMPCallsToUnitTestHandler();
    DestinationManager destinationManager = _messageProcessor.getDestinationManager();
    DestinationIndex destinationIndex = destinationManager.getDestinationIndex();
    LinkIndex linkIndex = destinationManager.getLinkIndex();

    SIMPIterator itr = null;
    
    boolean run = true;
            
    try
    {
      //Check if we should synchronize with the unit tests
      if( unitTestCallback != null ) 
        asynchLock = unitTestCallback.getAsynchLock(this);
      synchronized(asynchLock)
      {
        //Check if we should synchronize with the unit tests
        if( unitTestCallback != null )
          unitTestCallback.asyncDeletionThreadReadyToStart(); 
        
        while (run && !isStopping())
        {      
  
          // Venu mock mock
          // no Neighbours for this version of Liberty profile
          /*	
          // Remove any unused Neighbours
          _messageProcessor.getProxyHandler().removeUnusedNeighbours(); */
          
          // Delete any redundant subscriptions
          List subscriptions = (List)(((ArrayList)destinationManager.getSubscriptionsToDelete()).clone());
          SubscriptionItemStream stream = null;
          while (!subscriptions.isEmpty())
          {
            stream = (SubscriptionItemStream)subscriptions.remove(0);      
            cleanupSubscription(stream);
            destinationManager.removeSubscriptionAsDeleted(stream);
          }
                
          DestinationTypeFilter destFilter = new DestinationTypeFilter();
          destFilter.ALIAS = Boolean.FALSE;
          destFilter.FOREIGN_DESTINATION = Boolean.FALSE;
          destFilter.and = false;
          destFilter.CLEANUP_PENDING = Boolean.TRUE;
          destFilter.DELETE_PENDING = Boolean.TRUE;
          itr = destinationIndex.iterator(destFilter);
          BaseDestinationHandler dh = null;
      
          while (itr.hasNext() && !isStopping())
          {             
                  
            dh = (BaseDestinationHandler)itr.next();
            boolean destinationCleanedUp = cleanUpDestination(dh);
            if (destinationCleanedUp)
            {
              //Success!
              synchronized (destinationIndex)
              {
                State state = destinationIndex.getState(dh);
                if(state.isCleanupPending())
                {
                  destinationIndex.cleanupComplete(dh);
                }
                else //DELETE_PENDING
                {
                  destinationIndex.remove(dh);
                }
              }
              if (tc.isDebugEnabled())
                SibTr.debug(tc, "Destination " + dh.getName() + " cleaned up");
            }
            else
            {
              if (tc.isDebugEnabled())
                SibTr.debug(tc, "Failed to cleanup destination " + dh.getName());
            }
          }
          itr.finished();
          
          LinkTypeFilter linkFilter = new LinkTypeFilter();
          linkFilter.and = false;
          linkFilter.CLEANUP_PENDING = Boolean.TRUE;
          linkFilter.DELETE_PENDING = Boolean.TRUE;
          itr = linkIndex.iterator(linkFilter);
    
          while (itr.hasNext() && !_isStopping)
          {                  
            dh = (BaseDestinationHandler)itr.next();
            boolean destinationCleanedUp = cleanUpDestination(dh);
            if (destinationCleanedUp)
            {
              //Success!
              synchronized(linkIndex)
              {
                State state = linkIndex.getState(dh);
                if(state.isCleanupPending())
                {
                  linkIndex.cleanupComplete(dh);
                }
                else //DELETE_PENDING
                {
                  linkIndex.remove(dh);
                }
              }
              if (tc.isDebugEnabled())
                SibTr.debug(tc, "Link " + dh.getName() + " cleaned up");
            }
            else
            {
              if (tc.isDebugEnabled())
                SibTr.debug(tc, "Failed to cleanup Link " + dh.getName());
            }          
          }      
          itr.finished();
          run = _rerunRequested;
          _rerunRequested = false;
        }
        asynchLock.notifyAll();
      }
    } 
    catch (Exception e ) 
    {      
      // FFDC
      FFDCFilter.processException(e, 
                                  "com.ibm.ws.sib.processor.impl.AsynchDeletionThread.run", 
                                  "1:263:1.50", 
                                  this);
      
      try
      {
        itr.finished();
      }
      catch(Exception e2)
      {
        // No FFDC code needed
        //do nothing!
      }
  
      // Some failure occurred, which we're going to throw on the deletion 
      // thread... to nobody.
      
      // Log warning to the console
  
      //    Report the error to the message processor. It will be ignored
      // if the message processor is running in production code.
      // If in unit test code, a junit error will occur, so we are more
      // aware of such cleanup failures.
      unitTestCallback = MessageProcessor.getMPCallsToUnitTestHandler();
      if( null != unitTestCallback )
      {
        // We're operating inside a unit test
        unitTestCallback.unitTestFailure( 
          "Async background deletion thread failed with an exception ",
          e );
      }
      SIErrorException exToThrow = 
        new SIErrorException( nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AsynchDeletionThread.run",
              "1:298:1.50" }, 
              null), 
            e);  
      SibTr.exception( tc, e );  
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "run");
      throw exToThrow;   
    }
    finally
    {            
      // Prepare to exit. Notify any waiters (i.e perhaps we're in MP.stop() processing) 
      // that we are done and set the isRunning flag to false 
      destinationManager.notifyAsynchDeletionEnd(this);
    }

    if (tc.isEntryEnabled())
        SibTr.exit(tc, "run");
  }
  
  /**
   * Delete the subscription in question.
   * 
   * @param stream
   */
  private void cleanupSubscription(SubscriptionItemStream stream)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupSubscription", stream);
      
    try
    {
      // Indicate that we don't want the asynch deletion thread restarted if the 
      // subscription fails to delete, otherwise we might end up in a tight loop trying
      // to delete the subscription
      stream.deleteIfPossible(false);      
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AsynchDeletionThread.cleanupSubscription",
        "1:340:1.50",
        stream);
      
      if (tc.isDebugEnabled()) 
      {
        SibTr.debug(tc, "Failed to delete subscription " + stream);       
        SibTr.exception(tc, e);
      }
      
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupSubscription");
  }

  private boolean cleanUpDestination(BaseDestinationHandler dh)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "cleanUpDestination", new Object[] { dh });
          
    /*
     * this is where we actually delete the local destinations
     */
    // The cleanupDestination routine will flush the outbound
    // streams when it processes remote destination localisations.  It will
    // also notice that the itemstreams holding the messagess are marked for 
    // deletion and once the flushes have been initiated, it will delete the 
    // itemstreams.
    boolean destinationCleanedUp = false;
    
    try
    {
      if (tc.isDebugEnabled())
        SibTr.debug(tc, "Starting clean-up for " + dh.getName() + " : " + dh.getUuid());
        
      destinationCleanedUp = dh.cleanupDestination();
      
      if (tc.isDebugEnabled())
        SibTr.debug(tc, "Ended clean-up for " + dh.getName() + " : " + dh.getUuid() + " : Success " + destinationCleanedUp);
    }
    catch(Exception e)
    {
      // No FFDC Code needed 
      SibTr.exception(tc, e);
      
      if (tc.isDebugEnabled())
        SibTr.debug(tc, " Failed to cleanup destination " + dh.getName() + " : " + dh.getUuid());

      // Report the error to the message processor. It will be ignored
      // if the message processor is running in production code.
      // If in unit test code, a junit error will occur, so we are more
      // aware of such cleanup failures.
      MPCallsToUnitTestHandler unitTestCallback = MessageProcessor.getMPCallsToUnitTestHandler();
      if( null != unitTestCallback )
      {
        // We're operating inside a unit test
        unitTestCallback.unitTestFailure( 
          "Failed to clean up a destination on the async deletion thread.\n" +
          "The test reporting this failure may not be the one which caused the problem.\n" +
          "Likely that the test which created + used the destination will show it up when\n" +
          "re-running the tests stand-alone.\n"+
          dh.toString()+"\n" ,
          e );
      } 
      // Swallow the error.  This is only background delete so if it cant work now,
      // it will be tried again at a later time.
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "cleanUpDestination", new Boolean(destinationCleanedUp));
      
    return destinationCleanedUp;        
  }      	         
          
  /**
   * Method setRunning - Set indicator that the asynchDeletionThread is running
   */
  public void setRunning(boolean running)
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setRunning");
      SibTr.exit(tc, "setRunning");
    }

    _isRunning = running;
    
    return;
  }
  
  /**
   * Method isRunning - Determine if the AsynchDeletionThread is running or not.
   * @return boolean
   */
  public boolean isRunning()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isRunning");
      SibTr.exit(tc, "isRunning", new Boolean(_isRunning));
    }
    
    return _isRunning;
  }
 
   /**
   * Method setStopping - Set indicator that the asynchDeletionThread is stopping
   */
  public void setStopping()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setStopping");
      SibTr.exit(tc, "setStopping");
    }

    _isStopping = true;
    
    return;
  }
  
  /**
   * Method isStopping - Determine if the AsynchDeletionThread is stopping or not.
   * @return boolean
   */
  public boolean isStopping()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isStopping");
      SibTr.exit(tc, "isStopping", new Boolean(_isStopping));
    }
    
    return _isStopping;
  }

  /**
   * Indicate that we want the Asynch deletion thread to run again
   */
  public void rerun()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "rerun");

    _rerunRequested = true;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "rerun");   
  } 
  
  /**
   * Stop this messaging engines asynchronous deletion thread.
   * <p>
   * Feature 183052
   */
  public void stopThread(StoppableThreadCache threadCache)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "stopThread", threadCache);
    
    synchronized (_messageProcessor.getDestinationManager().deletionThreadLock)
    {    
      // Allow the thread to finish the immediate job in hand before
      // returning.
      //set the thread to stopping, so that it'll stop if in progress
      // and no new instances will be started         
      setStopping();
  
      // Now wait till the thread's gone
      while (isRunning())
      {
        try
        {
          // wait for thread to end
          // I put a timeout on this - to handle the case where we've hung
          // in delete processing. The best thing in such a case is probably
          // to allow the whole process to exit.
  
          // But is 5 secs right? - NY
          _messageProcessor.getDestinationManager().deletionThreadLock.wait(5000);
          if (Thread.holdsLock(asynchLock)) asynchLock.wait(5000);
        }
        catch (InterruptedException iex)
        {
          // No FFDC code needed
          SibTr.exception(tc, iex);
        }
      }
    }
    
    // Remove this thread from the thread cache
    threadCache.deregisterThread(this);
   
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "stopThread");
  }
}
