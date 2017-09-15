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

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationForeignDefinition;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.impl.ForeignDestination;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * @author caseyj
 * 
 *         This class represents foreign destinations
 */
public class ForeignDestinationHandler extends AbstractAliasDestinationHandler
{
    /** Trace for the component */
    private static final TraceComponent tc =
                    SibTr.register(
                                   ForeignDestinationHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /** The definition of this alias. */
    private DestinationForeignDefinition _definition;

    private JsDestinationAddress _foreignDestinationAddr = null;

    /**
     * Constructor
     */
    public ForeignDestinationHandler(
                                     DestinationForeignDefinition foreignDefinition,
                                     MessageProcessor messageProcessor,
                                     SIMPItemStream parentItemStream,
                                     DestinationHandler resolvedDestinationHandler,
                                     String busName)
    {
        super(messageProcessor, resolvedDestinationHandler, busName);

        if (tc.isEntryEnabled())
            SibTr.entry(tc, "ForeignDestinationHandler", new Object[] {
                                                                       foreignDefinition,
                                                                       messageProcessor, parentItemStream,
                                                                       resolvedDestinationHandler, busName });

        _definition = foreignDefinition;
        _foreignDestinationAddr = SIMPUtils.createJsDestinationAddress(_definition.getName(), null, getBus());

        createControlAdapter();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "ForeignDestinationHandler", new Object[] { this, _foreignDestinationAddr });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getContextProperty(java.lang.String)
     */
    @Override
    public Object getContextValue(String keyName)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getContextValue", keyName);

        Map context = _definition.getDestinationContext();

        Object property = context.get(keyName);

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultPriority");

        int pri = _definition.getDefaultPriority();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultPriority", new Integer(pri));

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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultReliability");

        Reliability rel = _definition.getDefaultReliability();

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getDescription");

        String desc = _definition.getDescription();

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getDestinationType");

        DestinationType dt = getTarget().getDestinationType();

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getMaxReliability");

        Reliability rel = _definition.getMaxReliability();

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "isOverrideOfQOSByProducerAllowed");

        boolean isOverrideOfQOSByProducerAllowed = _definition.isOverrideOfQOSByProducerAllowed();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "isOverrideOfQOSByProducerAllowed",
                       new Boolean(isOverrideOfQOSByProducerAllowed));

        return isOverrideOfQOSByProducerAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isAlias()
     */
    @Override
    public boolean isAlias()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isForeign()
     */
    @Override
    public boolean isForeign()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveAllowed()
     */
    @Override
    public boolean isReceiveAllowed()
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "isReceiveAllowed");

        boolean isReceiveAllowed = getTarget().isReceiveAllowed();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "isReceiveAllowed", new Boolean(isReceiveAllowed));

        return isReceiveAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSendAllowed()
     */
    @Override
    public boolean isSendAllowed()
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "isSendAllowed");

        boolean isSendAllowed = true;

        // We check first whether send is disallowed at the bus definition level. A 
        // setting at the bus level overrides a setting on the Foreign Destination
        // Definition.
        if (_sendAllowedOnTargetForeignBus != null)
            isSendAllowed = _sendAllowedOnTargetForeignBus.booleanValue();

        // If allowed at the bus level, check at the foreign dest def level
        if (isSendAllowed)
            isSendAllowed = _definition.isSendAllowed();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "isSendAllowed", new Boolean(isSendAllowed));

        return isSendAllowed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefinition()
     */
    @Override
    public BaseDestinationDefinition getDefinition()
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "getDefinition");

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "getDefinition", _definition);

        return _definition;
    }

    @Override
    public void updateDefinition(BaseDestinationDefinition destinationDefinition)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "updateDefinition", destinationDefinition);

        // At the moment always clone the destination definition 
        // passed in.  
        _definition = (DestinationForeignDefinition) destinationDefinition;

        if (tc.isEntryEnabled())
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
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAdapter");
        controlAdapter = new ForeignDestination(messageProcessor, this);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAdapter", controlAdapter);
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
        // Foreign destinations must map to destinations with the same name in
        // the other bus 
        return getName();
    }

    @Override
    public String getTargetBus()
    {
        // Foreign destinations must map to destinations with the same name in
        // the other bus 
        return getBus();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getForwardRoutingPath()
     */
    @Override
    public List getForwardRoutingPath()
    {
        // No op for foreign destinations
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultForwardRoutingPath()
     */
    @Override
    public SIDestinationAddress[] getDefaultForwardRoutingPath()
    {
        // No op for foreign destinations
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getReplyDestination()
     */
    @Override
    public JsDestinationAddress getReplyDestination()
    {
        // No op for foreign destinations
        return null;
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

        // As we're a foreign destination definition we have to set our own address
        // into the message so that it's seen by the other end of the link (so that
        // it can use it to forward the message correctly)
        outAddress = _foreignDestinationAddr;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRoutingDestinationAddr", outAddress);

        return outAddress;
    }

    /**
     * Check permission to access a Destination
     * 
     * @param secContext
     * @param busName
     * @param destinationName
     * @param operation
     * @return
     * @throws SICoreException
     */
    @Override
    public boolean checkDestinationAccess(
                                          SecurityContext secContext,
                                          OperationType operation)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationAccess",
                        new Object[] { secContext, operation });

        boolean allow = false;

        // This style of access check is against the proxy foreign dest.
        if (accessChecker.checkDestinationAccess(secContext,
                                                 getBus(),
                                                 getDefinition().getName(),
                                                 operation))
        {
            allow = true;
        }

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationAccess", new Boolean(allow));

        return allow;
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
                                            OperationType operation)
    {
        // We don't apply discriminator access checks to foreign destinations
        return true;
    }

    /**
     * Override the implementation in AbstractAliasDestinationHandler
     */
    @Override
    public boolean isTopicAccessCheckRequired()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.AbstractAliasDestinationHandler#notifyReceiveAllowed(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
     */
    @Override
    public void notifyReceiveAllowed(DestinationHandler destinationHandler)
    {
        //Do nothing for foreign destinations
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.AbstractAliasDestinationHandler#notifyReceiveAllowedRCD(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
     */
    @Override
    public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler)
    {
        //Do nothing for foreign destinations
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isOrdered()
     */
    @Override
    public boolean isOrdered() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "isOrdered");

        boolean returnValue = getTarget().isOrdered();
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "isOrdered", new Boolean(returnValue));

        return returnValue;
    }

    @Override
    public ConsumerManager chooseConsumerManager(SIBUuid12 gatheringTargetDestUuid, SIBUuid8 fixedME, HashSet<SIBUuid8> scopedMEs) throws SIResourceException
    {
        // We should never get here - we don't allow consumers on foreign destinations
        return null;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQHighMsgDepth()
 */
    @Override
    //117505-adding dummy method
    public long getQHighMsgDepth() {

        return -1;
    }
/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcherAndAttachCP(com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint,
 * com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
 */
    @Override
    //Thsi is dummy method. Liberty does not use this class.
    public ConsumerKey createSubscriptionConsumerDispatcherAndAttachCP(LocalConsumerPoint consumerPoint, ConsumerDispatcherState subState) throws SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException, SINonDurableSubscriptionMismatchException, SINotPossibleInCurrentConfigurationException, SIDestinationLockedException, SISessionDroppedException {
        return null;
    }

}
