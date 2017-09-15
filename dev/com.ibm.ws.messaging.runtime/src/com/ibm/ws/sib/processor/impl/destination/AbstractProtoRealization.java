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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;


/**
 * @author Neil Young
 * 
 * <p>The AbstractProtoRealization class is the parent class of the PtoPRealization
 * and PubSubRealization. It holds abstract methods and code that is common to
 * each realization.
 */
public abstract class AbstractProtoRealization
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      AbstractProtoRealization.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   
  /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);    
    
  /** Reference to the associated BaseDestinationHandler */
  protected BaseDestinationHandler _baseDestinationHandler;
  protected MessageProcessor _messageProcessor;
  protected DestinationManager _destinationManager;
   
  /** Used to synch sourcestream reallocation with new messages arriving at the stream */
  private LockManager _reallocationLockManager = null;

  /** The LocalisationManager handles the set of localisations and 
   * interfaces to WLM */
  protected LocalisationManager _localisationManager = null; 

  /** The object that manages state for GD and Anycast */
  protected AbstractRemoteSupport _remoteSupport = null; 

  
  /**
   * Maintain a hashmap of post mediated itemstreams for the destination 
   * (transmit queues + possibly a local queue point) that have been 
   * deleted and are awaiting clean-up
   */
  HashMap _postMediatedItemStreamsRequiringCleanup = new HashMap(1);
  /**
   * Method reconstituteEnoughForDeletion
   * 
   * <p> For temporary destinations, we only need to set up the parent streams of
   * the message items and references.
   * @throws MessageStoreException
   * @throws SIRollbackException
   * @throws SIConnectionLostException
   * @throws SIIncorrectCallException
   * @throws SIResourceException
   */
  public abstract void reconstituteEnoughForDeletion()
    throws
      MessageStoreException,
      SIRollbackException,
      SIConnectionLostException,
      SIIncorrectCallException,
      SIResourceException;
      
  public void reconstituteGDTargetStreams() 
    throws MessageStoreException, SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteGDTargetStreams");

    _remoteSupport.
      reconstituteGDTargetStreams();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteGDTargetStreams");
  }

  /**
   * Method choosePtoPOutputHandler
   * 
   * <p> Choose OutputHandler appropriate to the address provided, the type
   * of destination and its localisations.
   * 
   * @param address
   * @param preferredME
   * @param localMessage
   * @param forcePut
   * @param singleServer
   * @param perferLocal
   * @return outputHandler
   * @throws SIRollbackException
   * @throws SIConnectionLostException
   * @throws SIResourceException
   */
  public OutputHandler choosePtoPOutputHandler(
    SIBUuid8 fixedMEUuid,
    SIBUuid8 preferredME,
    boolean localMessage,
    boolean forcePut,
    boolean singleServer,
    HashSet<SIBUuid8> scopedMEs) 
      throws 
        SIRollbackException, 
        SIConnectionLostException, 
        SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "choosePtoPOutputHandler",
        new Object[] { fixedMEUuid,
                       preferredME,
                       localMessage,
                       forcePut,
                       singleServer,
                       scopedMEs});

    
    OutputHandler result = null;

    // If we have a suitable local message point (depending on whether we need mediating or not)
    // then we can only check it if any ME restictions include this local ME.
    boolean checkLocal = false;
    if(_localisationManager.hasLocal())
    {
      if(fixedMEUuid == null)
      {
        if((scopedMEs == null) || scopedMEs.contains(_messageProcessor.getMessagingEngineUuid())) 
          checkLocal = true;
      }
      else if (fixedMEUuid.equals(_messageProcessor.getMessagingEngineUuid()))
      {
        if((scopedMEs == null) || scopedMEs.contains(_messageProcessor.getMessagingEngineUuid())) 
          checkLocal = true;
      }
    }

    // If the scoped set of MEs does not contain the preferred ME, or we're fixed
    // to any ME ignore any preference
    if((fixedMEUuid != null) ||
       ((scopedMEs != null) && !scopedMEs.contains(preferredME)))
      preferredME = null;

    // If we have a local message point and it's the preferred one (or the only possible one) then
    // check to see if it's available
    if(checkLocal &&
       (singleServer || // Only possible one
        ((preferredME != null) && (preferredME.equals(_messageProcessor.getMessagingEngineUuid()))) || // Preferred one
        (fixedMEUuid != null))) // Already fixed to the local one
    {
    	
        result = getLocalPostMedPtoPOH(localMessage, 
                forcePut,
                singleServer);

    }
    
    // If we haven't already found a local message point (or didn't try as we don't prefer it)
    // then ask WLM to pick one
    if((result == null) &&  // nothing found yet
       !singleServer && // Not a single server (otherwise the local check was enough
       !(checkLocal && (fixedMEUuid != null))) // We're not fixed to the local ME
    {
      HashSet<SIBUuid8> localScopedMEs = scopedMEs;
      
      // If we've been mandated a message point on another ME then that's the
      // only one we want to consider
      if(fixedMEUuid != null)
      {
        localScopedMEs = new HashSet<SIBUuid8>();
        localScopedMEs.add(fixedMEUuid);
        
        preferredME = fixedMEUuid;
      }
        
      result = _localisationManager.searchForOutputHandler(preferredME,
          localMessage,
          _remoteSupport,
          localScopedMEs);
    }
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "choosePtoPOutputHandler", result);

    return result;
  }  
 
  
  /**
   * Get the output handler for the local queue point.
   * 
   * @param localMessage
   * @param forcePut
   * @param singleServer
   * @return
   * @throws SIRollbackException
   * @throws SIConnectionLostException
   * @throws SIResourceException
   */
  public abstract OutputHandler getLocalPostMedPtoPOH(boolean localMessage,
                                                    boolean forcePut,
                                                    boolean singleServer) 
    throws 
      SIRollbackException, 
      SIConnectionLostException,
      SIResourceException;


  public void updateLocalisationSet(SIBUuid8 messagingEngineUuid,
                                    Set newQueuePointLocalisingMEUuids)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateLocalisationSet",
        new Object[] {messagingEngineUuid,
          newQueuePointLocalisingMEUuids});

    _localisationManager.updateLocalisationSet(messagingEngineUuid,
                                                          newQueuePointLocalisingMEUuids);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLocalisationSet");
  }
 
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasRemote()
   */
  public boolean hasRemote()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "hasRemote");
      
    boolean hasRemote = _localisationManager.hasRemote();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())      
      SibTr.exit(tc, "hasRemote", new Boolean(hasRemote));
    
    return hasRemote;
  }
  
  /**
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  abstract public boolean cleanupLocalisations() throws SIResourceException;
  

  /**
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  public boolean cleanupPremediatedItemStreams()
  {

	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.entry(tc, "cleanupPremediatedItemStreams");

	    // true if all localisations have been cleaned up successfully    
	    boolean allCleanedUp = true;

	    //Only clean up the post mediated itemstreams when all the pre mediated 
	    //itemstreams have been succesfully cleaned up
	    if (allCleanedUp)
	    {
	      // Check each localisation in turn to see if it should be deleted.
	      synchronized (_postMediatedItemStreamsRequiringCleanup)
	      {
	        HashMap clonedLocalisations =
	          (HashMap) _postMediatedItemStreamsRequiringCleanup.clone();
	        // Reallocate the messages from each localisations itemstream that is in 
	        // to-be-deleted state.
	        Iterator i = clonedLocalisations.keySet().iterator();

	        while (i.hasNext())
	        {
	          SIBUuid8 uuid = (SIBUuid8) i.next();
	          PtoPMessageItemStream ptoPMessageItemStream =
	            (PtoPMessageItemStream) _postMediatedItemStreamsRequiringCleanup.get(uuid);
	             
	          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	            SibTr.debug(tc, "Removing Localisation " + uuid + 
	                            " for destination " + _baseDestinationHandler.getName() + 
	                            " : " + _baseDestinationHandler.getUuid());

	          boolean itemStreamCleanedUp =
	            ptoPMessageItemStream.reallocateMsgs();
	              
	          //we do not remove from xmit queues if it was never added
	          boolean removeFromXmitPoints =
	            !ptoPMessageItemStream.getDeleteRequiredAtReconstitute();

	          if (itemStreamCleanedUp)
	          {
	            _postMediatedItemStreamsRequiringCleanup.remove(uuid);

	            //If the queuePoint is in the "live" queuePoints set, remove it from there too
	            if(removeFromXmitPoints) 
	              _localisationManager.removeXmitQueuePoint(uuid);                
	          }

	          if (allCleanedUp)
	          {
	            allCleanedUp = itemStreamCleanedUp;
	          }
	        }
	      }
	    }  
	     
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "cleanupPremediatedItemStreams", new Boolean(allCleanedUp));

	    return allCleanedUp;
	  
  }
  


  
  /**
   * <p>Add all the live localisations to the set requiring clean-up</p>
   */
  public void addAllLocalisationsForCleanUp(boolean singleServer)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "addAllLocalisationsForCleanUp",
                  new Object[]{ 
                  new Boolean(singleServer)});

        
    _localisationManager.addAllLocalisationsForCleanUp(singleServer,_postMediatedItemStreamsRequiringCleanup);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addAllLocalisationsForCleanUp");
  }  

  /**
   * Method addLocalisationForCleanUp
   * <p>Add a localisations to the set requiring clean-up</p>
   */
  
  // THIS method can be deleted, I do not see the need for this.   
  public void addLocalisationForCleanUp(SIBUuid8 meUuid,
                                         PtoPMessageItemStream ptoPMessageItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "addLocalisationForCleanUp",
                  new Object[]{ 
                    meUuid,
                    ptoPMessageItemStream});
    
    _postMediatedItemStreamsRequiringCleanup.put(meUuid, ptoPMessageItemStream);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addLocalisationForCleanUp");
  }  

    /**
   * @return reallocationLockManager
   */
  public LockManager getReallocationLockManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getReallocationLockManager");
      SibTr.exit(tc, "getReallocationLockManager", _reallocationLockManager);
    }
    
    if (_reallocationLockManager == null)
      _reallocationLockManager = new LockManager();
    
    return _reallocationLockManager;
  }

  /**
   * Method to indicate the destination is to be deleted.   
   */
  public void setToBeDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setToBeDeleted");

    
    // Tell the remote code the destination is to be deleted
    if (_remoteSupport != null)
      _remoteSupport.setToBeDeleted();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setToBeDeleted");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
   */
  public void registerControlAdapters()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerControlAdapters");

    // do nothing
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerControlAdapters");    
  }
  

 
  /**
   * @return
   */
  public AbstractRemoteSupport getRemoteSupport()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteSupport");
      SibTr.exit(tc, "getRemoteSupport", _remoteSupport);
    }
    return _remoteSupport;
  }
  
  abstract public void onExpiryReport();
}
