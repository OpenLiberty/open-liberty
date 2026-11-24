/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.inbound;

import java.util.concurrent.CompletionStage;

import com.ibm.ws.http.channel.internal.h2spool.H2SealedBody;

import io.netty.channel.ChannelHandlerContext;

/** Dispatcher ownership contract for a completed HTTP/2 body. */
public interface H2SealedBodyInput {
    void nettyConfigureSealedBody(H2SealedBody body, ChannelHandlerContext context,
                                  String contentEncoding, Runnable onClose);

    CompletionStage<Void> closeSealedBodyAsync();
}
