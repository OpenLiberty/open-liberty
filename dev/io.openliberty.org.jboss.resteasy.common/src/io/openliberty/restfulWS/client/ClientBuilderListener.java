/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Registered ClientBuilderListeners (via DS) will be called when a
 * {@code jakarta.ws.rs.client.ClientBuilder} is invoked allowing
 * customization of the resulting {@code jakarta.ws.rs.client.Client}
 * instance.
 */
@Sensitive
public interface ClientBuilderListener {
    static final TraceComponent tc = Tr.register(ClientBuilderListener.class);

    default void building(ClientBuilder clientBuilder) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "building - no-op");
        }
    }

    default void built(Client client) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "built - no-op");
        }
    }
}
