/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 *
 */
public class OpentracingConfiguration {

    public static final String MP_OT_SERVER_SKIP_PATTERN_KEY = "mp.opentracing.server.skip-pattern";
    public static final String MP_OT_SERVER_OPERATION_NAME_PROVIDER_KEY = "mp.opentracing.server.operation-name-provider";
    public static final String MP_OT_SERVER_OPERATION_NAME_PROVIDER_HTTP_PATH = "http-path";

    public static String getServerSkipPattern() {
        Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
        return config.getOptionalValue(MP_OT_SERVER_SKIP_PATTERN_KEY, String.class).orElse(null);
    }

    static String getOpertionNameProvider() {
        Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
        return config.getOptionalValue(MP_OT_SERVER_OPERATION_NAME_PROVIDER_KEY, String.class).orElse(null);
    }

    public static boolean isOperationNameProviderHttpPath() {
        String providerName = getOpertionNameProvider();
        if (providerName != null) {
            return MP_OT_SERVER_OPERATION_NAME_PROVIDER_HTTP_PATH.equals(providerName);
        } else {
            return false;
        }
    }
}
