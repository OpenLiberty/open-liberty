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

import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * An object which can be notified when the server quiesces.
 * <p>
 * Must be manually registered with {@link QuiesceRegister}.
 * <p>
 * Used by components in Reactive Messaging which aren't OSGi components and so can't easily register themselves as a {@link ServerQuiesceListener}
 */
public interface QuiesceParticipant {

    /**
     * Called when the server is stopping
     */
    void quiesce();
}
