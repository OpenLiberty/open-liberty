/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics50.helper;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

/**
 *
 */
public class Util {

    public static ClassLoader BUNDLE_ADD_ON_CLASSLOADER;

    public static SharedMetricRegistries SHARED_METRIC_REGISTRIES;

    public static Class<?> SR_SHARED_METRIC_REGISTRIES_CLASS;

    public static Class<?> SR_LEGACY_METRIC_REGISTRY_CLASS;

    public static Class<?> SR_METRICS_REQUEST_HANDLER_CLASS;

    public static Class<?> SR_REST_RESPONDER_INTERFACE;

    public static Class<?> SR_APPLICATION_NAME_RESOLVER_INTERFACE;

    public static Class<?> SR_CDI_EXTENSION_CLASS;

    public static Class<?> SR_LEGACY_METRIC_REGISTRY_EXTENSION_CLASS;

    // CDI classes
    public static Class<?> SR_METRIC_REGISTRY_PRODUCER_CLASS;

    public static Class<?> SR_METRICS_PRODUCER_CLASS;

    // other
    public static Class<?> SR_METRIC_NAME_CLASS;
}
