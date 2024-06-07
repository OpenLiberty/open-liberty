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

// <repository>
// <feature>
//    <repository-type>repository</repository-type>
//
//    <file>fileName</file>
//    <checksum>checksum</checksum>
//
//    <name>name</name>
//    <symbolic>symbolicName</symbolic>
//    <short>shortName</short>
//    <version>version</version>
//    <ibm-version>ibmVersion</ibm-version>
//    <supported-version>supported</supported-version>
//
//    <auto>auto</auto>
//    <visibility>visibility</visibility>
//    <server>isServer</server>
//    <client>isClient</client>
//    <singleton>singleton</singleton>
//
//    <restart>restart</restart>
//
//    <constituent>constituent</constituent>
// </feature>
// </repository>

public interface RepoXMLConstants {
    String REPOSITORY_TAG = "repository";

    String REPOSITORY_TYPE_TAG = "repository-type";

    String FEATURE_TAG = "feature";
    String FILE_TAG = "file";
    String CHECKSUM_TAG = "checksum";

    String NAME_TAG = "name";
    String SYMBOLIC_NAME_TAG = "symbolic-name";
    String SHORT_NAME_TAG = "short-name";
    String VERSION_TAG = "version";
    String IBM_VERSION_TAG = "ibm-version";

    String AUTO_TAG = "auto";
    String SINGLETON_TAG = "singleton";
    String VISIBILITY_TAG = "visibility";
    String SERVER_TAG = "server";
    String CLIENT_TAG = "client";

    String RESTART_TAG = "restart";
    String SUPPORTED_VERSION_TAG = "supported-version";

    String CONSTITUENT_TAG = "constituent";
    String LOCATION_TAG = "location";

    String START_LEVEL_TAG = "start-level";
    String ACTIVATION_TYPE_TAG = "activation-type";
    String TYPE_TAG = "type";
    String JAVA_RANGE_TAG = "java-range";
    String VERSION_RANGE_TAG = "version-range";

    String TOLERATE_TAG = "tolerate";
    String WLP_PLATFORM_TAG = "WLP-Platform";
}
