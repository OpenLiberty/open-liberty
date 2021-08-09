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
package com.ibm.ws.sib.processor.impl.interfaces;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.BaseLocalizationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.destination.AbstractRemoteSupport;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/**
 * @author nyoung
 * 
 * <p>The PtoPRealization the PtoP state specific to a BaseDestinationHandler
 * that represents a Queue. 
 */
public interface PtoPRealization
{  
  /**
   * Method initialise
   * <p>Initialise the PtoPRealization
   */
  public void initialise();

  /**
   * Method createInputHandlersForPtoP.
   * <p> Create the PtoP and preMediated InputHandlers
   */
  public void createInputHandlersForPtoP();
  
  
  /**
   * Method reconstitute
   * <p>Recover a BaseDestinationHandler retrieved from the MessageStore.
   * 
   * @param processor
   * @param durableSubscriptionsTable
   * 
   * @throws Exception
   */
  public void reconstitute(int startMode,
                           DestinationDefinition definition,
                           boolean isToBeDeleted,
                           boolean isSystem) 
    throws SIDiscriminatorSyntaxException, MessageStoreException, SIResourceException;

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#reconstituteEnoughForDeletion()
   */
  public void reconstituteEnoughForDeletion()
    throws
      MessageStoreException, SIResourceException;

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#searchLocalPtoPOutputHandler(boolean, boolean, boolean)
   */
  public OutputHandler getLocalPostMedPtoPOH(boolean localMessage,
                                                    boolean forcePut,
                                                    boolean singleServer);

  /**
   * Method getAnycastOutputHandler
   * <p>Called to get the AnycastOutputHandler for this Destination
   * @return
   */
  public AnycastOutputHandler getAnycastOutputHandler(DestinationDefinition definition,
                                                                         boolean restartFromStaleBackup);
 
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
   */
  public ControlHandler getControlHandler(SIBUuid8 sourceMEUuid);

  /**
   * Methodget QueuePointOutputHandler 
   * <p>Retrieves a queue point output handler
   * @param SIBUuid8
   * @return OutputHandler if it can be found or null
   */  
  public OutputHandler getQueuePointOutputHandler(SIBUuid8 meUuid);

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getLocalPtoPConsumerDispatcher()
   */
  public ConsumerManager getLocalPtoPConsumerManager();

  /**
   * Method assignQueuePointOutputHandler.
   * @param outputHandler
   * <p>Add the outputHandler to the set of queuePointOutputHanders</p>
   */
  public void assignQueuePointOutputHandler(
    OutputHandler outputHandler,
    SIBUuid8 messagingEngineUuid);

  /**
   * Method dereferenceLocalisation
   * <p>Called back by a PtoPMessageItemStream when the Transaction containing it commits.
   * Removes the localisation and its associated OutputHandler
   * from the destination.
   * 
   * @param localisation  The localisation to dereference.
   */
  public void dereferenceLocalisation(LocalizationPoint localizationPoint);
  
  /**
   * Method localQueuePointRemoved
   * @param isDeleted
   * @param isSystem
   * @param isTemporary
   * @param messagingEngineUuid
   * @throws SIResourceException
   */
  public void localQueuePointRemoved(boolean isDeleted,
                                     boolean isSystem,
                                     boolean isTemporary,
                                     SIBUuid8 messagingEngineUuid)
    throws SIResourceException;

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQueuePoint(com.ibm.ws.sib.utils.SIBUuid8)
   */
  public LocalizationPoint getQueuePoint(SIBUuid8 meUuid);

  /**
   * Returns the guess set for queue points.
   * Unit tests only
   * @return
   */
  public HashSet getQueuePointGuessSet();

  /**
   * Method updateLocalisationDefinition.
   * <p>This method updates the destinationLocalizationDefinition associated with the
   * destinationHandler (if the destination is localised on this ME)
   * and performs any necessary modifications to the
   * message store and other components to reflect the new state of the
   * destinationHandler.</p>
   * @param destinationLocalizationDefinition
   * <p>Updates the DestinationLocalizationDefinition associated with the 
   * destination.</p>
   */
  public void updateLocalisationDefinition(BaseLocalizationDefinition destinationLocalizationDefinition,
    TransactionCommon transaction) 
     throws 
       SIResourceException;

  /**
   * Method addNewPtoPLocalisation
   * 
   * <p> Create a new PtoPMessageItemStream and add it to this Destination's Localisations.
   * <p>
   * In addition to creating and adding it, this function also performs all the 
   * necessary updates to make it a recognized part of the Destination.
   * 
   * @param localisationIsRemote should be true if the localisation is remote.
   * @param transaction  The Transaction to add under.  Cannot be null.
   * @param messagingEngineUuid The uuid of the messaging engine that owns the localisation
   * @return destinationLocalizationDefinition.
   * 
   * @throws SIResourceException if the add fails due to a Message Store problem.
   */
  public LocalizationPoint addNewPtoPLocalization(
    boolean localisationIsRemote,
    TransactionCommon transaction,
    SIBUuid8 messagingEngineUuid,
    BaseLocalizationDefinition destinationLocalizationDefinition,
    boolean queuePoint) throws SIResourceException;

  /**
   * Method cleanupLocalisations.
   * <p>Cleanup any localisations of the destination that require it</p>
   */
  public boolean cleanupLocalisations() throws SIResourceException;

  /**
   * Method clearLocalisingUuidsSet.
   * Clear the set of ME's that localise the destination
   */
  public void clearLocalisingUuidsSet();

  /**
   * Method addAllLocalisationsForCleanUp.
   * <p>Add all the live localisations to the set requiring clean-up</p>
   */
  public void addAllLocalisationsForCleanUp(boolean singleServer);

  public LocalizationPoint getPtoPLocalLocalizationPoint();
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
   */
  public void registerControlAdapters();

  /**
   * Method chooseConsumerDispatcher
   * 
   * <p> Retrieves a ConsumerManager associated with this PtoP Destination.
   * @param definition
   * @param isReceiveAllowed
   * @return ConsumerManager
   * @throws SIResourceException
   */
  public ConsumerManager chooseConsumerManager(
    DestinationDefinition definition,
    boolean isReceiveAllowed,
    SIBUuid12 gatheringTargetUuid,
    SIBUuid8 fixedMEUuid,
    HashSet<SIBUuid8> scopedMEs)
    throws SIResourceException;
  
  /**
   * Method attachPtoPLocalisation
   * <p> Attach a Localisation to this Destination's Localisations.
   * This entails:
   * <p>
   * 1. Initializing the Localisation with this BaseDestinationHandler's details.
   * 2. Adding the Localisation to this BaseDestinationHandler's list.
   * 3. Creating new input/output handlers as appropriate.
   * <p>
   * Feature 174199.2.7
   * 
   * @param ptoPMessageItemStream is the PtoPMessageItemStream to add.
   * @param localisationIsRemote should be true if the PtoPMessageItemStream is remote.
   * @param transaction is the Transaction to add it under.
   */
  public void attachPtoPLocalisation(
    LocalizationPoint localizationPoint,
    boolean localisationIsRemote) throws SIResourceException;

  /**
   * Method attachLocalPtoPLocalisation
   * 
   * <p> Attach a local Localisation to this Destination's Localisations.
   * 
   * @param ptoPMessageItemStream is the PtoPMessageItemStream to add.
   * @param localisationIsRemote should be true if the PtoPMessageItemStream is remote.
   * @param transaction is the Transaction to add it under.
   */
  public void attachLocalPtoPLocalisation(LocalizationPoint localizationPoint);

  /**
   * Method runtimeEventOccurred.
   * @param pevent
   */
  public void runtimeEventOccurred(MPRuntimeEvent event);

  /**
   * Method flushQueuePointOutputHandler.
   *
   * <p>Add the outputHandler to the set of MediationPointOutputHanders</p>
   */
  public boolean flushQueuePointOutputHandler()
    throws SIResourceException;
    
  /**
   * Method registerDestination.
   * <p>Register the destination vith WLM via TRM</p>
   * 
   * System destinations and Temporary destinations are not registered
   * with WLM.  The destinations themselves have their own addressing 
   * mechanisms.
   */
  public void registerDestination(boolean hasLocal,
                           boolean isDeleted);
  
  public void onExpiryReport();
  
  /**
   * @return
   */
  public boolean isLocalMsgsItemStream();

  /**
   * Method checkAbleToSend
   * 
   * <p> Return the sendallowed value associated with the itemstream.
   * @return 
   */
  public int checkAbleToSend();

  /**
   * Method updateRemoteQueuePointSet
   * @param newQueuePointLocalisingMEUuids
   * @throws SIResourceException
   */
  public void updateRemoteQueuePointSet(Set newQueuePointLocalisingMEUuids)
    throws SIResourceException;

  /**
   * Mark the DestinationHandler for deletion.
   */
  public void setToBeDeleted();

  /**
   * @return
   */
  public AbstractRemoteSupport getRemoteSupport();
}
