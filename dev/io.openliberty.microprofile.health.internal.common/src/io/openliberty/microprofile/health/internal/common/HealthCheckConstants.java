/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.health.internal.common;

/**
 *
 */
public class HealthCheckConstants {

    // HTTP Headers
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";

    // Health Check Procedures
    public static final String HEALTH_CHECK_ALL = "ALL";
    public static final String HEALTH_CHECK_LIVE = "LIVE";
    public static final String HEALTH_CHECK_READY = "READY";
    public static final String HEALTH_CHECK_START = "START";

    // Health Check JSON payload names
    public static final String HEALTH_CHECK_PAYLOAD_STATUS = "status";
    public static final String HEALTH_CHECK_PAYLOAD_CHECKS = "checks";
    public static final String HEALTH_CHECK_PAYLOAD_NAME = "name";
    public static final String HEALTH_CHECK_PAYLOAD_DATA = "data";

    // Default Overall Readiness Status MP Config property name
    public static final String DEFAULT_OVERALL_READINESS_STATUS = "mp.health.default.readiness.empty.response";

    // Default Overall Startup Status MP Config property name
    public static final String DEFAULT_OVERALL_STARTUP_STATUS = "mp.health.default.startup.empty.response";

    public static final String HEALTH_SERVER_CONFIG_FILE_UPDATE_INTERVAL = "fileUpdateInterval";

    public static final String HEALTH_ENV_CONFIG_FILE_UPDATE_INTERVAL = "MP_HEALTH_FILE_UPDATE_INTERVAL";

}
