/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.server;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.error;
import static com.ibm.ws.sib.utils.ras.SibTr.exception;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;

import java.util.List;
import java.util.Optional;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.jfapchannel.richclient.framework.impl.RichClientFramework;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * An abstraction of the essential Channel Framework / TCP Channel network
 * services wrapped into a JetStream friendly class. In essence, this class
 * attempts to hide the complexities required to play in the channel
 * framework from the rest of the communications code.
 */
@SuppressWarnings("unchecked")
public abstract class ServerConnectionManager
{
    private static final TraceComponent tc = SibTr.register(ServerConnectionManager.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    private static boolean initialised = false;
    private static volatile OutboundConnectionTracker connectionTracker;

    /**
     * Initialises the channel framework. This must be called before any
     * other method may be invoked in this class. Don't worry - you can call
     * this multiple times without anything bad happening.
     */
    public static void initialise(ChannelFramework chfw)
    {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "initialise");

        if (!initialised)
        {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "not previously initialised");
            try
            {
                // Create the maintainer of the configuration.
                Framework framework = new RichClientFramework(chfw);
                // Extract the chain reference.
                connectionTracker = new OutboundConnectionTracker(framework);
            } catch (Exception e)
            {
                FFDCFilter.processException
                                (e, "com.ibm.ws.sib.jfapchannel.ServerConnectionManager.{}",
                                 JFapChannelConstants.SRVRCONNMGR_INITIALISE_01);

                error(tc, "EXCP_DURING_INIT_SICJ0003", new Object[] {ServerConnectionManager.class, e});
                if (isAnyTracingEnabled() && tc.isEventEnabled()) exception(tc, e);
            }
            initialised = true;
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "initialise");
    }

    /**
     * Obtains a list of active outbound ME to ME conversations in this JVM.
     * 
     * @return Returns a list of Conversations
     */
    public static List<Conversation> getActiveOutboundMEtoMEConversations() {
        return Optional.ofNullable(connectionTracker)
                .map(OutboundConnectionTracker::getAllOutboundConversations)
                .orElse(null);
    }
}
