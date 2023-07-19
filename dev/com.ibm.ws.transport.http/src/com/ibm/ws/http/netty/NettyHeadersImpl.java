/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.ArrayList;

/**
 *
 */
public class NettyHeadersImpl {

    /** Flag used to identify if an X-Forwarded-* header has been added */
    private final boolean processedXForwardedHeader = false;
    /** Flag used to identify if a Forwarded header has been added */
    private final boolean processedForwardedHeader = false;
    /**
     * Flag used to identify if there was an error parsing the Forwarded header and it should
     * not be further parsed.
     */
    private final boolean forwardHeaderErrorState = false;
    /**
     * String Builder representing a comma delimited list of processed X-Forwarded-For / Forwarded "for"
     * node identifiers.
     */
    private final ArrayList<String> forwardedForList = null;
    /**
     * String Builder representing a comma delimited list of processed X-Forwarded-By / Forwarded "by"
     * node identifiers.
     */
    private final ArrayList<String> forwardedByList = null;
    /** Identifies the original client request's used protocol, as defined by X-Forwarded-Proto / Forwarded "proto" */
    private final String forwardedProto = null;
    /** Identifies the original client request's host used as defined by the Forwarded "host" parameter. */
    private final String forwardedHost = null;
    /**
     * Identifies the original client requet's port as defined by X-Fowarded-Port / or the inclusion of the port in
     * the first address of the Forwarded "for" list.
     */
    private final String forwardedPort = null;

    //Empty constructor
    public NettyHeadersImpl() {
    }

}
