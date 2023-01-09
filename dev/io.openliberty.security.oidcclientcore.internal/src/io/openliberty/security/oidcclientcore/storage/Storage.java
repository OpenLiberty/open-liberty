/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.storage;

/**
 * OIDCClientCore storage subsystem. Allows storing values either in cookies or HTTP session.
 * Will be used with AuthorizationCodeFlow to store state and nonce values.
 * Use CookieBasedStorage or SessionBasedStorage instead.
 */
public interface Storage {

    /**
     * Stores a name value pair in an underlying storage data structure. Values are either a cookie or HTTP session.
     *
     * @param name  - the name of the value being stored, used for access
     * @param value - value to be stored
     */
    void store(String name, String value);

    /**
     * Stores a name value pair in an underlying storage data structure. Values are either a cookie or HTTP session.
     *
     * @param name  - the name of the value being stored, used for access
     * @param value - value to be stored
     * @param properties - additional properties to set for this particular value in the storage medium
     */
    void store(String name, String value, StorageProperties properties);

    /**
     * Returns the appropriate value given a name.
     *
     * @param name - name associated with the value to be returned
     * @return String value for item stored
     */
    String get(String name);

    /**
     * Removes the value associated with the provided name from storage.
     */
    void remove(String name);

}
