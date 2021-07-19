/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local;

import com.ibm.wsspi.channelfw.ConnectionReadyCallback;

/**
 * Exposes a read-only lookup service for looking up the LocalCommConnLink associated
 * with a given native LocalCommClientConnHandle. 
 * 
 * The service is used, e.g., by WolaOutboundConnMgr, on behalf of an app inside Liberty 
 * that wants to go outbound over WOLA (via JCA) to an available client service. The app 
 * first obtains the LocalCommClientConnHandle associated with the client's registerName/
 * serviceName (via a separate service), then provides the LocalCommClientConnHandle
 * to this service to obtain the LocalCommConnLink -> WolaConnLink associated
 * with that client's conn handle.
 * 
 */
public interface LocalCommClientConnHandleMap {
    
    /**
     * Lookup the upstream channel associated with the given native LocalCommClientConnHandle.
     * 
     * @return the application callback (upstream channel) associated with the LocalCommConnLink
     *         that is associated with the given LocalCommClientConnHandle.
     */
    public ConnectionReadyCallback getApplicationCallback( LocalCommClientConnHandle clientConnHandle);

}
