/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.util;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

@Component
public class LocationUtils {

    private static final TraceComponent tc = Tr.register(LocationUtils.class);

    private static WsLocationAdmin locationAdmin;

    public LocationUtils() {}

    /* constructor for the unit test */
    @SuppressWarnings("static-access")
    public LocationUtils(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    @SuppressWarnings("static-access")
    @Reference
    protected synchronized void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    public static boolean isServer() {
        return locationAdmin.resolveString(WsLocationConstants.SYMBOL_PROCESS_TYPE).equals(WsLocationConstants.LOC_PROCESS_TYPE_SERVER);
    }
}