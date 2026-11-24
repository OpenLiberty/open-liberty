/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.h2spool;

import io.netty.util.AttributeKey;

/** Transport-private handoff from channel initialization to H2 negotiation. */
public final class H2SpoolChannelState {
    public static final AttributeKey<Boolean> ORDINARY_ENABLED = AttributeKey.valueOf(H2SpoolChannelState.class, "ordinaryEnabled");
    public static final AttributeKey<H2SpoolResources.ConnectionScope> ORDINARY_SCOPE = AttributeKey.valueOf(H2SpoolChannelState.class, "ordinaryScope");

    private H2SpoolChannelState() {}
}
