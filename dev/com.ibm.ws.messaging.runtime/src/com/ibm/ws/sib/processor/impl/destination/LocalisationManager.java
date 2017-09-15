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
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.trm.dlm.Selection;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author Neil Young
 *
 * <p>The LocalisationManager is responsible for managing the set of localisations
 * defined for a Destination.
 */

// Venu temp
// Localisation Manager is basically to contact to WLM for obtaining information about
// OutPutHanlder,QueuePoint.
// Modifying this class so that only LocalME consumerdispachters are returned as OutPutHandler
// Few functions are made as dummy
public class LocalisationManager
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      LocalisationManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * NLS for component
   */
  static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  /** Reference to the associated BaseDestinationHandler */
  protected BaseDestinationHandler _baseDestinationHandler;

  /**
   * For a point to point destination, there can be more than one instance of the
   * destination i.e. it can be cloned.  In this case there will be an OutputHandler
   * associated with each ME that localises the destination and these are stored in
   * the outputHandlers hashmap.
   *
   * For remote destinations, there will be a PtoPOutputHandler.
   */
  HashMap<SIBUuid8, OutputHandler> _queuePointOutputHandlers = null;

  /**
   * For each destination, the set of Uuids of MEs which host a queue point
   * is required.
   * <p>It is passed on calls to TRM when choosing a localisation
   * of a destination to send a message too.
   */
  private HashSet<SIBUuid8> _queuePointsGuessSet = null;

  /**
   * For each destination, the set of Uuids of MEs which host a queue point
   * (excluding the local ME) is required.
   * <p>It is passed on calls to TRM when choosing a localisation
   * of a destination to issue a remote get from
   */
  private HashSet<SIBUuid8> _remoteQueuePointsGuessSet = null;


  /** The LocalisationManager handles the set of localisations and
   * interfaces to WLM */
  protected TRMFacade _trmFacade = null;

  /** Does this destination have a local localization */
  private boolean _hasLocal = false;

  /** Does this destination have any remote localizations */
  private boolean _hasRemote = false;

  /** Is this a temporary destination? */
  private boolean _isTemporary = false;

  /** Is this a system destination? */
  private boolean _isSystem;

  /** Indicator as to whether the ME is running in an ND environment */
  private boolean _singleServer;

  /**
   * Each Destination has 1..n Localisations.  Each Localisation represents
   * a partition of a point to point Destination.  Non partitioned point to
   * point Destinations and Topicspace Destinations will have exactly one
   * Localisation.
   * <p>
   * Feature 174199.2.19
   */
  protected HashMap<SIBUuid8, LocalizationPoint> _xmitQueuePoints = null;
  private MessageProcessor _messageProcessor;

  /**
   * <p>Constructor.</p>
   *
   * @param destinationName
   * @param destinationDefinition
   * @param parentStream  The Itemstream this DestinationHandler should be
   *         added into.
   * @param durableSubscriptionsTable  Required only by topicspace
   *         destinations.  Can be null if point to point (local or remote).
   * @param busName The name of the bus on which the destination resides
   */
  public LocalisationManager(BaseDestinationHandler baseDestinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "LocalisationManager",
        new Object[] {
          baseDestinationHandler });

    _baseDestinationHandler = baseDestinationHandler;
    _messageProcessor = _baseDestinationHandler.getMessageProcessor();
    _queuePointOutputHandlers = new HashMap<SIBUuid8, OutputHandler>(1);
    _queuePointsGuessSet = new HashSet<SIBUuid8>(1);
    _remoteQueuePointsGuessSet = new HashSet<SIBUuid8>(1);

    _isSystem = baseDestinationHandler.isSystem();
    _isTemporary = baseDestinationHandler.isTemporary();

    _trmFacade = new TRMFacade(baseDestinationHandler);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LocalisationManager", this);
  }

  public void initialise(boolean singleServer)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initialise");

    //Check if we are running in an ND environment.  If not we can skip
    //some performance intensive WLM work
    _singleServer = singleServer;

    //  Remote init
    _xmitQueuePoints = new HashMap<SIBUuid8, LocalizationPoint>();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initialise");
  }

  /**
   * Method performWLMLookup
   *
   * <p> Calls through to the TRM facade with the appropriate guess set in
   * order to find a localisation of a destination.
   *
   * @param preferredME
   * @param localMessage
   * @return
   */
  public Selection performWLMLookup(SIBUuid8 preferredME,
                                    boolean localMessage,
                                    HashSet<SIBUuid8> scopedMEs)
  {
	  //Venu temp
	  // For Liberty runtime, there would not be WLM and as in this release of Liberty, in runtime this 
	  //function is not get called. Hence just returning null
	  return null;
	  
  }

  /**
   * Method updateTrmAdvertisements
   *
   * <p> Works out what capabilities need to be registered in TRM for our current
   * state, and what needs to be deregistered. It will then update TRM,
   * registering and deregistering only those capabilities that have changed since
   * the last update.
   */
  public void updateTrmAdvertisements()
  {}

  /**
   * Method getQueuePointOutputHandler
   *
   * <p> Retrieves a queue point output handler
   * @param SIBUuid8
   * @return OutputHandler if it can be found or null
   */
  public OutputHandler getQueuePointOutputHandler(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuePointOutputHandler", new Object[] { meUuid });

    OutputHandler result = null;

    synchronized(_queuePointOutputHandlers)
    {
      result = (OutputHandler) _queuePointOutputHandlers.get(meUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuePointOutputHandler", result);
    return result;
  }

  /**
   * Method removeQueuePointOutputHandler
   * <p> Removes a queue point output handler
   * @param SIBUuid8
   * @return OutputHandler if it can be found or null
   */
  public void removeQueuePointOutputHandler(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeQueuePointOutputHandler", new Object[] { meUuid });

    synchronized(_queuePointOutputHandlers)
    {
      _queuePointOutputHandlers.remove(meUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeQueuePointOutputHandler");
  }

  /**
   * Method doesMEHostQueuePoint
   *
   * <p> Checks whether an ME hosts a queue point
   * @param SIBUuid8
   * @return boolean
   */
  public boolean doesMEHostQueuePoint(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "doesMEHostQueuePoint", new Object[] { meUuid });

    boolean result = _queuePointsGuessSet.contains(meUuid);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doesMEHostQueuePoint", Boolean.valueOf(result));
    return result;
  }



  public OutputHandler searchForOutputHandler(SIBUuid8 MEUuid,
                                              boolean localMessage,
                                              AbstractRemoteSupport remoteSupport,
                                              HashSet<SIBUuid8> scopedMEs)
  throws
    SIRollbackException,
    SIConnectionLostException,
    SIResourceException
  {   
	  //Venu temp
	  // For Liberty runtime, there would not be WLM and as in this release of Liberty, in runtime this 
	  //function is not get called. Hence just returning null
	  return null;
  }


  /**
   * Method localQueuePointRemoved.
   *
   * <p>Remove the queuePoint from our queue points guess set, which we use to
   * guess where to send messages if WLM isnt working</p>
   *
   * Also update TRM if necessary.
   */
  public void localQueuePointRemoved(SIBUuid8 messagingEngineUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "localQueuePointRemoved",
        new Object[] {messagingEngineUuid});

    synchronized(_queuePointsGuessSet)
    {
      _queuePointsGuessSet.remove(messagingEngineUuid);

      // This ME no longer localises the destination
      _hasLocal = false;
    }

    //update TRM if necessary
    updateTrmAdvertisements();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "localQueuePointRemoved");

  }


  /**
   * Method isQueuePointStillAdvertisedForGet
   *
   * <p> Asks the TRMFacade whether the QueuePoint is still advertised for GET.
   * @param meUuid
   * @return
   */
  public boolean isQueuePointStillAdvertisedForGet(SIBUuid8 meUuid)
  {
	  //Venu temp
	  // Without contacting WLM, returning as true as default
    return true;
  }

  /**
   * Method updateLocalisationSet
   *
   * <p> Updates the Localisation sets of a Destination.
   *
   * @param messagingEngineUuid
   * @param newQueuePointLocalisingMEUuids
   * @throws SIResourceException
   */
  public void updateLocalisationSet(SIBUuid8 messagingEngineUuid,
                                    Set newQueuePointLocalisingMEUuids)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateLocalisationSet",
        new Object[] {messagingEngineUuid,
          newQueuePointLocalisingMEUuids});

    // If the Queue points are not null, then synchronize (Queue destination)
    if (_queuePointsGuessSet != null)
    {
      // Add in any new localisations into the queuepoint guess set
      synchronized (_queuePointsGuessSet)
      {
        _queuePointsGuessSet.clear();
        _hasLocal = false;
        _hasRemote = false;
        Iterator i = newQueuePointLocalisingMEUuids.iterator();
        while (i.hasNext())
        {
          SIBUuid8 meUuid = new SIBUuid8((String) i.next());
          _queuePointsGuessSet.add(meUuid);
          if(meUuid.equals(messagingEngineUuid))
          {
            _hasLocal = true;
          }
          else
          {
            _hasRemote = true;
          }
        }
      }
    }

    // If the Remote Queue points are not null, then synchronize (Queue destination)
    if (_remoteQueuePointsGuessSet != null)
    {
      synchronized(_remoteQueuePointsGuessSet)
      {
        HashSet<SIBUuid8> temp = (HashSet<SIBUuid8>)_queuePointsGuessSet.clone();
        synchronized(temp)
        {
          _remoteQueuePointsGuessSet = temp;
          _remoteQueuePointsGuessSet.remove(messagingEngineUuid);
        }
      }
    }

    updateTrmAdvertisements();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateLocalisationSet");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasLocal()
   */
  public boolean hasLocal()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "hasLocal");
      SibTr.exit(tc, "hasLocal", Boolean.valueOf(_hasLocal));
    }

    return _hasLocal;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasRemote()
   */
  public boolean hasRemote()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "hasRemote");
      SibTr.exit(tc, "hasRemote", Boolean.valueOf(_hasRemote));
    }

    return _hasRemote;
  }

  /**
   * Method setRemote
   *
   * <p> Resets hasRemote.
   */
  public void setRemote(boolean hasRemote)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRemote", Boolean.valueOf(hasRemote));

    _hasRemote = hasRemote;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRemote");
  }

  /**
   * Method setLocal
   *
   * <p> Resets hasLocal.
   */
  public void setLocal()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setLocal", Boolean.valueOf(_hasLocal));

    _hasLocal = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setLocal");
  }


  /**
   * Method chooseRemoteQueuePoint
   *
   * <p> Picks a QueuePoint localisations through TRM.
   *
   * @return SIBUuid8
   */
  public SIBUuid8 chooseRemoteQueuePoint(SIBUuid8 fixedMEUuid, HashSet<SIBUuid8> scopedMEs)
  {
	//Venu temp
	  // For Liberty runtime, there would not be WLM and as in this release of Liberty, in runtime this 
	  //function is not get called. Hence just returning null
	  return null;
  }

  /**
   * Method clearLocalisingUuidsSet.
   *
   * <p> Clear the set of ME's that localise the destination
   */
  public void clearLocalisingUuidsSet()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "clearLocalisingUuidsSet");

    if (_queuePointsGuessSet != null)
    {
      synchronized (_queuePointsGuessSet)
      {
        _queuePointsGuessSet.clear();
      }
    }

    if (_remoteQueuePointsGuessSet != null)
    {
      synchronized (_remoteQueuePointsGuessSet)
      {
        _remoteQueuePointsGuessSet.clear();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "clearLocalisingUuidsSet");
    return;
  }


  /**
   * Method flushQueuePointOutputHandler.
   *
   */
  public boolean flushQueuePointOutputHandler()
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "flushQueuePointOutputHandler");

    boolean done = true;

    for(Iterator nextHandler = _queuePointOutputHandlers.values().iterator();
        nextHandler.hasNext();)
    {
      Object handlerQ = nextHandler.next();
      // Try and drive flush on every output handler.
      // Note that we'll try and drive every output handler
      // even if an earlier output handler required a deferred
      // delete.
      if (handlerQ instanceof PtoPOutputHandler)
        done &= ((PtoPOutputHandler) handlerQ).flushAllForDelete();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "flushQueuePointOutputHandler", new Boolean(done));
    return done;
  }

  /**
   * Method checkQueuePointOutputHandlers
   *
   * <p> Iterate over the set of QueuePoint OutputHandlers to see if
   * any have not exceeded their QHigh limit.
   * @return
   */
  public int checkRemoteMessagePointOutputHandlers(SIBUuid8 fixedMEUuid,
                                                   HashSet<SIBUuid8> scopedMEs)
  {
	  
	//Venu temp
	  // For Liberty runtime, there would not be WLM and as in this release of Liberty, in runtime this 
	  //function is not get called. Hence just returning zero
	  
	  return 0;
  }

  /**
   * Method getQueuePointGuessSet
   * <p> Returns the guess set for queue points.
   * Unit tests only
   * @return
   */
  HashSet getQueuePointGuessSet()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getQueuePointGuessSet");

    HashSet theQueuePoints = null;

    synchronized (_queuePointsGuessSet)
    {
      theQueuePoints = (HashSet) _queuePointsGuessSet.clone();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getQueuePointGuessSet", theQueuePoints);

    return theQueuePoints;
  }

  /**
   * Method addMEToQueuePointGuessSet
   *
   * <p> Adds an ME to the guess set for queue points.
   * Unit tests only
   * @return
   */
  public void addMEToQueuePointGuessSet(SIBUuid8 messagingEngineUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMEToQueuePointGuessSet");

    synchronized (_queuePointsGuessSet)
    {
      _queuePointsGuessSet.add(messagingEngineUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMEToQueuePointGuessSet");
  }

  /**
   * Method addMEToRemoteQueuePointGuessSet
   * <p> Adds an ME to the remote guess set for queue points.
   * Unit tests only
   * @return
   */
  public void addMEToRemoteQueuePointGuessSet(SIBUuid8 messagingEngineUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMEToRemoteQueuePointGuessSet");

    synchronized (_remoteQueuePointsGuessSet)
    {
      _remoteQueuePointsGuessSet.add(messagingEngineUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMEToRemoteQueuePointGuessSet");
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

    synchronized (_queuePointOutputHandlers)
    {
      _queuePointOutputHandlers.put(messagingEngineUuid, outputHandler);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assignQueuePointOutputHandler");
  }

  /**
   * Method updateQueuePointOutputHandler.
   * @param outputHandler
   * <p>Add the outputHandler to the set of queuePointOutputHanders</p>
   */
  public void updateQueuePointOutputHandler(
    SIBUuid8 newLocalisingMEUuid,
    OutputHandler outputHandler,
    SIBUuid8 existingUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "updateQueuePointOutputHandler",
        new Object[] { newLocalisingMEUuid, outputHandler, existingUuid });

    synchronized(_queuePointOutputHandlers)
    {
      _queuePointOutputHandlers.remove(existingUuid);
      _queuePointOutputHandlers.put(newLocalisingMEUuid, outputHandler);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateQueuePointOutputHandler");
  }

  /**
   * Method addNewRemotePtoPLocalisation
   * <p> Suppports the addition of a new remote PtoP Localisation of a
   * destination.
   * @param transaction
   * @param messagingEngineUuid
   * @param destinationLocalizationDefinition
   * @param queuePoint
   * @return
   * @throws SIResourceException
   */
  public PtoPMessageItemStream addNewRemotePtoPLocalization(
    TransactionCommon transaction,
    SIBUuid8 messagingEngineUuid,
    LocalizationDefinition destinationLocalizationDefinition,
    boolean queuePoint,
    AbstractRemoteSupport remoteSupport) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addNewRemotePtoPLocalization",
        new Object[] {
          transaction,
          messagingEngineUuid,
          destinationLocalizationDefinition,
          Boolean.valueOf(queuePoint),
          remoteSupport });

    PtoPMessageItemStream newMsgItemStream = null;

    // Add to the MessageStore
    try
    {

        if (queuePoint)
        {
          newMsgItemStream =
            new PtoPXmitMsgsItemStream(_baseDestinationHandler, messagingEngineUuid);
        }

      transaction.registerCallback(
        new LocalizationAddTransactionCallback(newMsgItemStream));

      Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
      _baseDestinationHandler.addItemStream(newMsgItemStream, msTran);

      // Set the default limits
      newMsgItemStream.setDefaultDestLimits();
      // Setup any message depth interval checking (510343)
      newMsgItemStream.setDestMsgInterval();

      attachRemotePtoPLocalisation(newMsgItemStream, remoteSupport);
    }
    catch (OutOfCacheSpace e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addNewRemotePtoPLocalization", "SIResourceException");

      throw new SIResourceException(e);
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.destination.LocalisationManager.addNewRemotePtoPLocalization",
        "1:1562:1.30",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "addNewRemotePtoPLocalization", e);

      throw new SIResourceException(e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNewRemotePtoPLocalization", newMsgItemStream);

    return newMsgItemStream;
  }


  /**
   * Method attachRemotePtoPLocalisation
   *
   * <p> Creates new remote PtoPOutputHandler.
   *
   * @param ptoPMessageItemStream
   * @throws SIResourceException
   */
  public void attachRemotePtoPLocalisation(
    LocalizationPoint ptoPMessageItemStream,
    AbstractRemoteSupport remoteSupport) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "attachRemotePtoPLocalisation",
        new Object[] {
          ptoPMessageItemStream,
          remoteSupport});

    setRemote(true);
    PtoPOutputHandler remoteOutputHandler;

      synchronized(_xmitQueuePoints)
      {
        _xmitQueuePoints.put(
          ptoPMessageItemStream.getLocalizingMEUuid(),
          ptoPMessageItemStream);
      }

      // Create the new point to point output handler for this
      // destination
      remoteOutputHandler =
        new PtoPOutputHandler(_baseDestinationHandler,
                              _baseDestinationHandler.getMessageProcessor(),
                              remoteSupport.getSourceProtocolItemStream(),
                              ptoPMessageItemStream.getLocalizingMEUuid(),
                              (PtoPMessageItemStream) ptoPMessageItemStream);

      ptoPMessageItemStream.setOutputHandler(remoteOutputHandler);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attachRemotePtoPLocalisation");
  }

  /**
   * Method getXmitQueuePoint
   * <p> Return the itemstream representing a transmit queue to a remote ME
   * @param meUuid
   * @return
   */
  public PtoPXmitMsgsItemStream getXmitQueuePoint(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getXmitQueuePoint", meUuid);

    PtoPXmitMsgsItemStream stream = null;

    if (_xmitQueuePoints != null)
    {
      synchronized(_xmitQueuePoints)
      {
        stream = (PtoPXmitMsgsItemStream) _xmitQueuePoints.get(meUuid);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getXmitQueuePoint", stream);

    return stream;
  }

  public int getXmitQueuePointsSize()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getXmitQueuePointsSize");

    int size = _xmitQueuePoints.size();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getXmitQueuePointsSize", Integer.valueOf(size));
    return size;
  }

  /**
   * Method removeXmitQueuePoint
   * @param meUuid
   * @return
   */
  public void removeXmitQueuePoint(SIBUuid8 meUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeXmitQueuePoint", meUuid);

    if (_xmitQueuePoints != null)
    {
      synchronized(_xmitQueuePoints)
      {
        _xmitQueuePoints.remove(meUuid);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeXmitQueuePoint");
  }

  /**
   * Method addXmitQueuePoint
   * @param meUuid
   * @param ptoPMessageItemStream
   */
  public void addXmitQueuePoint(SIBUuid8 meUuid,
                                PtoPMessageItemStream ptoPMessageItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addXmitQueuePoint", meUuid);

    if (_xmitQueuePoints != null)
    {
      synchronized(_xmitQueuePoints)
      {
        _xmitQueuePoints.put(meUuid,ptoPMessageItemStream);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addXmitQueuePoint");
  }

  /**
   * Method getAllXmitQueuePoints
   * <p>Return the itemstream representing a transmit queue to a remote ME
   * @param meUuid
   * @return
   */
  public HashMap getAllXmitQueuePoints()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAllXmitQueuePoints");
      SibTr.exit(tc, "getAllXmitQueuePoints", _xmitQueuePoints);
    }

    return _xmitQueuePoints;
  }

  /**
   * Method getXmitQueueIterator
   * <p>Return a cloned iterator over the transmit queue points
   * for this link.
   * @return Iterator
   */
  public Iterator<PtoPMessageItemStream> getXmitQueueIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getXmitQueueIterator");

    Iterator<PtoPMessageItemStream> itr = null;

    synchronized(_xmitQueuePoints)
    {
      itr = ((HashMap)_xmitQueuePoints.clone()).values().iterator();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getXmitQueueIterator", itr);

    return itr;
  }


  /**
   * Method dereferenceLocalisation
   * <p>Called back by a PtoPMessageItemStream when the Transaction containing it commits.
   * Removes the localisation and its associated OutputHandler
   * from the destination.
   *
   * @param ptoPMessageItemStream  The localisation to dereference.
   */
  public void dereferenceLocalisation(LocalizationPoint ptoPMessageItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceLocalisation", ptoPMessageItemStream);

    // Reset the reference to the local messages itemstream if it is being removed.
      synchronized(_xmitQueuePoints)
      {
        _xmitQueuePoints.remove(ptoPMessageItemStream.getLocalizingMEUuid());
      }

      /*
       * Remove the outputHandler from the set of OutputHandlers, if
       * it exists.
       */
      removeQueuePointOutputHandler(ptoPMessageItemStream.getLocalizingMEUuid());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceLocalisation");
  }


  /**
   * Method reallocateTransmissionStreams
   * @param ignoredStream
   */
  public void reallocateTransmissionStreams(PtoPXmitMsgsItemStream ignoredStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reallocateTransmissionStreams", ignoredStream);

    if(_xmitQueuePoints != null )
    {
      LockManager lockManager = _baseDestinationHandler.getReallocationLockManager();
      lockManager.lockExclusive();
      try
      {
        if (_xmitQueuePoints != null)
        {
          //PK57432 Cant hold queuePoints lock while we call reallocate
          HashMap clonedXmitQueuePoints;
          synchronized(_xmitQueuePoints)
          {
            // It's possible that while we're iterating over the existing xmit streams and
            // their messages that we inadvertently create a new xmit stream (while in
            // searchForPtoPOutputHandler). This obviously invalidates the list, so instead
            // we take a copy up front, safe in the knowledge that any newly created xmit
            // stream can't possibly need its messages reallocated.
            clonedXmitQueuePoints =
                     (HashMap)_xmitQueuePoints.clone();
          }

          Iterator itr = clonedXmitQueuePoints.values().iterator();
          while(itr.hasNext())
          {
            PtoPXmitMsgsItemStream xmitQueue = (PtoPXmitMsgsItemStream)itr.next();
            if (xmitQueue != ignoredStream)
              xmitQueue.reallocateMsgs();
          }
        }
      }
      finally
      {
        lockManager.unlockExclusive();
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reallocateTransmissionStreams");
  }

  /**
   * Method addAllLocalisationsForCleanUp
   * <p>Add all the live localisations to the set requiring clean-up</p>
   */
  public void addAllLocalisationsForCleanUp(boolean singleServer,HashMap postMediatedItemStreamsRequiringCleanup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "addAllLocalisationsForCleanUp" ,
                  new Object[]{
                  Boolean.valueOf(singleServer)});

    // If _xmitQueuePoints not null, then this is a Queue
    if (_xmitQueuePoints != null)
    {
      synchronized(_xmitQueuePoints)
      {
        {
          Iterator itr = _xmitQueuePoints.values().iterator();
          while(itr.hasNext())
          {
            PtoPXmitMsgsItemStream xmitQueue = (PtoPXmitMsgsItemStream)itr.next();
            postMediatedItemStreamsRequiringCleanup.put(xmitQueue.getLocalizingMEUuid(), xmitQueue);
          }
        }
      }
    }

    

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addAllLocalisationsForCleanUp");
  }


  /**
   * Method updateRemoteQueuePointSet
   * <p>
   * @param newQueuePointLocalisingMEUuids
   * @throws SIResourceException
   */
  public void updateRemoteQueuePointSet(Set newQueuePointLocalisingMEUuids,HashMap _postMediatedItemStreamsRequiringCleanup)
    throws SIResourceException
  {}

  /**
   * Method searchForPtoPOutputHandler
   * <p>Method will attempt to get an output handler to a remote localisation
   * If the outputHandler does not exist, a transmit queue is created etc
    * @param preferredME
   * @param localMessage
   * @return
   * @throws SIRollbackException
   * @throws SIConnectionLostException
   * @throws SIResourceException
   */
  private OutputHandler searchForPtoPOutputHandler(SIBUuid8 preferredME,
                                                  boolean localMessage,
                                                  AbstractRemoteSupport remoteSupport,
                                                  HashSet<SIBUuid8> scopedMEs)
    throws
      SIRollbackException,
      SIConnectionLostException,
      SIResourceException
  {
	  //Venu temp
	  // For Liberty runtime, there would not be WLM. In this release of Liberty, in runtime this 
	  //function is not get called. Hence just returning null
	  
	  return null;
	  }

  /**
   * Method registerDestination
   * <p>Register the destination vith WLM via TRM</p>
   *
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing
   * mechanisms.
   *
   */
  public void registerDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerDestination");

    updateTrmAdvertisements();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerDestination");
  }

  /**
   * Method deregisterDestination.
   * <p>Deregister the destination vith WLM via TRM</p>
   *
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing
   * mechanisms.
   */
  public void deregisterDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deregisterDestination");

    _trmFacade.deregisterDestination();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deregisterDestination");
  }

  /**
   * Method addNewLocalPtoPLocalisation
   * <p>
   * @param transaction
   * @param newMsgItemStream
   * @throws SIResourceException
   */
  public void addNewLocalPtoPLocalisation(
    TransactionCommon transaction,
    PtoPMessageItemStream newMsgItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addNewLocalPtoPLocalisation",
        new Object[] {
          transaction,
          newMsgItemStream });

      transaction.registerCallback(
        new LocalizationAddTransactionCallback(newMsgItemStream));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNewLocalPtoPLocalisation");

  }

  /**
   * Method getTRMFacade
   * @return __trmFacade
   */
  public TRMFacade getTRMFacade()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTRMFacade", this);
    // Instantiate DA manager to interface to WLM

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTRMFacade",_trmFacade);

    return _trmFacade;
  }

  /**
   * Called on completion of a transaction that adds a localization
   * Feature 176658.3.2
   */
  public class LocalizationAddTransactionCallback
    implements TransactionCallback
  {
    private PtoPMessageItemStream _newMsgItemStream;

    public LocalizationAddTransactionCallback(PtoPMessageItemStream newMsgItemStream)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "LocalizationAddTransactionCallback", newMsgItemStream);
      _newMsgItemStream = newMsgItemStream;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LocalizationAddTransactionCallback",this);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public void beforeCompletion(TransactionCommon transaction)
    {
      // Nothing to do
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
     */
    public void afterCompletion(TransactionCommon transaction, boolean committed)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "afterCompletion",
          new Object[] { transaction, Boolean.valueOf(committed)});

      if (committed)
      {

        /**
         * The localisation is now available.  If its got an associated
         * OutputHandler, then assign this to the destinationHandler
         */
        if (_newMsgItemStream.getOutputHandler() != null)
        {

            // Careful, need to drive appropriate override if this
            // is for a LinkHandler
            _baseDestinationHandler.assignQueuePointOutputHandler(
              _newMsgItemStream.getOutputHandler(),
              _newMsgItemStream.getLocalizingMEUuid());


          /*
           * If its a point to point destination and the outputHandler is
           * associated with the local ME, then unlock it.
           */
          if (!_baseDestinationHandler.isPubSub()
            && (_newMsgItemStream.getOutputHandler()
              instanceof ConsumerDispatcher))
          {
            ConsumerDispatcher consumerDispatcher =
              (ConsumerDispatcher) _newMsgItemStream.getOutputHandler();
            consumerDispatcher.setReadyForUse();
          }
        }

        // If we have added a new local queue localization to this ME, advertise in WLM
        if((_baseDestinationHandler.getPtoPRealization() != null) &&
           (_newMsgItemStream.getLocalizingMEUuid().equals(_messageProcessor.getMessagingEngineUuid())))
        {
          _baseDestinationHandler.
            getPtoPRealization().
              registerDestination(hasLocal(),
                                  _baseDestinationHandler.isDeleted());
        }

        _newMsgItemStream.registerControlAdapterAsMBean();

        _newMsgItemStream = null;

      }
      else
      { // !committed

        // Dereferemce the outputHandler
        _newMsgItemStream.setOutputHandler(null);

        /*
         * Tell destinationHandler to dereference this localisation
         * definition and other objects associated with it such as
         * the outputHandler
         */
        dereferenceLocalisation(_newMsgItemStream);

        _newMsgItemStream = null;

      }
      //update TRM if necessary
      updateTrmAdvertisements();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "afterCompletion");
    }
  }

  /**
   * Called on completion of a transaction that removes a localization
   * Feature 176658.3.3
   */
  public class LocalizationRemoveTransactionCallback
    implements TransactionCallback
  {
    private PtoPMessageItemStream _itemStream;

    public LocalizationRemoveTransactionCallback(PtoPMessageItemStream itemStream)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "LocalizationRemoveTransactionCallback", itemStream);
      _itemStream = itemStream;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LocalizationRemoveTransactionCallback", this);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public void beforeCompletion(TransactionCommon transaction)
    {
      // Nothing to do
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
     */
    public void afterCompletion(TransactionCommon transaction, boolean committed)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(
          tc,
          "afterCompletion",
          new Object[] { transaction, Boolean.valueOf(committed)});

      if (committed)
      {

        /*
         * 174199.2.14
         *
         * Tell destinationHandler to dereference this localisation
         * definition and other objects associated with it such as
         * the outputHandler
         */
        dereferenceLocalisation(_itemStream);

      }
      else
      { // !committed

        /*
         * The delete failed, so the localisation must be unlocked
         * and made available for use again.
         */

        if (_itemStream.getOutputHandler() != null)
        {
          /*
           * If its a point to point destination and the outputHandler is
           * associated with the local ME, then unlock it.
           */
          if (!_baseDestinationHandler.isPubSub()
            && (_itemStream.getOutputHandler() instanceof ConsumerDispatcher))
          {
            ConsumerDispatcher consumerDispatcher =
              (ConsumerDispatcher) _itemStream.getOutputHandler();
            consumerDispatcher.setReadyForUse();
          }
        }

      }
      //update TRM if necessary
      updateTrmAdvertisements();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "afterCompletion");
    }
  }


  /**
   * Set the message processor instance.
   * This is normally done when reconstituting a LinkHandler.
   * @param messageProcessor
   */
  public void setMessageProcessor(MessageProcessor messageProcessor)
  {
    _messageProcessor = messageProcessor;
  }

  /**
   * Method getAllGetLocalisations returns a set of messaging engine uuids for all the messaging
   * engines currently localisaing the destination with a GET capability
   * @return Set of messaging engine uuids currently localising the destination
   */
  public Set<SIBUuid8> getAllGetLocalisations () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAllGetLocalisations");

    final Set<SIBUuid8> rc = _trmFacade.getAllGetLocalisations();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAllGetLocalisations", rc);
    return rc;
  }

}
