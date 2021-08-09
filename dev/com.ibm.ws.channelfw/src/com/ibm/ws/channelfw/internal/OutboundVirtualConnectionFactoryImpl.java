/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;

/**
 * Implementation of a factory that will create outbound virtual connections
 * for a specific chain definition.
 * 
 */
public class OutboundVirtualConnectionFactoryImpl implements VirtualConnectionFactory {
    /**
     * TraceComponent
     */
    private static final TraceComponent tc = Tr.register(OutboundVirtualConnectionFactoryImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME,
                                                         ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * ChannelFramework instance that contains this factory.
     */
    private ChannelFrameworkImpl cf = null;

    /**
     * reference count of how many have grabbed a copy of this vcf
     */
    private int refCount = 0;

    /**
     * name of this vcf (chain name)
     */
    private String name = null;

    /**
     * Outbound chain used by this factory.
     */
    private Chain outboundChain = null;

    /**
     * Channels contained within the chain used by this factory.
     */
    private Channel[] chans = null;

    /**
     * Constructor for the outbound VC factory for the given chain.
     * 
     * @param chainData
     * @param framework
     * @throws ChannelException
     * @throws ChainException
     */
    public OutboundVirtualConnectionFactoryImpl(ChainData chainData, ChannelFrameworkImpl framework) throws ChannelException, ChainException {
        this.name = chainData.getName();
        this.refCount = 1;
        this.cf = framework;
        this.cf.initChainInternal(chainData);
        this.outboundChain = this.cf.getRunningChain(getName());
        if (getChain() == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to create factory; name=" + getName());
            }
            throw new InvalidChainNameException("Invalid chain when trying to access via the channel framework");
        }
        this.chans = getChain().getChannels();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created factory; " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#createConnection()
     */
    public VirtualConnection createConnection() throws ChannelException, ChainException {
        OutboundVirtualConnectionImpl vc = new OutboundVirtualConnectionImpl();
        if (getChain().getState() != RuntimeState.STARTED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Starting outbound chain, " + getName());
            }
            this.cf.startChainInternal(getChain().getChainData());
            // Reset the reference to the new outboundChain.
            this.outboundChain = this.cf.getRunningChain(getName());
            // Handle issues where the refCount may have gone negative due
            // to too many VCF.destroy calls from the same VCF.
            if (getRefCount() <= 0) {
                this.refCount = 1;
            }
        }
        ConnectionLink[] links = new ConnectionLink[chans.length];
        links[0] = chans[0].getConnectionLink(vc);
        for (int i = 1; i < chans.length; i++) {
            try {
                links[i] = chans[i].getConnectionLink(vc);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".createConnection", "125", new Object[] { this, chans[i] });
                // Channel implementation had an error.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error getting conn link from " + chans[i].getName() + ", " + e.getMessage());
                }
                throw new ChannelException(e.getMessage());
            }
            links[i - 1].setDeviceLink(links[i]);
            links[i].setApplicationCallback(links[i - 1]);
        }
        // do the single cast here instead of many times later
        vc.setConnectionLink((OutboundConnectionLink) links[0]);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Putting name of chain into vc statemap: " + getName());
        }
        // Set the name of the chain into the statemap of the vc.
        // (Currently used on Z in XMEM channel)
        vc.getStateMap().put("ChainName", getName());

        return vc;
    }

    /**
     * Increment the reference to the users of the outbound chain.
     * This is called by the framework when a request is made for an
     * existing VCF.
     */
    public void incrementRefCount() {
        synchronized (this.cf) {
            this.refCount++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Increased refcount; " + this);
            }
        }
    }

    /**
     * Decrement the reference to the users of the outbound chain.
     */
    public void decrementRefCount() {
        synchronized (this.cf) {
            this.refCount--;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decreased refcount; " + this);
            }
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#destroy()
     */
    public void destroy() throws ChainException, ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy; " + this);
        }
        synchronized (this.cf) {
            // Verify that the VCF hasn't already been destroyed.
            if (0 == getRefCount()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Virtual connection factory already destroyed");
                }
                throw new ChainException("Virtual connection factory already destroyed");
            }
            decrementRefCount();
            // Check if there are no more users of the chain.
            if (0 == getRefCount()) {
                // Verify the chain was ever used beyond init. That is a call was
                // made to get a VCF, but never a VC. Note, outbound chains can
                // never be in QUIESCED state.
                if (getChain().getState() == RuntimeState.STARTED) {
                    this.cf.stopChainInternal(getChain(), 0);
                }
                this.cf.destroyChainInternal(getChain());
            }
        } // end sync block
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * This method is only ever called from the framework's clear method which is
     * only
     * called when the framework is being destroyed or when automated tests are
     * clearing
     * out the frameworks config.
     */
    public void destroyInternal() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroyInternal; " + this);
        }
        // Stop and destroy the chain. Ignore exceptions
        try {
            this.cf.stopChainInternal(getChain(), 0);
            this.cf.destroyChainInternal(getChain());
        } catch (ChannelException e) {
            // No FFDC required
        } catch (ChainException e) {
            // No FFCD required
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroyInternal");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#getType()
     */
    public FlowType getType() {
        return FlowType.OUTBOUND;
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * Accessor method for the number of references exist to the outbound chain.
     * 
     * @return refCount
     */
    public int getRefCount() {
        return this.refCount;
    }

    /**
     * Accessor method for the outbound chain in this factory.
     * 
     * @return outbound chain of factory
     */
    public Chain getChain() {
        return this.outboundChain;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return ("Outbound VCF: chain = " + getName() + ", refCount = " + getRefCount());
    }

}
