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

public interface FeatureXMLConstants {
    String XML_FEATURES = "features";
    String XML_FEATURE = "feature";

    String XML_SUBSYSTEM_SYMBOLIC_NAME = "symbolic-name";

    String XML_FEATURE_NAME = "feature-name";
    String XML_DESCRIPTION = "description";

    String XML_SHORT_NAME = "short-name";
    String XML_ALSO_KNOWN_AS = "also-known-as";

    String XML_PLATFORMS = "platforms";

    String XML_EDITION = "edition";
    String XML_KIND = "kind";
    String XML_SINGLETON = "singleton";
    String XML_VISIBILITY = "visibility";

    String XML_IS_SINGLETON = "singleton";
    String XML_FORCE_RESTART = "force-restart";
    String XML_PARALLEL = "parallel";
    String XML_DISABLE_ON_CONFLICT = "disable-on-conflict";
    String XML_INSTANT_ON_ENABLED = "instant-on";

    String XML_LICENSE_INFORMATION = "license-information";
    String XML_LICENSE_AGREEMENT= "license-agreement";

    String XML_DEPENDENT = "dependent";

    String XML_API_PACKAGE = "api";
    String XML_SPI_PACKAGE = "spi";
}
