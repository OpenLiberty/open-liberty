/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.logging.internal;

/**
 * This class contains the transformed Liberty event field names to match OpenTelemetry Log Attributes naming convention.
 */
public class MpTelemetryLogFieldConstants {

    //Common Mapped OTel Attribute Log fields
    public static final String LIBERTY_TYPE = "io.openliberty.type";
    public static final String LIBERTY_SEQUENCE = "io.openliberty.sequence";
    public static final String LIBERTY_CLASSNAME = "io.openliberty.class_name";

    //Mapped OTel Attribute Liberty message and trace log fields
    public static final String LIBERTY_MESSAGEID = "io.openliberty.message_id";
    public static final String LIBERTY_MODULE = "io.openliberty.module";
    public static final String LIBERTY_METHODNAME = "io.openliberty.method_name";

    //Mapped OTel Attribute Liberty FFDC log fields
    public static final String LIBERTY_PROBEID = "io.openliberty.probe_id";
    public static final String LIBERTY_OBJECTDETAILS = "io.openliberty.object_details";

    //Mapped OTel Attribute Liberty LogRecordContext Extension fields
    public static final String LIBERTY_EXT_APP_NAME = "io.openliberty.ext.app_name";

    // Miscellaneous
    public static final String EXT_APPNAME = "ext_appName";
    public static final String EXT_THREAD = "ext_thread";
    public static final String IO_OPENLIBERTY_TAG = "io.openliberty.";
    public static final String IO_OPENLIBERTY_EXT_TAG = "io.openliberty.ext.";

    // OpenTelemetry Scope Info field
    public static final String OTEL_SCOPE_INFO = "scopeInfo:";
}
