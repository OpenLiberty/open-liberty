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
package com.ibm.ws.sib.processor.impl.destination;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;


/**
 * @author nyoung
 * 
 * <p>The RemotePtoPSupport class holds the remote PtoP state specific to a 
 * BaseDestinationHandler that represents a Queue. 
 */
public class RemotePtoPSupport extends AbstractRemoteSupport 
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      RemotePtoPSupport.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      

  /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);    
    
  public RemotePtoPSupport(
    BaseDestinationHandler myBaseDestinationHandler,
    MessageProcessor messageProcessor)
  {
    super(myBaseDestinationHandler,messageProcessor);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#reconstituteLocalQueuePoint(int, com.ibm.ws.sib.processor.impl.ConsumerDispatcher)
   */
  public int reconstituteLocalQueuePoint(int startMode) 
    throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "reconstituteLocalQueuePoint",
                  new Object[] { 
                  Integer.valueOf(startMode)});  
                  

    // also check if there is an AOContainerItemStream for Remote Get
    NonLockingCursor cursor =
    _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(AOContainerItemStream.class));
    int aoCount = 0;
    AOContainerItemStream aoTempItemStream = null;
    do
    {
      aoTempItemStream = (AOContainerItemStream) cursor.next();
      if (aoTempItemStream != null)
      {
        // NOTE: since this destination is PtoP it should NOT be
        // possible to end up recovering an aostream used for durable.
        // Still, bugs happen, so here's a sanity check
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && (aoTempItemStream.getDurablePseudoDestID() != null))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "reconstituteLocalQueuePoint", "SIResourceException");
          throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.RemotePtoPSupport",
              "1:138:1.8.1.5",
              null },
            null));
        }
        
        aoCount++;
        _aoContainerItemStream = aoTempItemStream;
        
        // Don't do flush if we are asked to start in recovery mode 
        if(   ((startMode & JsConstants.ME_START_FLUSH ) == JsConstants.ME_START_FLUSH )
           && ((startMode & JsConstants.ME_START_RECOVERY ) == 0 ) )
        {
          getAnycastOutputHandler((DestinationDefinition)_baseDestinationHandler.
                                    getDefinition(),true); 
        }
        else
        {
          getAnycastOutputHandler((DestinationDefinition)_baseDestinationHandler.
                                    getDefinition(),false); 
        }
      }
    }
    while (aoTempItemStream != null);
    
    cursor.finished();
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteLocalQueuePoint", Integer.valueOf(aoCount));
      
    return aoCount;
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#getAnycastInputHandlerByPseudoDestId(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public AnycastInputHandler getAnycastInputHandlerByPseudoDestId(SIBUuid12 destID)
  {
    //pub sub only
    return null;
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#getAnycastOutputHandlerByPseudoDestId(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public AnycastOutputHandler getAnycastOutputHandlerByPseudoDestId(SIBUuid12 destID)
  {
    //pub sub only
    return null;
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#getPostReconstitutePseudoIds()
   */
  public Object[] getPostReconstitutePseudoIds()
  {
    //pub sub only    
    return null;
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#closeConsumers()
   */
  public void closeConsumers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeConsumers");

    //This must be a rme
    for(Iterator i=_anycastInputHandlers.keySet().iterator(); i.hasNext(); )
    {
      AnycastInputHandler next = _anycastInputHandlers.get(i.next());
      ConsumerDispatcher rcd = next.getRCD();
      
      rcd.closeAllConsumersForDelete(_baseDestinationHandler);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeConsumers");

    return;
  }

  /**
   * Method notifyReceiveAllowedRCD
   * <p>Notify Remote Consumer Dispatchers consumers on the RME for this destination
   * of the change to the receive allowed attribute</p>
   * @param isReceiveAllowed
   * @param destinationHandler
   */
  public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyReceiveAllowedRCD", new Object[] {destinationHandler});

    // For PtoP iterate over anycastInputHandlers
    synchronized (_anycastInputHandlers) 
    { 
      for(Iterator i=_anycastInputHandlers.keySet().iterator(); i.hasNext();) 
      { 
        String key = (String) i.next(); 
        AnycastInputHandler aih = _anycastInputHandlers.get(key); 
        if (aih != null) 
        { 
          RemoteConsumerDispatcher rcd = aih.getRCD();
          rcd.notifyReceiveAllowed(destinationHandler);
        }
      }
    }
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyReceiveAllowedRCD");
  }

  /**
   * Method cleanupLocalisations
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  public boolean cleanupLocalisations() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupLocalisations");

    // true if all localisations have been cleaned up successfully    
    boolean allCleanedUp = true;

    // removing a local localisation. Cleanup the anycastContainerItemStream and anycastOutputHandler first
    if (_anycastOutputHandler != null
      || _aoContainerItemStream != null)
    {
      try
      {
        boolean aohCleanup = false;
        if (_anycastOutputHandler != null)
        {
          aohCleanup = _anycastOutputHandler.cleanup(true, true);
          if (aohCleanup)
            _anycastOutputHandler = null;
        }
                
        if (aohCleanup)
        {
                
          LocalTransaction siTran =
            _baseDestinationHandler.getTransactionManager().createLocalTransaction(true);

          if (_aoContainerItemStream != null)
          {
            _aoContainerItemStream.removeAll((Transaction) siTran);
            _aoContainerItemStream = null;
          }

          siTran.commit();
        }
                
        if (allCleanedUp)
          allCleanedUp = aohCleanup;
      }
      catch (Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.destination.RemotePtoPSupport.cleanupLocalisations",
          "1:303:1.8.1.5",
        this);

        SibTr.exception(tc, e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(
            tc,
            "cleanupLocalisations",
            "SIResourceException");
        if (allCleanedUp)
          allCleanedUp = false;
        throw new SIResourceException(e);
      }
    } // end if 

    //Now remove the infrastructure for any consumers issuing remote gets to another
    //localisation of the destination
    Iterator i = _anycastInputHandlers.keySet().iterator();
    boolean anycastCleanUp = true;
  
    while(i.hasNext())
    {
      String key = (String) i.next();
              
      AnycastInputHandler aih = _anycastInputHandlers.get(key);
      if ( !(aih.destinationDeleted()) )
      {
        //The AIH is not flushed yet
        anycastCleanUp = false;
      }
      else if ( !(removeAnycastInputHandlerAndRCD(key)) )
      {
        //We failed to cleanup the AIH and RCD itemStream
        anycastCleanUp = false;
      }
    }
            
    if (allCleanedUp)
    {
      allCleanedUp = anycastCleanUp;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupLocalisations", Boolean.valueOf(allCleanedUp));

    return allCleanedUp;
  }
  
  public Iterator<AnycastInputControl> getAIControlAdapterIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAIControlAdapterIterator");
    
    ArrayList<AnycastInputControl> controlAdapters = new ArrayList<AnycastInputControl>();
    synchronized (_anycastInputHandlers) 
    { 
      Iterator<AnycastInputHandler> it = _anycastInputHandlers.values().iterator();
      while(it.hasNext())
        controlAdapters.add(new AnycastInputControl(it.next()));     
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAIControlAdapterIterator", controlAdapters);
    return controlAdapters.iterator();
  }
  
  public Iterator<ControlAdapter> getAOControlAdapterIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAOControlAdapterIterator");
    
    Iterator<ControlAdapter> it = 
      getAnycastOutputHandler((DestinationDefinition)_baseDestinationHandler.getDefinition(), false)
      .getAOControlAdapterIterator();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAOControlAdapterIterator", it);
    return it;
  }

  public void reconstituteIMELinks(ArrayList<AOValue> valueTicks) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteIMELinks", valueTicks);
    
    Iterator<AOValue> it = valueTicks.iterator();
    while(it.hasNext())
    {
      AOValue val = it.next();
      String key = SIMPUtils.getRemoteGetKey(val.getSourceMEUuid(), null);
      AIContainerItemStream stream = _aiContainerItemStreams.get(key);
      if (stream !=null) // We are an IME with AIStreams
        stream.addAOLink(val);    
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteIMELinks");
  }
  
}
