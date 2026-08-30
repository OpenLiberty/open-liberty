/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandleMap;
import com.ibm.ws.zos.channel.local.queuing.LocalChannelProvider;
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
@Component(configurationPolicy=ConfigurationPolicy.IGNORE, property="service.vendor=IBM")
public class LocalCommClientConnHandleMapImpl implements LocalCommClientConnHandleMap {
    
	@Reference
    private LocalChannelProvider localChannelProvider;

    /**
     * @return the application callback (upstream channel) associated with the LocalCommConnLink
     *         that is associated with the given LocalCommClientConnHandle; or null if the conn
     *         link doesn't exist.
     */
    @Override
    public ConnectionReadyCallback getApplicationCallback( LocalCommClientConnHandle clientConnHandle) {
        LocalCommConnLink localCommConnLink = localChannelProvider.getConnHandleToConnLinkMap().get(clientConnHandle);
        return (localCommConnLink != null) ? localCommConnLink.getApplicationCallback() : null;
    }

}
