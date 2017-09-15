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
package com.ibm.ws.sib.processor.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.ExtendedBoolean;
import com.ibm.ws.sib.admin.QualifiedDestinationName;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionTypeFilter;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.Alias;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * Class to represent Alias Destinations
 */
public class AliasDestinationHandler extends AbstractAliasDestinationHandler
{
    /** Trace for the component */
    private static final TraceComponent tc =
                    SibTr.register(
                                   AliasDestinationHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /** The definition of this alias. */
    private DestinationAliasDefinition _definition;

    /** The administered fwd routing path */
    private List<JsDestinationAddress> _forwardRoutingPath = null; // List version
    SIDestinationAddress _defaultFrp[] = null; // Array version (?!?)

    /** The administered reply destination */
    JsDestinationAddress _replyDest = null;

    /** Receive allowed setting - default to true */
    private boolean _isReceiveAllowed;

    private JsDestinationAddress _aliasDestinationAddr = null;
    private JsDestinationAddress _targetDestinationAddr = null;

    /**
     * An alias that directly targets a Queue destination in the local bus has the
     * potential to scope the set of accessible message points when sending to or
     * consuming from this alias (SIB0113)
     */
    private boolean _hasScopedMessagePoints = false;
    private SIBUuid8 _singleScopedQueuePointME = null; // Optimisation for single scoped queue point
    private HashSet<SIBUuid8> _scopedQueuePointMEs = null; // Entire set (even it only one)

    /**
     * <p>Cold start constructor.</p>
     * <p>Create a new instance of a destination, passing in the name of the
     * destination and its definition. A destination represents a topicspace in
     * pub/sub or a queue in point to point.</p>
     * 
     * @param destinationName
     * @param destinationDefinition
     * @param messageProcessor
     * @param parentStream The Itemstream this DestinationHandler should be
     *            added into.
     * @param durableSubscriptionsTable Required only by topicspace
     *            destinations. Can be null if point to point (local or remote).
     * 
     * @throws SIDestinationWrongTypeException The specified alias destination
     *             type conflicts with the target destination type.
     */
    public AliasDestinationHandler(
                                   DestinationAliasDefinition aliasDefinition,
                                   MessageProcessor messageProcessor,
                                   SIMPItemStream parentItemStream,
                                   DestinationHandler resolvedDestinationHandler,
                                   String busName)
    {
        super(messageProcessor, resolvedDestinationHandler, busName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "AliasDestinationHandler", new Object[] {
                                                                     aliasDefinition,
                                                                     messageProcessor, parentItemStream,
                                                                     resolvedDestinationHandler, busName });

        // Pull out the interesting bits from the definition
        processDefinition(aliasDefinition);

        createControlAdapter();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "AliasDestinationHandler", new Object[] { this, _aliasDestinationAddr, _targetDestinationAddr });
    }

    /**
     * Cache all the properties in the definition which take a bit of work to
     * generate (i.e. are in a different format from those in the actual definition
     * object).
     * 
     * Called when the Alias is first created and on subsequent config changes
     * 
     * @param definition
     */
    private void processDefinition(DestinationAliasDefinition definition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processDefinition", definition);

        _definition = definition; // Null for BusHandler (foreign bus)

        // Null out previous cached settings first
        _forwardRoutingPath = null;
        _defaultFrp = null;
        _replyDest = null;
        _aliasDestinationAddr = null;
        _targetDestinationAddr = null;
        _hasScopedMessagePoints = false;
        _singleScopedQueuePointME = null;
        _scopedQueuePointMEs = null;

        if (_definition != null)
        {
            _aliasDestinationAddr = SIMPUtils.createJsDestinationAddress(_definition.getName(), null, getBus());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Alias destination address: " + _aliasDestinationAddr);

            _targetDestinationAddr = SIMPUtils.createJsDestinationAddress(getTargetName(), null, getTargetBus());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Target destination address: " + _targetDestinationAddr);

            // Convert array of names to list of jsDestinationAddresses for ForwardRoutingPath
            QualifiedDestinationName[] names = _definition.getForwardRoutingPath();
            if (names != null)
            {
                _forwardRoutingPath = new ArrayList<JsDestinationAddress>(names.length);
                _defaultFrp = new SIDestinationAddress[names.length];

                for (int name = 0; name < names.length; name++)
                {
                    JsDestinationAddress address =
                                    SIMPUtils.createJsDestinationAddress(names[name].getDestination(),
                                                                         null, // ME never set via config
                                                                         names[name].getBus());
                    _forwardRoutingPath.add(address);
                    _defaultFrp[name] = address;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Forward routing path [" + name + "]: " + address);
                }
            }

            // Create a JsDestinationAddress from the qualifiedName of the reply destination
            QualifiedDestinationName replyName = _definition.getReplyDestination();
            if (replyName != null)
            {
                _replyDest = SIMPUtils.createJsDestinationAddress(replyName.getDestination(),
                                                                  messageProcessor.getMessagingEngineUuid(),
                                                                  replyName.getBus());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Reply destination: " + _replyDest);
            }

            SIBUuid8[] queuePoints = _definition.getScopedQueuePointMEs();
            if ((queuePoints != null) && (queuePoints.length > 0))
            {
                _hasScopedMessagePoints = true;

                if (queuePoints.length == 1)
                {
                    _singleScopedQueuePointME = queuePoints[0];
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Single scoped Queue Point: " + queuePoints[0]);
                }
                else
                {
                    _scopedQueuePointMEs = new HashSet<SIBUuid8>();
                    for (int i = 0; i < queuePoints.length; i++)
                    {
                        _scopedQueuePointMEs.add(queuePoints[i]);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Scoped Queue Point: " + queuePoints[i]);
                    }
                }
            }
            else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "No scoped Queue Point");

            setReceiveAllowed(_definition);
        }
        else
            _isReceiveAllowed = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processDefinition");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getContextProperty(java.lang.String)
     */
    @Override
    public Object getContextValue(String keyName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getContextValue", keyName);

        Map context = _definition.getDestinationContext();

        Object property = context.get(keyName);

        if (null == property)
            property = getTarget().getContextValue(keyName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getContextValue", property);

        return property;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultPriority()
     */
    @Override
    public int getDefaultPriority()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultPriority");

        int pri = _definition.getDefaultPriority();

        // Admin have said -1 will signal return priority from target.  At
        // some point they should provide a constant with which this magic number
        // can be replaced.
        if (pri == DestinationAliasDefinition.DEFAULT_DEFAULTPRIORITY)
        {
            pri = getTarget().getDefaultPriority();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultPriority", Integer.valueOf(pri));

        return pri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultReliability()
     */
    @Override
    public Reliability getDefaultReliability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultReliability");

        Reliability rel = _definition.getDefaultReliability();

        // A reliability of NONE signals use target value.
        if (rel.equals(Reliability.NONE))
        {
            rel = getTarget().getDefaultReliability();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultReliability", rel);

        return rel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDescription()
     */
    @Override
    public String getDescription()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDescription");

        String desc = _definition.getDescription();

        // Assume that a null description means inherit.
        if (null == desc)
        {
            desc = getTarget().getDescription();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDescription", desc);

        return desc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDestinationType()
     */
    @Override
    public DestinationType getDestinationType()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationType");

        DestinationType dt = getTarget().getDestinationType();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDestinationType", dt);

        return dt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxReliability()
     */
    @Override
    public Reliability getMaxReliability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMaxReliability");

        Reliability rel = _definition.getMaxReliability();

        // A reliability of NONE signals use target value.
        if (rel.equals(Reliability.NONE))
        {
            rel = getTarget().getMaxReliability();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMaxReliability", rel);

        return rel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isOverrideOfQOSByProducerAllowed()
     */
    @Override
    public boolean isOverrideOfQOSByProducerAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed");

        ExtendedBoolean aliasIsProducerQOSOverrideEnabled = _definition.isOverrideOfQOSByProducerAllowed();
        boolean isProducerQOSOverrideEnabled = false;

        // A reliability of NONE signals use target value.
        if (aliasIsProducerQOSOverrideEnabled.equals(ExtendedBoolean.NONE))
        {
            isProducerQOSOverrideEnabled = getTarget().isOverrideOfQOSByProducerAllowed();
        }
        else if (aliasIsProducerQOSOverrideEnabled.equals(ExtendedBoolean.FALSE))
        {
            isProducerQOSOverrideEnabled = false;
        }
        else
        {
            isProducerQOSOverrideEnabled = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed",
                       Boolean.valueOf(isProducerQOSOverrideEnabled));

        return isProducerQOSOverrideEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isAlias()
     */
    @Override
    public boolean isAlias()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isForeign()
     */
    @Override
    public boolean isForeign()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSendAllowed()
     */
    @Override
    public boolean isSendAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isSendAllowed");

        boolean isSendAllowed = true;

        // We check first whether send is disallowed at the bus definition level. A
        // setting at the bus level overrides a setting on the Alias Destination
        // Definition.
        DestinationHandler target = getTarget();
        if (_sendAllowedOnTargetForeignBus != null)
            isSendAllowed = _sendAllowedOnTargetForeignBus.booleanValue();

        ExtendedBoolean aliasIsSendAllowed = _definition.isSendAllowed();

        // If we're allowed to send at the bus level, or there is no Foreign
        // Bus target in the chain, then move to the next level of checking.
        if (isSendAllowed)
        {
            // A setting of NONE signals use target value.
            if (aliasIsSendAllowed.equals(ExtendedBoolean.NONE))
            {
                isSendAllowed = target.isSendAllowed();
            }
            else if (aliasIsSendAllowed.equals(ExtendedBoolean.FALSE))
            {
                isSendAllowed = false;
            }
            else
            {
                isSendAllowed = true;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isSendAllowed", Boolean.valueOf(isSendAllowed));

        return isSendAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveAllowed()
     */
    @Override
    public synchronized boolean isReceiveAllowed()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isReceiveAllowed");
            SibTr.exit(tc, "isReceiveAllowed", Boolean.valueOf(_isReceiveAllowed));
        }

        return _isReceiveAllowed;
    }

    /**
     * Set receive allowed flag based on the value stored in the ExtendedBoolean
     * 
     * @param aliasIsReceiveAllowed
     */
    private synchronized void setReceiveAllowed(DestinationAliasDefinition aliasDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setReceiveAllowed", aliasDefinition);

        ExtendedBoolean aliasIsReceiveAllowed = aliasDefinition.isReceiveAllowed();
        // A reliability of NONE signals use target value.
        if (aliasIsReceiveAllowed.equals(ExtendedBoolean.NONE))
            _isReceiveAllowed = getTarget().isReceiveAllowed();
        else if (aliasIsReceiveAllowed.equals(ExtendedBoolean.FALSE))
            _isReceiveAllowed = false;
        else
            _isReceiveAllowed = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setReceiveAllowed", Boolean.valueOf(_isReceiveAllowed));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefinition()
     */
    @Override
    public BaseDestinationDefinition getDefinition()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefinition");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefinition", _definition);

        return _definition;
    }

    @Override
    public void updateDefinition(BaseDestinationDefinition destinationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateDefinition", destinationDefinition);

        DestinationAliasDefinition oldDefinition = _definition;

        // definition must be updated before notifying consumer dispatcher(s) of update
        // so pull out the interesting bits from the definition
        processDefinition((DestinationAliasDefinition) destinationDefinition);

        if ((oldDefinition == null) ||
            (oldDefinition.isReceiveAllowed() != _definition.isReceiveAllowed()))
        {
            if (isPubSub())
            {
                // Notify consumers on this localization
                SubscriptionTypeFilter filter = new SubscriptionTypeFilter();
                filter.LOCAL = Boolean.TRUE;
                SIMPIterator itr = getSubscriptionIndex().iterator(filter);
                while (itr.hasNext())
                {
                    ControllableSubscription subscription = (ControllableSubscription) itr.next();
                    ConsumerDispatcher cd = (ConsumerDispatcher) subscription.getOutputHandler();
                    if (cd != null)
                    {
                        //Only notify subscriptions made through this alias
                        if (cd.getConsumerDispatcherState().getTopicSpaceUuid().equals(getUuid()))
                        {
                            cd.notifyReceiveAllowed(this);
                        }
                    }
                }
                itr.finished();
            }
            else
            {
                //tell the local consumer dispatcher that this destinations receiveAllowed has changed
                ConsumerDispatcher cm = (ConsumerDispatcher) getLocalPtoPConsumerManager();
                if (cm != null)
                {
                    cm.notifyReceiveAllowed(this);
                }
            }

            //tell the any RME remote consumer dispatchers that this destinations receiveAllowed has changed
            notifyReceiveAllowedRCD(this);

            //Tell any aliases that inherit the receive allowed value to
            //also react to the change.
            notifyTargettingAliasesReceiveAllowed();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateDefinition");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
     */
    @Override
    public void createControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAdapter");
        controlAdapter = new Alias(messageProcessor, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAdapter", controlAdapter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getControlAdapter");
            SibTr.exit(tc, "getControlAdapter", controlAdapter);
        }
        return controlAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean()
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean()
    {}

    @Override
    public String getTargetName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getTargetName");
            SibTr.exit(tc, "getTargetName", _definition.getTargetName());
        }
        return _definition.getTargetName();
    }

    @Override
    public String getTargetBus()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getTargetBus");
            SibTr.exit(tc, "getTargetBus", _definition.getTargetBus());
        }
        return _definition.getTargetBus();
    }

    @Override
    public JsDestinationAddress getRoutingDestinationAddr(JsDestinationAddress inAddress,
                                                          boolean fixedMessagePoint)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getRoutingDestinationAddr", new Object[] { this,
                                                                       inAddress,
                                                                       Boolean.valueOf(fixedMessagePoint) });

        JsDestinationAddress outAddress = null;

        DestinationHandler target = getTarget();

        // As we're an alias we just recurse the chain to find the routing address.
        // Passing in our address in case it is needed (e.g. if we map to a foreign bus handler)
        outAddress = target.getRoutingDestinationAddr(_targetDestinationAddr, fixedMessagePoint);

        // If after resolving the routing address we get back a null then we must ultimately
        // map to a real queue or topicspace in the local bus, without and fixed QP requirement.
        // In this case we need to make sure we're not one of those Aliases that scope down
        // the message point sets, if we are we still need to make sure that knowledge stays with the
        // message so we set our own address into the message so that everyone knows that we're
        // 'special' and handles the message through the alias rather than the target destination
        // (e.g. on remote MEs).
        if ((outAddress == null) && _hasScopedMessagePoints)
        {
            if (((_singleScopedQueuePointME != null) || (_scopedQueuePointMEs != null)))
                outAddress = _aliasDestinationAddr;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRoutingDestinationAddr", outAddress);

        return outAddress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#choosePtoPOutputHandler(com.ibm.ws.sib.mfp.JsDestinationAddress)
     */
    @Override
    public OutputHandler choosePtoPOutputHandler(SIBUuid8 fixedMEUuid,
                                                 SIBUuid8 preferredMEUuid,
                                                 boolean localMessage,
                                                 boolean forcePut,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIResourceException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "choosePtoPOutputHandler", new Object[] { fixedMEUuid,
                                                                     preferredMEUuid,
                                                                     localMessage,
                                                                     forcePut,
                                                                     scopedMEs });

        OutputHandler result = null;
        boolean error = false;

        // We're an alias so we should never be called with a scoped ME set
        if (scopedMEs != null)
        {
            SIMPErrorException e = new SIMPErrorException("Alias called with scoped ME set");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
            }

            e.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                "1:750:1.71.2.6",
                                                SIMPUtils.getStackTrace(e) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "choosePtoPOutputHandlers", e);
            throw e;
        }

        // Pull out any scoped ME set that we have configured
        SIBUuid8 singleScopedME = null;
        HashSet<SIBUuid8> definedScopedMEs = null;

        // If this is an alias that's scoped the message points of the target destination
        // then we need to pass these onto the target.
        if (_hasScopedMessagePoints)
        {

            if (_singleScopedQueuePointME != null)
                singleScopedME = _singleScopedQueuePointME;
            else
                definedScopedMEs = _scopedQueuePointMEs;

            // If the caller has fixed an ME, first check it's in our set
            if (fixedMEUuid != null)
            {
                if ((singleScopedME != null) && !fixedMEUuid.equals(singleScopedME))
                    error = true;
                else if ((definedScopedMEs != null) && !definedScopedMEs.contains(fixedMEUuid))
                    error = true;
            }

            // If we have a single ME scoped by this alias we may as pass it to the target as
            // a fixed ME (which it must match as we've already checked that) to save
            // having to parse a HashSet everywhere for no need.
            if (singleScopedME != null)
                fixedMEUuid = singleScopedME;
        }

        // Look to the resolved destination to choose an output handler as the
        // alias destination doesnt store any messages
        if (!error)
            result = _targetDestinationHandler.choosePtoPOutputHandler(fixedMEUuid,
                                                                       preferredMEUuid,
                                                                       localMessage,
                                                                       forcePut,
                                                                       definedScopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "choosePtoPOutputHandler", result);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#chooseConsumerDispatcher()
     */
    @Override
    public ConsumerManager chooseConsumerManager(SIBUuid12 gatheringTargetUuid,
                                                 SIBUuid8 fixedMEUuid,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chooseConsumerManager", new Object[] { gatheringTargetUuid,
                                                                   fixedMEUuid,
                                                                   scopedMEs });

        // We're an alias (or foreign destination) so we should never be called with a scoped ME set
        if (scopedMEs != null)
        {
            SIMPErrorException e = new SIMPErrorException("Alias called with scoped ME set");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
            }

            e.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                "1:837:1.71.2.6",
                                                SIMPUtils.getStackTrace(e) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "chooseConsumerManager", e);
            throw e;
        }

        boolean error = false;
        ConsumerManager consumerManager = null;

        // If the caller has fixed an ME, first check it's in our set
        if (fixedMEUuid != null)
        {
            if ((_singleScopedQueuePointME != null) && !fixedMEUuid.equals(_singleScopedQueuePointME))
                error = true;
            else if ((_scopedQueuePointMEs != null) && !_scopedQueuePointMEs.contains(fixedMEUuid))
                error = true;
        }

        if (!error)
        {
            // If we have a single ME scoped by this alias we may as well pass it to the target as
            // a fixed ME (which it must match as we've already checked that) to save
            // having to parse a HashSet everywhere for no need.
            if (_singleScopedQueuePointME != null)
                fixedMEUuid = _singleScopedQueuePointME;

            // If a gatheringUuid was supplied, replace it with the alias uuid
            if (gatheringTargetUuid != null)
                gatheringTargetUuid = _definition.getUUID();
            // Pass on any scoped queue points (this is a consumer on an alias)
            consumerManager = _targetDestinationHandler.chooseConsumerManager(gatheringTargetUuid, fixedMEUuid, _scopedQueuePointMEs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chooseConsumerManager", consumerManager);

        return consumerManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getForwardRoutingPath()
     */
    @Override
    public List getForwardRoutingPath()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getForwardRoutingPath");

        // The 'rule' (existing behaviour) is that we only use an FRP if this alias
        // has one, i.e. we don't walk the chain until we find the first FRP we come
        // to. So all we do here is return our FRP (which could be null).

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getForwardRoutingPath", _forwardRoutingPath);

        return _forwardRoutingPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultForwardRoutingPath()
     */
    @Override
    public SIDestinationAddress[] getDefaultForwardRoutingPath()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getDefaultForwardRoutingPath");
            SibTr.exit(tc, "getDefaultForwardRoutingPath", _defaultFrp);
        }

        return _defaultFrp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getReplyDestination()
     */
    @Override
    public JsDestinationAddress getReplyDestination()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getReplyDestination");
            SibTr.exit(tc, "getReplyDestination", _replyDest);
        }

        return _replyDest;
    }

    /**
     * Check permission to access a Destination
     * 
     * @param secContext
     * @param operation
     * @return
     */
    @Override
    public boolean checkDestinationAccess(
                                          SecurityContext secContext,
                                          OperationType operation)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationAccess",
                        new Object[] { secContext, operation });

        boolean allow = false;

        // It may be that we need to "tunnel through" the alias destination
        // to the target in order to perform security checks. This depends
        // on the DelegateAuthorizationCheckToTarget flag which is set on the
        // alias definition at config time.
        if (_definition.getDelegateAuthorizationCheckToTarget())
        {
            // The security check will be performed against the target dest
            if (getTarget().checkDestinationAccess(secContext,
                                                   operation))
            {
                allow = true;
            }
        }
        else
        {
            // The security check is against the alias itself
            if (accessChecker.checkDestinationAccess(secContext,
                                                     getBus(),
                                                     getDefinition().getName(),
                                                     operation))
            {
                allow = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationAccess", Boolean.valueOf(allow));

        return allow;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTopicAccessCheckRequired()
     */
    @Override
    public boolean isTopicAccessCheckRequired()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isTopicAccessCheckRequired");
        // Look to the resolved destination for this value
        boolean ret = getTarget().isTopicAccessCheckRequired();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isTopicAccessCheckRequired", Boolean.valueOf(ret));
        return ret;
    }

    /**
     * Check permission to access a Discriminator
     * 
     * @param secContext
     * @param operation
     * @return
     * @throws SICoreException
     */
    @Override
    public boolean checkDiscriminatorAccess(
                                            SecurityContext secContext,
                                            OperationType operation) throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDiscriminatorAccess",
                        new Object[] { secContext, operation });

        boolean allow = true;

        // Discriminator checks are always made against the target
        if (isTopicAccessCheckRequired())
        {
            if (!getTarget().checkDiscriminatorAccess(secContext,
                                                      operation))
            {
                allow = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDiscriminatorAccess", Boolean.valueOf(allow));

        return allow;
    }

    @Override
    public int checkPtoPOutputHandlers(SIBUuid8 fixedMEUuid,
                                       HashSet<SIBUuid8> scopedMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkPtoPOutputHandlers", new Object[] { fixedMEUuid,
                                                                     scopedMEs });

        int result = DestinationHandler.NOT_SET;

        // We're an alias so we should never be called with a scoped ME set
        if (scopedMEs != null)
        {
            SIMPErrorException e = new SIMPErrorException("Alias called with scoped ME set");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
            }

            e.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                "1:1044:1.71.2.6",
                                                SIMPUtils.getStackTrace(e) });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkPtoPOutputHandlers", e);
            throw e;
        }

        // Pull out any scoped ME set that we have configured
        SIBUuid8 singleScopedME = null;
        HashSet<SIBUuid8> definedScopedMEs = null;

        // If this is an alias that's scoped the message points of the target destination
        // then we need to pass these onto the target.
        if (_hasScopedMessagePoints)
        {

            if (_singleScopedQueuePointME != null)
                singleScopedME = _singleScopedQueuePointME;
            else
                definedScopedMEs = _scopedQueuePointMEs;

            // If the caller has fixed an ME, first check it's in our set
            if (fixedMEUuid != null)
            {
                if ((singleScopedME != null) && !fixedMEUuid.equals(singleScopedME))
                    result = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;
                else if ((definedScopedMEs != null) && !definedScopedMEs.contains(fixedMEUuid))
                    result = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;
            }

            // If we have a single ME scoped by this alias we may as well pass it to the target as
            // a fixed ME (which it must match as we've already checked that) to save
            // having to parse a HashSet everywhere for no need.
            if (singleScopedME != null)
                fixedMEUuid = singleScopedME;
        }

        // Look to the resolved destination to check output handlers as the
        // alias destination doesn't store any messages
        if (result == DestinationHandler.NOT_SET)
            result = _targetDestinationHandler.checkPtoPOutputHandlers(fixedMEUuid, definedScopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkPtoPOutputHandlers", new Integer(result));
        return result;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#notifyReceiveAllowed(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
 */
    @Override
    public void notifyReceiveAllowed(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyReceiveAllowed", new Object[] { destinationHandler });

        //If this alias destination inherits its receive allowed value, then
        //we must react to a change to the inherited value and tell any aliases
        //that inherit from us about the change
        if (_definition.isReceiveAllowed() == ExtendedBoolean.NONE)
        {
            getTarget().notifyReceiveAllowed(destinationHandler);
            notifyTargettingAliasesReceiveAllowed();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyReceiveAllowed");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isOrdered()
     */
    @Override
    public boolean isOrdered() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isOrdered");

        boolean returnValue = getTarget().isOrdered();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isOrdered", Boolean.valueOf(returnValue));

        return returnValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasLocal()
     */
    @Override
    public boolean hasLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "hasLocal");

        boolean hasLocal = false;

        // If this alias scopes the set of possible queue points then to qualify
        // as having a local one it must be in this set.
        if (_hasScopedMessagePoints)
        {
            if (_singleScopedQueuePointME != null)
            {
                if (_singleScopedQueuePointME.equals(getMessageProcessor().getMessagingEngineUuid()))
                    hasLocal = true;
            }
            else if ((_scopedQueuePointMEs != null) && _scopedQueuePointMEs.contains(getMessageProcessor().getMessagingEngineUuid()))
                hasLocal = true;
        }

        // Because we can't trust the scoped set being up-to-date with the
        // target destination (which could have removed an ME and hence queue point
        // since the alias was created) we still check that the target has
        // a local queue point.
        if (hasLocal || !_hasScopedMessagePoints)
            hasLocal = _targetDestinationHandler.hasLocal();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasLocal", Boolean.valueOf(hasLocal));

        return hasLocal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQHighMsgDepth()
     */
    //117505-Adding dummy method
    @Override
    public long getQHighMsgDepth() {

        return -1;
    }

}
