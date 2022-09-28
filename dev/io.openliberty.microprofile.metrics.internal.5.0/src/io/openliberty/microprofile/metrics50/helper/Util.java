/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.helper;

import org.eclipse.microprofile.metrics.MetricRegistry;

import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;

import io.openliberty.microprofile.metrics50.internal.Constants;
import io.openliberty.microprofile.metrics50.internal.impl.SharedMetricRegistries;

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

    private static MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        if (!Constants.REGISTRY_NAMES_LIST.contains(registryName)) {
            throw new NoSuchRegistryException();
        }
        return SHARED_METRIC_REGISTRIES.getOrCreate(registryName);
    }

}
