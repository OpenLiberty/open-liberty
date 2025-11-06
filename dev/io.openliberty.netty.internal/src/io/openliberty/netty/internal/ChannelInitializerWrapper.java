/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * Wrapper for {@link ChannelInitializer} implementations
 *
 */
public abstract class ChannelInitializerWrapper extends ChannelInitializer<Channel> {

    /**
     * invoke initChannel(channel) on this object
     *
     * @param channel
     * @throws Exception
     */
    public void init(Channel channel) throws Exception {
        this.initChannel(channel);
    }
}
