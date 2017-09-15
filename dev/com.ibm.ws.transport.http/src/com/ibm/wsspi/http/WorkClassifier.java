/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.util.concurrent.Executor;

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
     * @param request HTTP request to classify.
     * @param inboundConnection HTTP connection related to the request.
     * @return an Executor to run the HTTP request.
     */
    Executor classify(HttpRequest request, HttpInboundConnection inboundConnection);

}
