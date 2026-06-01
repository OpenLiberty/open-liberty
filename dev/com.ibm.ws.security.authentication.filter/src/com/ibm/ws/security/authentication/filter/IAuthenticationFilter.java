/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.ibm.ws.security.authentication.filter;

import java.util.List;

/**
 * Internal interface to expose AuthFilterConfig data for protected resource metadata.
 * This interface should only be used internally and is not part of the public API.
 */
public interface IAuthenticationFilter {

    /**
     * Get the authentication filter ID.
     *
     * @return the filter ID, or null if not configured
     */
    String getAuthFilterId();

    /**
     * Get the list of request URL patterns configured for this authentication filter.
     * Each Properties object contains the urlPattern value under AuthFilterConfig.KEY_URL_PATTERN.
     *
     * @return list of Properties containing URL patterns, or null if not configured
     */
    List<String> getRequestUrlPatterns();
}