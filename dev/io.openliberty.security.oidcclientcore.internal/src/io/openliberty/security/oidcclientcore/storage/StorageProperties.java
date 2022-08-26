/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

public class StorageProperties {

    protected int storageLifetimeSeconds = -1;

    public void setStorageLifetimeSeconds(int lifetime) {
        storageLifetimeSeconds = lifetime;
    }

    public int getStorageLifetimeSeconds() {
        return storageLifetimeSeconds;
    }

    @Override
    public String toString() {
        String result = "StorageProperties:{";
        result += "storageLifetimeSeconds=" + storageLifetimeSeconds;
        result += "}";
        return result;
    }

}
