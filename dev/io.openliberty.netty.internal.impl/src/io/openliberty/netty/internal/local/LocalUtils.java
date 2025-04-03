/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.local;

import java.util.Map;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;
import io.openliberty.netty.internal.impl.NettyConstants;

public class LocalUtils {

    private static final TraceComponent tc = Tr.register(LocalUtils.class, NettyConstants.NETTY_TRACE_NAME, null);
            //NettyConstants.BASE_BUNDLE);

    /**
     * Create a {@link ServerBootstrapExtended} for local channels that are not
     * based on host address/port addressing but can use {@link LocalAddress}
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return the bootstrap to which a child handler can be added to deal with
     *         protocol specific tasks
     * @throws NettyException
     */
    public static ServerBootstrapExtended createLocalBootstrap(NettyFrameworkImpl framework,
            Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, true);

        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(framework.getParentGroup(), framework.getChildGroup());
        bs.channel(LocalServerChannel.class);
        bs.applyConfiguration(config);
        return bs;
    }


    /**
     * Create a {@link BootstrapExtended} for outbound local channels TODO GDH - not
     * found where we will use this from yet in WOLA.
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createLocalBootstrapOutbound(NettyFrameworkImpl framework,
            Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, false);

        BootstrapExtended bs = new BootstrapExtended();
        bs.group(framework.getChildGroup());
        bs.channel(LocalChannel.class);
        bs.applyConfiguration(config);
        return bs;
    }

    
    /**
     * 
     * @param channel
     */
    public static void logChannelStopped(Channel channel) {
        Tr.info(tc, "stopped" + channel.toString());
    }

    /**
     * 
     * @param channel
     */
    public static void logChannelStarted(Channel channel) {
        Tr.info(tc, "started" + channel.toString());
    }
}
