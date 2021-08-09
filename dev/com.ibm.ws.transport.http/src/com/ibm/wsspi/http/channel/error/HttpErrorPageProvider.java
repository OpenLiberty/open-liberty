/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.error;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

/**
 * Interface for a provider of HTTP error pages used by the error page service.
 * 
 */
public interface HttpErrorPageProvider {

    /**
     * Access the configured error page, if it exists, for the given information.
     * The host may be a hostname or an IP address. The request may or may not
     * be null, depending on what error scenario is happening. The response
     * message will always exist and will have the status code set to the desired
     * value.
     * 
     * @param localHost
     *            - this is from the socket layer
     * @param localPort
     *            - this is from the socket layer
     * @param request
     * @param response
     * @return WsByteBuffer[] - may be null if no page was found
     */
    WsByteBuffer[] accessPage(String localHost, int localPort, HttpRequestMessage request, HttpResponseMessage response);

}
