/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.ee8;

import com.ibm.wsspi.http.HttpRequest;

/**
 * Representation of an HTTP 2 request message provided by the dispatcher to any
 * HTTP container.
 */
public interface Http2Request extends HttpRequest {

    /*
     * Are push request supported? is push reques
     */
    boolean isPushSupported();

    /**
     * Initiate a Push request
     *
     * @return
     */
    void pushNewRequest(Http2PushBuilder pushBuilder);

}
