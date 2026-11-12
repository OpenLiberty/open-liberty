/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat.common;

import java.util.List;

import com.ibm.ws.security.fat.common.apps.AppConstants;

/**
 * Constants for the CSIv2 FAT tests.
 */
public class Constants {

    public static final String CSIv2_UPSTREAM_SERVER = "com.ibm.ws.security.csiv2.fat.servlet.client";
    public static final String CSIv2_JAVA2_UPSTREAM_SERVER = "com.ibm.ws.security.csiv2.fat.servlet.client.java2";
    public static final String CSIv2_DOWNSTREAM_SERVER = "com.ibm.ws.security.csiv2.fat.downstream.server";
    public static final String CSIv2_JAVA2_DOWNSTREAM_SERVER = "com.ibm.ws.security.csiv2.fat.downstream.server.java2";
    public static final String CSIv2_INTERMEDIATE_SERVER = "com.ibm.ws.security.csiv2.fat.intermediate.server";

    public static final String CSIv2_UPSTREAM = "CSIv2_UPSTREAM";
    public static final String CSIv2_DOWNSTREAM = "CSIv2_DOWNSTREAM";
    public static final String CSIv2_INTERMEDIATE = "CSIv2_INTERMEDIATE";

    public static final String CSIv2_CLIENT_APP = "csiv2Client";
    public static final String CSIv2_DOWNSTREAM_APP = "DownstreamApp";
    public static final String CSIv2_INTERMEDIATE_APP = "IntermediateApp";
    public static final String TESTMARKER_APP = AppConstants.TESTMARKER_CONTEXT_ROOT;

    public static final List<String> NO_EXTRA_MSGS = null;
    public static final Boolean JUNIT_REPORTING = true;
    public static final Boolean NO_JUNIT_REPORTING = false;
    public static final boolean FORCE_SERVER_RESTART = false;

}
