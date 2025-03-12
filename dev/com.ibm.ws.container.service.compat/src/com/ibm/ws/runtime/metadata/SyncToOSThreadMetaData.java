/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.runtime.metadata;

/**
 * Mix-in interface to not require dependency on web container function by the
 * SyncToOSThread function
 */
public interface SyncToOSThreadMetaData extends ModuleMetaData {

    /**
     * Returns true of the SyncToOSThread function is enabled
     */
    public boolean isSyncToOSThreadEnabled();
}
