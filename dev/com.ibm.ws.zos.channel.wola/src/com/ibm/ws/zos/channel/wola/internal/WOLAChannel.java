/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.wola.internal.natv.RegistryToken;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * WOLA Channel implementation.
 */
public class WOLAChannel implements InboundChannel {

    private static final TraceComponent tc = Tr.register(WOLAChannel.class);

    /**
     * Configuration data for this channel.
     */
    private final ChannelData channelConfigData;

    /**
     * Discrimination process reference assigned by the framework specifically for this channel.
     */
    private DiscriminationProcess discriminationProcess;

    /**
     * Discriminator reference. Holds discrimination logic for accessing this channel.
     */
    private final Discriminator discriminator;

    /**
     * Cached registry token that contains the address of the WOLA group shared memory area.
     * The token is obtained when we attach to the area. It is passed back down on the detach.
     */
    private RegistryToken bboashrAttachToken;

    /**
     * Cached registry token that contains the ENQ used to advertise the WOLA server's
     * presence to prospective WOLA clients. It must be passed back on the deadvertise.
     */
    private RegistryToken advertiseToken;

    /**
     * Cached registry token that contains the address of the WOLA server's BBOARGE.
     * It must be passed back on deactivateWolaRegistration.
     */
    private RegistryToken bboargeToken;

    /**
     * Constructor.
     */
    public WOLAChannel(ChannelData data) {
        channelConfigData = data;
        discriminator = new WOLADiscriminator(this);
    }

    /**
     * Retrieves this channel's current config data.
     *
     * @return
     */
    public ChannelData getChannelConfigData() {
        return channelConfigData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new WolaConnLink();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ChannelException {

        WOLAConfig wolaConfig = WOLAChannelFactoryProvider.getInstance().getWOLAConfig();
        String wolaGroup = wolaConfig.getWolaGroup();
        String wolaName2 = wolaConfig.getWolaName2();
        String wolaName3 = wolaConfig.getWolaName3();
        boolean allowCicsTaskUserId = wolaConfig.allowCicsTaskUserIdPropagation();

        bboashrAttachToken = WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().attachToWolaGroupSharedMemoryArea(wolaGroup);

        WOLAChannelFactoryProvider.getInstance().setAttachedToWolaGroupSharedMemoryArea(true);

        bboargeToken = WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().activateWolaRegistration(wolaGroup, wolaName2, wolaName3, allowCicsTaskUserId);

        try {
            advertiseToken = WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().advertiseWolaServer(wolaGroup, wolaName2, wolaName3);
        } catch (Exception e) {
            if (e.toString().indexOf("Unable to acquire resource") >= 0) {
                Tr.error(tc, "WOLA_SERVER_UNAVAILABLE", wolaGroup, wolaName2, wolaName3);
            }
            throw new ChannelException(e.toString());
        }
        Tr.info(tc, "WOLA_SERVER_REGISTERED", new Object[] { wolaGroup, wolaName2, wolaName3 });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(long millisec) throws ChannelException {

        synchronized (this) {
            if (advertiseToken != null) {
                // Deadvertize, then null out the registry token.  This is to prevent double-deadvertising
                // in the event CFW calls stop on this channel more than once (which can happen, apparently).
                WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().deadvertiseWolaServer(advertiseToken);
                advertiseToken = null;
            }

            if (bboargeToken != null) {
                WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().deactivateWolaRegistration(bboargeToken);
                bboargeToken = null;
            }

            if (bboashrAttachToken != null) {
                WOLAChannelFactoryProvider.getInstance().setAttachedToWolaGroupSharedMemoryArea(false);
                boolean requestsCompeted = WOLAChannelFactoryProvider.getInstance().waitForRequestsToComplete();
                if (requestsCompeted == true) {
                    // Detach, then null out the registry token.  This is to prevent double-detaching
                    // in the event CFW calls stop on this channel more than once (which can happen, apparently).
                    WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().detachFromWolaGroupSharedMemoryArea(bboashrAttachToken);
                }
                bboashrAttachToken = null;
            }

            // The channel framework can stop and quiesce channels with this method.  If it's
            // a quiesce, the 'millisec' parameter will be greater than 0.  In th event of a
            // quiesce, it appears that the channel framework will 'wait' until the channel
            // tells it that it is fully stopped, before it issues a second stop with a
            // millisec value of 0.  It's after this second stop that the channel framework will
            // move on and tell the OSGi framework that it is stopped.
            //
            // So if we're behaving properly, we'll see the quiesce, do our thing, and then tell
            // channel framework we're done by firing an event.  Being done means that all of
            // the connections are done processing data.  But we've already disconnected from
            // the shared memory, etc!  I guess we're done, lets tell channel framework that
            // we're really done.
            //
            // Why has this worked for so long?  I can only assume that the channel framework
            // OSGi service was stopping after the WOLA OSGi service, because the WOLA OSGi
            // service will stop all the channels and chains when it stops.  Now apparently
            // the channel framework service is stopping first, so we're getting stuck.
            if (millisec > 0L) {
                EventEngine e = WOLAChannelFactoryProvider.getInstance().getEventEngine();
                if (e != null) {
                    Event event = e.createEvent(ChannelFramework.EVENT_STOPCHAIN);
                    event.setProperty(ChannelFramework.EVENT_CHANNELNAME, channelConfigData.getExternalName());
                    e.postEvent(event);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws ChannelException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws ChannelException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return channelConfigData.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getApplicationInterface() {
        // This channel is at the top of the chain. Return null.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getDeviceInterface() {
        return LocalCommServiceContext.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(ChannelData cc) {
        // TODO.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiscriminationProcess getDiscriminationProcess() {
        return discriminationProcess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        discriminationProcess = dp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getDiscriminatoryType() {
        // This is the top of the chain. Return null.
        return null;
    }
}
