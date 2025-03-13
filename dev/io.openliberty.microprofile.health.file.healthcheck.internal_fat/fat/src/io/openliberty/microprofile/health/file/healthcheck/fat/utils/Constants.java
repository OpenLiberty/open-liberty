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

    public static final String SHOULD_HAVE = " should have been created.";
    public static final String SHOULD_NOT_HAVE = " should not have been created.";

    public static final String HEALTH_DIR_SHOULD_HAVE = "/health" + SHOULD_HAVE;

    public static final String LIVE_SHOULD_HAVE = "/health/live" + SHOULD_HAVE;
    public static final String LIVE_SHOULD_NOT_HAVE = "/health/live" + SHOULD_NOT_HAVE;

    public static final String STARTED_SHOULD_HAVE = "/health/started" + SHOULD_HAVE;
    public static final String STARTED_SHOULD_NOT_HAVE = "/health/started" + SHOULD_NOT_HAVE;

    public static final String READY_SHOULD_HAVE = "/health/ready" + SHOULD_HAVE;
    public static final String READY_SHOULD_NOT_HAVE = "/health/ready" + SHOULD_NOT_HAVE;

}
