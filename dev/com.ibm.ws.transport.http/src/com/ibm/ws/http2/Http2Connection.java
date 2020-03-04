/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2;

/**
 * HTTP/2 connection
 */
public interface Http2Connection {

    /**
     * Terminate this connection
     *
     * @param Exception e
     */
    public void destroy(Exception e);

    /**
     * @return int the port this connection is using
     */
    public int getPort();

    /**
     * @return String authority
     */
    public String getAuthority();
}
