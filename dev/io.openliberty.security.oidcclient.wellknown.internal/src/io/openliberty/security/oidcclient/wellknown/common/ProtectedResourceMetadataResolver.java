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
 *
 */
/**
 * Resolves protected resource metadata for a normalized protected resource path.
 */
public interface ProtectedResourceMetadataResolver {

    /**
     * Resolves metadata JSON for the supplied protected resource path.
     *
     * @param protectedResourcePath normalized protected resource path, for example {@code /myApp/protected}
     * @return metadata JSON, or {@code null} if metadata should not be served for the path
     */
    String resolveMetadataJson(String protectedResourcePath);
}
