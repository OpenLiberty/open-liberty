/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.internal;

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

    // Health Check JSON payload names
    public static final String HEALTH_CHECK_PAYLOAD_STATUS = "status";
    public static final String HEALTH_CHECK_PAYLOAD_CHECKS = "checks";
    public static final String HEALTH_CHECK_PAYLOAD_NAME = "name";
    public static final String HEALTH_CHECK_PAYLOAD_DATA = "data";

}
