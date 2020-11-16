/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm.handle;

/**
 * HandleListInterface is mostly ported to Liberty, except for the Handle interface,
 * which is added as an abstraction to avoid this bundle having dependencies on the
 * connection management bundle, which isn't always available.
 */
public interface HandleListInterface {
    interface Handle {
        void close();

        void park();

        void reassociate();
    }

    /**
     * Adds a non-dissociatable handle to the handle list, and returns this
     * HandleList or the underlying HandleList if this interface is implemented
     * as a proxy.
     */
    HandleList addHandle(Handle a);

    /**
     * Remove the specified handle from the list.
     */
    void removeHandle(Object r);

    /**
     * Reassociate the managed connection for all handles in the list.
     */
    void reAssociate();

    /**
     * Park the managed connection for all handles in the list
     */
    void parkHandle();
}
