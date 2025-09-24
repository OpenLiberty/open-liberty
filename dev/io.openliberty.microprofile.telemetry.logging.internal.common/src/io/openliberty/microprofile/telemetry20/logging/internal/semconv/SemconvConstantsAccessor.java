/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package io.openliberty.microprofile.telemetry20.logging.internal.semconv;

import io.opentelemetry.api.common.AttributeKey;

//TODO proper javadoc
public interface SemconvConstantsAccessor {

    //Network Attributes
    public AttributeKey<String> errorType();

    public AttributeKey<String> httpRequestMethod();

    public AttributeKey<String> httpRoute();

    public AttributeKey<String> accessRequestHost(); //TODO fix method name

    public AttributeKey<String> clientAddress();

    public AttributeKey<Long> httpResponseStatusCode();

    public AttributeKey<Long> localNetworkPort();

    public AttributeKey<String> networkPeerAddress();

    public AttributeKey<String> networkProtocolName();

    public AttributeKey<String> networkProtocolVersion();

    //Thread attributes
    public AttributeKey<String> threadName();

    public AttributeKey<Long> threadId();

    //Exception Attributes
    public AttributeKey<String> exceptionType();

    public AttributeKey<String> exceptionStackTrace();

    public AttributeKey<String> exceptionStackMessage();

}
