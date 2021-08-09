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
package com.ibm.ws.ssl.internal;

import java.util.Collection;

import org.osgi.framework.BundleContext;

/**
 * Component that need to be notified by the KeyringMonitorImpl when the keyring
 * they are interested in are modified or recreated need to implement this interface
 * and pass themselves to a new instance of SecurityFileMonitor.
 */
public interface KeyringBasedActionable {

    /**
     * Callback method to be invoked by the keyring monitor
     * to instruct the implementation to perform its action.
     *
     * @param modifiedKeystore location
     */
    void performKeyStoreAction(Collection<String> modifiedKeyStores);

    /**
     * Returns the implementation's BundleContext.
     */
    BundleContext getBundleContext();
}
