/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;

import com.ibm.wsspi.channelfw.base.OutboundConnectorLink;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 *
 *
 */
public abstract class TCPProxyConnLink extends OutboundConnectorLink implements TCPConnectionContext // @350394A
{
    abstract protected boolean isAsyncConnect();

    abstract protected boolean isSyncError();

    abstract protected void connectFailed(IOException e);
}
