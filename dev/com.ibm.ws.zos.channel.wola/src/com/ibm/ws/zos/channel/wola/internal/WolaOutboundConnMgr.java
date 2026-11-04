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
package com.ibm.ws.zos.channel.wola.internal;

import java.io.IOException;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandleMap;
import com.ibm.ws.zos.channel.wola.internal.natv.WOLANativeUtils;

/**
 * Simple service for looking up the WolaConnLink associated with
 * a client-hosted WOLA service.
 *
 * This guy uses WOLANativeUtils.getClientService to find the LocalCommClientConnHandle
 * for a given client's registerName/serviceName, then uses LocalCommClientConnHandleMap
 * to obtain the LocalCommConnLink->WolaConnLink for the client conn handle.
 *
 */
public class WolaOutboundConnMgr {

    /**
     * DS ref.
     */
    private WOLANativeUtils wolaNativeUtils;

    /**
     * DS ref.
     */
    private WOLAConfig wolaConfig;

    /**
     * DS ref.
     */
    private LocalCommClientConnHandleMap localCommClientConnHandleMap;

    /**
     * Set DS ref.
     */
    protected void setWolaNativeUtils(WOLANativeUtils wolaNativeUtils) {
        this.wolaNativeUtils = wolaNativeUtils;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaNativeUtils(WOLANativeUtils wolaNativeUtils) {
        if (this.wolaNativeUtils == wolaNativeUtils) {
            this.wolaNativeUtils = null;
        }
    }

    /**
     * Set DS ref.
     */
    protected void setWolaConfig(WOLAConfig wolaConfig) {
        this.wolaConfig = wolaConfig;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaConfig(WOLAConfig wolaConfig) {
        if (this.wolaConfig == wolaConfig) {
            this.wolaConfig = null;
        }
    }

    /**
     * Inject DS.
     */
    protected void setLocalCommClientConnHandleMap(LocalCommClientConnHandleMap map) {
        this.localCommClientConnHandleMap = map;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetLocalCommClientConnHandleMap(LocalCommClientConnHandleMap map) {
        if (this.localCommClientConnHandleMap == map) {
            this.localCommClientConnHandleMap = null;
        }
    }

    /**
     * Look up the native localcomm client connection handle that hosts the
     * given registerName and serviceName, then find the WolaConnLink associated
     * with that connection handle.
     *
     * @param registerName - client's registration name
     * @param serviceName  - name of service hosted by client
     * @param timeout_s    - the time to wait (in seconds) for the client service to become available before giving up
     *
     * @return The WolaConnLink to use for invoking the given serviceName for the given
     *         client registerName.
     */
    public WolaConnLink getWolaConnLink(String registerName, String serviceName, int timeout_s) throws IOException {

        LocalCommClientConnHandle clientConnHandle = wolaNativeUtils.getClientService(wolaConfig.getWolaGroup(),
                                                                                      registerName,
                                                                                      serviceName,
                                                                                      timeout_s);
        return (WolaConnLink) localCommClientConnHandleMap.getApplicationCallback(clientConnHandle);
    }

}
