/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package com.ibm.ws.sip.channel.resolver.impl;

import java.io.IOException;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public interface SipResolverTransport {
	
	public void writeRequest(WsByteBuffer requestBuffer) throws IOException;
    
    public void prepareForReConnect();
}
