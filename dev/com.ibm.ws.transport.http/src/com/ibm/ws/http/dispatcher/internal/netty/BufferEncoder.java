/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;

/**
 *
 */
public class BufferEncoder BufferEncoder extends MessageToByteEncoder<WsByteBuffer> {

    @Override
    public void encode(ChannelHandler context, WsByteBuffer message, ByteBuf out) throws Exception {
        out.writeBytes(message.getWrappedByteBuffer());
    }

}
