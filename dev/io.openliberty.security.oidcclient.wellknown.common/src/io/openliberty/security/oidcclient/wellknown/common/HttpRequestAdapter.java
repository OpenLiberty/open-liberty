/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclient.wellknown.common;

/**
 * Servlet-independent view of an HTTP request.
 * <p>
 * This allows common metadata logic to build resource URLs without depending on either
 * {@code javax.servlet} or {@code jakarta.servlet}.
 * </p>
 */
public interface HttpRequestAdapter {

    String getRequestURL();

    String getRequestURI();

    String getScheme();

    String getServerName();

    int getServerPort();
}