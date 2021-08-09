/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.internal.JsMainAdminComponentImpl;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.DurableInputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AIContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPReceiveMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author nyoung
 * 
 * <p>The RemotePubSub class holds the remote pubsub state specific to a 
 * BaseDestinationHandler that represents a TopicSpace. 
 */
public class RemotePubSubSupport extends AbstractRemoteSupport
{
  /** 
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      RemotePubSubSupport.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
   /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);    

  /**
   * For topicspace destinations, this field maps pseudo destination IDs to instances
   * of AnycastOuputHandler or AnycastInputHandler.  These handlers are created to
   * handle the anycast streams used for remote access to durable subscriptions.
   */
  private Map _pseudoDurableMap = null;

  /**
   * For topicspace destinations, this field maps durable subscription names to 
   * the local AnycastOutputHandler handling remote access to this durable
   * subscription.  Note that the durable subscription name includes the UUID
   * of the home of the subscription.
   */
  private Map<String, AnycastOutputHandler> _pseudoDurableAOHMap = null;

  /**
   * For topicspace destinations, this field maps remote durable subscription names
   * to the local AnycastInputHandler which is accessing that subscription.
   * This mapping is maintained until the subscription disconnects, at which point
   * both the mapping and the AnycastInputHandler are removed.  Note that the 
   * durable subscription name includes the UUID of the home of the subscription.
   */
  private Map<String, AnycastInputHandler> _pseudoDurableAIHMap = null;

  public RemotePubSubSupport(
    BaseDestinationHandler myBaseDestinationHandler,
    MessageProcessor messageProcessor)
  {
    super(myBaseDestinationHandler,messageProcessor);
  }
   
  /**
   * Method initialisePseudoMaps
   */
  public void initialisePseudoMaps()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "initialisePseudoMaps");

    _pseudoDurableMap = Collections.synchronizedMap(new HashMap());
    _pseudoDurableAOHMap = Collections.synchronizedMap(new HashMap<String, AnycastOutputHandler>());
    _pseudoDurableAIHMap = Collections.synchronizedMap(new HashMap<String, AnycastInputHandler>());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialisePseudoMaps");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#reconstituteLocalQueuePoint(int, com.ibm.ws.sib.processor.impl.ConsumerDispatcher)
   */
  public int reconstituteLocalQueuePoint(int startMode) 
  {
    // noop on pubsub
    return 0;
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#closeConsumers()
   */
  public synchronized void closeConsumers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeConsumers");

    // For PubSub iterate over pseudoDurableAIHMap
    synchronized (_pseudoDurableAIHMap)
    {
      for(Iterator i=_pseudoDurableAIHMap.keySet().iterator(); i.hasNext(); )
      {
        AnycastInputHandler next = (AnycastInputHandler) _pseudoDurableAIHMap.get(i.next());
        ConsumerDispatcher rcd = next.getRCD();
          
        // Hmmm, is it possible that a remote durable was created for an alias?
        rcd.closeAllConsumersForDelete(_baseDestinationHandler);
      }
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
      SibTr.entry(
        tc,
        "notifyReceiveAllowedRCD",
        new Object[] { destinationHandler });

    // For PubSub iterate over pseudoDurableAIHMap
    synchronized (_pseudoDurableAIHMap)
    {
      for (Iterator i = _pseudoDurableAIHMap.keySet().iterator(); i.hasNext();)
      {
        String subName = (String) i.next();
        AnycastInputHandler aih =
          (AnycastInputHandler) _pseudoDurableAIHMap.get(subName);

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
   * Method getPostReconstitutePseudoIds
   * <p>Returns an array of all pseudoDestination UUIDs which should
   * be mapped to this BaseDestinationHandler.  This method is 
   * used by the DestinationManager to determine what pseudo 
   * references need to be added after a destination is 
   * reconstituted.
   * 
   * @return An array of all pseudoDestination UUIDs to be mapped
   * to this BaseDestinationHandler.
   */
  public Object[] getPostReconstitutePseudoIds()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPostReconstitutePseudoIds");

    Object[] result = null;

    if (_pseudoDurableMap != null)
      result = _pseudoDurableMap.keySet().toArray();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPostReconstitutePseudoIds", result);

    return result;
  }  

  /**
   * Method reconstituteRemoteDurable
   * <p>Reconstitute any state for remote durable.  There are two types of
   * state here: state for the DME which is stored at the "home" of
   * a durable subscription; and state for the RME which is stored at
   * the ME requesting remote access to a durable subscription.
   * 
   */
  public void reconstituteRemoteDurable(int startMode,
                                        HashMap consumerDispatchersDurable)
    throws
      MessageStoreException,
      SIRollbackException,
      SIConnectionLostException,
      SIIncorrectCallException,
      SIResourceException,
      SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteRemoteDurable", 
          new Object[]{Integer.valueOf(startMode)});

    // At the DME, remote durable will store one AOContainerItemStream
    // for each local durable subscription which was accessed remotely
    // before the ME went down.
    NonLockingCursor cursor =
      _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(AOContainerItemStream.class));

    AOContainerItemStream aoTempItemStream = null;
    for (aoTempItemStream = (AOContainerItemStream) cursor.next();
      aoTempItemStream != null;
      aoTempItemStream = (AOContainerItemStream) cursor.next())
    {
      // NOTE: since this destination is PubSub it should NOT be
      // possible to end up recovering an aostream used for PtoP.
      // Still, bugs happen, so here's a sanity check
      if (aoTempItemStream.getDurablePseudoDestID() == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "reconstituteRemoteDurable", "SIResourceException");

        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport",
              "1:319:1.27",
              null },
            null));
      }

      // Ok, now rebuild remote access to the local durable subscription.
      // There are two cases to consider here:
      // 1) Normal: the durable subscription still exists (NOTE: WE ASSUME
      //    THAT AN EXISTING SUBSCRIPTION HAS NOT CHANGED FROM THE LAST
      //    RESTART!!!!).
      // 2) Deleted: the durable subscription was deleted and the ME
      //    was stopped before the AOH could be cleaned up.
      // For 1, we reconstruct the AOH as usual.  For 2, we remove the streams
      // from the destination.  Note that it's possible that there are
      // in doubt messages in case two, but since this is really only a
      // reference stream there's not much we can do since the durable subscription
      // has been deleted.  We rely on a separate protocol to clean up
      // any stale RMEs in this case.
      String subName = aoTempItemStream.getDurableSubName();
      SIBUuid12 pseudoID = aoTempItemStream.getDurablePseudoDestID();
      String destName = _baseDestinationHandler.constructPseudoDurableDestName(subName);

      // Note that we hold the lock for the entire recovery of a particular
      // stream so that the subscription can't disappear (or reappear)
      synchronized (consumerDispatchersDurable)
      {
        // See if we're in case 1
        ConsumerDispatcher cd =
          (ConsumerDispatcher) consumerDispatchersDurable.get(subName);

        if (cd != null)
        {
          // Case 1: Create aand link up the AOH.  The AOH will handle any
          // existing streams, etc.  
          DestinationDefinition pseudoDest = null;
          AnycastOutputHandler pseudoHandler = null;

          try
          {
            // Create the pseudo destination ID and hack up a definition for it
            // NOTE: have to create the destination first since we need it's ID
            // for the container stream below.
            pseudoDest =
              _messageProcessor.createDestinationDefinition(
                DestinationType.TOPICSPACE,
                destName);

            //defect 259036: if cloned then we are NOT receive
            //exclusive
            pseudoDest.setReceiveExclusive(
              !cd.getConsumerDispatcherState().isCloned());
            pseudoDest.setUUID(pseudoID);

            // Already have the stream
            AOContainerItemStream aostream = aoTempItemStream;

            boolean restartFromStaleBackup = false;

            // Don't do flush if we are asked to start in recovery mode 
            if (((startMode & JsConstants.ME_START_FLUSH)
              == JsConstants.ME_START_FLUSH)
              && ((startMode & JsConstants.ME_START_RECOVERY) == 0))
            {
              restartFromStaleBackup = true;
            }

            // Regenerate the AOH
            pseudoHandler =
              new AnycastOutputHandler(
                pseudoDest.getName(),
                pseudoDest.getUUID(),
                pseudoDest.isReceiveExclusive(),
                null,
                cd,
                aostream,
                _messageProcessor,
                _destinationManager.getAsyncUpdateThread(),
                _destinationManager.getPersistLockThread(),
                System.currentTimeMillis(),
                restartFromStaleBackup);

            // Store the destination in our map and make sure the DestinationManager
            // knows about it for future messages.
            synchronized (_pseudoDurableAOHMap)
            {
              _pseudoDurableAOHMap.put(destName, pseudoHandler);
              _pseudoDurableMap.put(pseudoDest.getUUID(), pseudoHandler);

              // NOTE: we can't actually add the pseudo destination yet because
              // the "real" destination won't be in the index until after
              // the reconstitute code finishes.  The DestinationManager takes
              // care of this after the reconstitute completes.
              //destinationManager.addPseudoDestination(pseudoDest.getUUID(), this);
            }

            // And we're done with this case
          } catch (Exception e)
          {
            // Exception shouldn't occur so FFDC.
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.reconstituteRemoteDurable",
              "1:421:1.27",
              this);
            SibTr.exception(tc, e);
            SibTr.error(
              tc,
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.reconstituteRemoteDurable",
                "1:429:1.27",
                e });

            // Then cleanup anything we managed to create
            if (pseudoDest != null)
            {
              synchronized (_pseudoDurableAOHMap)
              {
                _pseudoDurableAOHMap.remove(destName);
                _pseudoDurableMap.remove(pseudoDest.getUUID());

                // NOTE: this is now unnecessary because we can't add in the
                // pseudoDest until after reconstitute completes.
                //destinationManager.removePseudoDestination(pseudoDest.getUUID());
              }
            }

            // Close off the handler if it ever got started
            if (pseudoHandler != null)
              pseudoHandler.close();

            // And finally throw an SIErrorException
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "reconstituteRemoteDurable", "SIErrorException");

            SIErrorException x =
              new SIErrorException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.reconstituteRemoteDurable",
                    "1:460:1.27",
                    e },
                  null));

            throw x;
          }
        } // if (cd != null)
        else
        {
          // Case 2: Remove the stream and any associated data from the destination
          // TODO: There are failure cases where an RME may still exist somewhere 
          // in the system.  This RME may send requests on an unknown destination (because
          // it's pseudo) and needs to be "flushed" so that it can cleanup properly.
          // To be fixed when anycast finished stale backup recovery.
          LocalTransaction siTran =
            _baseDestinationHandler
              .getTransactionManager()
              .createLocalTransaction(true);
          aoTempItemStream.removeAll((Transaction) siTran);
          siTran.commit();
        }
      } // synchronized (consumerDispatchersDurable)
    } // for(aoTempItemStream...

    cursor.finished();
    // At the RME, remote durable will store one AIContainerItemStream
    // for each remote durable subscription which some user attempted
    // to access in the past.
    cursor =
      _baseDestinationHandler.newNonLockingItemStreamCursor(
        new ClassEqualsFilter(AIContainerItemStream.class));

    AIContainerItemStream aiTempItemStream = null;
    for (aiTempItemStream = (AIContainerItemStream) cursor.next();
      aiTempItemStream != null;
      aiTempItemStream = (AIContainerItemStream) cursor.next())
    {
      // NOTE: since this destination is PubSub it should NOT be
      // possible to end up recovering an aistream used for PtoP.
      // Still, bugs happen, so here's a sanity check
      if ((aiTempItemStream.getDurablePseudoDestID() == null))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "reconstituteRemoteDurable", "SIResourceException");

        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport",
              "1:510:1.27",
              _baseDestinationHandler.getName() },
            null));
      }

      // 571149: There's a chance that we have an empty AIContainerItemStream lying around
      // from a partial removal of a previous remote durable subscription (the removal
      // is not atomic). In this situation we don't want to create an RCD+AIH to represent
      // it as that would prevent subsequent consumers from attaching if it isn't shareable.
      // So instead we simply delete the empty ItemStream
      NonLockingCursor aiStreamCursor = aiTempItemStream.newNonLockingItemStreamCursor(null);
      boolean aiStreamExists = true;
      if(aiStreamCursor.next() == null)
        aiStreamExists = false;
      aiStreamCursor.finished();
      
      if(aiStreamExists)
      {
        // Until anycast handles stale backups, there are two cases to 
        // handle here:
        // 1) All clients were disconnected we just didn't finish cleaning
        //    up the stream before the ME shut down
        // 2) The ME was shut down abrubtly, possibly with a durable client
        //    still connected.
        // We can't actually tell these two cases apart, so the appropriate
        // action is to restart the AIH and force any existing streams
        // to flush.  Once that's done we can delete the AIH (and allow 
        // a new connection from this ME).

        // TODO: for a stale backup, there may be no DME around to handle
        // our flush, or the durable subscription may have been deleted.
        // In these cases, we need a new flush-like protocol to "flush"
        // the subscription and allow the RME to cleanup.
        String subName = aiTempItemStream.getDurableSubName();
        _aiContainerItemStreams.put(subName, aiTempItemStream);

        // Got the ai stream, now track down the rcd stream
        // Unfortunately, there may be more than one so we have
        // to iterate.
        NonLockingCursor rcdCursor =
          _baseDestinationHandler.newNonLockingItemStreamCursor(
            new ClassEqualsFilter(PtoPReceiveMsgsItemStream.class));

        PtoPReceiveMsgsItemStream rcdTempItemStream;
        findRCD : for (
          rcdTempItemStream = (PtoPReceiveMsgsItemStream) rcdCursor.next();
            rcdTempItemStream != null;
            rcdTempItemStream = (PtoPReceiveMsgsItemStream) rcdCursor.next())
        {
          if (rcdTempItemStream.getDurableSubName().equals(subName))
          {
            // Found it, clean it up
            synchronized (consumerDispatchersDurable)
            {
              rcdTempItemStream.reconstitute(_baseDestinationHandler);
              rcdTempItemStream.reallocateMsgs(); // Actually remove
              _rcdItemStreams.put(subName, rcdTempItemStream);

              // Now start restoring the AIH
              SIBUuid12 pseudoDestID = aiTempItemStream.getDurablePseudoDestID();
              DestinationDefinition pseudoDest =
                _messageProcessor.createDestinationDefinition(
                  DestinationType.TOPICSPACE,
                  subName);
              pseudoDest.setUUID(pseudoDestID);
              pseudoDest.setReceiveExclusive(true);

              // Create and register the AIH
              boolean restartFromStaleBackup = false;
              // not clear if durable is concerned with this
              AnycastInputHandler aih =
                new AnycastInputHandler(
                  pseudoDest.getName(),
                  pseudoDest.getUUID(),
                  pseudoDest.isReceiveExclusive(),
                  _messageProcessor,
                  aiTempItemStream,
                  aiTempItemStream.getDmeId(),
                  null,
                  _destinationManager.getAsyncUpdateThread(),
                  _baseDestinationHandler,
                  restartFromStaleBackup,
                  true);
              //we need a control adapter
              _pseudoDurableAIHMap.put(subName, aih);
              _pseudoDurableMap.put(pseudoDestID, aih);

              // Register the handler now because when we create the RCD
              // it will cause the AIH to reject all.

              // NOTE: we can't actually add the pseudo destination yet because
              // the "real" destination won't be in the index until after
              // the reconstitute code finishes.  The DestinationManager takes
              // care of this after the reconstitute completes.
              //destinationManager.addPseudoDestination(pseudoDestID, this);

              //defect 254574: ConsumerDispatcherState subName must be
              //of the form 'string', not 'uuid##string'
              String strippedSubName = _baseDestinationHandler.
                                         getSubNameFromPseudoDestination(subName);
              ConsumerDispatcherState subState =
                new ConsumerDispatcherState(
                  strippedSubName,
                  _baseDestinationHandler.getUuid(),
                  JsMainAdminComponentImpl.getSelectionCriteriaFactory()
                    .createSelectionCriteria(),
                  false,
                  aiTempItemStream.getDurableSubHome(),
                  _baseDestinationHandler.getName(),
                  _baseDestinationHandler.getBus());
              
              // Retrieve the me uuid for the remote dur sub 
              SIBUuid8 durableHomeID = aiTempItemStream.getDmeId();
              // Stash the durableHomeID in the ConsumerDispatcherState
              subState.setRemoteMEUuid(durableHomeID);
             
              // Create the RemoteConsumerDispatcher
              // This is temporary and will be removed once the flush is complete
              // Note that this constructor causes the AIH to start flushing (well,
              // actually not until the inactivity timer kicks in).
              RemoteConsumerDispatcher rcd =
                new RemoteConsumerDispatcher(
                  _baseDestinationHandler,
                  pseudoDest.getName(),
                  rcdTempItemStream,
                  subState,
                  aih,
                  _baseDestinationHandler.getTransactionManager(),
                  true);

              // Mark the RCD as being deleted (so we don't reuse it without checking its
              // state first)
              rcd.setPendingDelete(true);
              
              // Register a callback to be invoked when the AIH
              // stream is next flushed
              aih.addFlushedCallback(new CleanupDurableRME(rcd));

              // The rcd gets entered in to prevent a reattachment to the same durable
              // until the old one is cleaned up.
              consumerDispatchersDurable.put(subName, rcd);
            } // synchronized(consumerDispatchersDurable)

            // done
            break findRCD;
          }
        } // for(rcdTempItemStream...

        rcdCursor.finished();

        // If rcdTempItemStream is null here then something weird happened and
        // we can't completely restore the aistream.  Throw an exception
        if (rcdTempItemStream == null)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteRemoteDurable", "SIResourceException");

          throw new SIResourceException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0003",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.reconstituteRemoteDurable",
                "1:673:1.27",
                null },
              null));
        }
      } // (AIStream exists)
      // No stored AIStream so there's nothing to reconstitute. Instead we remove this empty
      // ItemStream
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Removing empty AIContainerItemStream for " + aiTempItemStream.getDurableSubName());
        
        SIMPTransactionManager txMan = _messageProcessor.getTXManager();
        LocalTransaction mpTxn = txMan.createLocalTransaction(false);
        try {
          Transaction msTxn = txMan.resolveAndEnlistMsgStoreTransaction(mpTxn);
          // No need to lock the item first as we haven't completed GD recovery yet
          aiTempItemStream.remove(msTxn, Item.NO_LOCK_ID);
          mpTxn.commit();
        }
        catch (Exception e) {
          // FFDC and continue. The state will remain.
          FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.reconstituteRemoteDurable",
              "1:698:1.27",
              this);
        }
      }
    } // for(aiTempItemStream...

    cursor.finished();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteRemoteDurable");
  }

  ////////////////////////////////////////////////////////
  // Inner Classes
  ////////////////////////////////////////////////////////
  /**
   * This inner class is used to handle a flush callback
   * on a reconstituted aih stream used for remote durable.
   * When the callback is invoked, it is safe to cleanup
   * the aih and allow other clients access to the remote
   * durable subscription. 
   */
  class CleanupDurableRME
    implements Runnable
  {
    RemoteConsumerDispatcher _rcd;
    
    public CleanupDurableRME(RemoteConsumerDispatcher rcd)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "CleanupDurableRME", rcd);

      _rcd = rcd;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "CleanupDurableRME", this);
    }
    
    public void run()
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "run");

      try
      {
        _rcd.deleteConsumerDispatcher(false);
      } 
      catch(Exception e)
      {
        // Since we're on our own thread, there's not much
        // we can do with the exception here except log it
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.CleanupDurableRME.run",
          "1:752:1.27",
          _rcd.getConsumerDispatcherState().getSubscriberID());

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.CleanupDurableRME",
            "1:759:1.27",
            _rcd.getConsumerDispatcherState().getSubscriberID()});        
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "run");
    }
  }

  /**
   * Method deleteRemoteDurableRME
   * <p>Clean up AIH Map.
   *
   * @param subState Subscription state.
   * @throws SIResourceException
   */
  public boolean deleteRemoteDurableRME(String subName,
                                     AnycastInputHandler aih,
                                     TransactionCommon siTran)
    throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "deleteRemoteDurableRME", 
                  new Object[] { subName, aih });

    boolean deleting = false;
    SIBUuid12 pseudoDestID = aih.getDestUuid();
    SIMPItemStream aihStream =
      (SIMPItemStream) _aiContainerItemStreams.get(subName);
    PtoPMessageItemStream rcdStream =
      (PtoPMessageItemStream) _rcdItemStreams.get(subName);      
    
    // It's possible that the RCD has been reused while we were waiting for the delete
    // to happen - better check that no-one is currently attached.
    RemoteConsumerDispatcher rcd = aih.getRCD();
    if(!rcd.hasConsumersAttached())
    {
      // Synchronize to avoid race with delete destination
      synchronized (_pseudoDurableAIHMap)
      {
        // It's possible we're racing with a delete in which case
        // all of this will be unnecessary
        if (_pseudoDurableAIHMap.containsKey(subName))
        {
          deleting = true;
          aih.delete();
          Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(siTran);
          aihStream.removeAll(msTran);
          _aiContainerItemStreams.remove(subName);
          rcdStream.removeAll(msTran);
          _rcdItemStreams.remove(subName);
          _pseudoDurableAIHMap.remove(subName);
          _pseudoDurableMap.remove(pseudoDestID);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteRemoteDurableRME", Boolean.valueOf(deleting));
      
    return deleting;
  }

  /**
   * Method deleteRemoteDurableDME
   * <p>Clean up the local AnycastOutputHandler that was created to handle
   * access to a locally homed durable subscription.  This method should
   * only be invoked as part of ConsumerDispatcher.deleteConsumerDispatcher.
   *
   * @param subName Name of the local durable subscription being removed.
   */
  public void deleteRemoteDurableDME(String subName)
    throws SIRollbackException, SIConnectionLostException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteRemoteDurableDME", new Object[] { subName });

    String destName = _baseDestinationHandler.constructPseudoDurableDestName(subName);

    // Short circuit if there was never any remote access
    if (!_pseudoDurableAOHMap.containsKey(destName))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteRemoteDurableDME", "No remote Access");
      return;
    }

    try
    {
      // Remove the DME side as if we were cleaning up the destination (which, in a sense, we are).
      AnycastOutputHandler aoh =
        (AnycastOutputHandler) _pseudoDurableAOHMap.get(destName);
      SIBUuid12 pseudoDestID = aoh.getDestUUID();
      SIMPItemStream aohStream = aoh.getItemStream();

      boolean aohCleanedup = aoh.cleanup(true, false);
      if (aohCleanedup)
      {
        LocalTransaction siTran = 
          _baseDestinationHandler.
            getTransactionManager().
            createLocalTransaction(true);
        aohStream.removeAll((Transaction) siTran);
        siTran.commit();
      }
      // remove these even if aoh is not done cleaning up
      _pseudoDurableAOHMap.remove(destName);
      _pseudoDurableMap.remove(pseudoDestID);
      _destinationManager.removePseudoDestination(pseudoDestID);
    } catch (SIIncorrectCallException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteRemoteDurableDME", "SIResourceException");
      throw new SIResourceException(e);
    } catch (MessageStoreException e)
    {
      // Exception shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.deleteRemoteDurableDME",
        "1:881:1.27",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteRemoteDurableDME", "SIResourceException");
      throw new SIResourceException(e);
    }

    // All done, return
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteRemoteDurableDME");
  }


  /**
   * Method getAIHByName
   * @param name
   * @return
   */
  public AnycastInputHandler getAIHByName(String name)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAIHByName", name);

    AnycastInputHandler result = 
      (AnycastInputHandler) _pseudoDurableAIHMap.get(name);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAIHByName", result);

    return result;
  }  

  /**
   * Method cleanupLocalisations
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  public synchronized boolean cleanupLocalisations(HashMap consumerDispatchersDurable) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanupLocalisations");

    // true if all localisations have been cleaned up successfully    
    boolean allCleanedUp = true;

    try
    {
      LocalTransaction siTran = _baseDestinationHandler.
                                          getTransactionManager().
                                          createLocalTransaction(true);
              
      // First cycle through the AnycastOutputHandlers
      synchronized (_pseudoDurableAOHMap)
      {
        List doneSet = new ArrayList();
        for(Iterator i=_pseudoDurableAOHMap.keySet().iterator(); i.hasNext();)
        {
          String               subName    = (String) i.next();
          AnycastOutputHandler aoh        = (AnycastOutputHandler) _pseudoDurableAOHMap.get(subName);
          SIBUuid12            pseudoDest = aoh.getDestUUID();
          SIMPItemStream       aohStream  = aoh.getItemStream();
                
          // any asynchronous cleanup is done by the aoh itself since redriveDeletionThread parameter is false
          boolean aohCleanedup = aoh.cleanup(true, false);
          if (aohCleanedup)
          {
            aohStream.removeAll((Transaction) siTran);
            _pseudoDurableMap.remove(pseudoDest);
            _baseDestinationHandler.getDestinationManager().removePseudoDestination(pseudoDest);
            doneSet.add(subName);
          }
          // update whether the entire aoh can be cleaned up
          allCleanedUp &= aohCleanedup;
        }
                
        // to avoid concurrent modification exceptions, remove the keys here
        for(Iterator r=doneSet.iterator(); r.hasNext();)
          _pseudoDurableAOHMap.remove(r.next());
      }

      // Then cycle through the AnycastInputHandlers
      synchronized (_pseudoDurableAIHMap)
      {
        List doneSet = new ArrayList();
        for(Iterator i=_pseudoDurableAIHMap.keySet().iterator(); i.hasNext();)
        {
          String                subName    = (String) i.next();
          AnycastInputHandler   aih        = (AnycastInputHandler) _pseudoDurableAIHMap.get(subName);
          SIBUuid12             pseudoDest = aih.getDestUuid();
          SIMPItemStream        aihStream  = (SIMPItemStream) _aiContainerItemStreams.get(subName);
          PtoPMessageItemStream rcdStream  = (PtoPMessageItemStream) _rcdItemStreams.get(subName);

          boolean aihCleanedUp = true;
          if ( !(aih.destinationDeleted()) )
          {
            //The AIH is not flushed yet
            aihCleanedUp = false;
          }
          else 
          {
            aih.delete();
            aihStream.removeAll((Transaction) siTran);
            _aiContainerItemStreams.remove(subName);
            rcdStream.removeAll((Transaction) siTran);
            _rcdItemStreams.remove(subName);
            _pseudoDurableMap.remove(pseudoDest);
            _baseDestinationHandler.getDestinationManager().removePseudoDestination(pseudoDest);
            
            // Lock the table before attempting to remove the subscription
            synchronized (consumerDispatchersDurable)
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
                SibTr.debug(tc, "cleanupLocalisations", "Remove subscription " + subName + " from durable CDs table");                    
              consumerDispatchersDurable.remove(subName);
            }
            doneSet.add(subName);
          }
                 
          allCleanedUp &= aihCleanedUp;
        }
                
        // to avoid concurrent modification exceptions, remove the keys here
        for(Iterator r=doneSet.iterator(); r.hasNext();)
          _pseudoDurableAIHMap.remove(r.next());
      }

      // Finally, commit the transaction              
      siTran.commit();
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.RemotePubSubSupport.cleanupLocalisations",
        "1:1017:1.27",
        this);

      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "cleanupLocalisations",
          "SIResourceException");
          throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupLocalisations", Boolean.valueOf(allCleanedUp));

    return allCleanedUp;
  }

  /**
   * Method getPseudoDurableAIHMap
   * @return
   */
  public Map getPseudoDurableAIHMap()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getPseudoDurableAIHMap");
      SibTr.exit(tc, "getPseudoDurableAIHMap", _pseudoDurableAIHMap);
    }
    return _pseudoDurableAIHMap;
  }
  
  /**
   * Method locateExistingAOH
   * <p>Attempt to handle a remote request to attach to a local durable
   * subscription.  The attach is successful if no exceptions are thrown.
   * 
   * @param request The ControlCreateStream request message.
   */
  public AnycastOutputHandler locateExistingAOH(ControlCreateStream request,
                                SIBUuid8 sender,
                                String destName,
                                ConsumerDispatcherState subState)
    throws
      SIDurableSubscriptionMismatchException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "locateExistingAOH", 
                  new Object[] { request,
                                 sender,
                                 destName,
                                 subState });

    AnycastOutputHandler handler = null;
    
    synchronized (_pseudoDurableAOHMap)
    {
      handler =
        (AnycastOutputHandler) _pseudoDurableAOHMap.get(destName);
      if (handler != null)
      {
        // Alreay exists, extract the ConsumerDispatcher and see if
        // the sub parameters match.

        ConsumerDispatcher cd = handler.getPubSubConsumerDispatcher();

        if (!cd.getConsumerDispatcherState().equals(subState))
        {
          // Subscriptions are different for the same subname
          // The old subscription must be deleted first, so throw
          // an exception.
          SIDurableSubscriptionMismatchException e =
            new SIDurableSubscriptionMismatchException(
              nls.getFormattedMessage(
                "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                new Object[] {
                  subState.getSubscriberID(),
                  _messageProcessor.getMessagingEngineName()},
                null));
          SibTr.exception(tc, e);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "locateExistingAOH", subState);
          throw e;
        }

        // If security is enabled, then check the user who is attempting 
        // to attach matches the user who created the durable sub.
        if (_messageProcessor.isBusSecure())
        {
          if (!cd.getConsumerDispatcherState().equalUser(subState))
          {
            // Users don't match 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "locateExistingAOH", subState);

            throw new SIDurableSubscriptionMismatchException(
              nls.getFormattedMessage(
                "USER_NOT_AUTH_ACTIVATE_ERROR_CWSIP0312",
                new Object[] {
                  subState.getUser(),
                  subState.getSubscriberID(),
                  _baseDestinationHandler.getName()},
                null));
          }
        }

        // Looks good, let the AOH handle it
        handler.handleControlMessage(sender, request);

      }
    }


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "locateExistingAOH", handler);
      
    return handler;
  }
  

  /**
   * Method storePseudoDestination
   * @param pseudoHandler
   * @param destName
   * @param pseudoDest
   */
  public void storePseudoDestination(AnycastOutputHandler pseudoHandler,
                                     String destName,
                                     DestinationDefinition pseudoDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "storePseudoDestination", 
                  new Object[] { pseudoHandler,
                                 destName,
                                 pseudoDest });

    synchronized (_pseudoDurableAOHMap)
    {
      _pseudoDurableAOHMap.put(destName, pseudoHandler);
      _pseudoDurableMap.put(pseudoDest.getUUID(), pseudoHandler);
      _destinationManager.addPseudoDestination(
        pseudoDest.getUUID(),
        _baseDestinationHandler);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "storePseudoDestination");
  }    

  /**
   * Method cleanupPseudoDestination
   * <p>Remove AOH from map
   * 
   * @param destName
   * @param pseudoDest
   */
  public void cleanupPseudoDestination(String destName,
                                       DestinationDefinition pseudoDest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, 
                  "cleanupPseudoDestination", 
                  new Object[] { destName,
                                 pseudoDest });

    synchronized (_pseudoDurableAOHMap)
    {
      _pseudoDurableAOHMap.remove(destName);
      _pseudoDurableMap.remove(pseudoDest.getUUID());
      _destinationManager.removePseudoDestination(pseudoDest.getUUID());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanupPseudoDestination");
  }    

  /**
   * Method getAnycastOHForPseudoDest
   * <p>Durable subscriptions homed on this ME but attached to from remote MEs
   * have AnycastOutputHandlers mapped by their pseudo destination names.
   * 
   * @return The AnycastOutput for this pseudo destination
   */
  public AnycastOutputHandler getAnycastOHForPseudoDest(String destName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAnycastOHForPseudoDest", destName);
    AnycastOutputHandler returnAOH = null;
    if (_pseudoDurableAOHMap != null)
    {
      synchronized (_pseudoDurableAOHMap)
      {
        returnAOH = (AnycastOutputHandler) _pseudoDurableAOHMap.get(destName);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAnycastOHForPseudoDest", returnAOH);
    return returnAOH;

  }
  
  /**
   * Method createRemoteConsumerDispatcher
   * @param remSubName
   * @param subState
   * @param durableME
   * @return
   * @throws Exception
   */
  public RemoteConsumerDispatcher createRemoteConsumerDispatcher(
    String remSubName,
    ConsumerDispatcherState subState,
    SIBUuid8 durableMEUuid)
    throws
      Exception
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "createRemoteConsumerDispatcher",
        new Object[] { remSubName, subState, durableMEUuid });

    RemoteConsumerDispatcher rcd = null;
    
        // Ask the local DurableInputHandler to issue the request.  On success, this method
        // will return with a ControlNotFLushed for the new pseudo destination.
        // Otherwise, an exception is thrown and passed through to our caller.  NOTE: the
        // calling thread is blocked while we're waiting for a reply.
        ControlNotFlushed notFlushed =
          DurableInputHandler.issueCreateStreamRequest(
            _messageProcessor,
            subState,
            _baseDestinationHandler.getDefinition().getUUID(),
            durableMEUuid);

        // Success.  Create and register the new pseudo destination.
        SIBUuid12 newDestID =
          notFlushed.getGuaranteedTargetDestinationDefinitionUUID();
        DestinationDefinition pseudoDest =
          _messageProcessor.createDestinationDefinition(
            DestinationType.TOPICSPACE,
            remSubName);
        pseudoDest.setUUID(newDestID);
        
        // If the subscription isn't cloned only a single consumer is allowed to
        // attach at a time. This is also an indication that messages should be delivered
        // in order - which remote get does by checking the (pseudo) destination's receive
        // exclusive setting.
        pseudoDest.setReceiveExclusive(!subState.isCloned());

        // Unlike regular anycast, we need to use a different key for the container item
        // streams as we may have many anycast connections to the same dme.  As a result,
        // we know we're always creating fresh item streams.
        LocalTransaction siTran = 
          _baseDestinationHandler.
            getTransactionManager().
            createLocalTransaction(true);

        // create aiContainerItemStream
        AIContainerItemStream aiContainerItemStream =
          new AIContainerItemStream(
            durableMEUuid,
            null,
            newDestID,
            remSubName,
            subState.getDurableHome());
        _baseDestinationHandler.addItemStream(aiContainerItemStream, (Transaction) siTran);
        _aiContainerItemStreams.put(remSubName, aiContainerItemStream);

        // create rcdItemStream
        PtoPReceiveMsgsItemStream rcdItemStream =
          new PtoPReceiveMsgsItemStream(
            _baseDestinationHandler,
            durableMEUuid,
            remSubName);
        _baseDestinationHandler.addItemStream(rcdItemStream, (Transaction) siTran);
        _rcdItemStreams.put(remSubName, rcdItemStream);

        siTran.commit();

        // Create and register the AIH
        boolean restartFromStaleBackup = false;
        // not clear if durable is concerned with this
        AnycastInputHandler aih =
          new AnycastInputHandler(
            pseudoDest.getName(),
            pseudoDest.getUUID(),
            pseudoDest.isReceiveExclusive(),
            _messageProcessor,
            aiContainerItemStream,
            durableMEUuid,
            null,
            _destinationManager.getAsyncUpdateThread(),
            _baseDestinationHandler,
            restartFromStaleBackup,
            true);
        //we want a controllable
        _pseudoDurableAIHMap.put(remSubName, aih);
        _pseudoDurableMap.put(newDestID, aih);

        // Create the RemoteConsumerDispatcher
        rcd =
          new RemoteConsumerDispatcher(
            _baseDestinationHandler,
            pseudoDest.getName(),
            rcdItemStream,
            subState,
            aih,
            _baseDestinationHandler.getTransactionManager(),
            !subState.isCloned()); //cardinalityOne=true if not cloned
        rcd.setReadyForUse();

        // Prepare the AIH, and deliver the NotFlushed message
        // ASSERT: durableME.getUuid().equals(notFlushed.getGuaranteedSourceMessagingEngineUUID())
        aih.prepareForDurableStartup(notFlushed.getRequestID());

        // Register the handler with the destination AFTER everything is set up
        // so we don't try and process liveness messages before we're ready.
        _destinationManager.addPseudoDestination(
          newDestID,
          _baseDestinationHandler);

        // Deliver the NotFlushed message
        aih.handleControlMessage(durableMEUuid, notFlushed);

    // All done, return
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createRemoteConsumerDispatcher", rcd);
    
    return rcd;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#getAnycastInputHandlerByPseudoDestId(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public AnycastInputHandler getAnycastInputHandlerByPseudoDestId(SIBUuid12 destID)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "getAnycastInputHandlerByPseudoDestId",
                  new Object[]{ 
                    destID});

    AnycastInputHandler aih = null;

    if (_pseudoDurableMap != null)
      aih = (AnycastInputHandler) _pseudoDurableMap.get(destID);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAnycastInputHandlerByPseudoDestId", aih);

    return aih;
  }
  
/* (non-Javadoc)
 * @see com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport#getAnycastOutputHandlerByPseudoDestId(com.ibm.ws.sib.utils.SIBUuid12)
 */
  public AnycastOutputHandler getAnycastOutputHandlerByPseudoDestId(SIBUuid12 destID)
  {
  	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
  	  SibTr.entry(tc, 
  				  "getAnycastOutputHandlerByPseudoDestId",
  				  new Object[]{ 
  					destID});
  
  	AnycastOutputHandler aoh = null;
  
  	if (_pseudoDurableMap != null)
  	  aoh = (AnycastOutputHandler) _pseudoDurableMap.get(destID);
      
  	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
  	  SibTr.exit(tc, "getAnycastOutputHandlerByPseudoDestId", aoh);
  
  	return aoh;
  }    
    
  public Iterator<AnycastInputControl> getAIControlAdapterIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getAIControlAdapterIterator");
    
    ArrayList<AnycastInputControl> controlAdapters = new ArrayList<AnycastInputControl>();
    synchronized (_pseudoDurableAIHMap) 
    { 
      Iterator<AnycastInputHandler> it = _pseudoDurableAIHMap.values().iterator();
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
    
    ArrayList<ControlAdapter> controlAdapters = new ArrayList<ControlAdapter>();
    synchronized (_pseudoDurableAOHMap) 
    { 
      Iterator<AnycastOutputHandler> it = _pseudoDurableAOHMap.values().iterator();
      while(it.hasNext())
      {
        Iterator<ControlAdapter> aoStreams = it.next().getAOControlAdapterIterator();
        while(aoStreams.hasNext())
          controlAdapters.add(aoStreams.next());
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getAOControlAdapterIterator", controlAdapters);
    return controlAdapters.iterator();
  }
}
