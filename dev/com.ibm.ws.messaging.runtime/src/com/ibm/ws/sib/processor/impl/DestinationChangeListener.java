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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationChangeListener;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.trm.dlm.Capability;
import com.ibm.ws.sib.trm.dlm.DestinationLocationChangeListener;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author millwood
 * <p>The DestinationChangeListener is registered with TRM and is called back
 * whenever a change occurs to a destination in WLM.  This change could be
 * a new localisation for the destination being added into WLM, or a
 * localisation for the destination being deleted from WLM.</p>
 */
public final class DestinationChangeListener implements DestinationLocationChangeListener
{
  private static final TraceComponent tc =
    SibTr.register(
      DestinationChangeListener.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private MessageProcessor _messageProcessor;
  private DestinationManager _destinationManager;
  private SIMPTransactionManager _txManager;
  private List<MPDestinationChangeListener> _destinationChangeListeners =  new ArrayList<MPDestinationChangeListener>();

  /**
   * Constructs a new DestinationChangeListener
   *
   * @param messageProcessor The MP main object
   */
  public DestinationChangeListener(
    MessageProcessor messageProcessor
  )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "DestinationChangeListener",
        messageProcessor);

    _messageProcessor = messageProcessor;
    _destinationManager = messageProcessor.getDestinationManager();
    _txManager = messageProcessor.getTXManager();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "DestinationChangeListener", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.dlm.DestinationLocationChangeListener#destinationLocationChange(com.ibm.ws.sib.utils.SIBUuid12, java.util.Set, java.util.Set, com.ibm.ws.sib.trm.dlm.Capability)
   */
  public void destinationLocationChange (SIBUuid12 destId, Set additions, Set deletions, Capability capability)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "destinationLocationChange",
        new Object[]{destId, additions, deletions, capability});

    BaseDestinationHandler destinationHandler =
      (BaseDestinationHandler) _destinationManager.getDestinationInternal(destId, false);

    if (destinationHandler != null)
    {

      // Check if the localisation should be deleted
      Set localitySet = getDestinationLocalitySet(destinationHandler, capability);
      
      synchronized(destinationHandler)
      {
        // d526250 - If a remote destination is deleted then its ME stopped we might
        // get a trm notification in. Meanwhile, admin has told us to delete the dest
        // already. If we dont do the following we end up performing cleanup on a 
        // dest that no longer exists and we get nullpointers.
        if (!destinationHandler.isToBeDeleted())
        {
          // Process deletions from the WLM set for the destination
          // (No requirement to react to additions)
          Iterator i = deletions.iterator();
  
          while(i.hasNext())
          {
            SIBUuid8 meUuid = (SIBUuid8) i.next();
  
            //No point reacting to our own ME uuid
            if (!(meUuid.equals(_messageProcessor.getMessagingEngineUuid())))
            {
              PtoPMessageItemStream ptoPMessageItemStream;
  
                ptoPMessageItemStream = destinationHandler.getXmitQueuePoint(meUuid);
  
              // Dont touch a localisation that doesnt exist
              if (ptoPMessageItemStream != null)
              {
                if ((localitySet == null) || !localitySet.contains(meUuid.toString()))
                {
                  // The localisation is not known in WCCM or WLM, so mark it for deletion
                  cleanupDestination(ptoPMessageItemStream, 
                      destinationHandler,
                      meUuid,
                      capability);
                }
              }
            }
          }

          if (capability != Capability.GET)
          {
            // Reallocate the transmitQs to evenly spread messages
            destinationHandler.requestReallocation();
          }
        }
      }
    }
    
    // Iterate over the registered MP listeners and alert them to the location change
    for (int i = 0; i < _destinationChangeListeners.size(); i++)
    {
      MPDestinationChangeListener listener = _destinationChangeListeners.get(i);
      if (listener != null)
        listener.destinationLocationChange(destId, additions, deletions, capability);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "destinationLocationChange");

    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.trm.dlm.DestinationLocationChangeListener#destinationUnavailable(com.ibm.ws.sib.utils.SIBUuid12, com.ibm.ws.sib.trm.dlm.Capability)
   */
  public void destinationUnavailable (SIBUuid12 destId, Capability capability)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "destinationUnavailable",
        new Object[]{destId, capability});
    
    Set<SIBUuid8> available = new HashSet<SIBUuid8>(); // Empty set of available MEs. The destination is unavailable.
    Set<SIBUuid8> unavailable = new HashSet<SIBUuid8>(); // We'll build up this set through the logic below
    BaseDestinationHandler destinationHandler =
      (BaseDestinationHandler) _destinationManager.getDestinationInternal(destId, false);

    //TRM might pass in the UUID of a link here as it can't always tell the difference between links and destinations. 
    //Only process the UUID if is is not a link. 
    
    if (destinationHandler != null && !destinationHandler.isLink())
    {
      // Check if the localisation should be deleted
      Set localitySet = getDestinationLocalitySet(destinationHandler, capability);

      synchronized(destinationHandler)
      {
        // d526250 - If a remote destination is deleted then its ME stopped we might
        // get a trm notification in. Meanwhile, admin has told us to delete the dest
        // already. If we dont do the following we end up performing cleanup on a 
        // dest that no longer exists and we get nullpointers.
        if (!destinationHandler.isToBeDeleted())
        {
          // Part 1
          // We'll iterate over the set of transmission points for this destination
          // to build the list of localisations that we were sending to which are now unavailable
          Iterator<PtoPMessageItemStream> i = null;
          
            i = destinationHandler.getLocalisationManager().getXmitQueueIterator();    
          
          // Drive the iterator
          while(i.hasNext())
          {
            PtoPMessageItemStream ptoPMessageItemStream = (PtoPMessageItemStream)i.next();  
          
            // Get the localising ME's uuid
            SIBUuid8 meUuid = ptoPMessageItemStream.getLocalizingMEUuid();
            // Add this ME to the set of those that are unavailable
            unavailable.add(meUuid);
              
            if ((localitySet == null) || !localitySet.contains(meUuid.toString()))
            {
              // The localisation is not known in WCCM or WLM, so mark it for deletion
              cleanupDestination(ptoPMessageItemStream, 
                                 destinationHandler,
                                 meUuid,
                                 capability);
                
            } // eof null localitySet or meUuid unknown by admin 
          } //eof while we have another XMIT itemstream to process 
          
          // Part 2
          // We now look through our list of MEs that we were receiving from (i.e. remoteget)
          // and add them to the list of unavailable destinations.
          Iterator<AnycastInputControl> it = destinationHandler.getAIControlAdapterIterator();
          while (it.hasNext())
            unavailable.add(it.next().getDMEUuid()); // note this could add MEs that we have previously already handled deletion for
          
          if (capability != Capability.GET)
          {
            // Drive reallocation
            destinationHandler.requestReallocation();
          }
        } // eof !isToBeDeleted
      } // eof synchronized on DestinationHandler
    } // eof destinationHandler not null

    //To preserve existing behaviour, if the destinationHandler is null we assume the destination is not a link.
    final boolean isNotLink = destinationHandler == null || (destinationHandler != null && !destinationHandler.isLink());
    
    if(isNotLink)
    {
       // Iterate over the registered MP listeners and alert them to the location change
       for (int i = 0; i < _destinationChangeListeners.size(); i++)
       {
         MPDestinationChangeListener listener = _destinationChangeListeners.get(i);
   
         // In this delegating call the available set should be empty, while the unavailable set will
         // comprise those ME uuids that we collected above.
         if (listener != null)
           listener.destinationLocationChange(destId, available, unavailable, capability);
       }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "destinationUnavailable");

    return;
  }  
  
  /**
   * This method is used internal to MP only and is used to register additional
   * destination change listeners that are need as well as the main MP listener
   * registered at startup.
   *
   * Adding a destinationLocationChangeListener will not override an existing one
   * but will add to a list of ones that will be called.
   *
   * @param destinationLocationChangeListener
   */
  public void addMPDestinationChangeListener(MPDestinationChangeListener destinationChangeListener)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMPDestinationChangeListener",
        new Object[]{destinationChangeListener});

    _destinationChangeListeners.add(destinationChangeListener);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMPDestinationChangeListener");
  }

  /**
   * This method is used internal to MP only and is used to remove a
   * destination change listener that was registered. This method will only remove a
   * destinationLocationChangeListener that was registered via addMPDestinationChangeListener
   *
   * @param destinationLocationChangeListener
   */
  public void removeMPDestinationChangeListener(MPDestinationChangeListener destinationChangeListener)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMPDestinationChangeListener",
        new Object[]{destinationChangeListener});

    _destinationChangeListeners.remove(destinationChangeListener);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMPDestinationChangeListener");
  }
  
  /**
   * Retrieve the Locality Set defined in Admin.
   *  
   * @param destinationHandler
   * @param capability
   * @return
   */
  private Set getDestinationLocalitySet (BaseDestinationHandler destinationHandler, Capability capability)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationLocalitySet",
        new Object[]{destinationHandler, capability});
    
      // Check if the localisation should be deleted
      Set localitySet = null;

      //Get the locality sets as known by admin
      try
      {
          localitySet = _messageProcessor.getSIBDestinationLocalitySet(null, destinationHandler.getUuid().toString(), true);
      }
      catch(SIBExceptionBase e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.DestinationChangeListener.getDestinationLocalitySet",
          "1:368:1.45",
          this);
      }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestinationLocalitySet", localitySet);

    return localitySet;
  } 
  
  /**
   * Cleanup destinations unknown to both WLM and WCCM.
   * 
   * @param ptoPMessageItemStream
   * @param destinationHandler
   * @param meUuid
   * @param capability
   */
  private void cleanupDestination(PtoPMessageItemStream ptoPMessageItemStream, 
                                  BaseDestinationHandler destinationHandler,
                                  SIBUuid8 meUuid,
                                  Capability capability)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupDestination",
        new Object[]{ptoPMessageItemStream, destinationHandler, meUuid, capability});
    
    // The localisation is not known in WCCM or WLM, so mark it for deletion
    try
    {
      ptoPMessageItemStream.markAsToBeDeleted(_txManager.createAutoCommitTransaction());
          //Capability == GET Close any remote consumers from the destination
          destinationHandler.closeRemoteConsumer(meUuid);
    }
    catch(SIResourceException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.DestinationChangeListener.cleanupDestination",
        "1:424:1.45",
        this);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupDestination");    
  }
  
}
