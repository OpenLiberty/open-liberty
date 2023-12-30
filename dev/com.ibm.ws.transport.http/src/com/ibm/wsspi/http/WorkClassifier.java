/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.wsspi.http;

import java.util.concurrent.Executor;

import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Work classification
 *
 * Used to classify a piece of inbound work and setup the execution environment.
 *
 */
public interface WorkClassifier {

    /**
     * Classify the request and return an Executor to run on.
     *
     * @param request           HTTP request to classify.
     * @param inboundConnection HTTP connection related to the request.
     * @return an Executor to run the HTTP request.
     */
    Executor classify(HttpRequest request, HttpInboundConnection inboundConnection);

    Executor classify(FullHttpRequest request, HttpInboundConnection inboundConnection);

}
