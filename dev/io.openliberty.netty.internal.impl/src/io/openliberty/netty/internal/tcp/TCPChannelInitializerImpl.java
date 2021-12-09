/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.ChannelInitializerWrapper;

/**
 * Registers channel handlers which implement various TCP configuration options. Handlers are
 * initialized with the current values of the registered TCPConfiguration.
 * 
 * Currently these handlers implement the following tcpOptions: 
 * inactivityTimeout, maxOpenConnections, addressExcludeList, addressIncludeList, hostNameExcludeList, hostNameIncludeList
 */
public class TCPChannelInitializerImpl extends ChannelInitializerWrapper {

    TCPConfigurationImpl config;
    
    public TCPChannelInitializerImpl(BootstrapConfiguration config) {
        this.config = (TCPConfigurationImpl) config;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        if (config.getInactivityTimeout() > 0) {
            channel.pipeline().addLast(new InactivityTimeoutHandler(0, 0, config.getInactivityTimeout(), TimeUnit.MILLISECONDS));
        }
        MaxOpenConnectionsHandler maxHandler = new MaxOpenConnectionsHandler(config.getMaxOpenConnections());
        if (config.getAccessLists() != null) {
            AccessListHandler includeHandler = new AccessListHandler(config.getAccessLists());
            channel.pipeline().addLast(includeHandler);
        }
        channel.pipeline().addLast(maxHandler);
    }

}
