/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroupFactory;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.comms.common.CommsLightTrace;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.comms.common.DestinationConfigurationImpl;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.ConsumerSetChangeCallback;
import com.ibm.wsspi.sib.core.DestinationAvailability;
import com.ibm.wsspi.sib.core.DestinationConfiguration;
import com.ibm.wsspi.sib.core.DestinationListener;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.Distribution;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.ProducerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SICoreConnectionListener;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.SIXAResource;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SICommandInvocationFailedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * A proxy representation of a Core Connection object.
 * Core API calls made to this proxy object from topology
 * are routed to an appropriate Messaging engine for execution.
 * Where the call on the Messaging Engine returns a new
 * Core Object, this is stored on the server and sufficient
 * data is returned to create a new proxy object.
 * <p>
 * <strong>Some notes on synchronization</strong>
 * A cut down version of synchronisation (as was for milestone 3)
 * is as follows:
 * <ul>
 * <li>Close must be synchronized against most other methods on
 * the connection object except receiveWithWait</li>
 * <li>All methods except close <strong>may</strong> be synchronized
 * with respect to each other. Technically this is stronger than
 * required, but since most of these methods won't be invoked in
 * a typical applications code path, and they don't hold the lock
 * for long - it makes a reasonable first approximation.</li>
 * <li>Close (on the Connection) must be synchronized against any actions
 * carried out by the producers and consumers created for the
 * connection (except of course receiveWithWait).</li>
 * </ul>
 * Although not explicitly noted, we should avoid using the form of the
 * synchronization which involves marking a method declaration as
 * synchronized. This is to prevent "unexpected" side effects from a
 * user program synchronizing on an instance of ConnectionProxy before
 * invoking its methods.
 * <p>
 * To implement the afore mentioned behavior, we use two locks:
 * <ul>
 * <li>A general lock that excludes two threads form executing
 * "most" code at the same time. This takes the form of a
 * object which is used as part of a synchronized statement
 * enclosing sensitive code.</li>
 * <li>A reader-writer lock which provides exclusion for close
 * methods, whilst allowing the maximum amount of concurrency
 * between 'child' consumer or producer sessions.</li>
 * </ul>
 * Perhaps it is worth describing the use of the reader-writer
 * lock in a little more detail. Recall the requirement to have
 * close on a connection synchronized against any actions carried out
 * by the producers and consumers created from the connection, as
 * well as against many of the methods of the connection itself.
 * <p>
 * One way to achieve this is to have every method share references
 * to an object which it synchronizes on prior to doing any work.
 * This would introduce a massive amount of contention for this
 * objects monitor, as apparently seperate consumer and producer
 * sessions which simply shared this connection would both contend
 * for the lock.
 * <p>
 * A better approach would be to allow many different consumers
 * and producers (from the same connection) execute concurrently,
 * and only require their close methods (and the close method of the
 * connection) to exclude all but a single thread of execution.
 * This can be achieved if a reader-writer lock is shared between
 * a connection and its consumers and producers. "Most" method
 * calls could obtain a read lock (allowing concurrency). However,
 * close methods would obtain a write lock, ensuring only one
 * close method could execute at a time, and excluding all readers.
 * <p>
 * A final observation is that care must be taken in the order
 * locks are obtained and held. To avoid dead locks, the general
 * lock must always be obtained before the reader-writer lock.
 * In addition, a try-finally block should be used to ensure that
 * any read-writer lock is released before exiting a method.
 */
public class ConnectionProxy extends Proxy implements SICoreConnection
{
    private static String CLASS_NAME = ConnectionProxy.class.getName();
    private static final TraceComponent tc = SibTr.register(ConnectionProxy.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);

    //@start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.217 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ConnectionProxy.java, SIB.comms, WASX.SIB, uu1215.01 11/09/15 07:55:15 [4/12/12 22:14:06]";
    //@end_class_string_prolog@

    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Source Info: " + $sccsid);
    }

    /** Allow section of PK86574 behaviour with custom property */
    private static final boolean defaultStrictRedeliveryOrdering;

    /** Allow section of PK86574 behaviour on a per-connection basis */
    private volatile boolean strictRedeliveryOrdering;

    /** Log Source code level on static load of class */
    static
    {

        // Statically determine whether the user has requested strict redelivery ordering
        boolean propertyValue = false;
        try {
            propertyValue =
                            CommsUtils.getRuntimeBooleanProperty(CommsConstants.STRICT_REDELIVERY_KEY,
                                                                 CommsConstants.STRICT_REDELIVERY_DEFAULT);
        } catch (Exception e) {
            FFDCFilter.processException(e, CLASS_NAME + ".<clinit>",
                                        CommsConstants.CONNECTIONPROXY_STATICINIT_02,
                                        null);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Encountered error querying strict redelivery enablement");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.exception(tc, e);
        }
        defaultStrictRedeliveryOrdering = propertyValue;

    }

    /**
     * Locking object used to exclude two threads executing
     * "most" (ie. non-close code) - see class comment.
     */
    private final Object generalLock = new Object();

    /**
     * Locking object used to exclude two threads executing
     * close code at the same time - see class comment.
     */
    protected ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();

    // F168604.2
    /** The amount of data to store on read ahead consumer sessions or browser sessions. */
    private static final int HIGH_QUEUE_BYTES =
                    CommsUtils.getRuntimeIntProperty(CommsConstants.RA_HIGH_QUEUE_BYTES_KEY,
                                                     CommsConstants.RA_HIGH_QUEUE_BYTES);

    /** The unique id that is given when we initially connect */
    private byte[] initialUniqueId = null;

    /* Cached Order Contexts */
    private final List<Short> orderContextPool = Collections.synchronizedList(new ArrayList<Short>());

    /** The meUuid of this connection */
    private String meUuid = null;

    /** The resolved User Id */
    private String resolvedUserId = null;

    /** The ME name */
    private String meName = null;

    /** Whether we should exchange transacted sends */
    private static final boolean exchangeTransactedSends =
                    CommsUtils.getRuntimeBooleanProperty(CommsConstants.EXCHANGE_TX_SEND_KEY,
                                                         CommsConstants.EXCHANGE_TX_SEND);

    /** Whether we should exchange express sends */
    private static final boolean exchangeExpressSends =
                    CommsUtils.getRuntimeBooleanProperty(CommsConstants.EXCHANGE_EXPRESS_END_KEY,
                                                         CommsConstants.EXCHANGE_EXPRESS_SEND);

    /** Does our peer require that we use optimized transactions? */
    private final boolean requiresOptimizedTransactions;

    /** Map of consumer sessions key'd by their Id */
    private final HashMap<Short, ConsumerSessionProxy> consumerSessions = new HashMap<Short, ConsumerSessionProxy>();

    /**
     * If messages are being received in slices, this is where the slices are held until they are
     * complete and ready to be re-assembled.
     */
    private List<DataSlice> pendingMessageSlices = null;

    /** An object to lock on when modifying the pendingMessageSlices map */
    private final Object pendingMessageSliceLock = new Object();

    /** Object used to synchronise asynchronous callbacks on this connection */
    private final AsyncCallbackSynchronizer asyncCallbackSynchronizer = new AsyncCallbackSynchronizer();

    // Note whether this is a cloned connection or not - to save on an exchange (line turn around) during SEG_CREATE_CLONED_CONNECTION we
    // can reset an existing cloned connection each time one is closed - the reset connection is then cached in the original parent
    // connection and can be used the next time clone connection is called on the parent. This field is assigned the reference to the
    // parent connection if this connection is a clone connection otherwise it is null.
    private final ConnectionProxy parent;

    // Cached clone connections - if this is a parent of one or or more clone connections then when then clone connections are closed
    // the clone connections can be reset and cached in the parent (provided the parent connection hasn't been closed). The cached clone
    // connections can be reused on subsequent calls on the parent to clone connection. A constant here specifies the maximum number of
    // clone connections that can be cached, above this limit clone connections will just be closed (rather than reset and cached). It is
    // possible for there to be more than the maximum number of connections in the cached set as the maxmimum number is only used when
    // deciding whether to close or reset an existing connection. Two threads making a reset/close decision at the same time could both
    // decide to reset when the cache is just 1 under the maximum -  this isn't a problem.
    private static final int MAX_CACHED_CLOSE_CONNECTIONS = 2;
    //@GuardedBy("this")
    private final Set<ConnectionProxy> cachedCloneConnections = new HashSet<ConnectionProxy>();

    // Constructors
    public ConnectionProxy(final Conversation con) {
        this(con, null);
    }

    public ConnectionProxy(final Conversation con, final ConnectionProxy parent) {
        super(con, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "con=" + con + ", parent=" + parent);

        // Determine if peer requires the use of optimized transaction flows.
        final HandshakeProperties handshakeProperties = getConversation().getHandshakeProperties();
        requiresOptimizedTransactions = (handshakeProperties.getFapLevel() >= JFapChannelConstants.FAP_VERSION_5) &&
                                        ((handshakeProperties.getCapabilites() & CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX) != 0);
        this.parent = parent;

        // Pick up the default strict message redelivery order setting for this JVM
        strictRedeliveryOrdering = defaultStrictRedeliveryOrdering;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // *******************************************************************************************
    // *                                    Session Creation Methods                             *
    // *******************************************************************************************

    // SIB0113.comms.1 start

    /**
     * This method creates a browser session based on a SIDestinationAddress.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param alternateUser
     * 
     * @return BrowserSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public BrowserSession createBrowserSession(SIDestinationAddress destAddress,
                                               DestinationType destType,
                                               SelectionCriteria criteria,
                                               String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowserSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         alternateUser
                        });

        BrowserSession browserSession = _createBrowserSession(destAddress, destType, criteria, alternateUser, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createBrowserSession", browserSession);
        return browserSession;

    }

    /**
     * This method creates a browser session based on a SIDestinationAddress with the allowMessageGathering
     * option.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param alternateUser
     * @param allowMessageGathering
     * 
     * @return BrowserSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public BrowserSession createBrowserSession(SIDestinationAddress destAddress,
                                               DestinationType destType,
                                               SelectionCriteria criteria,
                                               String alternateUser,
                                               boolean allowMessageGathering)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowserSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         alternateUser,
                         allowMessageGathering
                        });

        // This method is only valid from FAP 9 (WAS v7.0) onwards with non-default values

        if (allowMessageGathering)
        {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);
        }

        BrowserSession browserSession = _createBrowserSession(destAddress, destType, criteria, alternateUser, allowMessageGathering);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createBrowserSession", browserSession);
        return browserSession;

    }

    /**
     * This method creates a browser session based on a SIDestinationAddress.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param alternateUser
     * @param allowMessageGathering
     * 
     * @return BrowserSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    private BrowserSession _createBrowserSession(SIDestinationAddress destAddress,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 String alternateUser,
                                                 boolean allowMessageGathering)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_createBrowserSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         alternateUser,
                         allowMessageGathering
                        });

        BrowserSessionProxy browserSession = null;
        synchronized (generalLock)
        {
            closeLock.readLock().lock();
            try
            {
                checkAlreadyClosed();

                ProxyQueueConversationGroup pqcg = getProxyQueueConversationGroup();
                BrowserProxyQueue proxyQueue = pqcg.createBrowserProxyQueue();

                CommsByteBuffer request = getCommsByteBuffer();

                request.putShort(getConnectionObjectID()); // connection object id
                if (proxyQueue != null) // client session id
                {
                    request.putShort(proxyQueue.getId());
                } else
                {
                    request.putShort(0);
                }
                request.putInt(HIGH_QUEUE_BYTES); // requested bytes

                // Now put the destination type
                if (destType == null) {
                    request.putShort(CommsConstants.NO_DEST_TYPE);
                } else {
                    request.putShort((short) destType.toInt());
                }

                // Add the browser flags if JFAP9 or above SIB0113.comms.1
                final boolean fap9OrAbove = getConversation().getHandshakeProperties()
                                .getFapLevel() >= JFapChannelConstants.FAP_VERSION_9;
                if (fap9OrAbove) {
                    short browserFlags = 0;

                    if (allowMessageGathering)
                        browserFlags |= CommsConstants.BF_ALLOW_GATHERING;

                    request.putShort(browserFlags);
                }

                // Now put the destination info
                request.putSIDestinationAddress(destAddress, getConversation()
                                .getHandshakeProperties().getFapLevel());

                // And now the selection criteria
                request.putSelectionCriteria(criteria);

                // And now the alternate user
                request.putString(alternateUser);

                // Exchange request with ME.
                CommsByteBuffer reply = jfapExchange(request,
                                                     JFapChannelConstants.SEG_CREATE_BROWSER_SESS,
                                                     JFapChannelConstants.PRIORITY_MEDIUM, true);

                try {
                    short err = CommsConstants.SI_NO_EXCEPTION;
                    try {
                        err = reply
                                        .getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_BROWSER_SESS_R);
                        if (err != CommsConstants.SI_NO_EXCEPTION) {
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SILimitExceededException(reply, err);
                            checkFor_SINotAuthorizedException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SISelectorSyntaxException(reply, err);
                            checkFor_SIDiscriminatorSyntaxException(reply, err);
                            checkFor_SITemporaryDestinationNotFoundException(reply, err);
                            checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                        }
                    } finally {
                        if (err != CommsConstants.SI_NO_EXCEPTION) {
                            // Bury the proxy queue
                            ClientConversationState convState = (ClientConversationState) getConversation()
                                            .getAttachment();
                            convState.getProxyQueueConversationGroup().bury(proxyQueue);
                        }
                    }

                    browserSession = new BrowserSessionProxy(getConversation(), this,
                                    reply, proxyQueue, destAddress);
                    proxyQueue.setBrowserSession(browserSession);
                } finally {
                    reply.release();
                }
            } finally {
                closeLock.readLock().unlock();
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_createBrowserSession", browserSession);
        return browserSession;
    }

    // SIB0113.comms.1 end

    /**
     * Form of createProducerSession that doesn't take a discriminator.
     * 
     * @param destAddr
     * @param destType
     * @param extendedMessageOrderingContext
     * @param alternateUser
     * 
     * @return ProducerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddr,
                                                 DestinationType destType,
                                                 OrderingContext extendedMessageOrderingContext,
                                                 String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException
    {
        return createProducerSession(destAddr,
                                     null,
                                     destType,
                                     extendedMessageOrderingContext,
                                     alternateUser);
    }

    /**
     * Proxies the identically named method on the server which
     * returns a ProducerSession, used to send messages to the named destination.
     * If the destination is inaccessible (for example if it does not exist of the
     * application does not have permission to access it), then an exception is
     * thrown. Optionally, a discriminator may be specified, to direct the message
     * to a subset of consumers attached to the destination. Most commonly, this is
     * used to provide the familiar publish/subscribe behaviour, in which case the
     * discriminator corresponds to a Topic. However, it can also be used for
     * DISTRIBUTION=ONE destinations. The syntax of the discriminator is significant:
     * it is hierarchical, with a '/' separating the levels of the hierarchy. The
     * hierarchy is significant both to consumers, in selecting a subset of messages
     * to receive, and to ACLs.
     * 
     * @param destAddress
     * @param discriminator
     * @param destType
     * @param orderContext
     * @param alternateUser
     * 
     * @return ProducerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddress,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext orderContext,
                                                 String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException,
                    SIIncorrectCallException, SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createProducerSession",
                        new Object[]
                        {
                         destAddress,
                         discriminator,
                         destType,
                         orderContext,
                         alternateUser
                        });

        final ProducerSession ps = _createProducerSession(destAddress, discriminator, destType, orderContext, alternateUser, false, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createProducerSession", ps);
        return ps;
    }

    // SIB0113.comms.1 start

    /**
     * Proxies the identically named method on the server which
     * returns a ProducerSession, used to send messages to the named destination.
     * If the destination is inaccessible (for example if it does not exist of the
     * application does not have permission to access it), then an exception is
     * thrown. Optionally, a discriminator may be specified, to direct the message
     * to a subset of consumers attached to the destination. Most commonly, this is
     * used to provide the familiar publish/subscribe behaviour, in which case the
     * discriminator corresponds to a Topic. However, it can also be used for
     * DISTRIBUTION=ONE destinations. The syntax of the discriminator is significant:
     * it is hierarchical, with a '/' separating the levels of the hierarchy. The
     * hierarchy is significant both to consumers, in selecting a subset of messages
     * to receive, and to ACLs.
     * 
     * @param destAddress
     * @param discriminator
     * @param destType
     * @param orderContext
     * @param alternateUser
     * @param bindToQueuePoint
     * @param preferLocalQueuePoint
     * 
     * @return ProducerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException
     */
    @Override
    public ProducerSession createProducerSession(SIDestinationAddress destAddress,
                                                 String discriminator,
                                                 DestinationType destType,
                                                 OrderingContext orderContext,
                                                 String alternateUser,
                                                 boolean bindToQueuePoint,
                                                 boolean preferLocalQueuePoint)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException,
                    SIIncorrectCallException, SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createProducerSession",
                        new Object[]
                        {
                         destAddress,
                         discriminator,
                         destType,
                         orderContext,
                         alternateUser,
                         bindToQueuePoint,
                         preferLocalQueuePoint
                        });

        // This method is only valid from FAP 9 (WAS v7.0) onwards with non-default values

        if (bindToQueuePoint || !preferLocalQueuePoint)
        {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);
        }

        final ProducerSession ps = _createProducerSession(destAddress, discriminator, destType, orderContext, alternateUser, bindToQueuePoint, preferLocalQueuePoint);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createProducerSession", ps);
        return ps;
    }

    // SIB0113.comms.1 end

    /**
     * Proxies the identically named method on the server which
     * returns a ProducerSession, used to send messages to the named destination.
     * If the destination is inaccessible (for example if it does not exist of the
     * application does not have permission to access it), then an exception is
     * thrown. Optionally, a discriminator may be specified, to direct the message
     * to a subset of consumers attached to the destination. Most commonly, this is
     * used to provide the familiar publish/subscribe behaviour, in which case the
     * discriminator corresponds to a Topic. However, it can also be used for
     * DISTRIBUTION=ONE destinations. The syntax of the discriminator is significant:
     * it is hierarchical, with a '/' separating the levels of the hierarchy. The
     * hierarchy is significant both to consumers, in selecting a subset of messages
     * to receive, and to ACLs.
     * 
     * @param destAddress
     * @param discriminator
     * @param destType
     * @param orderContext
     * @param alternateUser
     * @param bindToQueuePoint
     * @param preferLocalQueuePoint
     * 
     * @return ProducerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException
     */

    private ProducerSession _createProducerSession(SIDestinationAddress destAddress,
                                                   String discriminator,
                                                   DestinationType destType,
                                                   OrderingContext orderContext,
                                                   String alternateUser,
                                                   boolean bindToQueuePoint, //SIB0113.comms.1
                                                   boolean preferLocalQueuePoint) //SIB0113.comms.1
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SINotPossibleInCurrentConfigurationException,
                    SITemporaryDestinationNotFoundException,
                    SIIncorrectCallException, SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_createProducerSession",
                        new Object[]
                        {
                         destAddress,
                         discriminator,
                         destType,
                         orderContext,
                         alternateUser,
                         bindToQueuePoint, //SIB0113.comms.1
                         preferLocalQueuePoint //SIB0113.comms.1
                        });

        ProducerSession ps = null;
        OrderingContextProxy oc = null;

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

                    // Before we flow the create, ensure we increment the use count on the order context
                    // if they passed one. This is to ensure one gets created if it does not exist
                    // already.
                    if (orderContext != null)
                    {
                        oc = (OrderingContextProxy) orderContext;
                        oc.incrementUseCount();
                    }

                    CommsByteBuffer request = getCommsByteBuffer();

                    // Connection Object Ref
                    request.putShort(getConnectionObjectID());

                    // Optionaly add the message order context id
                    if (orderContext != null)
                    {
                        oc = (OrderingContextProxy) orderContext;
                        request.putShort(oc.getId());
                    }
                    else
                    {
                        request.putShort(CommsConstants.NO_ORDER_CONTEXT);
                    }

                    // Now put the destination type
                    if (destType == null)
                    {
                        request.putShort(CommsConstants.NO_DEST_TYPE);
                    }
                    else
                    {
                        request.putShort(destType.toInt());
                    }

                    // Add the producer flags if JFAP9 or above                                               SIB0113.comms.1
                    final boolean fap9OrAbove = getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9;
                    if (fap9OrAbove) {
                        short producerFlags = 0;

                        if (bindToQueuePoint)
                            producerFlags |= CommsConstants.PF_BIND_TO_QUEUE_POINT;
                        if (preferLocalQueuePoint)
                            producerFlags |= CommsConstants.PF_PREFER_LOCAL_QUEUE_POINT;

                        request.putShort(producerFlags);
                    }

                    // Destination
                    request.putSIDestinationAddress(destAddress, getConversation().getHandshakeProperties().getFapLevel());

                    request.putString(discriminator);

                    // Add alternate user id.
                    request.putString(alternateUser);

                    // Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_CREATE_PRODUCER_SESS,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    try
                    {
                        short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_PRODUCER_SESS_R);
                        if (err != CommsConstants.SI_NO_EXCEPTION)
                        {
                            // If we failed, ensure we decrement the use count again
                            if (oc != null)
                                oc.decrementUseCount();

                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SILimitExceededException(reply, err);
                            checkFor_SINotAuthorizedException(reply, err);
                            checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                            checkFor_SITemporaryDestinationNotFoundException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SIDiscriminatorSyntaxException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                        }

                        ps = new ProducerSessionProxy(getConversation(),
                                        this,
                                        oc,
                                        reply,
                                        destAddress,
                                        destType);
                    } finally
                    {
                        reply.release();
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_createProducerSession", ps);
        return ps;
    }

    /**
     * Creates a consumer session.
     * <p>
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param reliability
     * @param enableReadAhead
     * @param nolocal
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param alternateUser
     * 
     * @return ConsumerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddress,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumerSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser
                        });

        ConsumerSession sess = _createConsumerSession(null, //subcription name null for non-shared consumers
                                                      destAddress, destType, criteria, reliability,
                                                      enableReadAhead, nolocal,
                                                      unrecoverableReliability, bifurcatable,
                                                      alternateUser, true, false, null); //SIB0113.comms.1,SIB0163.comms.1

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSession", sess);
        return sess;
    }

    /**
     * Creates a consumer session with the option of specifying ignoreInitialIndoubts. This method
     * will throw an exception if it is called and we are connected to a <6.1 server.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param reliability
     * @param enableReadAhead
     * @param nolocal
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param alternateUser
     * @param ignoreInitialIndoubts
     * 
     * @return ConsumerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddress,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumerSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser,
                         ignoreInitialIndoubts
                        });

        // This method is only valid from FAP 5 onwards. Note that we only will check this if the
        // ignoreInitialIndoubts is set to the non-default value as it is possible that Core SPI
        // users like the RA may also whack in default values here. The actual value will not be
        // put into the FAP unless the FAP level supports it, regardless of whether we perform this
        // check.
        if (!ignoreInitialIndoubts)
        {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_5);
        }

        // Now continue the call
        ConsumerSession sess = _createConsumerSession(null, //subcription name null for non-shared consumers 
                                                      destAddress, destType, criteria, reliability,
                                                      enableReadAhead, nolocal,
                                                      unrecoverableReliability, bifurcatable,
                                                      alternateUser, ignoreInitialIndoubts, false, null); //SIB0113.comms.1,SIB0163.comms.1

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSession", sess);
        return sess;
    }

    // SIB0113.comms.1 start

    /**
     * Creates a consumer session with the option of specifying allowMessageGathering. This method
     * will throw an exception if it is called and we are connected to a <7.0 server.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param reliability
     * @param enableReadAhead
     * @param nolocal
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param alternateUser
     * @param ignoreInitialIndoubts
     * @param allowMessageGathering
     * 
     * @return ConsumerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */

    public ConsumerSession createConsumerSession(SIDestinationAddress destAddress,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts,
                                                 boolean allowMessageGathering)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumerSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser,
                         ignoreInitialIndoubts,
                         allowMessageGathering
                        });

        // This method is only valid from FAP 9 (WAS v7.0) onwards with non-default values

        if (allowMessageGathering)
        {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);
        }

        // Now continue the call
        ConsumerSession sess = _createConsumerSession(null,//subcription name null for non-shared consumers
                                                      destAddress, destType, criteria, reliability,
                                                      enableReadAhead, nolocal,
                                                      unrecoverableReliability, bifurcatable,
                                                      alternateUser, ignoreInitialIndoubts,
                                                      allowMessageGathering, null); // SIB0163.comms.1

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSession", sess);
        return sess;
    }

    // SIB0113.comms.1 end

    // SIB0163.comms.1 start

    /**
     * Creates a consumer session with the option of specifying allowMessageGathering. This method
     * will throw an exception if it is called and we are connected to a <7.0 server.
     * 
     * @param destAddress
     * @param destType
     * @param criteria
     * @param reliability
     * @param enableReadAhead
     * @param nolocal
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param alternateUser
     * @param ignoreInitialIndoubts
     * @param allowMessageGathering
     * @param messageControlProperties
     * 
     * @return ConsumerSession
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */

    @Override
    public ConsumerSession createConsumerSession(SIDestinationAddress destAddress,
                                                 DestinationType destType,
                                                 SelectionCriteria criteria,
                                                 Reliability reliability,
                                                 boolean enableReadAhead,
                                                 boolean nolocal,
                                                 Reliability unrecoverableReliability,
                                                 boolean bifurcatable,
                                                 String alternateUser,
                                                 boolean ignoreInitialIndoubts,
                                                 boolean allowMessageGathering,
                                                 Map<String, String> messageControlProperties)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumerSession",
                        new Object[]
                        {
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser,
                         ignoreInitialIndoubts,
                         allowMessageGathering,
                         messageControlProperties
                        });

        // This method is only valid from FAP 9 (WAS v7.0) onwards with non-default values

        if (messageControlProperties != null && !messageControlProperties.isEmpty()) {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);
        }

        // Now continue the call
        ConsumerSession sess = _createConsumerSession(null,//subcription name null for non-shared consumers
                                                      destAddress, destType, criteria, reliability,
                                                      enableReadAhead, nolocal,
                                                      unrecoverableReliability, bifurcatable,
                                                      alternateUser, ignoreInitialIndoubts,
                                                      allowMessageGathering, messageControlProperties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSession", sess);
        return sess;
    }

    // SIB0163.comms.1 end

    @Override
    public ConsumerSession createSharedConsumerSession(String subscriptionName,
                                                       SIDestinationAddress destAddress,
                                                       DestinationType destType,
                                                       SelectionCriteria criteria,
                                                       Reliability reliability,
                                                       boolean enableReadAhead,
                                                       boolean supportsMultipleConsumers,
                                                       boolean nolocal,
                                                       Reliability unrecoverableReliability,
                                                       boolean bifurcatable,
                                                       String alternateUser,
                                                       boolean ignoreInitialIndoubts,
                                                       boolean allowMessageGathering,
                                                       Map<String, String> messageControlProperties)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumerSession",
                        new Object[]
                        {
                         subscriptionName,
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         supportsMultipleConsumers,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser,
                         ignoreInitialIndoubts,
                         allowMessageGathering,
                         messageControlProperties
                        });

        // This method is only valid from FAP 9 (WAS v7.0) onwards with non-default values

        if (messageControlProperties != null && !messageControlProperties.isEmpty()) {
            final HandshakeProperties props = getConversation().getHandshakeProperties();
            CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);
        }

        // Check we are running at a suitable FAP level to support JMS 2.0 Api
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_20);

        // Now continue the call
        ConsumerSession sess = _createConsumerSession(subscriptionName,
                                                      destAddress,
                                                      destType,
                                                      criteria,
                                                      reliability,
                                                      enableReadAhead,
                                                      nolocal,
                                                      unrecoverableReliability,
                                                      bifurcatable,
                                                      alternateUser,
                                                      ignoreInitialIndoubts,
                                                      allowMessageGathering,
                                                      messageControlProperties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSession", sess);
        return sess;
    }

    private ConsumerSession _createConsumerSession(String subscriptionName,
                                                   SIDestinationAddress destAddress,
                                                   DestinationType destType,
                                                   SelectionCriteria criteria,
                                                   Reliability reliability,
                                                   boolean enableReadAhead,
                                                   boolean nolocal,
                                                   Reliability unrecoverableReliability,
                                                   boolean bifurcatable,
                                                   String alternateUser,
                                                   boolean ignoreInitialIndoubts,
                                                   boolean allowMessageGathering, //SIB0113.comms.1
                                                   Map<String, String> messageControlProperties) // SIB0163.comms.1
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_createConsumerSession",
                        new Object[]
                        {
                         subscriptionName,
                         destAddress,
                         destType,
                         criteria,
                         reliability,
                         enableReadAhead,
                         nolocal,
                         unrecoverableReliability,
                         bifurcatable,
                         alternateUser,
                         ignoreInitialIndoubts,
                         allowMessageGathering, //SIB0113.comms.1
                         messageControlProperties // SIB0163.comms.1
                        });

        ConsumerSessionProxy cs = null;
        ClientConversationState state = (ClientConversationState) getConversation().getAttachment();

// Currently we do not support both bifurcated and read ahead.
        if (bifurcatable)
            enableReadAhead = false;

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();
                    AsynchConsumerProxyQueue proxyQueue = null;
                    ProxyQueueConversationGroup pqcg = null;

// Are we creating a read-ahead consumer?
                    if (enableReadAhead)
                    {
// We need to create a read ahead proxy queue,
// first check if we already have a proxy queue
// group to create the proxy queue in.
                        pqcg = getProxyQueueConversationGroup();

// Create the read ahead proxy queue.
                        proxyQueue = pqcg.createReadAheadProxyQueue(unrecoverableReliability);
                    }

                    CommsByteBuffer request = getCommsByteBuffer();

// Connection Object Ref
                    request.putShort(getConnectionObjectID());
// Client Session ID
                    if (proxyQueue != null)
                    {
                        request.putShort(proxyQueue.getId());
                    }
                    else
                    {
                        request.putShort(0);
                    }

// Consumer flags
                    short consumerFlags = 0;

                    if (enableReadAhead)
                        consumerFlags |= CommsConstants.CF_READAHEAD;
                    if (nolocal)
                        consumerFlags |= CommsConstants.CF_NO_LOCAL;
                    if (bifurcatable)
                        consumerFlags |= CommsConstants.CF_BIFURCATABLE;

                    consumerFlags |= CommsConstants.CF_UNICAST;

// Only add this option if we are FAP 5 or greater
                    boolean fap5OrAbove = getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5;
                    if (ignoreInitialIndoubts && fap5OrAbove)
                        consumerFlags |= CommsConstants.CF_IGNORE_INITIAL_INDOUBTS;

// Only add this option if we are FAP 9 or greater
                    boolean fap9OrAbove = getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9; //SIB0113.comms.1
                    if (allowMessageGathering && fap9OrAbove)
                        consumerFlags |= CommsConstants.CF_ALLOW_GATHERING; //SIB0113.comms.1
// Only add this option if we are FAP 17 or greater
                    boolean fap20OrAbove = getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_20;
                    if ((null != subscriptionName) && fap20OrAbove)
                    { //0x0004 for supportsMultipleConsumers to enable shared non durable subscribers
                        consumerFlags |= CommsConstants.CF_MULTI_CONSUMER;
                    }

                    request.putShort(consumerFlags);

// Reliability
                    if (reliability != null)
                    {
                        request.putShort(reliability.toInt());
                    }
                    else
                    {
                        request.putShort(-1); // A -1 is to signify that no reliability has been set
                    }

// Requested bytes
                    request.putInt(HIGH_QUEUE_BYTES);

// Now put the destination type
                    if (destType == null)
                    {
                        request.putShort(CommsConstants.NO_DEST_TYPE);
                    }
                    else
                    {
                        request.putShort(destType.toInt());
                    }

// Unrecoverable reliability
                    if (unrecoverableReliability == null)
                    {
                        unrecoverableReliability = Reliability.NONE;
                    }
                    request.putShort(unrecoverableReliability.toInt());

// Add destination info
                    request.putSIDestinationAddress(destAddress, getConversation().getHandshakeProperties().getFapLevel());
// Add Subscription Name to enable shared non durable subscribers
                    if ((null != subscriptionName) && fap20OrAbove)
                        request.putString(subscriptionName);

// Add the selection criteria
                    request.putSelectionCriteria(criteria);

// Add the alternate user id
                    request.putString(alternateUser);

// Add messageControlProperties
                    if (fap9OrAbove) { //SIB0163.comms.1
                        request.putMap(messageControlProperties); //SIB0163.comms.1
                    } //SIB0163.comms.1

// Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_CREATE_CONSUMER_SESS,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    try
                    {
                        short err = CommsConstants.SI_NO_EXCEPTION;
                        try
                        {
                            err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_CONSUMER_SESS_R);
                            if (err != CommsConstants.SI_NO_EXCEPTION)
                            {
                                checkFor_SIConnectionUnavailableException(reply, err);
                                checkFor_SIConnectionDroppedException(reply, err);
                                checkFor_SIResourceException(reply, err);
                                checkFor_SIConnectionLostException(reply, err);
                                checkFor_SILimitExceededException(reply, err);
                                checkFor_SINotAuthorizedException(reply, err);
                                checkFor_SIIncorrectCallException(reply, err);
                                checkFor_SIDestinationLockedException(reply, err);
                                checkFor_SITemporaryDestinationNotFoundException(reply, err);
                                checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                                checkFor_SISelectorSyntaxException(reply, err);
                                checkFor_SIDiscriminatorSyntaxException(reply, err);
                                checkFor_SIErrorException(reply, err);
                                defaultChecker(reply, err);
                            }
                        } finally
                        {
                            if (err != CommsConstants.SI_NO_EXCEPTION)
                            {
// Bury the proxy queue
                                if (proxyQueue != null)
                                    state.getProxyQueueConversationGroup().bury(proxyQueue);
                            }
                        }

// At this point we need to examine the flags that have been returned by the
// server so we can decide what we should do.
                        long messageProcessorId = reply.getLong();
                        short flags = reply.getShort();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(this, tc, "Message Processor Id", "" + messageProcessorId);
                            SibTr.debug(this, tc, "Consumer flags", "" + flags);
                        }

// We didn't want multicast - did the server tell us to go multicast?
// If it did - we have a problem
                        if ((flags & CommsConstants.CF_MULTICAST) != 0)
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "The server told us to go multicast even though we did not ask for it");
                            throw new SIErrorException(
                                            nls.getFormattedMessage("SERVER_REQUESTED_MULTICAST_SICO1026", null, null));
                        }

// Create the real session
                        cs = new ConsumerSessionProxy(getConversation(),
                                        this,
                                        reply,
                                        proxyQueue,
                                        unrecoverableReliability,
                                        destAddress,
                                        destType,
                                        messageProcessorId);
                    } finally
                    {
                        reply.release();
                    }

// Now inform the proxy queue of the session ID + consumer session.
                    if (proxyQueue != null)
                    {
                        proxyQueue.setConsumerSession(cs);
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
// No FFDC code needed
            }
        }

        synchronized (consumerSessions)
        {
// Add the consumer session to our map
            consumerSessions.put(Short.valueOf(cs.getProxyID()), cs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_createConsumerSession", cs);
        return cs;
    }

    /**
     * Creates a special consumer session that represents an existing consumer session. The returned
     * session is used to manage message acknowledgement / read messages from another place other
     * than an asynchronous callback.
     * <p>
     * The parameter supplied is the id of the consumer session as decided by the message processor
     * on an original consumer session.
     * 
     * @param id The id of the consumer session to create the bifurctated session for.
     * 
     * @return Returns a bifurcated consumer session.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     */
    @Override
    public BifurcatedConsumerSession createBifurcatedConsumerSession(long id)
                    throws SISessionUnavailableException, SISessionDroppedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBifurcatedConsumerSession", "" + id);

        BifurcatedConsumerSession bcs = null;
        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

                    // Get a buffer
                    CommsByteBuffer request = getCommsByteBuffer();

                    // Connection Object Ref
                    request.putShort(getConnectionObjectID());
                    // Id
                    request.putLong(id);

                    // Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_CREATE_BIFURCATED_SESSION,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    try
                    {
                        short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_BIFURCATED_SESSION_R);
                        if (err != CommsConstants.SI_NO_EXCEPTION)
                        {
                            checkFor_SISessionUnavailableException(reply, err);
                            checkFor_SISessionDroppedException(reply, err);
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SILimitExceededException(reply, err);
                            checkFor_SINotAuthorizedException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                        }

                        bcs = new BifurcatedConsumerSessionProxy(getConversation(),
                                        this,
                                        reply);
                    } finally
                    {
                        reply.release();
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createBifurcatedConsumerSession", bcs);
        return bcs;
    }

    /**
     * Creates a durable subscription on the server.
     * 
     * @param subscriptionName
     * @param durableSubscriptionHome
     * @param destAddr
     * @param criteria
     * @param supportsMultipleConsumers
     * @param nolocal
     * @param alternateUser
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException
     */
    @Override
    public void createDurableSubscription(String subscriptionName,
                                          String durableSubscriptionHome,
                                          SIDestinationAddress destAddr,
                                          SelectionCriteria criteria, // F207007.2
                                          boolean supportsMultipleConsumers,
                                          boolean nolocal,
                                          String alternateUser) // F219476.2
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDurableSubscriptionAlreadyExistsException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableSubscription",
                        new Object[]
                        {
                         subscriptionName,
                         durableSubscriptionHome,
                         destAddr,
                         criteria, // F207007.2
                         "" + supportsMultipleConsumers,
                         "" + nolocal,
                         alternateUser // F219476.2
                        });

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

//               ClientConversationState convState = (ClientConversationState) getConversation().getAttachment();   // f173559

                    CommsByteBuffer request = getCommsByteBuffer();

                    // BIT 16 Connection Object Ref
                    request.putShort(getConnectionObjectID());

                    // BIT 16 Consumer flags
                    int consumerFlags = 0x0000;
                    if (nolocal)
                    { //0x0002 for nolocal
                        consumerFlags = (consumerFlags | 0x0002);
                    }
                    if (supportsMultipleConsumers)
                    { //0x0004 for supportsMultipleConsumers
                        consumerFlags = (consumerFlags | 0x0004);
                    }
                    request.putShort(consumerFlags);

                    // Add destination info
                    request.putSIDestinationAddress(destAddr, getConversation().getHandshakeProperties().getFapLevel());

                    // Add Subscription Name
                    request.putString(subscriptionName);

                    // Add Subscription Home
                    request.putString(durableSubscriptionHome);

                    // Add selection criteria
                    request.putSelectionCriteria(criteria);

                    // Add alternate user id
                    request.putString(alternateUser);

                    // Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_CREATE_DURABLE_SUB,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    try
                    {
                        short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_DURABLE_SUB_R);
                        if (err != CommsConstants.SI_NO_EXCEPTION)
                        {
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SILimitExceededException(reply, err);
                            checkFor_SINotAuthorizedException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                            checkFor_SIDurableSubscriptionAlreadyExistsException(reply, err);
                            checkFor_SISelectorSyntaxException(reply, err);
                            checkFor_SIDiscriminatorSyntaxException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                        }
                    } finally
                    {
                        reply.release();
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Interrupted", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createDurableSubscription");
    }

    /**
     * Creates a durable subscription on the server.
     * 
     * @param subscriptionName
     * @param durableSubscriptionHome
     * @param destAddr
     * @param criteria
     * @param supportsMultipleConsumers
     * @param nolocal
     * @param reliability
     * @param enableReadAhead
     * @param unrecoverableReliability
     * @param bifurcatable
     * @param alternateUser
     * 
     * @return Returns a Consumer Session for the durable subscription
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException
     * @throws com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     */
    @Override
    public ConsumerSession createConsumerSessionForDurableSubscription(String subscriptionName,
                                                                       String durableSubscriptionHome,
                                                                       SIDestinationAddress destAddr,
                                                                       SelectionCriteria criteria,
                                                                       boolean supportsMultipleConsumers,
                                                                       boolean nolocal,
                                                                       Reliability reliability,
                                                                       boolean enableReadAhead,
                                                                       Reliability unrecoverableReliability,
                                                                       boolean bifurcatable,
                                                                       String alternateUser) // F219476.2
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException, SIDurableSubscriptionMismatchException,
                    SIDestinationLockedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConsumerSessionForDurableSubscription",
                        new Object[]
                        {
                         subscriptionName,
                         durableSubscriptionHome,
                         destAddr,
                         criteria,
                         reliability,
                         "" + enableReadAhead,
                         "" + supportsMultipleConsumers,
                         "" + nolocal,
                         unrecoverableReliability,
                         "" + bifurcatable,
                         alternateUser
                        });

        ConsumerSessionProxy cs = null;
        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

                    ClientConversationState state = (ClientConversationState) getConversation().getAttachment();

                    // Currently we do not support both bifurcated and read ahead.
                    if (bifurcatable)
                        enableReadAhead = false;

                    AsynchConsumerProxyQueue proxyQueue = null;
                    // Are we creating a read-ahead consumer?
                    if (enableReadAhead)
                    {
                        // We need to create a read ahead proxy queue,
                        // first check if we already have a proxy queue
                        // group to create the proxy queue in.
                        ProxyQueueConversationGroup pqcg = getProxyQueueConversationGroup();

                        // Create the read ahead proxy queue.
                        proxyQueue = pqcg.createReadAheadProxyQueue(unrecoverableReliability);
                    }

                    CommsByteBuffer request = getCommsByteBuffer();

                    // BIT 16 Connection Object Ref
                    request.putShort(getConnectionObjectID());

                    // BIT 16 Client Session ID
                    if (proxyQueue != null)
                    {
                        request.putShort(proxyQueue.getId());
                    }
                    else
                    {
                        request.putShort(0);
                    }

                    // BIT 16 Consumer flags -
                    int consumerFlags = 0x0000;
                    if (enableReadAhead) //0x0001 for read-ahead
                    {
                        consumerFlags = 0x0001;
                    }
                    if (nolocal)
                    { //0x0002 for nolocal
                        consumerFlags = (consumerFlags | 0x0002);
                    }
                    if (supportsMultipleConsumers)
                    { //0x0004 for supportsMultipleConsumers
                        consumerFlags = (consumerFlags | 0x0004);
                    }
                    if (bifurcatable)
                        consumerFlags |= CommsConstants.CF_BIFURCATABLE;
                    request.putShort(consumerFlags);

                    // BIT 16 Reliability
                    if (reliability != null)
                    {
                        request.putShort(reliability.toInt());
                    }
                    else
                    {
                        request.putShort(-1);
                    }

                    // BIT 32 Requested bytes
                    request.putInt(HIGH_QUEUE_BYTES);

                    // BIT16 Unrecoverable reliability
                    if (unrecoverableReliability == null)
                    {
                        unrecoverableReliability = Reliability.NONE;
                    }
                    request.putShort(unrecoverableReliability.toInt());

                    // Add destination info
                    request.putSIDestinationAddress(destAddr, getConversation().getHandshakeProperties().getFapLevel());

                    // Add Subscription Name
                    request.putString(subscriptionName);

                    // Add Subscription Home
                    request.putString(durableSubscriptionHome);

                    // Add selection criteria
                    request.putSelectionCriteria(criteria);

                    // Add alternate user id
                    request.putString(alternateUser);

                    // Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_CREATE_CONS_FOR_DURABLE_SUB,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    try
                    {
                        short err = CommsConstants.SI_NO_EXCEPTION;
                        try
                        {
                            err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_CONS_FOR_DURABLE_SUB_R);
                            if (err != CommsConstants.SI_NO_EXCEPTION)
                            {
                                checkFor_SIConnectionUnavailableException(reply, err);
                                checkFor_SIConnectionDroppedException(reply, err);
                                checkFor_SIResourceException(reply, err);
                                checkFor_SIConnectionLostException(reply, err);
                                checkFor_SILimitExceededException(reply, err);
                                checkFor_SINotAuthorizedException(reply, err);
                                checkFor_SIIncorrectCallException(reply, err);
                                checkFor_SIDurableSubscriptionNotFoundException(reply, err);
                                checkFor_SIDurableSubscriptionMismatchException(reply, err);
                                checkFor_SIDestinationLockedException(reply, err);
                                checkFor_SIErrorException(reply, err);
                                defaultChecker(reply, err);
                            }
                        } finally
                        {
                            if (err != CommsConstants.SI_NO_EXCEPTION)
                            {
                                // Bury the proxy queue
                                if (proxyQueue != null)
                                    state.getProxyQueueConversationGroup().bury(proxyQueue);
                            }
                        }

                        long messageProcessorId = reply.getLong();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Message processor Id:",
                                        "" + messageProcessorId);

                        // Create the real session
                        cs = new ConsumerSessionProxy(getConversation(),
                                        this,
                                        reply,
                                        proxyQueue,
                                        unrecoverableReliability,
                                        //                                                false,
                                        destAddr,
                                        DestinationType.TOPICSPACE,
                                        messageProcessorId);

                        // Now inform the proxy queue of the session ID + consumer session.
                        if (proxyQueue != null)
                        {
                            proxyQueue.setConsumerSession(cs);
                        }
                    } finally
                    {
                        reply.release();
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Interrupted", e);
            }
        }

        synchronized (consumerSessions)
        {
            // Add the consumer session to our map
            consumerSessions.put(Short.valueOf(cs.getProxyID()), cs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConsumerSessionForDurableSubscription", cs);
        return cs;
    }

    /**
     * Deletes a durable subscription.
     * 
     * @param subscriptionName
     * @param durableSubscriptionHome
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     */
    @Override
    public void deleteDurableSubscription(String subscriptionName, String durableSubscriptionHome)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteDurableSubscription",
                        new Object[]
                        {
                         subscriptionName, durableSubscriptionHome
                        });

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

                    CommsByteBuffer request = getCommsByteBuffer();

                    // Connection Object Ref
                    request.putShort(this.getConnectionObjectID());

                    // Add Subscription Name
                    request.putString(subscriptionName);

                    // Add Subscription home
                    request.putString(durableSubscriptionHome);

                    // Pass on call to server
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_DELETE_DURABLE_SUB,
                                                         JFapChannelConstants.PRIORITY_MEDIUM,
                                                         true);

                    // Start d186970
                    try
                    {
                        short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_DELETE_DURABLE_SUB_R);
                        if (err != CommsConstants.SI_NO_EXCEPTION)
                        {
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIResourceException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            checkFor_SINotAuthorizedException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SIDurableSubscriptionNotFoundException(reply, err);
                            checkFor_SIDestinationLockedException(reply, err);
                            checkFor_SIErrorException(reply, err);
                            defaultChecker(reply, err);
                        }
                    } finally
                    {
                        reply.release();
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteDurableSubscription");
    }

    // *******************************************************************************************
    // *                                Destination manipulation Methods                         *
    // *******************************************************************************************

    /**
     * This method proxies the identical call on the actual SICoreConnection.
     * 
     * @param destAddr
     * 
     * @return the destination's configuration
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public DestinationConfiguration getDestinationConfiguration(SIDestinationAddress destAddr) // f192759.2
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        DestinationConfiguration dc = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestinationConfiguration");

        checkAlreadyClosed();

        CommsByteBuffer request = getCommsByteBuffer();
        request.putShort(getConnectionObjectID());
        request.putSIDestinationAddress(destAddr, getConversation().getHandshakeProperties().getFapLevel());

        CommsByteBuffer reply = jfapExchange(request,
                                             JFapChannelConstants.SEG_GET_DESTINATION_CONFIGURATION,
                                             JFapChannelConstants.PRIORITY_MEDIUM,
                                             true);

        try
        {
            short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_GET_DESTINATION_CONFIGURATION_R);
            if (err != CommsConstants.SI_NO_EXCEPTION)
            {
                checkFor_SIConnectionUnavailableException(reply, err);
                checkFor_SIConnectionDroppedException(reply, err);
                checkFor_SIResourceException(reply, err);
                checkFor_SIConnectionLostException(reply, err);
                checkFor_SINotAuthorizedException(reply, err);
                checkFor_SIIncorrectCallException(reply, err);
                checkFor_SITemporaryDestinationNotFoundException(reply, err);
                checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                checkFor_SIErrorException(reply, err);
                defaultChecker(reply, err);
            }

            int defaultPriority = reply.getInt();
            int maxFailedDevliveries = reply.getInt();
            short defaultReliabilityShort = reply.getShort();
            short maxReliabilityShort = reply.getShort();
            short destinationTypeShort = reply.getShort();
            short destinationFlags = reply.getShort();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "Default Priority     :", "" + defaultPriority);
                SibTr.debug(this, tc, "Max failed deliveries:", "" + maxFailedDevliveries);
                SibTr.debug(this, tc, "Default Reliability  :", "" + defaultReliabilityShort);
                SibTr.debug(this, tc, "Max Reliability      :", "" + maxReliabilityShort);
                SibTr.debug(this, tc, "Destination Type     :", "" + destinationTypeShort);
                SibTr.debug(this, tc, "Destination Flags    :", "" + destinationFlags);
            }

            Reliability defaultReliability = null;
            if (defaultReliabilityShort != -1)
            {
                defaultReliability = Reliability.getReliability(defaultReliabilityShort);
            }
            Reliability maxReliability = null;
            if (maxReliabilityShort != -1)
            {
                maxReliability = Reliability.getReliability(maxReliabilityShort);
            }
            DestinationType destType = null;
            if (destinationTypeShort != CommsConstants.NO_DEST_TYPE) // D202625
            {
                destType = DestinationType.getDestinationType(destinationTypeShort);
            }

            boolean producerQOSOverrideEnabled = (destinationFlags & 0x0001) != 0;
            boolean receiveAllowed = (destinationFlags & 0x0002) != 0;
            boolean receiveExclusive = (destinationFlags & 0x0004) != 0;
            boolean sendAllowed = (destinationFlags & 0x0008) != 0;

            // Fap version 5 and onwards uses the 5th bit of the flags BIT16 to determine whether
            // the destination requires strict ordering.
            short fapLevel = getConversation().getHandshakeProperties().getFapLevel();
            final boolean strictOrderingRequired;
            if (fapLevel >= JFapChannelConstants.FAP_VERSION_5)
            {
                strictOrderingRequired = (destinationFlags & 0x0010) != 0;
            }
            else
            {
                strictOrderingRequired = false;
            }

            // Get the UUID
            String uuid = reply.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "UUID:", uuid);

            // Get the description
            String description = reply.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Description:", description);

            // Get the exception destination
            String exceptionDestination = reply.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "ExceptionDestination name:", exceptionDestination);

            // Get the destination name
            String name = reply.getString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Name:", name);

            // Get the reply destination address
            SIDestinationAddress replyDestAddr = reply.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Reply Dest Addr:", replyDestAddr);

            // The rest of the data will contain a set of name value String pairs that need to
            // be added to a map to pass to the DestinationConfiguration
            HashMap<String, String> destContext = new HashMap<String, String>();

            // Get the number of name / value pairs
            short numberOfValues = reply.getShort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Number of name value pairs: ", "" + numberOfValues);

            for (int x = 0; x < numberOfValues; x++)
            {
                String contextName = reply.getString();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Name:", contextName);

                String contextValue = reply.getString();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Value:", contextValue);

                // Add them to the map
                destContext.put(contextName, contextValue);
            }

            // Get the number of SIDestinationAddresses in the forward routing path
            SIDestinationAddress[] frp = null;

            short numberOfFRPAddress = reply.getShort();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Number of SI Destination Address: ", "" + numberOfFRPAddress);

            if (numberOfFRPAddress != 0)
            {
                frp = new SIDestinationAddress[numberOfFRPAddress];

                for (int x = 0; x < numberOfFRPAddress; x++)
                {
                    frp[x] = reply.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel());
                }
            }

            dc = new DestinationConfigurationImpl(defaultPriority,
                            defaultReliability,
                            description,
                            destContext,
                            destType,
                            exceptionDestination,
                            maxFailedDevliveries,
                            maxReliability,
                            name,
                            uuid,
                            producerQOSOverrideEnabled,
                            receiveAllowed,
                            receiveExclusive,
                            sendAllowed,
                            frp,
                            replyDestAddr,
                            strictOrderingRequired);
        } finally
        {
            reply.release();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestinationConfiguration", dc);
        return dc;
    }

    /**
     * Exchanges with the server to create a temporary destination. A model destination
     * name is needed to create the queue from and the actual name of the destination
     * is returned.
     * 
     * @param distribution
     * @param destinationPrefix
     * 
     * @return Returns an SIDestinationAddress representing the destination.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.wsspi.sib.core.exception.SIInvalidDestinationPrefixException
     */
    @Override
    public SIDestinationAddress createTemporaryDestination(Distribution distribution,
                                                           String destinationPrefix)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIInvalidDestinationPrefixException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryDestination",
                        new Object[] { distribution, destinationPrefix });

        checkAlreadyClosed();

//      ClientConversationState convState = (ClientConversationState) getConversation().getAttachment();   // f173559

        // Now perform the exchange to create the temp destination on the server
        CommsByteBuffer request = getCommsByteBuffer();

        // Connection Object Ref
        request.putShort(getConnectionObjectID());
        // Distribution
        request.putShort(distribution.toInt());
        // Add destintion prefix
        request.putString(destinationPrefix);

        // Pass on call to server
        CommsByteBuffer reply = jfapExchange(request,
                                             JFapChannelConstants.SEG_CREATE_TEMP_DESTINATION,
                                             JFapChannelConstants.PRIORITY_MEDIUM,
                                             true);

        SIDestinationAddress destAddress = null;

        try
        {
            short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_TEMP_DESTINATION_R);
            if (err != CommsConstants.SI_NO_EXCEPTION)
            {
                checkFor_SIConnectionUnavailableException(reply, err);
                checkFor_SIConnectionDroppedException(reply, err);
                checkFor_SIResourceException(reply, err);
                checkFor_SIConnectionLostException(reply, err);
                checkFor_SILimitExceededException(reply, err);
                checkFor_SINotAuthorizedException(reply, err);
                checkFor_SIInvalidDestinationPrefixException(reply, err);
                checkFor_SIErrorException(reply, err);
                defaultChecker(reply, err);
            }

            // Now reconstruct the returned SIDestinationAddress
            destAddress = reply.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel());
        } finally
        {
            reply.release();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createTemporaryDestination", destAddress);
        return destAddress;
    }

    /**
     * The real deleteTemporaryDestination method taking a SIDestinationAddress
     * 
     * @param destAddr
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    @Override
    public void deleteTemporaryDestination(SIDestinationAddress destAddr)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteTemporaryDestination", destAddr);

        checkAlreadyClosed();

        CommsByteBuffer request = getCommsByteBuffer();
        request.putShort(getConnectionObjectID());
        request.putSIDestinationAddress(destAddr, getConversation().getHandshakeProperties().getFapLevel());

        // Pass on call to server
        CommsByteBuffer reply = jfapExchange(request,
                                             JFapChannelConstants.SEG_DELETE_TEMP_DESTINATION,
                                             JFapChannelConstants.PRIORITY_MEDIUM,
                                             true);

        try
        {
            short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_DELETE_TEMP_DESTINATION_R);
            if (err != CommsConstants.SI_NO_EXCEPTION)
            {
                checkFor_SIConnectionUnavailableException(reply, err);
                checkFor_SIConnectionDroppedException(reply, err);
                checkFor_SIResourceException(reply, err);
                checkFor_SIConnectionLostException(reply, err);
                checkFor_SINotAuthorizedException(reply, err);
                checkFor_SIDestinationLockedException(reply, err);
                checkFor_SITemporaryDestinationNotFoundException(reply, err);
                checkFor_SIErrorException(reply, err);
                defaultChecker(reply, err);
            }
        } finally
        {
            reply.release();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteTemporaryDestination");
    }

    /**
     * This method is used to send a message to the exception destination or to the named exception
     * destination.
     * 
     * @param address The optional destination address (may be null)
     * @param message
     * @param reason
     * @param inserts
     * @param tran
     * @param alternateUser
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public void sendToExceptionDestination(SIDestinationAddress address,
                                           SIBusMessage message,
                                           int reason,
                                           String[] inserts,
                                           SITransaction tran,
                                           String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendToExceptionDestination",
                        new Object[]
                        {
                         address,
                         message,
                         "" + reason,
                         inserts == null ? "<null>" : inserts.length + " insert(s)",
                         tran,
                         alternateUser
                        });

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();

                try
                {
                    checkAlreadyClosed();

                    // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
                    // slices to make life easier on the Java memory manager
                    final HandshakeProperties props = getConversation().getHandshakeProperties();
                    if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9)
                    {
                        sendChunkedExceptionMessage(address,
                                                    message,
                                                    reason,
                                                    inserts,
                                                    tran,
                                                    alternateUser);
                    }
                    else
                    {
                        sendEntireExceptionMessage(address,
                                                   message,
                                                   reason,
                                                   inserts,
                                                   tran,
                                                   alternateUser);
                    }
                } catch (SITemporaryDestinationNotFoundException e)
                {
                    // No FFDC Code Needed
                    // This is needed just because we are using the common method sendData() which has
                    // to trap this exception for the send() case. We therefore never expect to receive
                    // this exception, and if we do flag it as a serious error.
                    throw new SIErrorException(e);
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC Code Needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendToExceptionDestination");
    }

    private void sendChunkedExceptionMessage(SIDestinationAddress address,
                                             SIBusMessage message,
                                             int reason,
                                             String[] inserts,
                                             SITransaction tran,
                                             String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendChunkedExceptionMessage",
                        new Object[] { address, message, reason, inserts, tran, alternateUser });

        CommsByteBuffer request = getCommsByteBuffer();
        List<DataSlice> messageSlices = null;

        // First job is to encode the message in data slices
        try
        {
            messageSlices = request.encodeFast((JsMessage) message, getCommsConnection(), getConversation());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message encoded into " + messageSlices.size() + " slice(s)");
        } catch (SIConnectionDroppedException e)
        {
            // No FFDC Code Needed
            // Simply pass this exception on
            throw e;
        } catch (Exception e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".sendChunkedMessage",
                                        CommsConstants.CONNECTIONPROXY_SENDCHUNKEDEXCEPTION_01,
                                        new Object[] { message, this });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to encode message", e);
            throw new SIResourceException(e);
        }

        // Do a quick check on the message size. If the size is less than our threshold for chunking
        // the message then send it as one.
        int msgLen = 0;
        for (DataSlice slice : messageSlices)
            msgLen += slice.getLength();
        if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING)
        {
            // The message is a tiddler, send it in one
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message is smaller than " +
                                      CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
            sendEntireExceptionMessage(address,
                                       message,
                                       reason,
                                       inserts,
                                       tran,
                                       alternateUser);
        }
        else
        {
            // Now we have the data slices, we can start sending the slices in their own message.
            for (int x = 0; x < messageSlices.size(); x++)
            {
                DataSlice slice = messageSlices.get(x);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Sending slice:", slice);

                boolean first = (x == 0);
                boolean last = (x == (messageSlices.size() - 1));
                byte flags = 0;

                // Work out the flags to send
                if (first)
                    flags |= CommsConstants.CHUNKED_MESSAGE_FIRST;
                if (last)
                    flags |= CommsConstants.CHUNKED_MESSAGE_LAST;
                else if (!first)
                    flags |= CommsConstants.CHUNKED_MESSAGE_MIDDLE;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Flags: " + flags);

                if (!first)
                {
                    // This isn't the first slice, grab a fresh buffer
                    request = getCommsByteBuffer();
                }

                request.putShort(getConnectionObjectID());
                // Transaction id
                request.putSITransaction(tran);
                // Flags to indicate first and last slices
                request.put(flags);

                // If this is the first chunk, send other information about this send
                if (first)
                {
                    // Destination
                    request.putSIDestinationAddress(address, getConversation().getHandshakeProperties().getFapLevel());
                    // Reason code
                    request.putInt(reason);
                    // Add alternate user
                    request.putString(alternateUser);
                    // Now add all the String inserts
                    if (inserts == null)
                        inserts = new String[0];
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Adding " + inserts.length + " insert(s)");
                    request.putShort(inserts.length);

                    for (int y = 0; y < inserts.length; y++)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Adding insert [" + y + "]: " + inserts[y]);
                        request.putString(inserts[y]);
                    }
                }

                // Now we can dump the slice into the message
                request.putDataSlice(slice);

                // And send the message
                if (!last)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Sending first / middle chunk");

                    jfapSend(request,
                             JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION,
                             JFapChannelConstants.PRIORITY_MEDIUM,
                             false,
                             ThrottlingPolicy.BLOCK_THREAD);
                }
                else
                {
                    sendData(request,
                             JFapChannelConstants.PRIORITY_MEDIUM,
                             true,
                             tran,
                             JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION,
                             0,
                             JFapChannelConstants.SEG_SEND_CHUNKED_TO_EXCEPTION_DESTINATION_R);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendChunkedExceptionMessage");
    }

    private void sendEntireExceptionMessage(SIDestinationAddress address,
                                            SIBusMessage message,
                                            int reason,
                                            String[] inserts,
                                            SITransaction tran,
                                            String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendEntireExceptionMessage",
                        new Object[] { address, message, reason, inserts, tran, alternateUser });

        CommsByteBuffer request = getCommsByteBuffer();
        request.putShort(getConnectionObjectID());
        // Transaction id
        request.putSITransaction(tran);
        // Destination
        request.putSIDestinationAddress(address, getConversation().getHandshakeProperties().getFapLevel());
        // Reason code
        request.putInt(reason);
        // Add alternate user
        request.putString(alternateUser);
        // Message
        request.putClientMessage(message, getCommsConnection(), getConversation());
        // Now add all the String inserts
        if (inserts == null)
            inserts = new String[0];
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Adding " + inserts.length + " insert(s)");
        request.putShort(inserts.length);

        for (int x = 0; x < inserts.length; x++)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Adding insert [" + x + "]: " + inserts[x]);
            request.putString(inserts[x]);
        }

        sendData(request,
                 JFapChannelConstants.PRIORITY_MEDIUM,
                 true,
                 tran,
                 JFapChannelConstants.SEG_SEND_TO_EXCEPTION_DESTINATION,
                 0,
                 JFapChannelConstants.SEG_SEND_TO_EXCEPTION_DESTINATION_R);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendEntireExceptionMessage");
    }

    // *******************************************************************************************
    // *                               Cloned connection related Methods                         *
    // *******************************************************************************************

    /**
     * Creates a seperate connection to the same ME using the same physical socket.
     * 
     * @return Returns an SICoreConnection to the same messaging engine, over the same physical
     *         socket. But is seperate in every other way.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public SICoreConnection cloneConnection()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cloneConnection");

        checkAlreadyClosed();

        ConnectionProxy cloneConnection = null;

        // See if there is a cached clone connection we can use instead of going to the expense of creating a new clone connection
        synchronized (cachedCloneConnections) {
            if (!cachedCloneConnections.isEmpty()) {
                final Iterator<ConnectionProxy> it = cachedCloneConnections.iterator();
                cloneConnection = it.next();
                cachedCloneConnections.remove(cloneConnection);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Connection allocated from cache=" + cloneConnection);

                cloneConnection.setOpen(); // Re-open the connection (was marked closed when cached)

                // Rest the connections listeners to match those of the parent connection at this time
                cloneConnection.removeAllConnectionListeners();
                cloneConnectionListeners(this, cloneConnection);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Cache is empty");
            }
        }

        // If we didn't get a cached clone connection we have to create a new clone connection
        if (cloneConnection == null) {
            cloneConnection = prepareNewCloneConnection(this);

            // Build clone request and send to server
            CommsByteBuffer request = getCommsByteBuffer();
            request.putShort(getConversation().getId());

            final CommsByteBuffer reply = cloneConnection.jfapExchange(request,
                                                                       JFapChannelConstants.SEG_CREATE_CLONE_CONNECTION,
                                                                       JFapChannelConstants.PRIORITY_MEDIUM,
                                                                       true);

            try
            {
                short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_CLONE_CONNECTION_R);
                if (err != CommsConstants.SI_NO_EXCEPTION)
                {
                    checkFor_SIConnectionUnavailableException(reply, err);
                    checkFor_SIConnectionDroppedException(reply, err);
                    checkFor_SIResourceException(reply, err);
                    checkFor_SIConnectionLostException(reply, err);
                    checkFor_SILimitExceededException(reply, err);
                    checkFor_SIErrorException(reply, err);
                    defaultChecker(reply, err);
                }

                completeNewCloneConnection(cloneConnection, reply);
                cloneConnectionListeners(this, cloneConnection);
            } finally {
                reply.release();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cloneConnection", cloneConnection);
        return cloneConnection;
    }

    // Prepare a new cloned connection - this is for preparation work done before the exchange with the server is performed
    private ConnectionProxy prepareNewCloneConnection(final ConnectionProxy parentConnection) throws SIResourceException, SIConnectionUnavailableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "prepareNewCloneConnection", "parentConnection=" + parentConnection);

        final short fapLevel = getConversation().getHandshakeProperties().getFapLevel();

        // The type of connection proxy we create is dependent on FAP version.  FAP version 5 supports a connection which
        // MSSIXAResourceProvider.  This is done so that PEV (introduced in line with FAP 5) can perform remote recovery
        // of resources.
        ConnectionProxy cloneConnection = null;
        if (fapLevel >= JFapChannelConstants.FAP_VERSION_5) {
            cloneConnection = new MSSIXAResourceProvidingConnectionProxy(getConversation().cloneConversation(new ProxyReceiveListener()), parentConnection); // Note that this is a clone connection by saving parent
        } else {
            cloneConnection = new ConnectionProxy(getConversation().cloneConversation(new ProxyReceiveListener()));
        }

        // Create the connection wide conversation state
        cloneConnection.createConversationState();

        // Ensure we save the right data in the clone
        cloneConnection.setCommsConnection(getCommsConnection());
        cloneConnection.setMeUuid(getMeUuid());
        cloneConnection.setResolvedUserId(getResolvedUserid());
        cloneConnection.setMeName(getMeName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "prepareNewCloneConnection", "rc=" + cloneConnection);
        return cloneConnection;
    }

    // Complete a new cloned connection using reply data from server
    private void completeNewCloneConnection(final ConnectionProxy newConnection, final CommsByteBuffer reply) throws SIConnectionUnavailableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "completeNewCloneConnection", "newConnection=" + newConnection + ", reply=" + reply);

        final short fapLevel = getConversation().getHandshakeProperties().getFapLevel();

        newConnection.setConnectionObjectID(reply.getShort());
        newConnection.setSICoreConnection(newConnection);

        // For JFAP 9 and above the messaging engine will have also returned a Unique Id & an Order Context which
        // we must cache until required
        if (fapLevel >= JFapChannelConstants.FAP_VERSION_9) {
            final byte[] uniqueId = reply.get(reply.getShort());
            newConnection.setInitialUniqueId(uniqueId);

            final short orderContext = reply.getShort();
            newConnection.addOrderContext(Short.valueOf(orderContext));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "completeNewCloneConnection");
    }

    // Add a copy of the original connectionListeners to the new ClonedConnection
    private void cloneConnectionListeners(final ConnectionProxy from, final ConnectionProxy to) throws SIConnectionUnavailableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cloneConnectionListeners", "from=" + from + ", to=" + to);

        final ClientConversationState convState = (ClientConversationState) from.getConversation().getAttachment();
        final SICoreConnectionListener[] listeners = convState.getCatConnectionListeners().getConnectionListeners();

        for (int i = 0; i < listeners.length; i++) {
            to.addConnectionListener(listeners[i]);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cloneConnectionListeners");
    }

    /**
     * Used to compare two SICoreConnection objects to test and see if calling
     * one method on both will have exactly the same effect on the bus.
     * 
     * @param rhs
     * 
     * @return Returns true if they are equivalent
     */
    @Override
    public boolean isEquivalentTo(SICoreConnection rhs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isEquivalentTo");

        boolean equivalant = false;

        // Get ME UUid and compare the conversation id, if they both match, return true
        if ((rhs.getMeUuid().equals(this.getMeUuid()) && ((ConnectionProxy) rhs).getConversation().sharesSameLinkAs(this.getConversation())))
            equivalant = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isEquivalentTo", "" + equivalant);
        return equivalant;
    }

    // *******************************************************************************************
    // *                                   Transaction related Methods                           *
    // *******************************************************************************************

    /**
     * This method will create an uncoordinated transaction. The transaction object
     * returned can be used against cloned connections or connections using the
     * same physical link connected to the same ME.
     * 
     * @return Returns an SIUncoordinatedTransaction object.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createUncoordinatedTransaction");

        final SIUncoordinatedTransaction result = createUncoordinatedTransaction(true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createUncoordinatedTransaction", result);
        return result;
    }

    /**
     * Creates an uncoordinated transaction - optionally supporting subordinate
     * resource enlistement.
     * <p>
     * This overloads a method present in the pre-WAS 6.1, which does not accept
     * a boolean argument. Thus we have to perform some extra leg-work when connecting
     * mixed levels of client / server code. The following table describes the mapping
     * that is done:
     * <table>
     * <tr>
     * <th>Client version</th>
     * <th>Server version</th>
     * <th>Action</th>
     * </tr><tr>
     * <td>&lt; 6.1</td>
     * <td>&lt; 6.1</td>
     * <td>None required. This is already supported by previous levels of the code.</td>
     * </tr><tr>
     * <td>&gt;= 6.1</td>
     * <td>&lt; 6.1</td>
     * <td>Client detects the server is at a previous level and formats the JFAP flow
     * as a pre 6.1 create uncoordinated transaction flow. This means that we effectively
     * ignore the value of the "allowSubordinateResources" flag</td>
     * </tr><tr>
     * <td>&lt; 6.1</td>
     * <td>&gt;= 6.1</td>
     * <td>Server detects that the client is at a back level and expects the pre 6.1 version
     * of the JFAP flow. This is mapped to creating a transaction with subordinates (i.e.
     * a call to createUncoordinateTransaction (with no argument) to the server side Core SPI).</td>
     * </tr><tr>
     * <td>&gt;= 6.1</td>
     * <td>&gt;= 6.1</td>
     * <td>Client detects server is capabile of supporting a new varient on the create uncoordinated
     * transaction flow. Likewise, the server detects the level of the client and expects the new
     * format of flow. The new format create uncoordinate transaction flow adds a byte (BIT8) flags
     * field which is used to propagate the value of the argument supplied.</td>
     * </tr>
     * </table>
     * 
     * @see SICoreConnection#createUncoordinatedTransaction(boolean)
     * @param allowSubordinateResources if a value of true is specified then the
     *            transaction returned is allowed to support subordinate enlistment.
     * @return an SIUncoordinatedTransaction
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SILimitExceededException
     * @throws SIIncorrectCallException
     */
    @Override
    public SIUncoordinatedTransaction createUncoordinatedTransaction(boolean allowSubordinateResources)
                    throws SIConnectionUnavailableException,
                    SIConnectionDroppedException,
                    SIResourceException,
                    SIConnectionLostException,
                    SILimitExceededException,
                    SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createUncoordinatedTransaction",
                        "" + allowSubordinateResources);

        LocalTransactionProxy localTranProxy = null;

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();

                try
                {
                    checkAlreadyClosed();

                    if (requiresOptimizedTransactions)
                    {
                        // Use "new" optimized transactions
                        localTranProxy = new OptimizedUncoordinatedTransactionProxy(getConversation(),
                                        this,
                                        allowSubordinateResources);
                    }
                    else
                    {
                        // Use "traditional" non-optimized transactions

                        // Create the instance of the client local transaction object
                        localTranProxy = new LocalTransactionProxy(getConversation(), this);

                        // Now we can get the ID
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Created the transaction proxy. ID: " + localTranProxy.getTransactionId());

                        CommsByteBuffer request = getCommsByteBuffer();

                        // Build Message
                        request.putShort(getConnectionObjectID());
                        request.putInt(localTranProxy.getTransactionId());

                        // Determine if the server supports the propagation of the
                        // "allowSubordinateResources" flag.  This capability was
                        // introduced at FAP version 5.
                        if (getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_5)
                        {
                            request.put(allowSubordinateResources ? (byte) 0x01 : (byte) 0x00);
                        }

                        // Pass on call to server
                        jfapSend(request,
                                 JFapChannelConstants.SEG_CREATE_UCTRANSACTION,
                                 JFapChannelConstants.PRIORITY_HIGH,
                                 true,
                                 ThrottlingPolicy.BLOCK_THREAD);
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createUncoordinatedTransaction", localTranProxy);
        return localTranProxy;
    }

    /**
     * This method proxies the identical call on the actual SICoreConnection.
     * In XA terms this is an XAOPEN, and this is the segment that will be flowed
     * across the wire.
     * 
     * @return Returned will be a proxy version of the SIXAResource that will trap
     *         the calls and flow them across the wire to the awaiting ME.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public SIXAResource getSIXAResource()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSIXAResource");
        SIXAResource xaResource = _internalGetSIXAResource(false);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSIXAResource", xaResource);
        return xaResource;
    }

    /**
     * This method is used to allow the MSSIXAResourceProxy to create an XAResource with the special
     * 'requiresMSResource' flag set into it. This method will create a SuspendableXAResource
     * wrapper which will have the appropriate transaction instance (either optimized or basic)
     * embedded into it.
     * 
     * @param requiresMSResource
     * 
     * @return Returns an instance of SuspendableXAResource that wraps the real transaction instance.
     * 
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SIErrorException
     */
    SIXAResource _internalGetSIXAResource(boolean requiresMSResource)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_internalGetSIXAResource",
                        requiresMSResource);

        checkAlreadyClosed();

        BaseSIXAResourceProxy xaResource = _createXAResource(requiresMSResource);
        SIXAResource result = new SuspendableXAResource(getConversation(), this, xaResource, requiresMSResource);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_internalGetSIXAResource", result);
        return result;
    }

    /**
     * This method actually created the underlying XAResource. This is needed so that when a new
     * XAResource is needed by the SuspendableXAResource class once can be created easily.
     * 
     * @param requiresMSResource
     * 
     * @return Returns either an OptimizedSIXAResourceProxy instance or an SIXAResourceProxy
     *         instance depending on the FAP level / client properties.
     * 
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     * @throws SIResourceException
     * @throws SIConnectionLostException
     * @throws SIErrorException
     */
    BaseSIXAResourceProxy _createXAResource(boolean requiresMSResource)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_createXAResource", requiresMSResource);

        BaseSIXAResourceProxy result = null;

        if (requiresOptimizedTransactions)
        {
            // Peer requires the use of optimized transactions.
            result = new OptimizedSIXAResourceProxy(getConversation(),
                            this,
                            requiresMSResource);
        }
        else
        {
            // Fall back to the old style transaction instnace
            result = new SIXAResourceProxy(getConversation(),
                            this,
                            requiresMSResource);

            CommsByteBuffer request = getCommsByteBuffer();
            request.putShort(getConnectionObjectID());
            request.putInt(result.getTransactionId());

            CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_XAOPEN,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

            try
            {
                short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_XAOPEN_R);
                if (err != CommsConstants.SI_NO_EXCEPTION)
                {
                    checkFor_SIConnectionUnavailableException(reply, err);
                    checkFor_SIConnectionDroppedException(reply, err);
                    checkFor_SIResourceException(reply, err);
                    checkFor_SIConnectionLostException(reply, err);
                    checkFor_SIErrorException(reply, err);
                    defaultChecker(reply, err);
                }
            } finally
            {
                reply.release();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_createXAResource", result);
        return result;
    }

    // *******************************************************************************************
    // *                                   Connection listener Methods                           *
    // *******************************************************************************************

    /**
     * Used to register a ConnectionListener.
     * 
     * @param listener
     * 
     * @throws SIConnectionUnavailableException
     * @throws SIConnectionDroppedException
     */
    @Override
    public void addConnectionListener(SICoreConnectionListener listener)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addConnectionListener", listener);

        checkAlreadyClosed();

        ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
        CatConnectionListenerGroup catConnectionListeners = state.getCatConnectionListeners();
        catConnectionListeners.add(listener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addConnectionListener");
    }

    /**
     * Used to unregister a connection listener
     * 
     * @param listener
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     */
    @Override
    public void removeConnectionListener(SICoreConnectionListener listener)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeConnectionListener", listener);

        ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
        CatConnectionListenerGroup catConnectionListeners = state.getCatConnectionListeners();
        catConnectionListeners.remove(listener);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeConnectionListener");
    }

    /**
     * @return Returns an array of all the connection listeners currently registered
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     */
    @Override
    public SICoreConnectionListener[] getConnectionListeners()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectionListeners");

        checkAlreadyClosed();

        ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
        CatConnectionListenerGroup catConnectionListeners = state.getCatConnectionListeners();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnectionListeners");
        return catConnectionListeners.getConnectionListeners();
    }

    /**
     * This method will remove all the connection listeners on this connection. This is normally
     * done as part of closing the connection.
     */
    private void removeAllConnectionListeners()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeAllConnectionListeners");

        ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
        CatConnectionListenerGroup catConnectionListeners = state.getCatConnectionListeners();
        catConnectionListeners.removeAllElements();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeAllConnectionListeners");
    }

    // *******************************************************************************************
    // *                               Connection / API inquiry methods                          *
    // *******************************************************************************************

    /**
     * Returns a string representing the API level supported by the messaging engine
     * to which connection has been made. This has the format
     * "<Major version>.<Minor version>"
     * 
     * @return the highest API level supported on this connection
     */
    @Override
    public String getApiLevelDescription()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getApiLevelDescription");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getApiLevelDescription");

        return getApiMajorVersion() + "." + getApiMinorVersion();
    }

    /**
     * Returns a value representing the API level which should be used in conjunction with
     * the value returned from getMajorVersion. For example, in WAS 6.0,
     * getApiMajorVersion will return 6, and getApiMinorVersion will return 0.
     * <p>
     * The version returned may not be the version of the ME. What will be
     * returned is the API level that is supported - so this will be the lowest
     * level of the client and the ME to which we are connected
     * 
     * @see ConnectionProxy#getApiMinorVersion()
     * 
     * @return the highest API level supported on this connection
     */
    @Override
    public long getApiMajorVersion()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getApiMajorVersion");

        final HandshakeProperties handshake = getConversation().getHandshakeProperties();

        long reportedApiMajor = SIMPConstants.API_MAJOR_VERSION;

        // The server is at a lower level than us
        // so use that version to report
        if (handshake.getMajorVersion() < SIMPConstants.API_MAJOR_VERSION)
        {
            reportedApiMajor = handshake.getMajorVersion();
        }
        // Otherwise we use the client version

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getApiMajorVersion", "" + reportedApiMajor);
        return reportedApiMajor;
    }

    /**
     * Returns a value representing the API level which should be used in conjunction with
     * the value returned from getMajorVersion. For example, in WAS 6.0,
     * getApiMajorVersion will return 6, and getApiMinorVersion will return 0.
     * <p>
     * The version returned may not be the version of the ME. What will be
     * returned is the API level that is supported - so this will be the lowest
     * level of the client and the ME to which we are connected
     * 
     * @see ConnectionProxy#getApiMajorVersion()
     * 
     * @return the highest API level supported on this connection
     */
    @Override
    public long getApiMinorVersion()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getApiMinorVersion");

        final HandshakeProperties handshake = getConversation().getHandshakeProperties();

        long reportedApiMinor = 0;

        // The server is a lower major version, so return the server minor version
        if (handshake.getMajorVersion() < SIMPConstants.API_MAJOR_VERSION)
        {
            reportedApiMinor = handshake.getMinorVersion();
        }
        // The server is greater major version, so return the client minor version
        else if (handshake.getMajorVersion() > SIMPConstants.API_MAJOR_VERSION)
        {
            reportedApiMinor = SIMPConstants.API_MINOR_VERSION;
        }
        // Otherwise the major version is the same
        else
        {
            // So just compare the minor versions and return the lowest
            if (handshake.getMinorVersion() < SIMPConstants.API_MINOR_VERSION)
            {
                reportedApiMinor = handshake.getMinorVersion();
            }
            else
            {
                reportedApiMinor = SIMPConstants.API_MINOR_VERSION;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getApiMinorVersion", "" + reportedApiMinor);
        return reportedApiMinor;
    }

    /**
     * Returns the name of the messaging engine we are connected to. This value
     * is retrieved as soon as we connct to the remote ME.
     * 
     * @return Returns the ME name.
     */
    @Override
    public String getMeName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMeName");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMeName", meName);
        return meName;
    }

    /**
     * @return Returns the Uuid of the ME on this connection.
     */
    @Override
    public String getMeUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMeUuid");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMeUuid", meUuid);
        return meUuid;
    }

    // *******************************************************************************************
    // *                               Other public Methods                                      *
    // *******************************************************************************************

    /**
     * Performs the checks on the destination addresses and user Ids to ensure that any attempt to
     * do messaging will succeed. This is used by Web Services primarily and from a comms perspective
     * no function is needed except to remote the flow.
     * <p>
     * NOTE: This method is only available when talking to servers that support JFAP2
     * 
     * @param requestDestAddr Destination the request message would be sent to
     * @param replyDestAddr Destination the reply message would be sent to
     * @param destinationType Type of destination required for the request
     *            destination
     * @param alternateUser User that access checks are performed against
     *            (if null the user associated with the connection
     *            is used)
     * 
     * @return Returns null if messaging is required. The SIDestinationAddress of
     *         the resolved request destination if messaging is not required
     * 
     * @throws SIConnectionDroppedException,
     * @throws SIConnectionUnavailableException,
     * @throws SIErrorException,
     * @throws SIIncorrectCallException,
     * @throws SITemporaryDestinationNotFoundException,
     * @throws SIResourceException,
     * @throws SINotAuthorizedException,
     * @throws SINotPossibleInCurrentConfigurationException;
     */
    @Override
    public SIDestinationAddress checkMessagingRequired(SIDestinationAddress requestDestAddr,
                                                       SIDestinationAddress replyDestAddr,
                                                       DestinationType destinationType,
                                                       String alternateUser)
                    throws SIConnectionDroppedException, SIConnectionUnavailableException,
                    SIIncorrectCallException, SITemporaryDestinationNotFoundException, SIResourceException,
                    SINotAuthorizedException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "checkMessagingRequired",
                        new Object[]
                        {
                         requestDestAddr,
                         replyDestAddr,
                         destinationType,
                         alternateUser
                        });

        SIDestinationAddress resolvedRequestDestination = null;

        try
        {
            // Ensure no one closes the connection while we are doing this
            closeLock.readLock().lockInterruptibly();

            try
            {
                checkAlreadyClosed();

                // Now check the fap level - we should only perform this method if we are V2 or higher
                final HandshakeProperties props = getConversation().getHandshakeProperties();
                CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_2);

                CommsByteBuffer request = getCommsByteBuffer();

                // Connection Object Ref
                request.putShort(getConnectionObjectID());

                // Add destination info
                request.putSIDestinationAddress(requestDestAddr, getConversation().getHandshakeProperties().getFapLevel());

                // Add the selection criteria
                request.putSIDestinationAddress(replyDestAddr, getConversation().getHandshakeProperties().getFapLevel());

                // Now put the destination type
                if (destinationType == null)
                {
                    request.putShort(CommsConstants.NO_DEST_TYPE);
                }
                else
                {
                    request.putShort(destinationType.toInt());
                }

                // Add the alternate user id
                request.putString(alternateUser);

                // Pass on call to server
                CommsByteBuffer reply = jfapExchange(request,
                                                     JFapChannelConstants.SEG_CHECK_MESSAGING_REQUIRED,
                                                     JFapChannelConstants.PRIORITY_MEDIUM,
                                                     true);

                try
                {
                    short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CHECK_MESSAGING_REQUIRED_R);
                    if (err != CommsConstants.SI_NO_EXCEPTION)
                    {
                        checkFor_SIConnectionUnavailableException(reply, err);
                        checkFor_SIConnectionDroppedException(reply, err);
                        checkFor_SIResourceException(reply, err);
                        checkFor_SINotAuthorizedException(reply, err);
                        checkFor_SIIncorrectCallException(reply, err);
                        checkFor_SITemporaryDestinationNotFoundException(reply, err);
                        checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                        checkFor_SIErrorException(reply, err);
                        defaultChecker(reply, err);
                    }

                    // Was there any data sent back?
                    if (reply.hasRemaining())
                    {
                        resolvedRequestDestination = reply.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel());
                    }

                    // If there was no data sent back then we should return null also
                } finally
                {
                    reply.release();
                }
            } finally
            {
                closeLock.readLock().unlock();
            }
        } catch (InterruptedException e)
        {
            // No FFDC Code Needed
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "checkMessagingRequired", resolvedRequestDestination);
        return resolvedRequestDestination;
    }

    /**
     * @return Returns an ordering context that can be used on calls to ensure ordering integrity.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public OrderingContext createOrderingContext()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createOrderingContext");

        checkAlreadyClosed();

        OrderingContextProxy oc = null;

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();

                try
                {
                    checkAlreadyClosed();

                    // Create the ordering context instance.
                    oc = new OrderingContextProxy(getConversation(), this);
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createOrderingContext", oc);
        return oc;
    }

    /**
     * @return Returns the resolved user ID.
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public String getResolvedUserid()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        return resolvedUserId;
    }

    /**
     * To allow client applications to generate their own message Id's this call can
     * provide a 16 bit unique stem. The client can then generate Id's using this stem
     * only needing to ask the ME for more Id's when it has used up all the possible numbers
     * in it's unique stem.
     * <p>
     * In practise, most JMS apps will call this at least once per connection and so
     * the unique stem will be collected at connect time. Therefore we only need a line
     * turnaround when that first stem has been used up.
     * 
     * @return byte[] 16bit unique identifier
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public byte[] createUniqueId()
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createUniqueId");

        checkAlreadyClosed();

        byte[] uniqueId = null;

        // First check and see if we have one cached from connection time
        if (initialUniqueId != null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Using cached initialUniqueId");

            uniqueId = initialUniqueId;
            initialUniqueId = null; // Destroy this now, so subsequent attempts mean a trip to the server
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No cached initialUniqueId so requesting a new one");

            CommsByteBuffer request = getCommsByteBuffer();
            request.putShort(getConnectionObjectID());

            CommsByteBuffer reply = jfapExchange(request,
                                                 JFapChannelConstants.SEG_GET_UNIQUE_ID,
                                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                                 true);

            try
            {
                short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_GET_UNIQUE_ID_R);
                if (err != CommsConstants.SI_NO_EXCEPTION)
                {
                    checkFor_SIConnectionUnavailableException(reply, err);
                    checkFor_SIConnectionDroppedException(reply, err);
                    checkFor_SIResourceException(reply, err);
                    checkFor_SIConnectionLostException(reply, err);
                    checkFor_SIErrorException(reply, err);
                    defaultChecker(reply, err);
                }

                // Now get the unique id
                short uniqueIdLength = reply.getShort();
                uniqueId = reply.get(uniqueIdLength);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.bytes(this, tc, uniqueId, 0, uniqueId.length, "ID:");
            } finally
            {
                reply.release();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createUniqueId", uniqueId);
        return uniqueId;
    }

    /**
     * This method will simply proxy the command invocation up to the server and call the identical
     * method there.
     * 
     * @param key
     * @param commandName
     * @param commandData
     * 
     * @return Returns any object that was returned to the server side call.
     * 
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     * @throws SIConnectionDroppedException
     * @throws SIConnectionUnavailableException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIErrorException
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData)
                    throws SINotAuthorizedException, SICommandInvocationFailedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException,
                    SIIncorrectCallException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "invokeCommand",
                        new Object[] { key, commandName, commandData });

        // Now check the fap level - we should only perform this method if we are V5 or higher
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_5);

        Serializable returnObject = _invokeCommand(key, commandName, commandData, null, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "invokeCommand", returnObject);
        return returnObject;
    }

    /**
     * This method will simply proxy the command invocation up to the server and call the identical
     * method there.
     * 
     * @param key
     * @param commandName
     * @param commandData
     * @param tran
     * 
     * @return Returns any object that was returned to the server side call.
     * 
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     * @throws SIConnectionDroppedException
     * @throws SIConnectionUnavailableException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     * @throws SIErrorException
     */
    @Override
    public Serializable invokeCommand(String key, String commandName, Serializable commandData,
                                      SITransaction tran)
                    throws SINotAuthorizedException, SICommandInvocationFailedException,
                    SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException,
                    SIIncorrectCallException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "invokeCommand",
                        new Object[] { key, commandName, commandData, tran });

        // Now check the fap level - we should only perform this method if we are V6 or higher
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_6);

        Serializable returnObject = _invokeCommand(key, commandName, commandData, tran, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "invokeCommand", returnObject);
        return returnObject;
    }

    /**
     * This method actually does the proxying of the command invocation up to the server and calls
     * the identical method there.
     * <p>
     * Note that we have to have a flag indicating which flavour of the original invokeCommand()
     * method was called as different handlers are registered for the transacted flavour of callback
     * and the non-transacted. As such, we need to distinguish the case where a transacted callback
     * is being used with a null transaction and ensure that the right callback is driven.
     * 
     * @param key
     * @param commandName
     * @param commandData
     * @param transaction
     * @param callTransactedCallback
     * 
     * @return Returns any object that was returned to the server side call.
     * 
     * @throws SINotAuthorizedException
     * @throws SICommandInvocationFailedException
     * @throws SIConnectionDroppedException
     * @throws SIConnectionUnavailableException
     * @throws SIResourceException
     * @throws SIIncorrectCallException
     */
    private Serializable _invokeCommand(String key, String commandName, Serializable commandData,
                                        SITransaction transaction, boolean callTransactedCallback)
                    throws SIConnectionDroppedException, SIConnectionUnavailableException, SIResourceException,
                    SIIncorrectCallException, SINotAuthorizedException, SICommandInvocationFailedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_invokeCommand",
                        new Object[] { key, commandName, commandData, transaction, callTransactedCallback });

        Serializable returnObject = null;
        ObjectInputStream ois = null;
        byte[] commandDataBytes = null;

        try
        {
            // Now we need to serialize the object we were given so that we can calculate it's length
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(commandData);
            commandDataBytes = baos.toByteArray();
        } catch (IOException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + "._invokeCommand",
                                        CommsConstants.CONNECTIONPROXY_INVOKECMD_01,
                                        new Object[] { key, commandName, commandData, transaction, this });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Failed to serialize command data", e);

            throw new SICommandInvocationFailedException(
                            nls.getFormattedMessage("FAILED_TO_SERIALIZE_COMMAND_SICO1062",
                                                    null,
                                                    "FAILED_TO_SERIALIZE_COMMAND_SICO1062"),
                            e);
        }

        CommsByteBuffer request = getCommsByteBuffer();

        // And add the data parts
        request.putShort(getConnectionObjectID());
        // Only write the transaction if the transacted invokeCommand() was called (even if
        // the transaction is null)
        if (callTransactedCallback)
            request.putSITransaction(transaction);
        request.putString(key);
        request.putString(commandName);
        request.putInt(commandDataBytes.length);
        request.put(commandDataBytes);

        CommsByteBuffer reply = null;

        if (!callTransactedCallback)
        {
            reply = jfapExchange(request, JFapChannelConstants.SEG_INVOKE_COMMAND,
                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                 true);
        }
        else
        {
            reply = jfapExchange(request, JFapChannelConstants.SEG_INVOKE_COMMAND_WITH_TX,
                                 JFapChannelConstants.PRIORITY_MEDIUM,
                                 true);
        }

        try
        {
            short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_INVOKE_COMMAND_R);
            if (err != CommsConstants.SI_NO_EXCEPTION)
            {
                checkFor_SIConnectionDroppedException(reply, err);
                checkFor_SIConnectionUnavailableException(reply, err);
                checkFor_SIIncorrectCallException(reply, err);
                checkFor_SIResourceException(reply, err);
                checkFor_SINotAuthorizedException(reply, err);
                checkFor_SICommandInvocationFailedException(reply, err);
                checkFor_SIErrorException(reply, err);
                defaultChecker(reply, err);
            }

            // Now get the data to return - If there is any data to get then get it.
            // Otherwise we will return null.
            if (reply.hasRemaining())
            {
                // First get the data length
                int dataLength = reply.getInt();
                byte[] retData = reply.get(dataLength);

                try
                {
                    // Now convert the data back into a Java object
                    ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
                    {
                        @Override
                        public ClassLoader run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    });
                    ois = new DeserializationObjectInputStream(new ByteArrayInputStream(retData), cl);
                    returnObject = (Serializable) ois.readObject();
                } catch (Exception e)
                {
                    FFDCFilter.processException(e, CLASS_NAME + "._invokeCommand",
                                                CommsConstants.CONNECTIONPROXY_INVOKECMD_02,
                                                new Object[] { key, commandName, commandData, transaction, this });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Failed to serialize command data", e);

                    throw new SICommandInvocationFailedException(
                                    nls.getFormattedMessage("FAILED_TO_DESERIALIZE_COMMAND_SICO1063",
                                                            null,
                                                            "FAILED_TO_DESERIALIZE_COMMAND_SICO1063"),
                                    e);
                }
            }
        } finally
        {
            reply.release();

            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException ex) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception closing the ObjectInputStream", ex);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_invokeCommand", returnObject);
        return returnObject;
    }

    /**
     * Proxy for the identically named method on the server. If FAP9+ and this is a cloned connection and if there is
     * space in the parents cache request a reset of this connection and then save the connection in the parents cache.
     * <p>
     * <quote>
     * Closes the connection (and any Producer/Consumer objects created from it). Any subsequent attempt to call
     * methods on the SICoreConnection will result in an SIInvalidStateForOperationException. Calling close on an
     * already-closed SICoreConnection has no effect. When an SICoreConnection is closed, any in-doubt transactions
     * created from the connection are rolled-back.
     * </quote>
     * 
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public void close() throws SIResourceException, SIConnectionLostException, SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");
        _close(false, true); // Allow a reset
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /* PM39926-Start */
    /**
     * Proxy for the identically named method on the server. If FAP9+ and this is a cloned connection and if there is
     * space in the parents cache request a reset of this connection and then save the connection in the parents cache.
     * <p>
     * <quote>
     * Closes the connection (and any Producer/Consumer objects created from it). Any subsequent attempt to call
     * methods on the SICoreConnection will result in an SIInvalidStateForOperationException. Calling close on an
     * already-closed SICoreConnection has no effect. When an SICoreConnection is closed, any in-doubt transactions
     * created from the connection are rolled-back.
     * </quote>
     * 
     * @param bForceFlag - Flag to indicate that connections have to be closed and cannot be reset.
     *            If marked reset the connection would not be released instead will be reused.
     * 
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     */
    @Override
    public void close(boolean bForceFlag) throws SIResourceException, SIConnectionLostException, SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close", bForceFlag);
        _close(bForceFlag, true);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close", bForceFlag);
    }

    /* PM39926-End */

    /**
     * Internal close method. We must handle a failure to send the SEG_CLOSE_CONNECTION to the messaging engine as a force close
     * on the connection since an inability to communicate probably means the connection has failed.
     * 
     * @param force true means a closed is forced, false means a reset is possible.
     * @param useCloseLock true means make use of close lock, false means don't. Should always be true unless recursively calling _close.
     */
    private void _close(final boolean force, final boolean useCloseLock) throws SIResourceException, SIConnectionLostException, SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_close", new Object[] { Boolean.valueOf(force), Boolean.valueOf(useCloseLock) });

        try {
            if (useCloseLock)
                closeLock.writeLock().lockInterruptibly();
            try {
                if (!isClosed()) {
                    // Close cached clone connections (even though this connection may itself end up cached in its parent) this action helps prevent too much
                    // hierachical caching building up in the client.
                    synchronized (cachedCloneConnections) {
                        final Iterator<ConnectionProxy> it = cachedCloneConnections.iterator();
                        while (it.hasNext()) {
                            final ConnectionProxy cachedConnection = it.next();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Closing cached connection=" + cachedConnection);
                            cachedConnection.setOpen(); // Mark the connection open (otherwise the following close is ignored)
                            cachedConnection._close(true, true); // Force a close (not a reset)
                        }
                    }

                    // If we own a group of proxy queues, notify it that we have closed
                    final ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
                    final ProxyQueueConversationGroup pqcg = state.getProxyQueueConversationGroup();

                    if (pqcg != null) {
                        pqcg.closeNotification();
                    }

                    // Build close message request
                    final CommsByteBuffer request = getCommsByteBuffer();
                    request.putShort(getConnectionObjectID());

                    // If FAP9+ determine whether we should request a connection reset or close
                    final boolean fap9OrAbove = getConversation().getHandshakeProperties().getFapLevel() >= JFapChannelConstants.FAP_VERSION_9;
                    if (fap9OrAbove) {
                        if (!force) {
                            if (parent != null) {
                                if (!parent.isClosed()) {
                                    synchronized (parent.cachedCloneConnections)
                                    {
                                        if (parent.cachedCloneConnections.size() <= MAX_CACHED_CLOSE_CONNECTIONS) {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                SibTr.debug(this, tc, "Requesting that this connection be reset");
                                            request.put((byte) 1);
                                        } else {
                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                SibTr.debug(this, tc, "Parent has reached maxmimum number of cached clone connection so not resetting this connection");
                                            request.put((byte) 0);
                                        }
                                    }
                                } else {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(this, tc, "Parent connection is closed - closing this connection");
                                    request.put((byte) 0);
                                }
                            } else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(this, tc, "This connection has no parent - this is not a clone connection - closing this connection");
                                request.put((byte) 0);
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Connection is being forced closed");
                            request.put((byte) 0);
                        }
                    }

                    // Pass on call to server - catch any exception here and treat it as a force close
                    CommsByteBuffer reply = null;
                    try {
                        reply = jfapExchange(request, JFapChannelConstants.SEG_CLOSE_CONNECTION, JFapChannelConstants.PRIORITY_LOWEST, true);
                    } catch (SIConnectionDroppedException e) {
                        // No FFDC code needed - connection failures will have already been recorded
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Exchange with ME failed");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.exception(this, tc, e);
                    } catch (SIConnectionLostException e) {
                        // No FFDC code needed - connection failures will have already been recorded
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Exchange with ME failed");
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.exception(this, tc, e);
                    }

                    // Was a connection reset or closed by server
                    boolean connectionReset = false;

                    try {
                        if (reply != null) {
                            final short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CLOSE_CONNECTION_R);
                            if (err != CommsConstants.SI_NO_EXCEPTION) {
                                checkFor_SIResourceException(reply, err);
                                checkFor_SIConnectionLostException(reply, err);
                                checkFor_SIConnectionUnavailableException(reply, err);
                                checkFor_SIConnectionDroppedException(reply, err);
                                checkFor_SIErrorException(reply, err);
                                defaultChecker(reply, err);
                            }

                            if (fap9OrAbove) {
                                connectionReset = (reply.get() == (byte) 1);
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Failed to receive a reply from the ME");
                        }

                        if (connectionReset) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Connection was reset");
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Connection was closed");
                        }

                        setClosed(); // Mark connection as closed (even if reset as we will re-open when the connection is used again)

                        // PK97427 We must invalidate our list of ordering contexts, as the ME will have
                        // cleared their list of objects while processing our close connection request
                        orderContextPool.clear();

                        removeAllConnectionListeners();

                        // Now close the conversation - if we are connected to a FAP 9 or above server then we can close
                        // the conversation quickly and avoid further additional costly exchanges since we know that the
                        // other end will do the same.
                        if (!connectionReset) {
                            if (fap9OrAbove) {
                                getConversation().fastClose();
                            } else {
                                getConversation().close();
                            }
                        }

                        if (pqcg != null) {
                            pqcg.close();

                            //Null out ProxyQueueConversationGroup to ensure we don't get issues if it is reused on a reset connection.
                            state.setProxyQueueConversationGroup(null);
                        }

                        if (connectionReset) { // Connection can never be reset when reply is null
                            completeNewCloneConnection(this, reply);

                            // Cache the new clone in the parent connection ready to be used on the next create clone call to the parent
                            //We want to prevent the build up of clones, so double check to see if we now have too many elements in the cache.
                            //If there are too many elements, close the connection.
                            boolean cacheFull = true;

                            synchronized (parent.cachedCloneConnections)
                            {
                                if (parent.cachedCloneConnections.size() <= MAX_CACHED_CLOSE_CONNECTIONS)
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(this, tc, "Size of cachedCloneConnections: " + cachedCloneConnections.size());
                                    parent.cachedCloneConnections.add(this);
                                    cacheFull = false;
                                }
                            }

                            if (cacheFull)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(this, tc, "Cache is full, forcing close.");

                                //We are currently marked as closed, need to reopen in order for close to work.
                                setOpen();
                                //Don't use close lock to prevent self deadlock.
                                _close(true, false);
                            }
                        }
                    } finally {
                        if (reply != null) {
                            reply.release();
                        }
                    }
                }
            } finally {
                if (useCloseLock)
                    closeLock.writeLock().unlock();
            }
        } catch (InterruptedException e) {
            // No FFDC code needed
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_close");
    }

    // *******************************************************************************************
    // *                                  Session send / receive Methods                         *
    // *******************************************************************************************

    /**
     * Provides a send straight on the specified destination.
     * 
     * @param msg
     * @param tran
     * @param destAddress
     * @param destType
     * @param orderingContext
     * @param alternateUser
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    @Override
    public void send(SIBusMessage msg, SITransaction tran, SIDestinationAddress destAddress,
                     DestinationType destType, OrderingContext orderingContext,
                     String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "send",
                        new Object[]
                        {
                         msg,
                         tran,
                         destAddress,
                         destType,
                         orderingContext,
                         alternateUser
                        });

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();

                try
                {
                    checkAlreadyClosed();

                    // Increment the use count on the ordering context if we have one
                    if (orderingContext != null)
                    {
                        ((OrderingContextProxy) orderingContext).incrementUseCount();
                    }

                    // Now we need to synchronise on the transaction object if there is one.
                    if (tran != null)
                    {
                        synchronized (tran)
                        {
                            // Check transaction is in a valid state.
                            // Enlisted for an XA UOW and not rolledback or
                            // completed for a local transaction.
                            if (!((Transaction) tran).isValid())
                            {
                                throw new SIIncorrectCallException(
                                                nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1067", null, null));
                            }

                            _send(msg, tran, destAddress, destType, orderingContext, alternateUser);
                        }
                    }
                    else
                    {
                        _send(msg, null, destAddress, destType, orderingContext, alternateUser);
                    }
                } finally
                {
                    closeLock.readLock().unlock();

                    // Now decrement the use count on the ordering context if we have one
                    if (orderingContext != null)
                    {
                        ((OrderingContextProxy) orderingContext).decrementUseCount();
                    }
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "send");
    }

    /**
     * This private version of send method completes the send call but does not obtain
     * any locks or do any checking of the parameters. It should be called by a method which does
     * do this checking.
     * 
     * @param msg
     * @param tran
     * @param destAddress
     * @param destType
     * @param orderContext
     * @param alternateUser
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    private void _send(SIBusMessage msg, SITransaction tran,
                       SIDestinationAddress destAddress, DestinationType destType,
                       OrderingContext orderContext, String alternateUser) // f200337  // F204529.3, F219476.2
    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_send");

        boolean sendSuccessful = false;
        // Get the message priority
        short jfapPriority = JFapChannelConstants.getJFAPPriority(msg.getPriority()); // f174317
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Sending with JFAP priority of " + jfapPriority); // f174317

        // *** Complex logic to determine if we can get away we need a reply to ***
        // *** this send operation.                                             ***
        final boolean requireReply;
        if (tran != null && !exchangeTransactedSends)
        {
            // If there is a transaction, and we haven't been explicitly told to exchange
            // transacted sends - then a reply is NOT required.
            requireReply = false;
        }
        else if (exchangeExpressSends)
        {
            // We have been prohibited from sending (rather than exchanging) low
            // qualities of service - thus there is no way that we can avoid requiring
            // a reply.
            requireReply = true;
        }
        else
        {
            // We CAN perform the optimization where low qualities of service can be sent
            // without requiring a reply.  Check the message quality of service.
            requireReply = (msg.getReliability() != Reliability.BEST_EFFORT_NONPERSISTENT) &&
                           (msg.getReliability() != Reliability.EXPRESS_NONPERSISTENT);
        }
        // *** end of "is a reply required" logic ***

        // If we are at FAP9 or above we can do a 'chunked' send of the message in seperate
        // slices to make life easier on the Java memory manager
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        if (props.getFapLevel() >= JFapChannelConstants.FAP_VERSION_9)
        {
            sendChunkedMessage(msg,
                               tran,
                               orderContext,
                               destType,
                               destAddress,
                               alternateUser,
                               jfapPriority,
                               requireReply);

        }
        else
        {
            sendEntireMessage(msg,
                              null,
                              tran,
                              orderContext,
                              destType,
                              destAddress,
                              alternateUser,
                              jfapPriority,
                              requireReply);
        }

        sendSuccessful = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_send");
    }

    /**
     * This method performs an SICoreConnection.send() call and sends the message in one big JFap
     * message. This requires the allocation of one big area of storage for the whole message.
     * 
     * @param msg
     * @param messageSlices
     * @param tran
     * @param orderContext
     * @param destType
     * @param destAddress
     * @param alternateUser
     * @param jfapPriority
     * @param requireReply
     * 
     * @throws SIResourceException
     * @throws SIConnectionUnavailableException
     * @throws SIIncorrectCallException
     * @throws SITemporaryDestinationNotFoundException
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void sendEntireMessage(SIBusMessage msg, List<DataSlice> messageSlices,
                                   SITransaction tran, OrderingContext orderContext,
                                   DestinationType destType, SIDestinationAddress destAddress,
                                   String alternateUser, short jfapPriority, boolean requireReply)
                    throws SIResourceException, SIConnectionUnavailableException, SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendEntireMessage",
                        new Object[] { msg, messageSlices, tran, orderContext, destType, destAddress, alternateUser, jfapPriority, requireReply });

        CommsByteBuffer request = getCommsByteBuffer();
        request.putShort(getConnectionObjectID());
        // Optionaly add the message order context id
        if (orderContext != null)
            request.putShort(((OrderingContextProxy) orderContext).getId());
        else
            request.putShort(CommsConstants.NO_ORDER_CONTEXT);
        // Optionaly add a Transaction
        request.putSITransaction(tran);
        // Add Destination Type
        if (destType != null)
            request.putShort(destType.toInt());
        else
            request.putShort(CommsConstants.NO_DEST_TYPE);
        // Add alternate user
        request.putString(alternateUser);
        // Add the SIDestinationAddress
        request.putSIDestinationAddress(destAddress, getConversation().getHandshakeProperties().getFapLevel());
        // Add message
        if (messageSlices == null)
        {
            request.putClientMessage(msg, getCommsConnection(), getConversation());
        }
        else
        {
            request.putMessgeWithoutEncode(messageSlices);
        }

        sendData(request,
                 jfapPriority,
                 requireReply,
                 tran,
                 JFapChannelConstants.SEG_SEND_CONN_MSG,
                 JFapChannelConstants.SEG_SEND_CONN_MSG_NOREPLY,
                 JFapChannelConstants.SEG_SEND_CONN_MSG_R);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendEntireMessage");
    }

    /**
     * This method will send a message but in chunks. The chunks that are sent are the exact chunks
     * that are given to us by MFP when we encode the message for the wire. These chunks are actually
     * sent in seperate transmissions.
     * 
     * @param msg
     * @param tran
     * @param orderContext
     * @param destType
     * @param destAddress
     * @param alternateUser
     * @param jfapPriority
     * @param requireReply
     * 
     * @throws SIResourceException
     * @throws SIConnectionUnavailableException
     * @throws SIIncorrectCallException
     * @throws SITemporaryDestinationNotFoundException
     * @throws SINotPossibleInCurrentConfigurationException
     */
    private void sendChunkedMessage(SIBusMessage msg, SITransaction tran, OrderingContext orderContext,
                                    DestinationType destType, SIDestinationAddress destAddress,
                                    String alternateUser, short jfapPriority, boolean requireReply)
                    throws SIResourceException, SIConnectionUnavailableException, SIIncorrectCallException,
                    SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendChunkedMessage",
                        new Object[] { msg, tran, orderContext, destType, destAddress, alternateUser, jfapPriority, requireReply });

        CommsByteBuffer request = getCommsByteBuffer();
        List<DataSlice> messageSlices = null;

        // First job is to encode the message in data slices
        try
        {
            messageSlices = request.encodeFast((JsMessage) msg, getCommsConnection(), getConversation());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message encoded into " + messageSlices.size() + " slice(s)");
        } catch (SIConnectionDroppedException e)
        {
            // No FFDC Code Needed
            // Simply pass this exception on
            throw e;
        } catch (Exception e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".sendChunkedMessage",
                                        CommsConstants.CONNECTIONPROXY_SENDCHUNKED_01,
                                        new Object[] { msg, this });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to encode message", e);
            throw new SIResourceException(e);
        }

        // Do a quick check on the message size. If the size is less than our threshold for chunking
        // the message then send it as one.
        int msgLen = 0;
        for (DataSlice slice : messageSlices)
            msgLen += slice.getLength();
        if (msgLen < CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING)
        {
            // The message is a tiddler, send it in one
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message is smaller than " +
                                      CommsConstants.MINIMUM_MESSAGE_SIZE_FOR_CHUNKING);
            sendEntireMessage(msg,
                              messageSlices,
                              tran,
                              orderContext,
                              destType,
                              destAddress,
                              alternateUser,
                              jfapPriority,
                              requireReply);
        }
        else
        {
            // Now we have the data slices, we can start sending the slices in their own message.
            // The JFap channel will guarentee to get the data to the other side or throw us an exception
            // (at some point). As such, we send each chunk (as opposed to exchanging it) and the final
            // chunk will be exchanged if the requireReply flag was set to true. At that point we will
            // catch any exceptions and throw them on.
            for (int x = 0; x < messageSlices.size(); x++)
            {
                DataSlice slice = messageSlices.get(x);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Sending slice:", slice);

                boolean first = (x == 0);
                boolean last = (x == (messageSlices.size() - 1));
                byte flags = 0;

                // Work out the flags to send
                if (first)
                    flags |= CommsConstants.CHUNKED_MESSAGE_FIRST;
                if (last)
                    flags |= CommsConstants.CHUNKED_MESSAGE_LAST;
                else if (!first)
                    flags |= CommsConstants.CHUNKED_MESSAGE_MIDDLE;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Flags: " + flags);

                if (!first)
                {
                    // This isn't the first slice, grab a fresh buffer
                    request = getCommsByteBuffer();
                }

                request.putShort(getConnectionObjectID());
                // Optionaly add the message order context id
                if (orderContext != null)
                    request.putShort(((OrderingContextProxy) orderContext).getId());
                else
                    request.putShort(CommsConstants.NO_ORDER_CONTEXT);
                request.putSITransaction(tran);
                // Flags to indicate first and last slices
                request.put(flags);

                // If this is the first chunk, send other information about this send
                if (first)
                {
                    // Add Destination Type
                    if (destType != null)
                        request.putShort(destType.toInt());
                    else
                        request.putShort(CommsConstants.NO_DEST_TYPE);
                    // Add alternate user
                    request.putString(alternateUser);
                    // Add the SIDestinationAddress
                    request.putSIDestinationAddress(destAddress, getConversation().getHandshakeProperties().getFapLevel());
                }

                // Now we can dump the slice into the message
                request.putDataSlice(slice);

                // And send the message
                if (!last)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Sending first / middle chunk");

                    jfapSend(request,
                             JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_NOREPLY,
                             jfapPriority,
                             false,
                             ThrottlingPolicy.BLOCK_THREAD);
                }
                else
                {
                    sendData(request,
                             jfapPriority,
                             requireReply,
                             tran,
                             JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG,
                             JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_NOREPLY,
                             JFapChannelConstants.SEG_SEND_CHUNKED_CONN_MSG_R);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendChunkedMessage");
    }

    /**
     * This helper method is used to send the final or only part of a message to our peer. It takes
     * care of whether we should be exchanging the message and deals with the exceptions returned.
     * 
     * @param request
     * @param jfapPriority
     * @param requireReply
     * @param tran
     * @param outboundSegmentType
     * @param outboundNoReplySegmentType
     * @param replySegmentType
     * 
     * @throws SIResourceException
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIIncorrectCallException
     * @throws SIConnectionUnavailableException
     * @throws SITemporaryDestinationNotFoundException
     */
    private void sendData(CommsByteBuffer request, short jfapPriority, boolean requireReply,
                          SITransaction tran, int outboundSegmentType, int outboundNoReplySegmentType,
                          int replySegmentType)
                    throws SIResourceException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException, SIConnectionUnavailableException,
                    SITemporaryDestinationNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "sendData",
                        new Object[] { request, jfapPriority, requireReply, tran, outboundSegmentType, outboundNoReplySegmentType, replySegmentType });

        if (requireReply)
        {
            // Pass on call to server
            CommsByteBuffer reply = jfapExchange(request,
                                                 outboundSegmentType,
                                                 jfapPriority,
                                                 false);

            try
            {
                short err = reply.getCommandCompletionCode(replySegmentType);
                if (err != CommsConstants.SI_NO_EXCEPTION)
                {
                    checkFor_SIConnectionUnavailableException(reply, err);
                    checkFor_SIConnectionDroppedException(reply, err);
                    checkFor_SIResourceException(reply, err);
                    checkFor_SIConnectionLostException(reply, err);
                    checkFor_SILimitExceededException(reply, err);
                    checkFor_SINotAuthorizedException(reply, err);
                    checkFor_SIIncorrectCallException(reply, err);
                    checkFor_SITemporaryDestinationNotFoundException(reply, err);
                    checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                    checkFor_SIErrorException(reply, err);
                    defaultChecker(reply, err);
                }
            } finally
            {
                reply.release();
            }
        }
        else
        {
            jfapSend(request,
                     outboundNoReplySegmentType,
                     jfapPriority,
                     false,
                     ThrottlingPolicy.BLOCK_THREAD);

            // Update the lowest priority
            if (tran != null)
            {
                ((Transaction) tran).updateLowestMessagePriority(jfapPriority);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "sendData");
    }

    /**
     * Proxies the identically named method on the server.
     * Calling receiveNoWait on a SIMPConnection is semantically equivalent to:
     * consumer = connection.createConsumerSession();
     * consumer.receiveNoWait(msg);
     * consumer.close();
     * 
     * @param tran
     * @param unrecoverableReliability
     * @param destAddr
     * @param destType
     * @param criteria
     * @param reliability
     * @param alternateUser
     * 
     * @return SIBusMessage
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     */
    @Override
    public SIBusMessage receiveNoWait(SITransaction tran,
                                      Reliability unrecoverableReliability,
                                      SIDestinationAddress destAddr,
                                      DestinationType destType,
                                      SelectionCriteria criteria,
                                      Reliability reliability,
                                      String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveNoWait",
                        new Object[]
                        {
                         tran,
                         unrecoverableReliability,
                         destAddr,
                         destType,
                         criteria,
                         reliability,
                         alternateUser
                        });

        SIBusMessage mess = null;

        synchronized (generalLock)
        {
            try
            {
                closeLock.readLock().lockInterruptibly();
                try
                {
                    checkAlreadyClosed();

                    // Now we need to synchronise on the transaction object if there is one.
                    if (tran != null)
                    {
                        synchronized (tran)
                        {
                            // Check transaction is in a valid state.
                            // Enlisted for an XA UOW and not rolledback or
                            // completed for a local transaction.
                            if (!((Transaction) tran).isValid())
                            {
                                throw new SIIncorrectCallException(
                                                nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1067", null, null));
                            }

                            mess = _receive(tran, unrecoverableReliability, destAddr, destType,
                                            criteria, reliability, -1, alternateUser);
                        }
                    }
                    else
                    {
                        mess = _receive(null, unrecoverableReliability, destAddr, destType,
                                        criteria, reliability, -1, alternateUser);
                    }
                } finally
                {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e)
            {
                // No FFDC code needed
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "receiveNoWait", mess);
        return mess;
    }

    /**
     * Proxies the identically named method on the server.
     * Calling receiveWithWait on a SICoreConnection is semantically equivalent to:
     * <pre>
     * consumer = connection.createConsumerSession();
     * consumer.receiveWithWait(msg, timeout);
     * consumer.close();
     * </pre>
     * 
     * @param tran
     * @param unrecoverableReliability
     * @param destAddr
     * @param destType
     * @param criteria
     * @param reliability
     * @param timeout
     * @param alternateUser
     * 
     * @return JsMessage
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    @Override
    public SIBusMessage receiveWithWait(SITransaction tran,
                                        Reliability unrecoverableReliability,
                                        SIDestinationAddress destAddr,
                                        DestinationType destType,
                                        SelectionCriteria criteria,
                                        Reliability reliability,
                                        long timeout,
                                        String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveWithWait",
                        new Object[]
                        {
                         tran,
                         unrecoverableReliability,
                         destAddr,
                         destType,
                         criteria,
                         reliability,
                         "" + timeout,
                         alternateUser
                        });

        SIBusMessage mess = null;

        synchronized (generalLock)
        {
            checkAlreadyClosed();

            // Now we need to synchronise on the transaction object if there is one.
            if (tran != null)
            {
                synchronized (tran)
                {
                    // Check transaction is in a valid state.
                    // Enlisted for an XA UOW and not rolledback or
                    // completed for a local transaction.
                    if (!((Transaction) tran).isValid())
                    {
                        throw new SIIncorrectCallException(
                                        nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1067", null, null));
                    }

                    mess = _receive(tran, unrecoverableReliability, destAddr, destType,
                                    criteria, reliability, timeout, alternateUser);
                }
            }
            else
            {
                mess = _receive(null, unrecoverableReliability, destAddr, destType,
                                criteria, reliability, timeout, alternateUser);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "receiveWithWait", mess);
        return mess;
    }

    /**
     * Internal use only receive method. Contains the code which
     * is common between the receive with wait and receive no
     * wait calls. Doesn't do any synchronization, debug trace or
     * paramater checking as these need to be different depending
     * on whether the method is invoked from receiveNoWait or
     * receiveWithWait.
     * 
     * @param tran
     * @param unrecoverableReliability
     * @param destAddr
     * @param destType
     * @param criteria
     * @param reliability
     * @param timeout
     * 
     * @return JsMessage
     * 
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
     * @throws com.ibm.websphere.sib.exception.SIResourceException
     * @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
     * @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
     * @throws com.ibm.websphere.sib.exception.SIErrorException
     * @throws com.ibm.wsspi.sib.core.exception.SINotAuthorizedException
     * @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
     * @throws com.ibm.wsspi.sib.core.exception.SIDestinationLockedException
     * @throws com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException
     * @throws com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException
     */
    private SIBusMessage _receive(SITransaction tran, Reliability unrecoverableReliability,
                                  SIDestinationAddress destAddr, DestinationType destType,
                                  SelectionCriteria criteria, Reliability reliability,
                                  long timeout, String alternateUser)
                    throws SIConnectionUnavailableException, SIConnectionDroppedException,
                    SIResourceException, SIConnectionLostException, SILimitExceededException,
                    SIErrorException,
                    SINotAuthorizedException,
                    SIIncorrectCallException,
                    SIDestinationLockedException,
                    SITemporaryDestinationNotFoundException,
                    SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_receive",
                        new Object[]
                        {
                         tran,
                         unrecoverableReliability,
                         destAddr,
                         destType,
                         criteria,
                         reliability,
                         "" + timeout,
                         alternateUser
                        });

        CommsByteBuffer request = getCommsByteBuffer();
        request.putShort(getConnectionObjectID());

        request.putSITransaction(tran);

        if (reliability != null)
        {
            request.putShort(reliability.toInt());
        }
        else
        {
            request.putShort(-1); // Send a -1 to signify a null value for reliability
        }

        // Add Timeout (0 would indicate no wait)
        request.putLong(timeout);
        if (destType != null)
        {
            request.putShort(destType.toInt());
        }
        else
        {
            request.putShort(CommsConstants.NO_DEST_TYPE);
        }

        // Unrecoverable reliability
        if (unrecoverableReliability == null)
        {
            unrecoverableReliability = Reliability.NONE;
        }
        request.putShort(unrecoverableReliability.toInt());

        // Add the SI Destination Address
        request.putSIDestinationAddress(destAddr, getConversation().getHandshakeProperties().getFapLevel());

        // Add selection criteria
        request.putSelectionCriteria(criteria);

        // Alternate user
        request.putString(alternateUser);

        boolean exchangeSuccessful = false;
        CommsByteBuffer reply = null;
        SIBusMessage mess = null;

        // Pass on call to server
        reply = jfapExchange(request,
                             JFapChannelConstants.SEG_RECEIVE_CONN_MSG,
                             JFapChannelConstants.PRIORITY_MEDIUM,
                             true);

        short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_RECEIVE_CONN_MSG_R);
        if (err != CommsConstants.SI_NO_EXCEPTION)
        {
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIResourceException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SILimitExceededException(reply, err);
            checkFor_SINotAuthorizedException(reply, err);
            checkFor_SIIncorrectCallException(reply, err);
            checkFor_SIDestinationLockedException(reply, err);
            checkFor_SITemporaryDestinationNotFoundException(reply, err);
            checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
        }

        exchangeSuccessful = true;

        // If the pending message slices are null then we are simply receiving an entire message
        // as opposed to the message in chunks
        synchronized (pendingMessageSliceLock)
        {
            if (pendingMessageSlices == null)
            {
                reply.getShort(); // BIT16 ConnectionObjectId
                mess = reply.getMessage(getCommsConnection());
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Received the final slice");

                reply.getShort(); // BIT16 ConnectionObjectId

                // Now get the final data slice
                addMessagePart(reply);

                // Now re-assemble the message
                try
                {
                    mess = JsMessageFactory.getInstance().createInboundJsMessage(pendingMessageSlices,
                                                                                 getCommsConnection());
                } catch (Exception e)
                {
                    FFDCFilter.processException(e, CLASS_NAME + "._receive",
                                                CommsConstants.CONNECTIONPROPS_GETCHAIN_01,
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Failed to recreate message", e);

                    throw new SIResourceException(e);
                }

                // Clear the reference to this
                pendingMessageSlices = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled())
            CommsLightTrace.traceMessageId(tc, "ReceiveMsgTrace", mess);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_receive", mess);
        return mess;
    }

    // *******************************************************************************************
    // *                           Private Helper Methods                                        *
    // *******************************************************************************************

    /**
     * Helper method that retrieves or creates a proxy queue conversation group.
     * 
     * @return ProxyQueueConversationGroup
     */
    private ProxyQueueConversationGroup getProxyQueueConversationGroup()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProxyQueueConversationGroup");

        // PQ Conversation groups are hung off the conversation, see if
        // we already have one.
        ClientConversationState state =
                        (ClientConversationState) getConversation().getAttachment();
        ProxyQueueConversationGroup pqcg = state.getProxyQueueConversationGroup();
        if (pqcg == null)
        {
            // No existing group - create a new one.
            ProxyQueueConversationGroupFactory pqFact =
                            ProxyQueueConversationGroupFactory.getRef();
            pqcg = pqFact.create(getConversation());

            // Save the proxy group with the conversation
            state.setProxyQueueConversationGroup(pqcg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProxyQueueConversationGroup", pqcg);
        return pqcg;
    }

    /**
     * Checks to see if the connection is closed. If it is, an
     * SIObjectClosedException is thrown
     * 
     * @throws SIConnectionUnavailableException
     */
    protected void checkAlreadyClosed() throws SIConnectionUnavailableException
    {
        if (isClosed()) {
            throw new SIConnectionUnavailableException(
                            nls.getFormattedMessage("CONNECTION_CLOSED_SICO1014", null, null));
        }
    }

    /**
     * This method is used to set the initial unique id that should be used
     * for this connection
     * 
     * @param id
     */
    void setInitialUniqueId(final byte[] id) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setInitialUnqiueId");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            SibTr.debug(tc, "ID Length", Integer.valueOf(id.length));
            SibTr.debug(tc, "ID: ");
            SibTr.bytes(tc, id);
        }

        this.initialUniqueId = id;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setInitialUniqueId");
    }

    /*
     * Method is used to add an order context to the order context pool for this connection
     */
    void addOrderContext(final Short context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addOrderContext", "context=" + context);

        orderContextPool.add(context);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addOrderContext");
    }

    /*
     * Method is used to get an order context from the order context pool for this connection
     */
    Short getOrderContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOrderContext");

        Short rc = null; // In case the orderContextPool is empty

        synchronized (orderContextPool) {
            if (!orderContextPool.isEmpty()) {
                rc = orderContextPool.remove(0);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOrderContext", "rc=" + rc);
        return rc;
    }

    /**
     * This method is used to set the ME Uuid for this connection. It is called
     * by the proxy receive listener when connection info data is received.
     * 
     * @param meUuid
     */
    void setMeUuid(String meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMeUuid", meUuid);
        this.meUuid = meUuid;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMeUuid");
    }

    /**
     * This method is used to the set the resolved user ID for this connection. It is called
     * by the proxy receive listener when connection info data is received.
     * 
     * @param resolvedUserId
     */
    void setResolvedUserId(String resolvedUserId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setResolvedUserId", resolvedUserId);
        this.resolvedUserId = resolvedUserId;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setResolvedUserId");
    }

    /**
     * This method is used to the set the ME name for this connection. It is called
     * by the proxy receive listener when connection info data is received.
     * 
     * @param meName
     */
    void setMeName(String meName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMeName", meName);
        this.meName = meName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMeName");
    }

    /**
     * Notification that a consumer session has been closed.
     * 
     * @param consumerSessionId
     */
    void consumerClosedNotification(short consumerSessionId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "consumerClosedNotification", consumerSessionId);
        synchronized (consumerSessions)
        {
            consumerSessions.remove(Short.valueOf(consumerSessionId));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "consumerClosedNotification");
    }

    /**
     * This method is called when part of a message is received asynchronously, as a result of a
     * receive request before the actual request has completed. This allows a message to be sent
     * back in individual parts rather than one (potentially) very large message which is easier on
     * the Java memory manager.
     * 
     * @param buffer
     */
    void addMessagePart(CommsByteBuffer buffer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addMessagePart", buffer);

        byte flags = buffer.get();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "Flags: ", flags);

        // Simple add the slice onto our list
        synchronized (pendingMessageSliceLock)
        {
            if (pendingMessageSlices == null)
            {
                pendingMessageSlices = new ArrayList<DataSlice>();
            }

            pendingMessageSlices.add(buffer.getDataSlice());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Message parts: ", pendingMessageSlices);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addMessagePart");
    }

    /**
     * Gets a consumer session that is currently active and owned by this connection. This is needed
     * when part of a synchronous message has been received and the consumer session that it belongs
     * to needs to be located so that message can be given to it.
     * 
     * @param consumerSessionId
     * @return Returns the consumer session represented by the Id.
     */
    ConsumerSessionProxy getConsumerSessionProxy(short consumerSessionId)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerSessionProxy", consumerSessionId);
        ConsumerSessionProxy cs = null;
        synchronized (consumerSessions)
        {
            cs = consumerSessions.get(Short.valueOf(consumerSessionId));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerSessionProxy", cs);
        return cs;
    }

    //SIB0137.comms.2 start

    /**
     * @see SICoreConnection#inationListener(java.lang.String, com.ibm.wsspi.sib.core.DestinationListener, com.ibm.wsspi.sib.core.DestinationType,
     *      com.ibm.wsspi.sib.core.DestinationAvailability)
     */
    @Override
    public SIDestinationAddress[] addDestinationListener(String destinationNamePattern,
                                                         DestinationListener destinationListener,
                                                         DestinationType destinationType,
                                                         DestinationAvailability destinationAvailability)
                    throws SIIncorrectCallException,
                    SICommandInvocationFailedException,
                    SIConnectionUnavailableException,
                    SIConnectionDroppedException,
                    SIConnectionLostException
    {
        SIDestinationAddress[] rc = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addDestinationListener",
                        new Object[] { destinationNamePattern, destinationListener, destinationType, destinationAvailability });
        // Check we are running at a suitable FAP level
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_9);

        synchronized (generalLock) {
            try {
                closeLock.readLock().lockInterruptibly();

                try {
                    checkAlreadyClosed();

                    CommsByteBuffer request = getCommsByteBuffer();

                    // Put connection object id
                    request.putShort(getConnectionObjectID());

                    // Always add the DestinationListener to the cache, if this DestinationListener instance already exists
                    // in the cache then the existing id value will be returned
                    final ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
                    final short destinationListenerId = state.getDestinationListenerCache().add(destinationListener);

                    // Put DestinationListener cache id
                    request.putShort(destinationListenerId);

                    // Put DestinationType
                    if (destinationType == null) {
                        request.putShort(CommsConstants.NO_DEST_TYPE);
                    } else {
                        request.putShort((short) destinationType.toInt());
                    }

                    // Put DestinationAvailability
                    if (destinationAvailability == null) {
                        request.putShort(CommsConstants.NO_DEST_AVAIL);
                    } else {
                        request.putShort((short) destinationAvailability.toInt());
                    }

                    // Put String (destinationNamePattern)
                    request.putString(destinationNamePattern);

                    // Exchange request with ME.
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_ADD_DESTINATION_LISTENER,
                                                         JFapChannelConstants.PRIORITY_MEDIUM, true);

                    try {
                        short err = CommsConstants.SI_NO_EXCEPTION;
                        err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_ADD_DESTINATION_LISTENER_R);

                        if (err != CommsConstants.SI_NO_EXCEPTION) {
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SICommandInvocationFailedException(reply, err);
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIConnectionLostException(reply, err);
                            defaultChecker(reply, err);
                        } else {
                            reply.getShort(); // Remove connection object Id
                            final short count = reply.getShort();
                            rc = new SIDestinationAddress[count];
                            for (int i = 0; i < count; i++) {
                                rc[i] = reply.getSIDestinationAddress(getConversation().getHandshakeProperties().getFapLevel());
                            }
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, count + " SIDestinationAddress objects received");
                        }
                    } finally {
                        reply.release();
                    }
                } finally {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "interrupted exception");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addDestinationListener", rc);
        return rc;
    }

    //SIB0137.comms.2 end

    /**
     * Return a reference to the asynchronous callback synchroniser object for this connection
     * 
     * @return AsyncCallbackSynchronizer
     */
    public AsyncCallbackSynchronizer getAsyncCallbackSynchronizer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAsyncCallbackSynchronizer");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getAsyncCallbackSynchronizer", asyncCallbackSynchronizer);
        return asyncCallbackSynchronizer;
    }

    /**
     * Allow selection of PK86574 behaviour.
     * The default setting for the JVM is configured with a tuning property.
     * 
     * @see CommsConstants#STRICT_REDELIVERY_KEY
     */
    public void setStrictRedeliveryOrdering(boolean value)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setStrictRedeliveryOrdering", new Boolean(value));
        this.strictRedeliveryOrdering = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setStrictRedeliveryOrdering");
    }

    /**
     * @returns whether strict redelivery ordering is enabled
     */
    protected boolean getStrictRedeliveryOrdering() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(this, tc, "getStrictRedeliveryOrdering");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(this, tc, "getStrictRedeliveryOrdering", Boolean.valueOf(strictRedeliveryOrdering));
        }
        return strictRedeliveryOrdering;
    }

    //F011127 STARTS
    /**
     * @see SICoreConnection#registerConsumerSetMonitor(SIDestinationAddress, String, ConsumerSetChangeCallback)
     */
    @Override
    public boolean registerConsumerSetMonitor(
                                              SIDestinationAddress destinationAddress,
                                              String discriminatorExpression, ConsumerSetChangeCallback callback)
                    throws SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SIConnectionUnavailableException,
                    SIConnectionDroppedException,
                    SIIncorrectCallException,
                    SICommandInvocationFailedException {

        boolean areConsumers = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerConsumerSetMonitor",
                        new Object[] { destinationAddress, discriminatorExpression, callback });
        // Check we are running at a suitable FAP level
        final HandshakeProperties props = getConversation().getHandshakeProperties();
        CommsUtils.checkFapLevel(props, JFapChannelConstants.FAP_VERSION_14);

        // Check that a listener has been specified
        if (callback == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_CONSUMERSETCHANGECALLBACK_SICO1069", null, null));
        } else if (destinationAddress == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_DESTINATIONADDRESS_SICO1070", null, null));

        } else if (discriminatorExpression == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerConsumerSetMonitor", "SIIncorrectCallException");
            // Parameter is null, throw an excepiton
            throw new SIIncorrectCallException(nls.getFormattedMessage("NULL_DISCRIMINATOREXPRESSION_SICO1071", null, null));

        }

        synchronized (generalLock) {
            try {
                closeLock.readLock().lockInterruptibly();

                try {
                    checkAlreadyClosed();

                    CommsByteBuffer request = getCommsByteBuffer();

                    // Put connection object id
                    request.putShort(getConnectionObjectID());

                    //put destinationAddress
                    request.putSIDestinationAddress(destinationAddress, getConversation().getHandshakeProperties().getFapLevel());

                    //put the topicexpression
                    request.putString(discriminatorExpression);

                    // Always add the ConsumerSetChangeCallback to the cache, if this ConsumerSetChangeCallback instance already exists
                    // in the cache then the existing id value will be returned
                    final ClientConversationState state = (ClientConversationState) getConversation().getAttachment();
                    final short consumerMonitorListenerID = state.getConsumerMonitorListenerCache().add(callback);
                    // Put consumerMonitorListenerID id
                    request.putShort(consumerMonitorListenerID);

                    // Exchange request with ME.
                    CommsByteBuffer reply = jfapExchange(request,
                                                         JFapChannelConstants.SEG_REGISTER_CONSUMER_SET_MONITOR,
                                                         JFapChannelConstants.PRIORITY_MEDIUM, true);

                    try {
                        short err = CommsConstants.SI_NO_EXCEPTION;
                        err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_REGISTER_CONSUMER_SET_MONITOR_R);

                        if (err != CommsConstants.SI_NO_EXCEPTION) {
                            checkFor_SIResourceException(reply, err);
                            checkFor_SINotPossibleInCurrentConfigurationException(reply, err);
                            checkFor_SIConnectionUnavailableException(reply, err);
                            checkFor_SIConnectionDroppedException(reply, err);
                            checkFor_SIIncorrectCallException(reply, err);
                            checkFor_SICommandInvocationFailedException(reply, err);
                            defaultChecker(reply, err);
                        } else {
                            reply.getShort(); // Remove connection object Id
                            areConsumers = reply.getBoolean();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Are there any consumers : " + areConsumers);
                        }
                    } finally {
                        reply.release();
                    }
                } finally {
                    closeLock.readLock().unlock();
                }
            } catch (InterruptedException e) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "interrupted exception");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerConsumerSetMonitor", areConsumers);
        return areConsumers;

    }
    //F011127 ENDS
}
