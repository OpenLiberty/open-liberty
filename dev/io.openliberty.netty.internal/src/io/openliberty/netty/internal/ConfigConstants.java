/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.netty.internal;

import io.netty.util.AttributeKey;

public interface ConfigConstants {

    public final String EXTERNAL_NAME = "ExternalName";
    public final AttributeKey<String> NAME_KEY = AttributeKey.valueOf(EXTERNAL_NAME);
    public final AttributeKey<String> HOST_KEY = AttributeKey.valueOf("Host");
    public final AttributeKey<String> EXTERNAL_HOST_KEY = AttributeKey.valueOf("ExternalHost");
    public final AttributeKey<Integer> PORT_KEY = AttributeKey.valueOf("Port");
    public final AttributeKey<Boolean> IS_INBOUND_KEY = AttributeKey.valueOf("IsInbound");

}
