/*******************************************************************************
 * Copyright (c) 2018,2024 IBM Corporation and others.
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

package com.ibm.ws.kernel.feature.internal.util;

public interface FeatureConstants {
    // BND:

    String SUBSYSTEM_NAME = "Subsystem-Name";
    String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";

    String SUBSYSTEM_SYMBOLIC_NAME = "Subsystem-SymbolicName";
    String SUBSYSTEM_VERSION = "Subsystem-Version";
    String SUBSYSTEM_TYPE = "Subsystem-Type";

    String SUBSYSTEM_CONTENT = "Subsystem-Content";

    String SUBSYSTEM_MANIFEST_VERSION = "Subsystem-ManifestVersion";

    String SUBSYSTEM_LOCALIZATION = "Subsystem-Localization";

    // ??

    String SYMBOLIC_NAME = "symbolicName";

    // OSGI:

    String OSGI_SUBSYSTEM_FEATURE = "osgi.subsystem.feature";
    String OSGI_IDENTITY = "osgi.identity=";

    // IBM:

    String IBM_SHORT_NAME = "IBM-ShortName";

    String IBM_FEATURE_VERSION = "IBM-Feature-Version";

    String IBM_PROVISION_CAPABILITY = "IBM-Provision-Capability";

    String IBM_INSTALL_POLICY = "IBM-Install-Policy";

    String IBM_APP_FORCE_RESTART = "IBM-App-ForceRestart";

    String IBM_LICENSE_INFORMATION = "IBM-License-Information";

    String IBM_LICENSE_AGREEMENT = "IBM-License-Agreement";

    String IBM_CONTENT_FILES = "-files";
    String IBM_CONTENT_JARS = "-jars";
    String IBM_CONTENT_FEATURES = "-features";
    String IBM_CONTENT_BUNDLES = "-bundles";

    String IBM_API_PACKAGE = "IBM-API-Package";
    String IBM_SPI_PACKAGE = "IBM-SPI-Package";
    String IBM_SPI = "ibm-spi";

    // IBM:

    String EDITION = "edition";
    String KIND = "kind";
    String SINGLETON = "singleton";
    String VISIBILITY = "visibility";
    String VISIBILITY_PRIVATE = "private";
    String VISIBILITY_PUBLIC = "public";

    // IBM/WLP:

    String WLP_ALSO_KNOWN_AS = "WLP-AlsoKnownAs";
    String WLP_ACTIVATION_TYPE = "WLP-Activation-Type";
    String WLP_ACTIVATION_TYPE_PARALLEL = "parallel";
    String WLP_DISABLE_ON_CONFLICT = "WLP-DisableAllFeatures-OnConflict";
    String WLP_INSTANT_ON_ENABLED = "WLP-InstantOn-Enabled";
    String WLP_PLATFORM = "WLP-Platform";
}
