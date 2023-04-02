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

    public AttributeKey<String> NameKey = AttributeKey.valueOf("Name");
    public AttributeKey<String> HostKey = AttributeKey.valueOf("Host");
    public AttributeKey<String> ExternalHostKey = AttributeKey.valueOf("ExternalHost");
    public AttributeKey<Integer> PortKey = AttributeKey.valueOf("Port");

}
