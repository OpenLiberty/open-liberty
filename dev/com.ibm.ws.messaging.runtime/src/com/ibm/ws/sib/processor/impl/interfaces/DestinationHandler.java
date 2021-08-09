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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.SecurityContext;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionIndex;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.proxyhandler.Neighbour;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * @author millwood
 * 
 *         <p>The destination class holds information about a destination. For
 *         cloned destinations, there is one destination object instance, with
 *         a number of queuing points associated with it. Destinations are
 *         managed by the DestinationManager class.
 */
public interface DestinationHandler extends ControllableResource
{
    /**
     * Method getInputHandler
     * 
     * @param type The protocol type
     * @param sourceCellule The source ME.
     * @param msg The JsMessage for which an input handler is being sought. The ANYCASTINPUT
     *            protocol type requires this message to properly resolve the input handler for
     *            remote durable data messages. This argument may be null for other protocols.
     * 
     * @return InputHandler
     *         <p>Temporary code. The allocation of input handlers and item streams
     *         to input handlers is changing in the updated design, so for the time
     *         being this temporary code that always assigns a new item stream to a
     *         new input handler is good enough</p>
     *         <p>The destination looks after the management of itemstreams that are used
     *         by producers and by the destination consumer dispatchers. When a producer
     *         requests a new producer point, the destination does the following with
     *         item streams when creating the input handler</p>
     *         <ul>
     *         <li> Point to point (non cloned): Use the consumer dispatcher itemstream</li>
     *         <li> Point to point (cloned): </li>
     *         <li> Pub / Sub: Use the ...</li>
     *         </ul>
     */
    public InputHandler getInputHandler(ProtocolType type, SIBUuid8 sourceMEUuid, JsMessage msg);

    public InputHandler getInputHandler();

    /**
     * There are certain times when a message sent to a destination will need a routing
     * address set into the message (e.g. sending to a foreign bus). This method returns
     * the address to use if required.
     * 
     * @param inAddress The address of the destination referencing this destination (e.g. an alias)
     * @param fixedMessagePoint Is the message producer bound to a single message point?
     * @return The address to set in the message (possibly null)
     * @throws SIResourceException
     */
    public JsDestinationAddress getRoutingDestinationAddr(JsDestinationAddress inAddress,
                                                          boolean fixedMessagePoint)
                    throws SIResourceException;

    /**
     * Method choosePtoPOutputHandler.
     * 
     * @param fixedME Not null if the sender mandates a particular ME
     * @param preferredME Not null if the sender prefers a particular ME
     * @param localMessage Was the message produced locally?
     * @param forcePut ensure the put does not fail due to queue high limits or msg properties
     * @param scopedMEs A list of MEs that the caller (scoped alias) restricts the choice to
     * @return OutputHandler
     * @throws SIException
     *             <p>Call TRM to pick an output handler and return it to the caller.</p>
     */
    public OutputHandler choosePtoPOutputHandler(SIBUuid8 fixedME,
                                                 SIBUuid8 preferredME,
                                                 boolean localMessage,
                                                 boolean forcePut,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIResourceException,
                    SIErrorException;

    /**
     * Choose a consumer dispatcher from which to get messages
     * 
     * @param gatherMessages Is message gathering enabled for the consumer
     * @param fixedME Not null if the sender mandates a particular ME
     * @param scopedMEs A list of MEs that the caller (scoped alias) restricts the choice to
     * @return ConsumerDispatcher
     */
    public ConsumerManager chooseConsumerManager(SIBUuid12 gatheringTargetUuid,
                                                 SIBUuid8 fixedME,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws SIResourceException;

    /**
     * Method checkPtoPOutputHandler.
     * 
     * @param fixedMEUuid Is the check scoped to a single ME (message point)?
     * @param scopedMessagePoints A list of message points that the caller (scoped alias) restricts the check to
     * @return int
     *         <p>Check all potential ouput handlers to ensure there is at least one that can take message.
     */
    public int checkPtoPOutputHandlers(SIBUuid8 fixedMEUuid, HashSet<SIBUuid8> scopedMEs);

    /**
     * Can a message be stored on the queue points associated with this destination.
     * 
     * @param meUuid Is the check scoped to a single ME (message point)?
     * @param scopedMEs scopedMessagePoints A list of message points that the caller (scoped alias) restricts the check to
     * @return OUTPUT_HANDLER_FOUND if the message can be stored or OUTPUT_HANDLER_NOT_FOUND, etc otherwise
     */
    public int checkCanAcceptMessage(SIBUuid8 meUuid, HashSet<SIBUuid8> scopedMEs);

    /**
     * Result of check of output handlers
     */
    public static final int NOT_SET = -1;
    public static final int OUTPUT_HANDLER_NOT_FOUND = 0;
    public static final int OUTPUT_HANDLER_FOUND = 1;
    public static final int OUTPUT_HANDLER_SEND_ALLOWED_FALSE = 2;
    public static final int OUTPUT_HANDLER_ALL_HIGH_LIMIT = 3;

    public RemoteConsumerDispatcher getRemoteConsumerDispatcher(SIBUuid8 meId, SIBUuid12 gatheringTargetDestUuid, boolean createAIH);

    /**
     * Get an iterator over all control adapter for the aistreams for this destination. Used by the
     * controllables to list remoteConsumerReceivers for a given remoteQueuePoint
     * 
     */
    public Iterator<AnycastInputControl> getAIControlAdapterIterator();

    /**
     * Get an iterator over all control adapter for the aostreams for this destination. Used by the
     * controllables to list remoteConsumerReceivers for a given remoteQueuePoint
     * 
     */
    public Iterator<ControlAdapter> getAOControlAdapterIterator();

    /**
     * @param type
     * @param sourceCellule
     * @param msg The ControlMessage to be handled.
     * @return The ControlHandler
     */
    public ControlHandler getControlHandler(ProtocolType type, SIBUuid8 sourceMEUuid, ControlMessage msg);

    /**
     * Return this destination's Destination Manager.
     * 
     * Feature 174199.2.5
     * 
     * @return DestinationManager
     */
    public DestinationManager getDestinationManager();

    /**
     * For a given durable subscription, retrieve or create a ConsumerDispatcher.
     * 
     * @param subState
     * @return ConsumerDispatcher that matches the passed in
     *         subscription state - if it has already been created previously
     */
    public ConsumerDispatcher getDurableSubscriptionConsumerDispatcher(
                                                                       ConsumerDispatcherState subState);

    /**
     * Method getLocalConsumerDispatcher.
     * 
     * @return ConsumerDispatcher
     *         <p>Returns the consumer dispatcher for the local messaging engine or null if
     *         there isn't one.</p>
     * 
     */
    public ConsumerManager getLocalPtoPConsumerManager();

    /**
     * <p>Creates a new consumer dispatcher for a non-durable subscription. This
     * is a temporary consumer dispatcher representing the subscriber.
     * 
     * @param subState The Subscription State object
     * @throws SIDiscriminatorSyntaxException
     * @throws SISelectorSyntaxException
     * @throws SIResourceException
     * 
     * @return ConsumerDispatcher
     * @throws SINonDurableSubscriptionMismatchException
     */
    public ConsumerDispatcher createSubscriptionConsumerDispatcher(ConsumerDispatcherState subState)
                    throws SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException, SINonDurableSubscriptionMismatchException;

    /**
     * <p>Creates a new consumer dispatcher for a non-durable subscription and then
     * attaches the consumer point to consumer dispatcher.
     * 
     * @param consumerPoint the consumer point to attach.
     * @param subState The Subscription State object
     * @throws SIDiscriminatorSyntaxException
     * @throws SISelectorSyntaxException
     * @throws SIResourceException
     * @throws SINonDurableSubscriptionMismatchException
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIDestinationLockedException
     * @throws SISessionDroppedException
     * 
     * @return ConsumerKey
     * @throws SINonDurableSubscriptionMismatchException
     */
    public ConsumerKey createSubscriptionConsumerDispatcherAndAttachCP(LocalConsumerPoint consumerPoint,
                                                                       ConsumerDispatcherState subState)
                    throws SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException,
                    SINonDurableSubscriptionMismatchException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDestinationLockedException,
                    SISessionDroppedException;

    /**
     * Call to get the PubSubOutputHandler for this Destination.
     * 
     * @param neighbourUUID The uuid of the Neighbouring ME that this
     *            handler will send messages to.
     * @return The PubSubOutputHandler representing this target ME.
     */
    public PubSubOutputHandler getPubSubOutputHandler(SIBUuid8 neighbourUUID);

    /**
     * Creates a PubSubOutputHandler for the given meName for this topic space
     * 
     * @param neighbour The remote neighbour definition
     * 
     * @return The PubSubOutputHandler representing this target ME.
     * 
     */
    public PubSubOutputHandler createPubSubOutputHandler(
                                                         Neighbour neighbour) throws SIResourceException;

    /**
     * Gets all the PubSubOutputHandlers associated with this topic space
     * destination. Obtains a read-only lock which must be unlocked via
     * unlockPubsubOutputHandlers()
     * 
     * @return HashMap The list of all PubSubOutputHandlers
     */
    public HashMap getAllPubSubOutputHandlers();

    /**
     * Unlocks a non-exclusive read lock held on the list of Pubsub
     * OutputHandlers
     */
    public void unlockPubsubOutputHandlers();

    /**
     * Deletes a PubSubOutputHandler.
     * 
     * Removes from the list of available PubSubOutputHandlers
     * 
     * @param neighbourUUID The neighbour for this OutputHandler.
     */
    public void deletePubSubOutputHandler(SIBUuid8 neighbourUUID);

    /**
     * Method createDurableSubscription
     * 
     * Used to create a durable subscription. This creates a subscription consumer dispatcher,
     * adds it to the MPs durable subscription table, and persists the state of the subscription
     * 
     * @param subState The subscription State
     * @param transaction The transaction from which to create the subscription
     * 
     */
    public void createDurableSubscription(
                                          ConsumerDispatcherState subState,
                                          TransactionCommon transaction)
                    throws
                    SIDurableSubscriptionAlreadyExistsException,
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException;

    /**
     * Attaches to an existing durable subscription.
     * 
     * @param consumerPoint The consumer point to attach
     * @param subState The durable subscription state information
     * @return The ConsumerKey.
     * @throws SIDestinationLockedException
     * @throws SIDurableSubscriptionMismatchException
     * @throws SIIncorrectCallException
     * @throws SIDiscriminatorSyntaxException
     * @throws SISelectorSyntaxException
     * @throws SIResourceException
     * @throws SIDurableSubscriptionNotFoundException
     */
    public ConsumableKey attachToDurableSubscription(LocalConsumerPoint consumerPoint,
                                                     ConsumerDispatcherState subState)
                    throws SIDestinationLockedException,
                    SIDurableSubscriptionMismatchException,
                    SIIncorrectCallException,
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException;

    /**
     * Delete a durable subscription. This maps to a JMSConnection.unsubscribe()
     * call
     * 
     * @param subscriptionId The subscription id to delete
     * @param durableHome The name of the messaging engine where the durable subscription
     *            is homed.
     * 
     * @throws SIIncorrectCallException
     * @throws SIResourceException
     * @throws SIDestinationLockedException
     */

    public void deleteDurableSubscription(
                                          String subscriptionId,
                                          String durableHome)
                    throws SIResourceException,
                    SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException;

    /**
     * Method addConsumerPointMatchTarget
     * Used to add a wrapped ConsumerKey to the MatchSpace.
     * 
     * @param consumerPointData
     * @param cmUuid uuid of the calling consumer manager
     * @param criteria
     * 
     * @throws SIDiscriminatorSyntaxException
     * @throws SISelectorSyntaxException
     * @throws SIResourceException
     */
    public void addConsumerPointMatchTarget(
                                            DispatchableKey consumerPointData,
                                            SIBUuid8 cmUuid,
                                            SelectionCriteria criteria)
                    throws SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException;

    /**
     * Method removeConsumerPointMatchTarget
     * Used to remove ConsumerPoints from the MatchSpace.
     * 
     * @param consumerPointData
     * 
     */
    public void removeConsumerPointMatchTarget(DispatchableKey consumerPointData);

    /**
     * Returns the name of this destination.
     * 
     * @return name.
     */
    public String getName();

    /**
     * Returns the UUID of this destination.
     * 
     * @return uuid.
     */
    public SIBUuid12 getUuid();

    /**
     * Returns the Base UUID of this destination.
     * 
     * @return uuid.
     */
    public SIBUuid12 getBaseUuid();

    /**
     * Get the value for the given context key for this destination.
     */
    public Object getContextValue(String keyName);

    /**
     * Get this DestinationHandler's definition. The definition returned will
     * be that which is common to all destination handler types. Callers which
     * want a more specific definition for a destination handler (such as base or
     * alias) will need to cast the result (so they will need to ensure they are
     * satisfied as to what destination handler type is represented by this
     * reference).
     * 
     * @return BaseDestinationDefinition
     */
    public BaseDestinationDefinition getDefinition();

    /**
     * Return the lockmanager used to prevent reallocation from occurring
     * during a send to a particular destination.
     * 
     * @return reallocationLockManager
     */
    public LockManager getReallocationLockManager();

    /**
     * Takes a message that failed to be consumed and handles it. This method is
     * called by the rolledbackGet callbacks from the message items. We call into
     * our ExceptionDestinationHandlerImpl object with a suitable transaction and
     * the message. If an ExceptionDestinationHandlerImpl
     * instance does not exist yet, we create it.
     * 
     * @param msg The message which could not be delivered
     * @param exceptionReason The reason code that cause the reroute
     * @param exceptionInserts An array of strings to insert into the error message
     * 
     * @throws SIStoreException
     */
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIMPMessage msg,
                                                              int exceptionReason,
                                                              String[] exceptionInserts,
                                                              TransactionCommon tran)
                    throws
                    SIIncorrectCallException,
                    SIResourceException;

    /**
     * Returns true if the destination has a local localization.
     * 
     * @return boolean
     */
    public boolean hasLocal();

    /**
     * Returns true if the destination has any remote localizations.
     * 
     * @return boolean
     */
    public boolean hasRemote();

    /**
     * Is the destination pub/sub (i.e. a topicspace?)
     * 
     * @return true if the destination is a topicspace.
     */
    public boolean isPubSub();

    /**
     * Is the destination a temporary destination?
     * 
     * @return true if the destination is temporary.
     */
    public boolean isTemporary();

    /**
     * Update the DestinationDefinition associated with the destination
     * and perform any necessary modifications to the
     * message store and other components to reflect the new definition.
     * <p>
     * The definition must be of the correct type (DestinationAliasDefinition for
     * an alias, etc.) though we only pass in a reference to the common
     * definition parent.
     * 
     * @param destinationDefinition
     */
    public void updateDefinition(
                                 BaseDestinationDefinition destinationDefinition);

    /**
     * Method getTxManager.
     * 
     * @return SIMPTransactionManager
     */
    public SIMPTransactionManager getTxManager();

    /**
     * Method close.
     * <p>Close any producers that are currently attached
     * to the destination on this ME.</p>
     */
    public void closeProducers();

    /**
     * Method closeConsumers.
     * <p>Close any Consumers or Browsers that are currently attached
     * to the destination on this ME.</p>
     */
    public void closeConsumers() throws SIResourceException;

    /**
     * <p>Call this method to determine if the destinationHandler has been reconciled
     * or not.</p>
     * 
     * @return boolean - true = reconciliation has occured, or the destinationHandler did
     *         not need reconciling. false = reconciliation has not yet occured.
     */
    public boolean isReconciled();

    /**
     * @return true if this is a system destination
     */
    public boolean isSystem();

    /**
     * Method getMessageProcessor.
     * 
     * @return MessageProcessor
     */
    public MessageProcessor getMessageProcessor();

    /**
     * Retrieve the subscription index for this destination.
     * 
     * @return SubscriptionIndex.
     */
    public SubscriptionIndex getSubscriptionIndex();

    public List getSubscriptionList() throws SIResourceException;

    /**
     * MP is announcing that it has completely started. Mediations can now start.
     */
    public void announceMPStarted();

    /**
     * MP is announcing that it is about to stop. Mediations should stop.
     */
    public void announceMPStopping();

    /**
     * Was is announcing that it is completely started. Mediations can now start.
     * 
     */
    public void announceWasOpenForEBusiness();

    /**
     * Was is announcing that it is completely stopping. Mediations can now stop.
     * 
     */
    public void announceWasClosedForEBusiness();

    /**
     * @return The MaxFailedDeliveries setting for the destination
     */
    public int getMaxFailedDeliveries();

    /**
     * @return the interval in milliseconds that a queue should be blocked when any message
     *         reaches its maximum failed delivery count but cannot be exceptioned
     */
    public long getBlockedRetryInterval();

    /**
     * @return returns the high message threshold set for that queue
     */
    public long getQHighMsgDepth();

    /**
     * @return the DefaultReliability for messages put to the destination
     */
    public Reliability getDefaultReliability();

    /**
     * @return the MaxReliability for messages put to the destination
     */
    public Reliability getMaxReliability();

    /**
     * Returns the exception Destination from this DestinationHandler.
     * <p>
     * When a message cannot be delivered to this destination due to problems that
     * may occur, the message may be rerouted to an exception destination. Every
     * destination points to the name of an ExceptionDestinationHandlerImpl.
     * 
     * @return The name of the exception destination.
     */
    public String getExceptionDestination();

    /**
     * Returns the reliability that messages with a matching reliability or
     * below are discarded rather than moved to an exception destination (or
     * block) at the point that they would normally be exceptioned.
     * 
     * @return the exception discard reliability threshold
     */
    public Reliability getExceptionDiscardReliability();

    /**
     * @return the DefaultPriority for messages put to the destination
     */
    public int getDefaultPriority();

    /**
     * @return the last alterationTime of the destination
     */
    public long getAlterationTime();

    /**
     * @return a user description of the destination
     */
    public String getDescription();

    /**
     * Return the type of this destination.
     * 
     * @return destinationType.
     */
    public DestinationType getDestinationType();

    /**
     * @return true if Send is allowed on the destination
     */
    public boolean isSendAllowed();

    /**
     * @return true if Receive is allowed on the destination
     */
    public boolean isReceiveAllowed();

    /**
     * @return true if receive exclusive is set and only one receiver at a time
     *         is allowed
     */
    public boolean isReceiveExclusive();

    /**
     * @return true if the Producer QOS override is enabled
     */
    public boolean isOverrideOfQOSByProducerAllowed();

    /**
     * @return true if the destination is an alias
     */
    public boolean isAlias();

    /**
     * @return true if we have detected that the destination is corrupt or indoubt
     */
    public boolean isCorruptOrIndoubt();

    /**
     * <p>Indicate that the destination is corrupt</p>
     * 
     * @param isCorrupt
     */
    public void setCorrupt(boolean isCorrupt);

    /**
     * <p>Indicate that the destination is indoubt</p>
     * 
     * @param isCorrupt
     */
    public void setIndoubt(boolean isIndoubt);

    /**
     * @return true if the destination is a foreign destination
     */
    public boolean isForeign();

    /**
     * @return true if the destination is a foreign bus
     */
    public boolean isForeignBus();

    /**
     * @return true if the destination is a link
     */
    public boolean isLink();

    /**
     * @return true if the destination is located in a foreign bus
     */
    public boolean isTargetedAtLink();

    /**
     * @return true if the destination is a MQlink
     */
    public boolean isMQLink();

    /**
     * Returns true if the destination is markded as to-be-deleted.
     * 
     * @return boolean
     */
    public boolean isToBeDeleted();

    /**
     * Register for message events
     */
    public void registerForMessageEvents(SIMPMessage msg);

    /**
     * The name of the bus on which the destination resides
     */
    public String getBus();

    /**
     * Return the targetProtocolItemStream for the destination
     */
    public TargetProtocolItemStream getTargetProtocolItemStream();

    /**
     * Return the ProxyReferenceStream for the destination
     */
    public ProxyReferenceStream getProxyReferenceStream();

    /**
     * Return the sourceProtocolItemStream for the destination
     */
    public SourceProtocolItemStream getSourceProtocolItemStream();

    /**
     * Return the resolved destinationHandler
     */
    public BaseDestinationHandler getResolvedDestinationHandler();

    /**
     * Stop the destination. Do anything which needs to be done at shutdown.
     */
    public void stop(int mode);

    /**
     * Start the destination. Do anything which needs to be done at startup.
     */
    public void start();

    /**
     * Return the itemstream representing the queue point
     * 
     * @param meUuid
     */
    public LocalizationPoint getQueuePoint(SIBUuid8 meUuid);

    /**
     * Return the itemstream representing the publish point
     */
    public PubSubMessageItemStream getPublishPoint();

    /**
     * Return the administered forward routing path on this destination
     */
    public List<SIDestinationAddress> getForwardRoutingPath();

    /**
     * Return the administered forward routing path on this destination as a
     * SIDestinationAddress array. This is required for the DestinationConfiguration object
     * for the method getDefaultForwardRoutingPath.
     */
    public SIDestinationAddress[] getDefaultForwardRoutingPath();

    /**
     * Return the administered reverse routing path on this destination
     */
    public JsDestinationAddress getReplyDestination();

    /**
     * Check permission to access a Destination
     * 
     * @param secContext
     * @param operation
     * @return
     * @throws SICoreException
     */
    public boolean checkDestinationAccess(
                                          SecurityContext secContext,
                                          OperationType operation);

    /**
     * Check permission to access a Discriminator
     * 
     * @param secContext
     * @param operation
     * @return
     */
    public boolean checkDiscriminatorAccess(
                                            SecurityContext secContext,
                                            OperationType operation) throws SIDiscriminatorSyntaxException;

    /**
     * Do we need to check discriminator access for a topicspace?
     * 
     * @return true if access checks are required.
     */
    public boolean isTopicAccessCheckRequired();

    /**
     * Add the passed AliasDestinationHandler to the list of aliases that
     * target this destination. If this destination is deleted, the
     * aliases must be invalidated
     * 
     * @param aliasDestinationHandler
     */
    public void addTargettingAlias(DestinationHandler aliasDestinationHandler);

    /**
     * Remove the passed AliasDestinationHandler from the list of aliases that
     * target this destination.
     * 
     * @param aliasDestinationHandler
     */
    public void removeTargettingAlias(DestinationHandler aliasDestinationHandler);

    /**
     * Delete all the alias destinations that target this destination
     */
    public void deleteTargettingAliases();

    /**
     * Is the destination deleted?
     * 
     * @return
     */
    public boolean isDeleted();

    /**
     * Indicate that the destination is deleted
     */
    public void setDeleted();

    /**
     * Reset the destination.
     * 
     * @throws InvalidOperationException if the operation is not valid for the
     *             destination.
     */
    public void reset() throws InvalidOperationException;

    /**
     * Register any available control adapters as MBeans. This method should
     * not be called until the DestinationHandler is fully ready to be controlled.
     * Calling this method more than once should not cause control adapters to be
     * registered more than once.
     */
    public void registerControlAdapters();

    /**
     * Returns the AnycastInputHandler for the given DME
     * 
     * @param dmeID the ID of the DME that is for the returned anycastInputHandler
     * @param createAIH if true, the AIH will be created (if it does not exist)
     * @return AnycastInputHandler
     */
    AnycastInputHandler getAnycastInputHandler(SIBUuid8 dmeID, SIBUuid12 gatheringTargetDestUuid, boolean createAIH);

    /**
     * <p>Notify consumers of the change to the receive allowed attribute</p>
     * 
     * @param isReceiveAllowed
     * @param destinationHandler
     */
    public void notifyReceiveAllowed(DestinationHandler destinationHandler);

    /**
     * <p>Notify Remote Consumer Dispatchers consumers on RME of the change to the receive allowed attribute</p>
     * 
     * @param isReceiveAllowed
     * @param destinationHandler
     */
    public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler);

    /**
     * isOrdered - determines whether total message ordering was specified.
     * 
     * @return isOrdered
     */
    public boolean isOrdered();

    /**
     * Used in the case where the Handler represents a Foreign Bus or
     * where the immediate or ultimate target of the Handler is a Foreign Bus.
     * Will return null for other DestinationHandlers.
     * In such cases the setting of sendAllowed at the Foreign Bus definition
     * level overrides all other sendAllowed settings.
     * 
     * @return
     */
    public Boolean getSendAllowedOnTargetForeignBus();

}
