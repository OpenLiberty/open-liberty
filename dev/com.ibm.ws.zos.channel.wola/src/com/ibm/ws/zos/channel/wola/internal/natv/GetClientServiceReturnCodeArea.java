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

package com.ibm.ws.zos.channel.wola.internal.natv;

/**
 * Return code area for WOLANativeUtils.ntv_getClientService
 *
 * Area contains 3 return codes:
 * 1) return_code
 * 2) iean4rt_rc
 * 3) getClientService_rc
 *
 * !! NOTE: This mapping must be kept in sync with com.ibm.zos.native/server_wola_services_jni.c
 */
public class GetClientServiceReturnCodeArea extends ByteBufferBacked<GetClientServiceReturnCodeArea> {

    /**
     * getClientService_rc indicating the caller timed out waiting for
     * the client service to become available.
     */
    public static final int WOLA_SERVICE_QUEUES_RC_PAUSE_TIMEOUT = 20;

    /**
     * CTOR.
     */
    public GetClientServiceReturnCodeArea() {
        super(12);
    }

    /**
     * @return the getClientService_rc.
     */
    public int getGetClientServiceRc() {
        return super.getInt(8);
    }

    /**
     * @return true if the return code area indicates a timeout.
     */
    public boolean isTimeout() {
        return getGetClientServiceRc() == WOLA_SERVICE_QUEUES_RC_PAUSE_TIMEOUT;
    }

}
