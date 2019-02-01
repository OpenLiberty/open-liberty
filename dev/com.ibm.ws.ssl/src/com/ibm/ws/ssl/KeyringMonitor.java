/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl;

/**
 * Components that want to monitor the keyring for changes should implement
 * the KeyringMonitor interface and register that implementation in the service registry.
 * <p>
 * The properties associated with the registered service specify name of keyring
 * to be monitored. Valid service properties are listed as constants below with
 * descriptive javadoc.
 */
public interface KeyringMonitor {

    /**
     * <h4>Service property</h4>
     *
     * The value should be a String, indicating the name of
     * keyring that should be monitored.
     */
    String KEYRING_NAME = "keyring.name";

    String SAF_PREFIX = "safkeyring://";

    /**
     * Method to call the update of SAF keyrings
     *
     * @param name - The name of the keyring to be refresh,
     *            or null if all keyrings should be refreshed.
     */
    public void refreshRequested(String name);

}
