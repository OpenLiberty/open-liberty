/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;

/**
 * The class holds client runtime meta data for target application, those data will be recreated once the application is restarted.
 */
public class JaxWsClientMetaData {

    private final LibertyApplicationBus clientBus;

    private final JaxWsModuleMetaData moduleMetaData;

    public JaxWsClientMetaData(JaxWsModuleMetaData moduleMetaData1) {
        this.moduleMetaData = moduleMetaData1;
        this.clientBus = AccessController.doPrivileged(new PrivilegedAction<LibertyApplicationBus>() {
            @Override
            public LibertyApplicationBus run() {
                return LibertyApplicationBusFactory.getInstance().createClientScopedBus(moduleMetaData);
            }
        });
    }

    public void destroy() {

        /* the server will not destroy the bus, we should also destroy the bus */
        if (clientBus != null)
            clientBus.shutdown(false);
    }

    public LibertyApplicationBus getClientBus() {
        return clientBus;
    }

    public JaxWsModuleMetaData getModuleMetaData() {
        return moduleMetaData;
    }

}
