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
//     <platform>platformName</platform>
//     <environment>name=value</environment>
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
    String KERNEL_TAG = "kernel";
    String ROOT_TAG = "root";
    String PLATFORM_TAG = "platform";
    String ENV_TAG = "environment";
    char ENV_CHAR = '=';

    String OUTPUT_TAG = "output";
    String RESOLVED_TAG = "resolved";
    String KERNEL_ONLY_TAG = "kernelOnly";
    String KERNEL_BLOCKED_TAG = "kernelBlocked";

    String PLATFORM_RESOLVED_TAG = "resolved-platform";
    String PLATFORM_MISSING_TAG = "missing-platform";
    String PLATFORM_DUPLICATE_TAG = "duplicate-platform";

    String VERSIONLESS_RESOLVED_TAG = "resolved-versionless";
    char RESOLVED_CHAR = '=';
    String VERSIONLESS_NO_PLATFORM_TAG = "no-platform-versionless";

    String RESOLVED_FEATURE_TAG = "resolved-feature";
    String MISSING_FEATURE_TAG = "missing-feature";
    String NON_PUBLIC_FEATURE_TAG = "non-public-feature";
    String WRONG_PROCESS_FEATURE_TAG = "wrong-process-feature";
    String CONFLICT_FEATURE_TAG = "conflict-feature";
}
