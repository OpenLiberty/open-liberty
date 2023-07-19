/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.Map;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

/**
 *
 */
public class NettyHttpChannelConfig extends HttpChannelConfig {

    public NettyHttpChannelConfig(Map<String, Object> config) {
        super(config);
        this.useNetty = Boolean.TRUE;
    }

    public NettyHttpChannelConfig() {
        this.useNetty = Boolean.TRUE;
    }

}
