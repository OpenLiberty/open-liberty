/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.internal;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

@Component(name = "io.openliberty.security.oidcclient.wellknown.internal.OAuthProtectedResourceMetadataServletService", service = {}, property = { "service.vendor=IBM" })
public class OAuthProtectedResourceMetadataServletService extends OAuthProtectedResourceMetadataServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String resolveMetadataJson(HttpServletRequest request, String protectedResourcePath) {
        return null;
    }
}

// Made with Bob
