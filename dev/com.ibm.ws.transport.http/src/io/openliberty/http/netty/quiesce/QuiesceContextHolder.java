/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.quiesce;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * Holds a reference to the current {@link Channel} so that quiesce tasks can access
 * the pipeline and determine the appropriate action.
 * 
 * This is a static context holder used by the QuiesceHandler and QuiesceStrategy to
 * retrieve the active channel at quiesce time.
 */
public final class QuiesceContextHolder {

    /**
     * The currently active channel for this connection. Assumed to be set once and
     * remain stable until channel close.
     */
    private static volatile Channel currentChannel;

    /**
     * Sets the currently active channel.
     *
     * @param channel The netty {@link Channel} for this connection.
     */
    public static void setChannel(Channel channel) {
        currentChannel = channel;
    }

    /**
     * Gets the currently active channel.
     *
     * @return The {@link Channel}, or null if not set.
     */
    public static Channel getChannel() {
        return currentChannel;
    }

    private QuiesceContextHolder() {}
}