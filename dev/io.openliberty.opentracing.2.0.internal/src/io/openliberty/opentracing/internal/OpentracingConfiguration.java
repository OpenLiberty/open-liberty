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
    public static final String MP_OT_SERVER_SKIP_PATTERN_MAY_CHANGE_KEY = "mp.opentracing.server.skip-pattern.may.change";
    public static final String MP_OT_SERVER_OPERATION_NAME_PROVIDER_KEY = "mp.opentracing.server.operation-name-provider";
    public static final String MP_OT_SERVER_OPERATION_NAME_PROVIDER_HTTP_PATH = "http-path";

    private static boolean MP_OT_SERVER_SKIP_PATTERN_MAY_CHANGE = false;
    private static boolean MP_OT_SERVER_SKIP_PATTERN_IS_SET = false;
    private static String MP_OT_SERVER_SKIP_PATTERN = null;

    static {
        Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
        MP_OT_SERVER_SKIP_PATTERN_MAY_CHANGE = config.getOptionalValue(MP_OT_SERVER_SKIP_PATTERN_MAY_CHANGE_KEY, Boolean.class).orElse(false);
    }

    public static String getServerSkipPattern() {
        if (!MP_OT_SERVER_SKIP_PATTERN_IS_SET || MP_OT_SERVER_SKIP_PATTERN_MAY_CHANGE) {
            Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
            MP_OT_SERVER_SKIP_PATTERN = config.getOptionalValue(MP_OT_SERVER_SKIP_PATTERN_KEY, String.class).orElse(null);
            MP_OT_SERVER_SKIP_PATTERN_IS_SET = true;
        }

        return MP_OT_SERVER_SKIP_PATTERN;
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
