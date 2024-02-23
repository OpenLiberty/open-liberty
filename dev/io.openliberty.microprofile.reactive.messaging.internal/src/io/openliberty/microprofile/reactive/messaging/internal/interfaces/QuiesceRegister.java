/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.internal.interfaces;

/**
 * Allows {@link QuiesceParticipant}s to register and unregister themselves.
 * <p>
 * An instance of this interface should be obtained by looking up the singleton service from OSGi.
 */
public interface QuiesceRegister {

    /**
     * Register an object to be notified about the server quiescing
     * <p>
     * All objects passed to this method must later be passed to {@link #remove(QuiesceParticipant)} to avoid a memory leak.
     *
     * @param participant the object to register
     */
    void register(QuiesceParticipant participant);

    /**
     * Deregister an object for notifications about the server quiescing
     *
     * @param participant the object to deregister
     */
    void remove(QuiesceParticipant participant);
}
