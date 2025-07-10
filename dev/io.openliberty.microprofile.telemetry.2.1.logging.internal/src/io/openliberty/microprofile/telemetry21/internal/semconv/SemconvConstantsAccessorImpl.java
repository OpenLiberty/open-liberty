/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry21.internal.semconv;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry20.logging.internal.semconv.SemconvConstantsAccessor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes;

@Component
public class SemconvConstantsAccessorImpl implements SemconvConstantsAccessor {

    @Override
    public AttributeKey<String> errorType() {
        return ErrorAttributes.ERROR_TYPE;
    }

    @Override
    public AttributeKey<String> httpRequestMethod() {
        return HttpAttributes.HTTP_REQUEST_METHOD;
    }

    @Override
    public AttributeKey<String> httpRoute() {
        return HttpAttributes.HTTP_ROUTE;
    }

    @Override
    public AttributeKey<String> accessRequestHost() { //TODO fix method name
        return ServerAttributes.SERVER_ADDRESS;
    }

    @Override
    public AttributeKey<String> clientAddress() {
        return ClientAttributes.CLIENT_ADDRESS;
    }

    @Override
    public AttributeKey<Long> httpResponseStatusCode() {
        return HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
    }

    @Override
    public AttributeKey<Long> localNetworkPort() {
        return NetworkAttributes.NETWORK_LOCAL_PORT;
    }

    @Override
    public AttributeKey<String> networkPeerAddress() {
        return NetworkAttributes.NETWORK_PEER_ADDRESS;
    }

    @Override
    public AttributeKey<String> networkProtocolName() {
        return NetworkAttributes.NETWORK_PROTOCOL_NAME;
    }

    @Override
    public AttributeKey<String> networkProtocolVersion() {
        return NetworkAttributes.NETWORK_PROTOCOL_VERSION;
    }

    @Override
    public AttributeKey<String> threadName() {
        return ThreadIncubatingAttributes.THREAD_NAME;
    }

    @Override
    public AttributeKey<Long> threadId() {
        return ThreadIncubatingAttributes.THREAD_ID;
    }

    @Override
    public AttributeKey<String> exceptionType() {
        return ExceptionAttributes.EXCEPTION_TYPE;
    }

    @Override
    public AttributeKey<String> exceptionStackTrace() {
        return ExceptionAttributes.EXCEPTION_STACKTRACE;
    }

    @Override
    public AttributeKey<String> exceptionStackMessage() {
        return ExceptionAttributes.EXCEPTION_MESSAGE;
    }

}
