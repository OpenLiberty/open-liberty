/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tls;

import java.util.Map;

import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;

public interface NettyTlsProvider {

    public SslHandler getOutboundSSLContext(Map<String, Object> sslOptions, String host, String port, Channel channel);

    public SslHandler getInboundSSLContext(Map<String, Object> sslOptions, String host, String port, Channel channel);

    public SslHandler getInboundALPNSSLContext(Map<String, Object> sslOptions, String host, String port, Channel channel);
}
