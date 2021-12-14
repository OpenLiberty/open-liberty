/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal;

import io.netty.util.AttributeKey;

public interface ConfigConstants {

    public final String ExternalName = "ExternalName";
    public final AttributeKey<String> NameKey = AttributeKey.valueOf(ExternalName);
    public final AttributeKey<String> HostKey = AttributeKey.valueOf("Host");
    public final AttributeKey<String> ExternalHostKey = AttributeKey.valueOf("ExternalHost");
    public final AttributeKey<Integer> PortKey = AttributeKey.valueOf("Port");
    public final AttributeKey<Boolean> IsInboundKey = AttributeKey.valueOf("IsInbound");

}
