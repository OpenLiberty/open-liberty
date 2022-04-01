/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.servlet;

import com.ibm.websphere.servlet40.IRequest40;
/**
 *
 */
public interface IRequest60 extends IRequest40 {

    /**
     * Obtain a unique (within the lifetime of the Servlet container) identifier string for this request.
     * <p>
     * There is no defined format for this string. The format is implementation dependent.
     * 
     * @return A unique identifier for the request
     * 
     * @since Servlet 6.0
     */
    public String getRequestId();

    /**
     * Obtain the request identifier for this request as defined by the protocol in use. Note that some protocols do not
     * define such an identifier.
     * <p>
     * Examples of protocol provided request identifiers include:
     * <dl>
     * <dt>HTTP 1.x</dt>
     * <dd>None, so the empty string should be returned</dd>
     * <dt>HTTP 2</dt>
     * <dd>The stream identifier</dd>
     * <dt>HTTP 3</dt>
     * <dd>The stream identifier</dd>
     * <dt>AJP</dt>
     * <dd>None, so the empty string should be returned</dd>
     * 
     * @return The request identifier if one is defined, otherwise an empty string
     * 
     * @since Servlet 6.0
     */
    public String getProtocolRequestId();

    /**
     * Obtain details of the network connection to the Servlet container that is being used by this request. The information
     * presented may differ from information presented elsewhere in the Servlet API as raw information is presented without
     * adjustments for, example, use of reverse proxies that may be applied elsewhere in the Servlet API.
     * 
     * @return The network connection details.
     * 
     * @since Servlet 6.0
     */
    //Active this later when jakarta.servlet is enable/ ServletConnection getServletConnection();
}
