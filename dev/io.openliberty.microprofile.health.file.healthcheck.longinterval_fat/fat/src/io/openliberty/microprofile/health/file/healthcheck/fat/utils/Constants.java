/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat.utils;

/**
 *
 */
public class Constants {

    public static final String SHOULD_HAVE_CREATED = " should have been created.";
    public static final String SHOULD_NOT_HAVE_CREATED = " should not have been created.";

    public static final String SHOULD_HAVE_UPDATED = " should have been updated.";
    public static final String SHOULD_NOT_HAVE_UPDATED = " should not have been updated.";

    public static final String HEALTH_DIR_SHOULD_HAVE_CREATED = "/health" + SHOULD_HAVE_CREATED;
    public static final String HEALTH_DIR_SHOULD_NOT_HAVE_CREATED = "/health" + SHOULD_NOT_HAVE_CREATED;

    public static final String LIVE_SHOULD_HAVE_CREATED = "/health/live" + SHOULD_HAVE_CREATED;
    public static final String LIVE_SHOULD_NOT_HAVE_CREATED = "/health/live" + SHOULD_NOT_HAVE_CREATED;

    public static final String STARTED_SHOULD_HAVE_CREATED = "/health/started" + SHOULD_HAVE_CREATED;
    public static final String STARTED_SHOULD_NOT_HAVE_CREATED = "/health/started" + SHOULD_NOT_HAVE_CREATED;

    public static final String READY_SHOULD_HAVE_CREATED = "/health/ready" + SHOULD_HAVE_CREATED;
    public static final String READY_SHOULD_NOT_HAVE_CREATED = "/health/ready" + SHOULD_NOT_HAVE_CREATED;

    public static final String LIVE_SHOULD_HAVE_UPDATED = "/health/live" + SHOULD_HAVE_UPDATED;
    public static final String LIVE_SHOULD_NOT_HAVE_UPDATED = "/health/live" + SHOULD_NOT_HAVE_UPDATED;

    public static final String STARTED_SHOULD_HAVE_UPDATED = "/health/started" + SHOULD_HAVE_UPDATED;
    public static final String STARTED_SHOULD_NOT_HAVE_UPDATED = "/health/started" + SHOULD_NOT_HAVE_UPDATED;

    public static final String READY_SHOULD_HAVE_UPDATED = "/health/ready" + SHOULD_HAVE_UPDATED;
    public static final String READY_SHOULD_NOT_HAVE_UPDATED = "/health/ready" + SHOULD_NOT_HAVE_UPDATED;

}
