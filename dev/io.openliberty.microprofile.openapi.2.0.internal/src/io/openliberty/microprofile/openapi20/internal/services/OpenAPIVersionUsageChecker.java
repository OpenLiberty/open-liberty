/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import org.jboss.jandex.IndexView;

import io.openliberty.microprofile.openapi20.internal.OpenAPIVersion;

/**
 * Checks if the user has used any annotation parameters which aren't supported by the version of OpenAPI in use.
 * <p>
 * For example, {@code info.summary} was added in OpenAPI 3.1, so we warn the user if they have used {@code Info(summary="foo")} and have OpenAPI 3.0 configured.
 */
public interface OpenAPIVersionUsageChecker {

    /**
     * Warns about any annotation parameters in the application which aren't applicable to the current OpenAPI version.
     *
     * @param index annotation index
     * @param openAPIVersion the configured OpenAPI version
     */
    public void checkAnnotations(IndexView index, OpenAPIVersion openAPIVersion);
}
