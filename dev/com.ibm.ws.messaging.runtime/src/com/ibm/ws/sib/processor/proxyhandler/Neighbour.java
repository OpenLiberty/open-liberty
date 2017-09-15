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
package com.ibm.ws.sib.processor.proxyhandler;

// Import required classes.
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessageType;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.ProducerSessionImpl;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**Structure to hold all information regarding a neighbouring broker.
 *
 * <p>
 * This class registers the durable subscription that is used for proxy subscription
 * updates.
 */
public final class Neighbour extends SIMPItemStream implements AlarmListener
{
  private static final TraceComponent tc =
    SibTr.register(
      Neighbour.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Persistent data version number.
   */
  private static final int PERSISTENT_VERSION = 1;

  /** The Bus Id for the Neighbour */
  private String iBusId;

  /** The Bus that this Neighbour belongs to */
  private BusGroup iBusGroup;

  /** The list of proxies that this Neighbour has registered
   * on this ME
   */
  private Hashtable iProxies;

  /** The cached reference to the ProxyHandler class */
  private MultiMEProxyHandler iProxyHandler;

  /** The name of the remote Queue to send proxy messages to.
   */
  private JsDestinationAddress iRemoteQueue;

  /** The producer session for sending the proxy messages from
   */
  private ProducerSessionImpl iProducerSession;

  /** The me uuid represented by this neighbour
   */
  private SIBUuid8 iMEUuid;

  /** The neighbours reference.
   */
  private Neighbours iNeighbours;

  /** A cached reference to the Destination Manager instance */
  private DestinationManager iDestinationManager;

  private HashSet iPubSubOutputHandlers;

  /** The timer that is used for waiting for the response from the remote Neighbour.
   * Current server start time is 2 minutes, so a 5 minute wait time should be ok*/
  private static final int REQUEST_TIMER = 300000;

  /** An indicater for if a REQUEST message has been sent */
  private boolean iRequestSent = false;

  /** If the Alarm has fired for the Request Message */
  private boolean iAlarmCancelled;

  /** The Alarm that was created */
  private Alarm iAlarm;

  /** Flag used to indicate if the reset message sent to the Neighbour failed and should be retried */
  private boolean iRequestFailed = false;

  /**
   * Empty constructer used when restoring the Neighbour from the
   * MessageStore at start time.
   *
   */
  public Neighbour()
  {
    super();
  }

  /**
   * Constructor for Neighbour objects.
   *
   * Contains the information for a particular Neighbour.
   *
   * @param proxyHandler   The singleton proxy handler class
   * @param neighbourUUID  The uuid of the neighbouring ME.
   * @param busId       The bus that this neighbour belongs to.
   * @param linkBundleIds  The array containing all the link bundles
   *                        this Neighbour belongs to.
   * @param neighbours     The neighbours instance.
   *
   * @exception MessageStoreException thrown if the item can't be created.
   *
   */
  Neighbour(
    MultiMEProxyHandler proxyHandler,
    SIBUuid8 meUUID,
    String busId,
    Neighbours neighbours)

  {
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "Neighbour",
        new Object[]{proxyHandler, meUUID, busId, neighbours});

    iMEUuid = meUUID;
    iBusId = busId;
    intialiseNonPersistent(proxyHandler, neighbours);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "Neighbour", this);
  }

  /** Setup the non persistent state.
   *
   * @param proxyHandler  The proxy handler instance.
   * @param neighbours The neighbours instance
   */
  protected void intialiseNonPersistent(MultiMEProxyHandler proxyHandler,
                                        Neighbours neighbours)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "intialiseNonPersistent");

    iProxyHandler = proxyHandler;
    iNeighbours = neighbours;
    iDestinationManager =
      iProxyHandler.getMessageProcessor().getDestinationManager();
    iRemoteQueue = SIMPUtils.createJsSystemDestinationAddress(
          SIMPConstants.PROXY_SYSTEM_DESTINATION_PREFIX, iMEUuid, iBusId);
    if (iMEUuid == null)
      iMEUuid = iRemoteQueue.getME();

    iProxies = new Hashtable();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "intialiseNonPersistent");
  }

  /**
   * Gets the UUID for this Neighbour
   * @return SIBUuid
   */
  public final SIBUuid8 getUUID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getUUID");
      SibTr.exit(tc, "getUUID", iMEUuid);
    }
    return iMEUuid;
  }

  /**
   * Get the Bus name for this Neighbour
   *
   * @return String  The Bus of this Neighbour
   */
  public final String getBusId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBusId");
      SibTr.exit(tc, "getBusId", iBusId);
    }

    return iBusId;
  }

  /**
   * Gets the Bus for this Neighbour
   *
   * @return BusGroup  The Bus that this Neighbour belongs to
   *
   */
  BusGroup getBus()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBus");
      SibTr.exit(tc, "getBus", iBusGroup);
    }

    return iBusGroup;
  }

  /** Gets all the proxies that this Neighbour has registered with the running ME.
   *
   * @return Hashtable  The hashtable containing all the proxies that have been registered.
   */
  Hashtable getRegisteredProxies()
  {
    return iProxies;
  }

  /**
   * Sets the Bus for this Neighbour
   *
   * @param BusGroup  The group that this Neighbour belongs to
   *
   */
  void setBus(BusGroup busGroup)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setBus");

    iBusGroup = busGroup;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setBus");
  }

  /**
   * Sends the proxy subscriptions to this Neighbour.
   *
   * Pulls all the subscriptions that have been sent to this
   * Bus and builds a message that will be sent to this Neighbour.
   *
   * @exception SIResourceException
   */
  void sendResetProxySubscriptions() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendResetProxySubscriptions");

    // Generate the subscription message that will be sent to this neighbour.
    SubscriptionMessage message = iBusGroup.generateResetSubscriptionMessage();

    if (message !=null)
      // Send to the Neighbour.
      sendToNeighbour(message, null);
    else
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "No subscriptions to forward to ME " + iMEUuid.toString());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendResetProxySubscriptions");
  }

  /**
   * Sends the proxy subscriptions to this Neighbour.
   *
   * Pulls all the subscriptions that have been sent to this
   * Bus and builds a message that will be sent to this Neighbour.
   *
   * @exception SIResourceException
   */
  void sendRequestProxySubscriptions() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendRequestProxySubscriptions");

    // Generate the subscription message that will be sent to this neighbour.
    SubscriptionMessage message = iBusGroup.generateResetSubscriptionMessage();

    if (message !=null)
    {
      message.setSubscriptionMessageType(SubscriptionMessageType.REQUEST);
      // Send to the Neighbour.
      sendToNeighbour(message, null);
    }
    else
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "No subscriptions to forward to ME " + iMEUuid.toString());

    setRequestedProxySubscriptions();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendRequestProxySubscriptions");
  }


  /**
   * Sends a reply message to the requesting neighbour.
   *
   * @throws SIResourceException
   */
  void sendReplyMessage() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendReplyMessage");

    // Generate the subscription message that will be sent to this neighbour.
    SubscriptionMessage message = iBusGroup.generateReplySubscriptionMessage();

    if (message !=null)
      // Send to the Neighbour.
      sendToNeighbour(message, null);
    else
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "No subscriptions to forward to ME " + iMEUuid.toString());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendReplyMessage");

  }

  /**
   * Creates a producer session for sending the proxy messages to the Neighbours
   *
   * Opens the producer on the Neighbours remote Queue for sending to that
   * Neighbour
   *
   * @exception SIResourceException  Thrown if the producer can not be created.
   */
  private synchronized void createProducerSession()

  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createProducerSession");

    if (iProducerSession == null)
    {

      try
      {
        iProducerSession =
          (ProducerSessionImpl) iProxyHandler
            .getMessageProcessor()
            .getSystemConnection()
            .createSystemProducerSession(iRemoteQueue, null, null, null, null);
      }
      catch (SIException e)
      {
        // No exceptions should occur as the destination should always exists
        // as it is created at start time.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour.createProducerSession",
          "1:434:1.107",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "createProducerSession", "SIResourceException");

        // this is already NLS'd //175907
        throw new SIResourceException(e);
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createProducerSession");
  }

  /**
   * proxyRegistered is called on this Neighbour object
   * when a proxy subscription is received from a Neighbour.
   *
   * A reference to that subscription is either created,
   * or the reference to that subscription is unMarked
   *
   * @param topicSpace   The topicSpace for the subscription.
   * @param localTopicSpaceName The name for the topic space on this messaging engine.
   * @param topic        The topic for the proxy
   * @param foreignTSName The foreign TS mapping
   * @param transaction  The transaction from which to create the
   *                      proxy
   * @param foreignSecuredProxy Flag to indicate whether a proxy sub
   *                      originated from a foreign bus where the home
   *                      bus is secured.
   * @param MESubUserId   Userid to be stored when securing foreign proxy subs
   *
   * @return An MESubscription if a new Subscription is created.
   */
  MESubscription proxyRegistered(SIBUuid12 topicSpaceUuid,
                                 String localTopicSpaceName,
                                 String topic,
                                 String foreignTSName,
                                 Transaction transaction,
                                 boolean foreignSecuredProxy,
                                 String MESubUserId)

  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "proxyRegistered", new Object[] { topicSpaceUuid,
                                                        localTopicSpaceName,
      	                                                topic,
      	                                                foreignTSName,
      	                                                transaction,
      	                                                new Boolean(foreignSecuredProxy),
      	                                                MESubUserId});

    // Generate a key for the topicSpace/topic
    final String key = BusGroup.subscriptionKey(topicSpaceUuid, topic);

    // Get the subscription from the Hashtable.
    MESubscription sub = (MESubscription) iProxies.get(key);

    if (sub != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Unmarking subscription " + sub);

      // unmark the subscription as it has been found
      sub.unmark();

      // Check whether we need to update the recovered subscription
      boolean attrChanged = checkForeignSecurityAttributesChanged(sub,
                                                                  foreignSecuredProxy,
                                                                  MESubUserId);

      // If the attributes have changed then we need to persist the update
      if(attrChanged)
      {
        try
        {
          sub.requestUpdate(transaction);
        }
        catch (MessageStoreException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.proxyhandler.Neighbour.proxyRegistered",
            "1:522:1.107",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
            "1:529:1.107",
            e });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "proxyRegistered", "SIResourceException");

          throw new SIResourceException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
                "1:539:1.107",
                e },
              null),
            e);
        }
      }

      // Set the subscription to null to indicate that this
      // is an old subscription
      sub = null;
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Subscription being created");


      // Create a new MESubscription as one wasn't found
      sub = new MESubscription(topicSpaceUuid,
                               localTopicSpaceName,
                               topic,
                               foreignTSName,
                               foreignSecuredProxy,
                               MESubUserId);

      // Add this subscription to the item Stream
      try
      {
        addItem(sub, transaction);
      }
      catch (MessageStoreException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour.proxyRegistered",
          "1:574:1.107",
          this);

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
          "1:581:1.107",
          e });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "proxyRegistered", "SIResourceException");

        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
              "1:591:1.107",
              e },
            null),
          e);
      }

      // Put the subscription in the hashtable.
      iProxies.put(key, sub);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "proxyRegistered", sub);

    return sub;
  }

  /**
   *  Remove the subscription in the case of a rollback
   * @param topicSpace
   * @param topic
   */
  protected void removeSubscription(SIBUuid12 topicSpace,
                                    String topic)
  {
    // Generate a key for the topicSpace/topic
    final String key = BusGroup.subscriptionKey(topicSpace, topic);

    iProxies.remove(key);
  }

  /**
   * proxyDeregistered is called on this Neighbour object
   * when a delete proxy subscription is received from a Neighbour.
   *
   * A reference to that subscription is either deleted,
   * or nothing happens.
   *
   * @param topic       The topic for the proxy
   * @param topicSpace  The topicSpace for the subscription.
   * @param transaction The transaction object to do the remove.
   *
   * @return MESubscription null if the proxy wasn't removed.
   *
   * @exception SIResourceException  If the MESubscription can't be removed.
   */
  MESubscription proxyDeregistered(SIBUuid12 topicSpace,
                                   String topic,
                                   Transaction transaction)

  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "proxyDeregistered",
        new Object[] { topicSpace, topic, transaction });

    // Generate a key for the topicSpace/topic
    final String key = BusGroup.subscriptionKey(topicSpace, topic);

    // Get the subscrition from the Hashtable.
    final MESubscription sub = (MESubscription) iProxies.get(key);

    if (sub != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "Subscription " + sub + " being removed");

      // Remove the subscription from the Neighbour item Stream.
      try
      {
        sub.remove(transaction, sub.getLockID());
      }
      catch (MessageStoreException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour.proxyDeregistered",
          "1:669:1.107",
          this);

        SibTr.exception(tc, e);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
          "1:676:1.107",
          e });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "proxyDeregistered", "SIResourceException");

        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
              "1:686:1.107",
              e },
            null),
          e);
      }

      // Remove the proxy from the list
      iProxies.remove(key);
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "No Subscription to be removed");
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "proxyDeregistered", sub);

    return sub;
  }

  /**
   * Adds the rolled back subscription to the list.
   *
   * @param topicSpace
   * @param topic
   */
  protected void addSubscription(SIBUuid12 topicSpace, String topic, MESubscription subscription)
  {
    // Generate a key for the topicSpace/topic
    final String key = BusGroup.subscriptionKey(topicSpace, topic);

    iProxies.put(key, subscription);
  }

  /**
   * Gets the MESubscription represented from this topicSpace and topic
   *
   * @param topicSpace
   * @param topic
   *
   * @return MESubscription representing this topicSpace/topic
   */
  protected MESubscription getSubscription(SIBUuid12 topicSpace, String topic)
  {
    return (MESubscription)iProxies.get(BusGroup.subscriptionKey(topicSpace, topic));
  }

  /**
   * Marks all the proxies from this Neighbour.
   *
   * This is used for resynching the state between what this
   * ME has registered and what the ME has sent.
   */
  void markAllProxies()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "markAllProxies");

    final Enumeration enu = iProxies.elements();

    // Cycle through each of the proxies
    while (enu.hasMoreElements())
    {
      final MESubscription sub = (MESubscription) enu.nextElement();

      // Mark the subscription.
      sub.mark();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "markAllProxies");
  }

  /**
   * Removes all Subscriptions that are no longer registered
   *
   * Loops through all the ME Subscriptions for this Neighbour
   * and finds the ones that still contain the reset mark and removes
   * them.
   *
   * @param topicSpaces  The list of topicSpaces to add the deletes to
   * @param topics      The list of topics to add the deletes to
   * @param okToForward  Whether to add the topics to the lists or not.
   *
   */
  void sweepMarkedProxies(
    List topicSpaces,
    List topics,
    Transaction transaction,
    boolean okToForward)
    throws SIResourceException, SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sweepMarkedProxies",
        new Object[] {
          topicSpaces,
          topics,
          transaction,
          new Boolean(okToForward)});

    // Get the list of proxies for this Neighbour
    final Enumeration enu = iProxies.elements();

    // Cycle through each of the proxies
    while (enu.hasMoreElements())
    {
      final MESubscription sub = (MESubscription) enu.nextElement();

      // If the subscription is marked, then remove it
      if (sub.isMarked())
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "Subscription " + sub + " being removed");

        // Remove the proxy from the list.
        proxyDeregistered(sub.getTopicSpaceUuid(), sub.getTopic(), transaction);

        // Remove this Subscription from the
        // match space and the item stream on which they are
        // stored.
        final boolean proxyDeleted =
          iNeighbours.deleteProxy(
            iDestinationManager.getDestinationInternal(sub.getTopicSpaceUuid(), false),
            sub,
            this,
            sub.getTopicSpaceUuid(),
            sub.getTopic(),
            true,
            false);

        // Generate the key to remove the subscription from.
        final String key =
          BusGroup.subscriptionKey(sub.getTopicSpaceUuid(), sub.getTopic());
        // Remove the proxy from the list
        iProxies.remove(key);

        // Add the details to the list of topics/topicSpaces to be deleted
        if (okToForward && proxyDeleted)
        {
          topics.add(sub.getTopic());
          topicSpaces.add(sub.getTopicSpaceUuid());
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sweepMarkedProxies");
  }

  /**
   * Forwards the message given onto this Neighbours Queue.
   *
   * This could be a create/change or delete message.
   *
   * @param message  The JsMessage to be sent.
   * @param transaction  The transaction to send the message under.
   */
  void sendToNeighbour(JsMessage message, Transaction transaction)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendToNeighbour", new Object[] { message, transaction });

    if (iProducerSession == null)
      createProducerSession();

    try
    {
      iProducerSession.send((SIBusMessage) message, (SITransaction) transaction);

    }
    catch (SIException e)
    {
      // No FFDC code needed

      SibTr.exception(tc, e);
      SibTr.warning(tc, "PUBSUB_CONSISTENCY_ERROR_CWSIP0383", new Object[]{JsAdminUtils.getMENameByUuidForMessage(iMEUuid.toString()), e});


      SIResourceException ee = null;
      if (! (e instanceof SIResourceException))
        ee = new SIResourceException(e);
      else
        ee = (SIResourceException)e;


      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendToNeighbour", ee);

      throw ee;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendToNeighbour");
  }

  /**
   * Recovers the Neighbours subscriptions that it had registered.
   *
   * @param proxyHandler the ProxyHandler instance
   */
  protected void recoverSubscriptions(MultiMEProxyHandler proxyHandler) throws MessageStoreException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "recoverSubscriptions", proxyHandler);

    iProxyHandler = proxyHandler;

    iProxies = new Hashtable();

    NonLockingCursor cursor = null;
    try
    {
      cursor = newNonLockingItemCursor(new ClassEqualsFilter(MESubscription.class));

      AbstractItem item = null;
      while ((item = cursor.next()) != null)
      {
        MESubscription sub = null;
          sub = (MESubscription)item;

        // Generate a key for the topicSpace/topic
        final String key = BusGroup.subscriptionKey(sub.getTopicSpaceUuid(),
                                                       sub.getTopic());

        // Add the Neighbour into the list of recovered Neighbours
        iProxies.put(key, sub);

        // Having created the Proxy, need to readd the proxy to the matchspace or just reference it.
        iNeighbours.createProxy(this,
                                iDestinationManager.getDestinationInternal(sub.getTopicSpaceUuid(), false),
                                sub,
                                sub.getTopicSpaceUuid(),
                                sub.getTopic(),
                                true);

        // When recovering subscriptions, we need to call the event post commit
        // code to either add a reference, or put in the MatchSpace.
        sub.eventPostCommitAdd(null);

      }
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "recoverSubscriptions");
  }

  /**
   * Deletes the Queue that would be used for sending proxy update messages
   * to this remote ME.
   *
   * This will only be called when the remote ME is to be deleted.  This method
   * also cancels any alarm that may have been set so it doesn't fire at some point
   * in the future.
   *
   * @throws SICoreException
   */
  protected void deleteDestination()
  throws SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteDestination");

    // If the producer session hasn't been closed, close it now
    synchronized(this)
    {
      if (iProducerSession != null)
        iProducerSession.close();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Removing destination " + iRemoteQueue.getDestinationName());

    try
    {
      iDestinationManager.deleteSystemDestination(iRemoteQueue);
    }
    catch (SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Destination " + iRemoteQueue + " already deleted");
    }

    // If the alarm hasn't been cancelled, do it now.
    synchronized (this)
    {
      if (!iAlarmCancelled)
      {
        if (iAlarm != null)
          iAlarm.cancel();
        iAlarmCancelled = true;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteDestination");
  }


  /**
   * Returns a string representation of the object.
   *
   * @return The string representation.
   *
   */
  public String toString()
  {
    return iMEUuid + ":" + iBusId;
  }

  /**
   * @return true if the objects are equal
   */
  public boolean equals(Object neighbour)
  {

    boolean equal = false;

    if (neighbour instanceof Neighbour)
    {
      if (iMEUuid.equals(((Neighbour)neighbour).getUUID()))
        equal = true;
    }

    return equal;
  }

  /**
   * Returns the hashcode of this object
   *
   * @return the hash code
   */
  public int hashCode()
  {
    return iMEUuid.hashCode();
  }


  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
   */
  public void restore(ObjectInputStream ois, int dataVersion)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc, "restore",new Object[] { ois, new Integer(dataVersion) });

    try
    {
      HashMap hm = (HashMap)ois.readObject();

      final byte meuuid[] = (byte[])hm.get("iMEUuid");
      iMEUuid = new SIBUuid8(meuuid);
      iBusId = (String)hm.get("iBusId");
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.Neighbour.restore",
        "1:1057:1.107",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
          "1:1064:1.107",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "restore", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
            "1:1074:1.107",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
   */
  public int getPersistentVersion()
  {
    return PERSISTENT_VERSION;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#getPersistentData(java.io.ObjectOutputStream)
   */
  public void getPersistentData(ObjectOutputStream oos)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData", oos);

    try
    {
      HashMap hm = new HashMap();

      hm.put("iMEUuid", iMEUuid.toByteArray());
      hm.put("iBusId", iBusId);

      oos.writeObject(hm);
    }
    catch (java.io.IOException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.Neighbour.getPersistentData",
        "1:1116:1.107",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
          "1:1123:1.107",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getPersistentData", "SIErrorException");

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.proxyhandler.Neighbour",
            "1:1133:1.107",
            e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");
  }

  /**  Checks to see if it is ok to send this message
   * to this Neighbour
   * @param busName  The name of the bus to check against
   *
   * @return true if it is ok to send to this Neighbour
   */
  public boolean okToForward(String busName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "okToForward", busName);

    boolean returnVal = false;

    if (busName == null)
      returnVal = true;
    else if (!busName.equals(iBusId))
      returnVal = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "okToForward", new Boolean(returnVal));

    return returnVal;
  }

  /**
   * Add a PubSubOutputHandler to the list of PubSubOutputHandlers that this neighbour
   * knows about.
   */
  protected void addPubSubOutputHandler(PubSubOutputHandler handler)
  {
    if (iPubSubOutputHandlers == null)
      iPubSubOutputHandlers = new HashSet();

    // Add the PubSubOutputHandler to the list that this neighbour
    // knows about.  This is so a recovered neighbour can add the list of
    // output handlers back into the matchspace.
    iPubSubOutputHandlers.add(handler);
  }

  protected HashSet getPubSubOutputHandlers()
  {
    return iPubSubOutputHandlers;
  }

  public void xmlWriteOn(FormattedWriter writer) throws IOException
  {
    writer.newLine();
    writer.taggedValue("iMEUuid", iMEUuid);
    writer.newLine();
    writer.taggedValue("iBusId", iBusId);
  }

  /**
   * Indicate that we have sent a request message to this neighbour.
   *
   * This will mean that a timer is created for 5 seconds waiting
   * for a response.  If no response is received, then the timer will pop
   * and log a console message to indicate that the pubsub topology will
   * not be consistent.
   *
   * To remove the Timer, this neighbour must receive either a RESET, REPLY
   * or a REQUEST SubscriptionMessageType.
   */
  void setRequestedProxySubscriptions()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRequestedProxySubscriptions");

    MPAlarmManager manager = iProxyHandler.getMessageProcessor().getAlarmManager();
    iAlarm = manager.create(REQUEST_TIMER, this);

    synchronized (this)
    {
      iRequestSent = true;
      iAlarmCancelled = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRequestedProxySubscriptions");
  }

  /**
   * The request for proxy subscriptions was responded to.
   *
   * Cancels the Alarm that was created.
   * Updates the underlying object to indicate that a response was received.
   *
   * @param transaction the transaction to do the update under
   */
  void setRequestedProxySubscriptionsResponded()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRequestedProxySubscriptionsResponded");

    synchronized (this)
    {
      iRequestSent = false;

      if (!iAlarmCancelled)
        iAlarm.cancel();

      iAlarmCancelled = true;
    }

    String meName = JsAdminUtils.getMENameByUuidForMessage(iMEUuid.toString());

    if (meName == null)
      meName = iMEUuid.toString();

    SibTr.push(iProxyHandler.getMessageProcessor().getMessagingEngine());
    SibTr.info(tc, "NEIGHBOUR_REPLY_RECIEVED_INFO_CWSIP0382",
               new Object[]{meName});
    SibTr.pop();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRequestedProxySubscriptionsResponded");
  }

  /**
   * If a request message was sent to this neighbour for the
   * proxy subscriptions
   *
   * @return true if a request was sent.
   */
  boolean wasProxyRequestSent()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "wasProxyRequestSent");
      SibTr.exit(tc, "wasProxyRequestSent", new Boolean(iRequestSent));
    }
    return iRequestSent;
  }

  /* (non-Javadoc)
   * @see com.ibm.ejs.util.am.AlarmListener#alarm(java.lang.Object)
   *
   * If this timer alarms, then a Console message will be written to
   * indicate that no response has been received.
   */
  public void alarm(Object alarmContext)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", alarmContext);

    synchronized (this)
    {
      iAlarmCancelled = true;

      // Did the request message fail
      if (iRequestFailed)
      {
        iRequestFailed = false;

        // Get the reset subscription list.
        SubscriptionMessage message = iBusGroup.generateResetSubscriptionMessage();
        message.setSubscriptionMessageType(SubscriptionMessageType.REQUEST);

        // If there is a reset message to send, then send it.
        LocalTransaction transaction = iProxyHandler.getMessageProcessor().getTXManager().createLocalTransaction(true);

        try
        {
          // Send to the list of all neighbours.
          sendToNeighbour(message, (Transaction) transaction);

          transaction.commit();

        }
        catch (SIException e)
        {
          // No FFDC code needed
          SibTr.exception(tc, e);

          iRequestFailed = true;
        }

        // Kick off a new timer to wait for the proxy response.
        setRequestedProxySubscriptions();

      }
      // Was the request actually sent
      else if (iRequestSent)
      {
        String meName = JsAdminUtils.getMENameByUuidForMessage(iMEUuid.toString());
        if (meName == null)
          meName = iMEUuid.toString();

        SibTr.push(iProxyHandler.getMessageProcessor().getMessagingEngine());
        SibTr.warning(tc, "NO_NEIGHBOUR_REPLY_WARNING_CWSIP0381", new Object[]{meName});
        SibTr.pop();
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm");
  }

  /**
   * Deletes all remote system destinations that are on referencing the me that this
   * neighbour is referenced too.
   *
   * This will only be called when the remote ME is to be deleted.  This method
   * also cancels any alarm that may have been set so it doesn't fire at some point
   * in the future.
   *
   * @throws SICoreException
   */
  protected void deleteSystemDestinations()
  throws SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteSystemDestinations");

    // If the producer session hasn't been closed, close it now
    synchronized(this)
    {
      if (iProducerSession != null)
        iProducerSession.close();
    }

    //Get all the system destinations tht need to be deleted
    List systemDestinations = iDestinationManager.getAllSystemDestinations(iMEUuid);
    Iterator itr = systemDestinations.iterator();

    while (itr.hasNext() )
    {
      JsDestinationAddress destAddress = (JsDestinationAddress)itr.next();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Removing destination " + destAddress.getDestinationName());

      try
      {
        iDestinationManager.deleteSystemDestination(destAddress);
      }
      catch (SINotPossibleInCurrentConfigurationException e)
      {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Destination " + destAddress + " already deleted");
      }
    }

    // If the alarm hasn't been cancelled, do it now.
    synchronized (this)
    {
      if (!iAlarmCancelled)
      {
        if (iAlarm != null)
          iAlarm.cancel();
        iAlarmCancelled = true;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteSystemDestinations");
  }

  /**
   * checkForeignSecurityAttributesChanged is called in order to determine
   * whether security attributes have changed.
   *
   * @param sub  The stored subscription
   * @param foreignSecuredProxy Flag to indicate whether the new proxy sub
   *                      originated from a foreign bus where the home
   *                      bus is secured.
   * @param MESubUserId   Userid to be stored when securing foreign proxy subs
   *
   * @return An MESubscription if a new Subscription is created.
   */
  private boolean checkForeignSecurityAttributesChanged(MESubscription sub,
                                 boolean foreignSecuredProxy,
                                 String MESubUserId)

  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkForeignSecurityAttributesChanged", new Object[] { sub,
                                                        new Boolean(foreignSecuredProxy),
                                                        MESubUserId });

    boolean attrChanged = false;
    if(foreignSecuredProxy)
    {
      if(sub.isForeignSecuredProxy())
      {
        // Need to check the userids
        if(MESubUserId != null)
        {
          if(sub.getMESubUserId() != null)
          {
            // Neither string is null, check whether they
            // are equal
            if(!MESubUserId.equals(sub.getMESubUserId()))
            {
              sub.setMESubUserId(MESubUserId);
              attrChanged = true;
            }
          }
          else
          {
            // Stored subscription was null
            sub.setMESubUserId(MESubUserId);
            attrChanged = true;
          }
        }
        else // MESubUserid is null
        {
          if(sub.getMESubUserId() != null)
          {
            sub.setMESubUserId(null);
            attrChanged = true;
          }
        }
      }
      else
      {
        // The stored subscription was not foreign secured
        sub.setForeignSecuredProxy(true);
        sub.setMESubUserId(MESubUserId);
        attrChanged = true;
      }
    }
    else // the new proxy sub is not foreign secured
    {
      if(sub.isForeignSecuredProxy())
      {
        // The stored subscription was foreign secured
        sub.setForeignSecuredProxy(false);
        sub.setMESubUserId(null);
        attrChanged = true;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkForeignSecurityAttributesChanged", new Boolean(attrChanged));

    return attrChanged;
  }

  /**
   * Method indicates that the reset list failed - update the alarm to indicate that this failed.
   */
  synchronized void resetListFailed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetListFailed");

    iRequestFailed = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetListFailed");
  }

}
