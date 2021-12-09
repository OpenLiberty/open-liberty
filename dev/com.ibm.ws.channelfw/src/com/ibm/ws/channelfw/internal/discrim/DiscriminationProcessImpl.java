/*******************************************************************************
 * Copyright (c) 2005, 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal.discrim;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * Implementation of DiscriminationProcess. Essentially contains all the
 * logic to perform discriminate against possible channels on the application
 * side of
 * an inbound connection.
 */
public class DiscriminationProcessImpl implements DiscriminationGroup {

    /**
     * Trace Component
     */
    private static final TraceComponent tc = Tr.register(DiscriminationProcessImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * the class used to discriminate
     */
    private final Class<?> discriminantClass;

    /**
     * algorithm to use to call and choose discriminators
     */
    private DiscriminationAlgorithm discriminationAlgorithm = null;

    /**
     * status of the current DiscriminationProcess. STARTED||STOPPED.
     */
    private int status = STOPPED;

    /**
     * status started, this DiscriminationProcess is in the channel and cannot be
     * modified.
     */
    private static final int STARTED = 1;

    /**
     * Status stopped, DiscriminationProcess may be modified.
     */
    private static final int STOPPED = 2;

    /**
     * To lock the index when creating new DPs
     */
    private static Object indexLock = new Object();

    /**
     * primary index to be incremented for each DiscriminationProcess.
     */
    private static int primaryIndex = 0;

    /**
     * the index of this entry;
     */
    private int myIndex;

    /**
     * channel name this process is bound to.
     */
    private String name = null;

    /**
     * A linked list wrapper around a discriminator
     */
    private DiscriminatorNode discriminators;

    /**
     * This holds the list of the discriminators to pass to the next
     * discriminationProcess.
     */
    private List<Discriminator> discAL = null;

    /**
     * List containing the name of the following channels.
     */
    private Channel[] channelList = null;
    /** Flag on whether this DP has changed or not */
    private boolean changed = false;

    /**
     * Constructor for DiscriminationProcessImpl. This Constructor is for
     * those channels that do not yet have a DiscriminationProcessImpl
     * 
     * @param discClass
     *            A class representing the type of discriminatory
     *            data which all the discriminators added to this group
     *            must be able to discriminate on.
     * @param channelName
     */
    public DiscriminationProcessImpl(Class<?> discClass, String channelName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ctor, discClass=" + discClass + ", channelName=" + channelName);
        }
        this.discriminators = null;
        this.discAL = new ArrayList<Discriminator>(0);
        this.discriminantClass = discClass;
        // Init with fail algorithm. This covers startup issues with device side
        // channels.
        this.discriminationAlgorithm = new FailureDiscriminatorAlgorithm();
        this.status = STOPPED;
        this.name = channelName;
        synchronized (indexLock) {
            this.myIndex = primaryIndex++;
        }
    }

    /**
     * Constructor for DiscriminationProcessImpl. This constructor is for channels
     * which already have a discriminationprocessImpl attached. this allows a
     * DiscriminationProcessImpl to be created from the already existing one.
     * 
     * @param discClass
     * @param dg
     */
    public DiscriminationProcessImpl(Class<?> discClass, DiscriminationGroup dg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ctor, discClass=" + discClass + ", DiscriminatorGroup=" + dg);
        }
        this.discriminators = null;
        this.discAL = new ArrayList<Discriminator>(0);
        this.discriminantClass = discClass;
        // Init with fail algorithm. This covers startup issues with device side
        // channels.
        this.discriminationAlgorithm = new FailureDiscriminatorAlgorithm();
        this.status = STOPPED;
        this.name = dg.getChannelName();
        DiscriminatorNode dn = (DiscriminatorNode) dg.getDiscriminatorNodes();
        buildDiscriminatorNodes(dn);
    }

    /**
     * Copy this DiscriminatorNode list.
     * 
     * @param node
     */
    private void buildDiscriminatorNodes(DiscriminatorNode node) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "buildDiscriminatorNodes: " + node);
        }
        DiscriminatorNode dn = node;
        if (dn == null) {
            return;
        }
        DiscriminatorNode newDN = null, lastDN = null;
        discriminators = new DiscriminatorNode(dn.disc, dn.weight, null, null);
        discAL.add(dn.disc);
        Channel chan = dn.disc.getChannel();
        addChannel(chan);
        newDN = discriminators;
        lastDN = discriminators;
        while (dn.next != null) {
            dn = dn.next;
            newDN = new DiscriminatorNode(dn.disc, dn.weight, null, lastDN);
            lastDN.next = newDN;
            lastDN = newDN;
            discAL.add(dn.disc);
            chan = dn.disc.getChannel();
            addChannel(chan);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.DiscriminationProcess#discriminate(com.ibm.wsspi
     * .channelfw.VirtualConnection, java.lang.Object,
     * com.ibm.wsspi.channelfw.ConnectionLink)
     */
    @Override
    public int discriminate(VirtualConnection vcx, Object discrimData, ConnectionLink ccl) throws DiscriminationProcessException {
        InboundVirtualConnection vc = null;
        if (vcx instanceof InboundVirtualConnection) {
            vc = (InboundVirtualConnection) vcx;
        }

        if (discriminationAlgorithm == null) {
            DiscriminationProcessException e = new DiscriminationProcessException("No Discriminators in this group or the group was not properly started");
            FFDCFilter.processException(e, getClass().getName() + ".discriminate", "202", this, new Object[] { vc });
            throw e;
        }
        if (this.changed) {
            // if we have changed the list of channels since the last discrim attempt
            // on
            // this vc, clear the previous status information
            if (null != vc) {
                vc.setDiscriminatorStatus(null);
                vc.setDiscriminationGroup(null);
            }
            this.changed = false;
        }

        if (vc == null && discriminationAlgorithm instanceof SingleDiscriminatorAlgorithm)
            return ((SingleDiscriminatorAlgorithm) discriminationAlgorithm).discriminate(vcx, discrimData, ccl);
        else
            return discriminationAlgorithm.discriminate(vc, discrimData, ccl);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.DiscriminationProcess#discriminate(com.ibm.wsspi
     * .channelfw.VirtualConnection, com.ibm.wsspi.channelfw.ConnectionLink,
     * java.lang.String)
     */
    @Override
    public int discriminate(VirtualConnection vc, ConnectionLink currentChannel, String inputChannelName) {
        Channel channel = null;
        String channelName = null;
        String matchString = (inputChannelName + ChannelDataImpl.CHILD_STRING);
        int result = FAILURE;
        // Iterate the channels of the current list.
        for (int i = 0; channelList != null && i < channelList.length; i++) {
            channel = channelList[i];
            // Find a channel that starts with the name passed in.
            // Note: Runtime channels are children channel data objects with names
            // like name_CFINTERNAL_CHILD_0
            // This is kept hidden from users.
            channelName = channel.getName();
            if (channelName != null && channelName.startsWith(matchString)) {
                // Found the channel. Connect the links.
                ConnectionLink link = channel.getConnectionLink(vc);
                currentChannel.setApplicationCallback(link);
                link.setDeviceLink(currentChannel);
                result = SUCCESS;
                break;
            }
        }
        return result;
    }

    /**
     * Adds a discriminator to the group. Attempts to add the same discriminator
     * more than once are ignored. It is an error to attempt to add a
     * discriminator which is not able to deal with the groups type of
     * discriminatory data. A class cast exception is thrown if this is
     * attempted.
     * 
     * @param d
     *            The discriminator to add.
     * @param weight
     *            The discrimintor weight. Must be greater than 0.
     * @throws DiscriminationProcessException
     */
    @Override
    public void addDiscriminator(Discriminator d, int weight) throws DiscriminationProcessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addDiscriminator: " + d + " weight=" + weight);
        }
        if (status == STARTED) {
            DiscriminationProcessException e = new DiscriminationProcessException("Should not add to DiscriminationGroup while started!");
            FFDCFilter.processException(e, getClass().getName() + ".addDiscriminator", "239", this, new Object[] { d });
            throw e;
        }
        if (weight < 0) {
            DiscriminationProcessException e = new DiscriminationProcessException("Invalid weight for discriminator, " + weight);
            FFDCFilter.processException(e, getClass().getName() + ".addDiscriminator", "260", this, new Object[] { Long.valueOf(weight) });
            throw e;
        }
        if (!discAL.contains(d)) {
            if (d.getDiscriminatoryDataType().isAssignableFrom(discriminantClass)) {
                if (d.getChannel() == null || d.getChannel().getName() == null) {
                    DiscriminationProcessException e = new DiscriminationProcessException("Discriminator does not have channel or its channel has no name");
                    FFDCFilter.processException(e, getClass().getName() + ".addDiscriminator", "273", this, new Object[] { d });
                    throw e;
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add discriminator " + d.getChannel().getName());
                }
                addDiscriminatorNode(new DiscriminatorNode(d, weight));
                discAL.add(d);
                Channel chan = d.getChannel();
                addChannel(chan);
                this.changed = true;
            } else {
                ClassCastException e = new ClassCastException();
                FFDCFilter.processException(e, getClass().getName() + ".addDiscriminator", "292", this, new Object[] { d });
                throw e;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Same discriminator added twice?");
            }
        }
    }

    /**
     * Determine if the input discriminator already exists in this group.
     * 
     * @param d
     *            - discriminator to search for.
     * @return true or false
     */
    public boolean containsDiscriminator(Discriminator d) {
        return discAL.contains(d);
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Index: ").append(getIndex());
        sb.append(" Discriminators:\n");
        DiscriminatorNode tempNode = discriminators;
        while (null != tempNode) {
            sb.append("\t").append(tempNode.disc.getChannel().getName());
            sb.append(" weight=").append(tempNode.disc.getWeight());
            tempNode = tempNode.next;
        }
        return sb.toString();
    }

    /**
     * add a discriminatorNode to the linked list.
     * 
     * @param dn
     */
    private void addDiscriminatorNode(DiscriminatorNode dn) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addDiscriminatorNode", "weight=" + dn.weight);
        }
        if (discriminators == null) {
            // add it as the first node
            discriminators = dn;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "addDiscriminatorNode");
            }
            return;
        }
        DiscriminatorNode thisDN = discriminators;
        if (thisDN.weight > dn.weight) {
            // add it as the first node
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding disc first in list");
            }
            thisDN.prev = dn;
            dn.next = thisDN;
            discriminators = dn;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "addDiscriminatorNode");
            }
            return;
        }
        DiscriminatorNode lastDN = discriminators;
        while (thisDN.next != null) {
            // somewhere in the middle
            lastDN = thisDN;
            thisDN = thisDN.next;
            if (thisDN.weight > dn.weight) {
                // put it in the middle
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding disc before " + thisDN.disc.getChannel().getName());
                }
                thisDN.prev = dn;
                dn.next = thisDN;
                lastDN.next = dn;
                dn.prev = lastDN;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "addDiscriminatorNode");
                }
                return;
            }
        }
        // guess its at the end
        thisDN.next = dn;
        dn.prev = thisDN;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addDiscriminatorNode");
        }
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup#removeDiscriminator
     * (com.ibm.wsspi.channelfw.Discriminator)
     */
    @Override
    public void removeDiscriminator(Discriminator d) throws DiscriminationProcessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "removeDiscriminator", d);
        }
        try {
            if (status == STARTED) {
                DiscriminationProcessException e = new DiscriminationProcessException("Should not remove form DiscriminationGroup while started!");
                FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminator", "401", this, new Object[] { d });
                throw e;
            }
            // remove it from the list
            if (!discAL.remove(d)) {
                NoSuchElementException e = new NoSuchElementException("Discriminator does not exist, " + d.getChannel().getName());
                FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminator", "410", this, new Object[] { d });
                throw e;
            }
            this.changed = true;
            String chanName = d.getChannel().getName();
            if (channelList == null) {
                NoSuchElementException e = new NoSuchElementException("No Channel's exist, " + chanName);
                FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminator", "422", this, new Object[] { d });
                throw e;
            }
            Channel[] oldList = channelList;
            channelList = new Channel[oldList.length - 1];
            for (int i = 0, j = 0; i < oldList.length; i++) {
                String tempName = oldList[i].getName();
                if (tempName != null && !(tempName.equals(chanName))) {
                    if (j >= oldList.length) {
                        NoSuchElementException e = new NoSuchElementException("Channel does not exist, " + d.getChannel().getName());
                        FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminator", "440", this, new Object[] { d });
                        throw e;
                    }
                    channelList[j++] = oldList[i];
                } else if (chanName == null) {
                    DiscriminationProcessException e = new DiscriminationProcessException("Channel does not have a name associated with it, " + oldList[i]);
                    FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminator", "454", this, new Object[] { oldList[i] });
                    throw e;
                }
            }
            // remove it from the node list
            removeDiscriminatorNode(d);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "removeDiscriminator");
            }
        }
    }

    /**
     * remove the discriminatorNode from the linkedList.
     * 
     * @param d
     * @throws DiscriminationProcessException
     */
    private void removeDiscriminatorNode(Discriminator d) throws DiscriminationProcessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeDiscriminatorNode", d);
        }
        try {
            if (d == null) {
                DiscriminationProcessException e = new DiscriminationProcessException("Can't remove a null discriminator");
                FFDCFilter.processException(e, getClass().getName() + ".removeDiscriminatorNode", "484", this);
                throw e;
            }
            if (discriminators.disc.equals(d)) {
                // removing the first discriminator
                discriminators = discriminators.next;
                if (discriminators != null) {
                    discriminators.prev = null;
                }
                return;
            }
            // search through the list of discriminators
            DiscriminatorNode thisDN = discriminators.next, lastDN = discriminators;
            while (thisDN.next != null) {
                if (thisDN.disc.equals(d)) {
                    thisDN.next.prev = lastDN;
                    lastDN.next = thisDN.next;
                    return;
                }
                // somewhere in the middle
                lastDN = thisDN;
                thisDN = thisDN.next;
            }
            if (thisDN.disc.equals(d)) {
                // found it!
                lastDN.next = null;
                return;
            }
            // Does not exist?
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removeDiscriminatorNode: not found");
            }
            throw new NoSuchElementException();
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "removeDiscriminatorNode");
            }
        }
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup#getDiscriminators
     * ()
     */
    @Override
    public List<Discriminator> getDiscriminators() {
        return this.discAL;
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup#getDiscriminatorNodes
     * ()
     */
    @Override
    public Object getDiscriminatorNodes() {
        return this.discriminators;
    }

    /*
     * @seecom.ibm.ws.channelfw.internal.discrim.DiscriminationGroup#
     * getDiscriminationAlgorithm()
     */
    @Override
    public DiscriminationAlgorithm getDiscriminationAlgorithm() {
        return this.discriminationAlgorithm;
    }

    /*
     * @seecom.ibm.ws.channelfw.internal.discrim.DiscriminationGroup#
     * setDiscriminationAlgorithm
     * (com.ibm.ws.channelfw.internal.discrim.DiscriminationAlgorithm)
     */
    @Override
    public void setDiscriminationAlgorithm(DiscriminationAlgorithm da) {
        this.discriminationAlgorithm = da;
    }

    /**
     * Start this DiscriminatorProcess.
     */
    @Override
    public void start() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Started discriminator list " + discAL + "with size" + discAL.size());
        }
        if (discAL.size() > 1) {
            rebuildDiscriminatorList();
            discriminationAlgorithm = new MultiDiscriminatorAlgorithm(this);
        } else if (discAL.size() == 1) {
            discriminationAlgorithm = new SingleDiscriminatorAlgorithm(this);
        } else {
            discriminationAlgorithm = new FailureDiscriminatorAlgorithm();
        }
        status = STARTED;
    }

    /**
     * @return the channel name associated with this DiscriminationProcess.
     */
    @Override
    public String getChannelName() {
        return this.name;
    }

    /**
     * Rebuild the array list from the linked list.
     * 
     */
    private void rebuildDiscriminatorList() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "rebuildDiscriminatorList");
        }
        discAL.clear();
        DiscriminatorNode dn = discriminators;
        discAL.add(dn.disc);
        dn = dn.next;
        while (dn != null) {
            discAL.add(dn.disc);
            dn = dn.next;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "rebuildDiscriminatorList");
        }
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(DiscriminationGroup o) {
        if (o == null || !(o instanceof DiscriminationProcessImpl)) {
            return -1;
        }
        return o.hashCode() - hashCode();
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || !(o instanceof DiscriminationProcessImpl)) {
            return false;
        }
        return hashCode() == o.hashCode();
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.myIndex;
    }

    /**
     * my index for comparisons
     * 
     * @return index
     */
    int getIndex() {
        return this.myIndex;
    }

    /**
     * Add a channel to the channel list to be searched.
     * 
     * @param chan
     */
    private void addChannel(Channel chan) {
        if (channelList == null) {
            channelList = new Channel[1];
            channelList[0] = chan;
        } else {
            Channel[] oldList = channelList;
            channelList = new Channel[oldList.length + 1];
            System.arraycopy(oldList, 0, channelList, 0, oldList.length);
            channelList[oldList.length] = chan;
        }
    }

}

/**
 * 
 * Linked List class to hold Discriminators.
 */
class DiscriminatorNode {

    /**
     * the discriminator
     */
    public Discriminator disc = null;

    /**
     * discriminator weight
     */
    public int weight;

    /**
     * pointer to next
     */
    public DiscriminatorNode next = null;

    /**
     * pointer to last
     */
    public DiscriminatorNode prev = null;

    /**
     * Constructor for a linked list node.
     * 
     * @param d
     * @param weight
     * @param next
     * @param last
     */
    public DiscriminatorNode(Discriminator d, int weight, DiscriminatorNode next, DiscriminatorNode last) {
        this.disc = d;
        this.weight = weight;
        this.next = next;
        this.prev = last;
    }

    /**
     * Constructor for linked list node with no next/last
     * 
     * @param d
     * @param weight
     */
    public DiscriminatorNode(Discriminator d, int weight) {
        this.disc = d;
        this.weight = weight;
    }

}
