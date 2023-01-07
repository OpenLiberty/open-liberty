/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.adaptable.module.internal;

import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

class NonPersistentCacheImpl implements NonPersistentCache {
    private final OverlayContainer rootOverlay;
    private final String path;

    public NonPersistentCacheImpl(OverlayContainer rootOverlay, String path) {
        this.rootOverlay = rootOverlay;
        this.path = path;
    }

    @Override
    public void addToCache(Class<?> owner, Object data) {
        rootOverlay.addToNonPersistentCache(path, owner, data);
    }

    @Override
    public void removeFromCache(Class<?> owner) {
        rootOverlay.removeFromNonPersistentCache(path, owner);
    }

    @Override
    public Object getFromCache(Class<?> owner) {
        return rootOverlay.getFromNonPersistentCache(path, owner);
    }
}