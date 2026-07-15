/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal;

import io.netty.util.AttributeKey;

public interface ConfigConstants {

    public final String EXTERNAL_NAME = "ExternalName";
    public final String HOST_KEY_NAME = "Host";
    public final String EXTERNAL_HOST_KEY_NAME = "ExternalHost";
    public final String PORT_KEY_NAME = "Port";
    public final String INBOUND_KEY_NAME = "IsInbound";
    public final String SUPPRESS_CHANNEL_STARTED_MSG_NAME = "SuppressChannelStartedMsg";

    public final AttributeKey<String> NAME_KEY = AttributeKey.valueOf(EXTERNAL_NAME);
    public final AttributeKey<String> HOST_KEY = AttributeKey.valueOf(HOST_KEY_NAME);
    public final AttributeKey<String> EXTERNAL_HOST_KEY = AttributeKey.valueOf(EXTERNAL_HOST_KEY_NAME);
    public final AttributeKey<Integer> PORT_KEY = AttributeKey.valueOf(PORT_KEY_NAME);
    public final AttributeKey<Boolean> IS_INBOUND_KEY = AttributeKey.valueOf(INBOUND_KEY_NAME);
    /**
     * When set to {@code Boolean#TRUE} on a server channel, suppresses the
     * CWWKO0219I (TCP_CHANNEL_STARTED) info message that TCPUtils would
     * otherwise emit on a successful inbound bind.  Set this on the bootstrap
     * before calling {@code startInbound} if the caller intends to emit the
     * message itself (e.g. after a duplicate-port check).
     */
    public final AttributeKey<Boolean> SUPPRESS_CHANNEL_STARTED_MSG =
        AttributeKey.valueOf(SUPPRESS_CHANNEL_STARTED_MSG_NAME);

}
