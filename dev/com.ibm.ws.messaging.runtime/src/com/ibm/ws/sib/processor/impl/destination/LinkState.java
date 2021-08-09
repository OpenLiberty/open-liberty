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
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.trm.links.LinkManager;
import com.ibm.ws.sib.trm.links.LinkSelection;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author nyoung
 * 
 * <p>The LinkState extends the PtoPRealization with state specific to a 
 * LinkHandler. 
 */
public class LinkState extends JSPtoPRealization
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      LinkState.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** NLS for component */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
      
  // Remember the outputHandler for the link, so it can be used as a "best-guess" for
  // where the link is, if WLM cannot provide an authoritative answer
  private OutputHandler _lastKnownMEOutputHandler = null;  

  /**
   * Remember the set of queue points marked TBD.  Only currently used for
   * links where a queue point can be resurrected.  This is so that the
   * stream associated with the xmit queue point can be reused.  We do 
   * this because the other end of the link is in another administrative
   * domain and doesnt know about the delete of this end of the link, so
   * it will still be using the old streams.
   */
  HashMap _deletedQueuePoints = null;


  /**
   * <p>Cold start constructor.</p>
   * <p>Create a new instance of a LinkState.</p>
   * 
   * @param destinationName
   * @param destinationDefinition
   * @param messageProcessor
   * @param parentStream  The Itemstream this DestinationHandler should be
   *         added into. 
   * @param durableSubscriptionsTable  Required only by topicspace 
   *         destinations.  Can be null if point to point (local or remote).
   * @param busName The name of the bus on which the destination resides
   */
  public LinkState(
    BaseDestinationHandler myBaseDestinationHandler,
    MessageProcessor messageProcessor,
    LocalisationManager localisationManager)
  {
    super(myBaseDestinationHandler,messageProcessor,localisationManager);
  }

  /**
   * Method initialise
   * <p>Initialise the PtoPRealization
   */
  public void initialise()
  {
    super.initialise();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initialise");
      
    _deletedQueuePoints = new HashMap();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialise");
  }
  /**
   * Method choosePtoPOutputHandler
   * 
   * <p> Choose OutputHandler for Link.
   *
   * @param linkUuid
   * @return
   * @throws SIResourceException
   */
  public OutputHandler choosePtoPOutputHandler(SIBUuid12 linkUuid) 
    throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "choosePtoPOutputHandler", linkUuid);      
      
    OutputHandler result = null;
    LinkSelection s = null;
    
    if (isLocalMsgsItemStream())
    {
      result = (OutputHandler) getLocalPtoPConsumerManager();
      result.setWLMGuess(false);
    }
    else
    {
      // Pick an ME to send the message to.
      s = _localisationManager.getTRMFacade().chooseLink(linkUuid);      
      
      //If the link is not available in WLM, we either continue putting the
      //messages to where we think the bridge is, but marking them as 
      //guesses, or if we dont know where the bridge is, we put them
      //to the uuid SIMPConstants.UNKNOWN_UUID
      if (s == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Link not available in WLM");

        //lastKnownMEOutputHandler should only ever be null if no outputhandlers exist
        //for the link.  In this case, one should be created.  As TRM/WLM cannot 
        //provide one, a temporary localisation is set up to hold the messages 
        //until TRM can provide a proper response.
        if (_lastKnownMEOutputHandler == null)
        {
          SIBUuid8 localisingME = new SIBUuid8(SIMPConstants.UNKNOWN_UUID);
             
          updateLocalisationSet(localisingME, localisingME);
        }
        
        result = _lastKnownMEOutputHandler;
        result.setWLMGuess(true);
      }
      else
      {
        SIBUuid8 choiceUuid = s.getInboundMeUuid();
        SIBUuid8 routingUuid = s.getOutboundMeUuid();
 
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "InboundMeUuid is " + choiceUuid + " OutboundMeUuid is  " + routingUuid);
        
        if (choiceUuid == null)
        {
          //In the MQLink case there is no inboundMeUuid, so send the message
          //to the outboundMeUuid
          choiceUuid = s.getOutboundMeUuid();
        }

        result = getQueuePointOutputHandler(choiceUuid);
        
  
        if (result == null)
        {        
          updateLocalisationSet(choiceUuid, routingUuid);
  
          result = getQueuePointOutputHandler(choiceUuid);
        }
        result.setWLMGuess(false);
        
        if (routingUuid != null)
        {
          // If this is the ME hosting the link then send to 
          // the foreign ME of the link (choiceUuid) 
          // Otherwise send to the ME hosting the link in this bus
          if(routingUuid.equals( _messageProcessor.getMessagingEngineUuid() ))
          {
            routingUuid = choiceUuid;
          }
          // Currently Links only have one queue point so preferLocal is always true.
          // When multiple links are introduced it will be possible for the result here
          // to be a ConsumerDispatcher and therefore this cast will fail.
          ((PtoPOutputHandler)result).updateRoutingCellule(routingUuid);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "choosePtoPOutputHandler", result);

    return result;   
  }

  /**
   * Method updateLocalisationSet.
   * <p>This method will compare the passed in set of localising ME uuids with the
   * set already known about.  If there are any new localising ME's the infrastructure
   * to be able to send messages to them is created.  If the ME knows about some
   * that are not in WCCM, they are marked for deletion.  If they are still being
   * advertised in WLM, nothing more is done until they are removed from WLM as
   * WLM can still return them as places to send messages too.  If the entries are
   * not in WLM, an attempt is made to rejig the messages.  This will move them
   * to another localisation if possible, or will put them to the exception destination
   * or discard them.  If after the rejig, there are no messages left awaiting 
   * transmission to the deleted localisation, the infrastructure for the localistion
   * is removed, otherwise it is left until the last message has been processed.</p>
   * @param newLocalisingMEUuids
   * @throws SIStoreException
   * @throws SIResourceException
   */
  public SIBUuid8 updateLocalisationSet(SIBUuid8 newLocalisingMEUuid,
                                             SIBUuid8 newRoutingMEUuid) 
    throws  SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "updateLocalisationSet", 
                  new Object[] {newLocalisingMEUuid, 
                                newRoutingMEUuid});
    // Create a local UOW
    LocalTransaction transaction = _baseDestinationHandler.
                                             getTransactionManager().
                                             createLocalTransaction(false);
    Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
    boolean rollback = true; // Assume the worse

    SIBUuid8 existingUuid = null;
    
    try
    {
      synchronized(_localisationManager.getAllXmitQueuePoints())
      {
        HashMap validXmits = new HashMap();
        Iterator it = _localisationManager.getAllXmitQueuePoints().values().iterator();
        while (it.hasNext())
        {
          PtoPXmitMsgsItemStream xmit = (PtoPXmitMsgsItemStream)it.next();
          if (xmit.isToBeDeleted())
            _deletedQueuePoints.put(xmit.getLocalizingMEUuid(), xmit);
          else
            validXmits.put(xmit.getLocalizingMEUuid(), xmit);
        }
    
        //Validity check - there should only by one existing localisation
        if (validXmits.size() > 1 || 
           (validXmits.size() > 0) && (_pToPLocalMsgsItemStream != null))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalisationSet");
          
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LinkHandler",
              "1:302:1.21"});
            
          throw new SIErrorException(nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.LinkState",
              "1:308:1.21"},
            null));        
        }
        
        //Is there a localisation for the new MEUuid that is in the process of
        //being deleted?
        PtoPMessageItemStream deletingItemStream = (PtoPMessageItemStream) _deletedQueuePoints.get(newLocalisingMEUuid);
        if ((deletingItemStream != null) &&
            (!(deletingItemStream.isInStore())))
        {
          //There was an old queuepoint but its no longer in the messagestore, so it
          //has been deleted and we can create a new one instead
          deletingItemStream = null;
        }

        if (validXmits.size() == 0 && _pToPLocalMsgsItemStream == null)
        {
          //There are no live localisations.  Is there one in deleting state?
          if (deletingItemStream != null)
          {
            deletingItemStream.unmarkAsToBeDeleted(msTran);
            _deletedQueuePoints.remove(newLocalisingMEUuid);
              
            _localisationManager.
              attachRemotePtoPLocalisation(deletingItemStream,
                                           _remoteSupport);

            assignQueuePointOutputHandler(deletingItemStream.getOutputHandler(), 
                                          newLocalisingMEUuid); 
          }
          else
          {
            // No localisations existed.  Create the new one
            PtoPMessageItemStream newStream = 
              _localisationManager.addNewRemotePtoPLocalization(transaction, 
                                                              newLocalisingMEUuid,
                                                              null,
                                                              true,
                                                              _remoteSupport);              
            ((PtoPOutputHandler)newStream
              .getOutputHandler())
              .updateRoutingCellule(newRoutingMEUuid);
          }
        }
        else
        {
          //Get the existing uuid
          
          PtoPMessageItemStream ptoPMessageItemStream = null;
          
          // If a local itemstream exists then we are an MQLink that is localised on this ME
          // Therefore dont bother to update state.
          if (_pToPLocalMsgsItemStream == null)
          {
            Iterator transmitQueueIterator = 
              validXmits.values().iterator(); 

            ptoPMessageItemStream = (PtoPMessageItemStream) transmitQueueIterator.next();
          
          
            PtoPOutputHandler outputHandler = (PtoPOutputHandler)ptoPMessageItemStream.getOutputHandler();
            existingUuid = ptoPMessageItemStream.getLocalizingMEUuid(); 

            //Its possible that we dont know the uuid of the localising ME at the other
            //end of the link, as its in another bus so we rely on TRM to get it for us.
            //In this case, the UUID constant UNKNOWN_UUID is used.
            if (newLocalisingMEUuid.toString().equals(SIMPConstants.UNKNOWN_UUID)) 
                 
            {
              // localisingMe is unknown - dont do anything
            }
            else if (existingUuid.equals(newLocalisingMEUuid) || 
                      existingUuid.equals(new SIBUuid8(SIMPConstants.UNKNOWN_UUID)))
            {
              //The existing localisation has the UNKNOWN_UUID constant, but the
              //localisation being passed in has a real uuid, so the existing 
              //localisation should be updated to use the real uuid.
              ptoPMessageItemStream.replaceUuid(newLocalisingMEUuid, msTran);
              
              //Update the localisation in the hashSets
              _localisationManager.removeXmitQueuePoint(existingUuid);
              _localisationManager.addXmitQueuePoint(
                newLocalisingMEUuid, ptoPMessageItemStream);

              outputHandler.updateTargetCellule(newLocalisingMEUuid);

              //Update the outputhandler lookup
              _localisationManager.
                updateQueuePointOutputHandler(newLocalisingMEUuid, 
                                              outputHandler,
                                              existingUuid);
            }
            else
            {
              // The localisation of the link has moved.  Add in the new localisation
              // and delete the old one.

              //Is there an instance of the localisation in deleting state?
              if (deletingItemStream != null)
              {
                deletingItemStream.unmarkAsToBeDeleted(msTran);
                _deletedQueuePoints.remove(newLocalisingMEUuid);
                
                _localisationManager.addXmitQueuePoint(
                  deletingItemStream.getLocalizingMEUuid(),
                  deletingItemStream);
                
                assignQueuePointOutputHandler(deletingItemStream.getOutputHandler(), 
                                              newLocalisingMEUuid);  
              }
              else
              {
                PtoPMessageItemStream newStream = 
                  _localisationManager.addNewRemotePtoPLocalization(transaction, 
                                       newLocalisingMEUuid,
                                       null,
                                       true,
                                       _remoteSupport);
                
                ((PtoPOutputHandler)newStream
                    .getOutputHandler())
                    .updateRoutingCellule(newRoutingMEUuid);
              }                
              
              // 176658.3.3
              transaction.registerCallback(
                  _localisationManager.new LocalizationRemoveTransactionCallback(ptoPMessageItemStream));

               
              // Mark the localisation for deletion
              ptoPMessageItemStream.markAsToBeDeleted(msTran);
              
              //Remember the deleted localisation incase we need to reincarnate it.
              //This is a behaviuor reserved for links.  Because the other end of the
              //link is in another bus and wont know if this end of the link is deleted,
              //it will still be using the stream state associated with this link,
              //so if at a future point the link is recreated to the same ME, we should
              //use the same stream state if we can.
              _deletedQueuePoints.put(existingUuid, ptoPMessageItemStream);
              
              //Get the background clean-up thread to reallocate the messages and
              //clean up the localisation if possible.
              _destinationManager.markDestinationAsCleanUpPending(_baseDestinationHandler);
              
              // If the old link is localised to this ME then it'll have a proxy
              // pubsub neighbour setup, so we need to delete that. The easiest way to 
              // check is to see if a neighbour actually exists or not.
              if(_messageProcessor.getProxyHandler().getNeighbour(existingUuid, true) != null)
              {
                try
                {
                  //Cleanup the neighbours
                  _messageProcessor.getProxyHandler().deleteNeighbourForced(existingUuid, _baseDestinationHandler.getBus() , (Transaction)transaction);
                }
                catch (Exception e)
                {
                  // As the neighbours aren't locked down there's a chance of a problem,
                  // but we'll report the error and carry on.
                  
                  // FFDC
                  FFDCFilter.processException(e, 
                                              "com.ibm.ws.sib.processor.impl.destination.LinkState.updateLocalisationSet", 
                                              "1:473:1.21", 
                                              this);
                  
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Caught exception when deleting neighbour, however this is not always an issue as it might have already been removed: " + e);
                }
              }
            }
            
            // Set the new routingCellule. Take into account the sending MEs that do not have
            // the link localised on their ME          
            if(newRoutingMEUuid != null)
            {
              // If link is localised here
              if (newRoutingMEUuid.equals(_messageProcessor.getMessagingEngineUuid()))
                newRoutingMEUuid = newLocalisingMEUuid;
              
              outputHandler.updateRoutingCellule(newRoutingMEUuid);
            }
          }
        }
      }
      
      // We got through it without failing!
      rollback = false;
    }
    finally
    {
      //Commit the transaction outside of synchronisation of the linkHandler
      try 
      {
        if(rollback)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Update failed, transaction rolling back");
            
          transaction.rollback();
        }
        else
          transaction.commit();
      }
      catch (SIIncorrectCallException e)
      {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "updateLocalisationSet", e);
        throw new SIResourceException(e);
      }
    }
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLocalisationSet", existingUuid);
    
    return existingUuid;
  }

  /**
   * Method assignQueuePointOutputHandler.
   * @param outputHandler
   * <p>Add the outputHandler to the set of queuePointOutputHanders</p>
   */
  public void assignQueuePointOutputHandler(
    OutputHandler outputHandler,
    SIBUuid8 messagingEngineUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "assignQueuePointOutputHandler",
        new Object[] { outputHandler, messagingEngineUuid });

    _localisationManager.assignQueuePointOutputHandler(outputHandler,
                                                                  messagingEngineUuid);
                                                                  
    _lastKnownMEOutputHandler = outputHandler;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assignQueuePointOutputHandler");
  }

  /**
   * Method getRemoteMEUuid 
   * <p>Returns the Uuid of the ME on the remote end of this Link
   */
  public SIBUuid8 getRemoteMEUuid(SIBUuid12 linkUuid) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getRemoteMEUuid", linkUuid);
       
    LinkSelection s = null;
    SIBUuid8 remoteMEUuid = null;
    
    s = _localisationManager.
          getTRMFacade().
            chooseLink(linkUuid);
 
    //If the link is not available in WLM return null as we don't know where to send this 
    if (s == null)
    {
      remoteMEUuid = null;
    }
    else
    {
      remoteMEUuid = s.getInboundMeUuid();
    } 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "getRemoteMEUuid", remoteMEUuid);
         
    return remoteMEUuid;
  }

  /**
   * Method setLinkManager
   * <p>Sets the LinkManager
   */
  public void setLinkManager(LinkManager linkManager)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setLinkManager", linkManager);
    
    _localisationManager.getTRMFacade().setLinkManager(linkManager);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "setLinkManager");
  }

  public void registerControlAdapters() {
    // Override and DO NOT register control adapters for the PtoPLocalItemstream
    // if we are a link
  }

}
