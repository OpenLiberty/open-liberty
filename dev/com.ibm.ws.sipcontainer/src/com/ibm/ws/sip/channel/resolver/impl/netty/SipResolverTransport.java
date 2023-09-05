/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl.netty;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

public interface SipResolverTransport {
	
	public void writeRequest(ByteBuf requestBuffer) throws IOException;
    
    public void prepareForReConnect();
}
