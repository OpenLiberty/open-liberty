/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

// <cases>
// <case>
//   <name>name</name>
//   <description>description</description>
//   <duration>123456</duration>
//   <input>
//     <kernel>featureName</kernel>
//     <root>featureName</root>
//     <client/>
//     <server/>
//   </input>
//   <output>
//     <dependent>FeatureName</dependent>
//     <resolved>FeatureName</resolved>
//   </output>
// </case>
// </cases>

public interface VerifyXMLConstants {
    String CASES_TAG = "cases";
    String CASE_TAG = "case";
    String NAME_TAG = "name";
    String DESCRIPTION_TAG = "description";
    String DURATION_TAG = "duration";

    String INPUT_TAG = "input";
    String MULTIPLE_TAG = "multiple";
    String CLIENT_TAG = "client";
    String SERVER_TAG = "server";
    String KERNEL_TAG = "kernel";
    String ROOT_TAG = "root";

    String OUTPUT_TAG = "output";
    String RESOLVED_TAG = "resolved";
    String KERNEL_ONLY_TAG = "kernelOnly";
    String KERNEL_BLOCKED_TAG = "kernelBlocked";
}
