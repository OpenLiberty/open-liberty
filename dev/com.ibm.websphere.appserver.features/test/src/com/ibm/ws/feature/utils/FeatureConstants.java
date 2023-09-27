/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

package com.ibm.ws.feature.utils;

/**
 * Feature constants.
 */
public interface FeatureConstants {

    // Base BND constants ...

    String BND_TRUE = "true";

    String BND_SYMBOLIC_NAME = "symbolicName";
    String BND_PRODUCT_EDITION = "edition";
    String BND_PRODUCT_KIND = "kind";
    String BND_SINGLETON = "singleton";
    String BND_VISIBILITY = "visibility";

    // Attribute values ...

    String KIND_NOSHIP = "noship";
    String KIND_BETA = "beta";
    String KIND_GA = "ga";

    String EDITION_FULL = "full";
    String EDITION_UNSUPPORTED = "unsupported";
    String EDITION_ZOS = "zos";
    String EDITION_ND = "nd";
    String EDITION_BASE = "base";
    String EDITION_CORE = "core";

    String VISIBILITY_AUTO = "auto";
    String VISIBILITY_PRIVATE = "private";
    String VISIBILITY_PROTECTED = "protected";
    String VISIBILITY_PUBLIC = "public";

    // Subsystem definition ...

    String SUBSYSTEM_NAME = "Subsystem-Name";
    String SUBSYSTEM_SYMBOLIC_NAME = "Subsystem-SymbolicName";
    String SUBSYSTEM_TYPE = "Subsystem-Type";
    String SUBSYSTEM_VERSION = "Subsystem-Version";
    String SUBSYSTEM_CONTENT = "Subsystem-Content";
    String SUBSYSTEM_DESCRIPTION = "Subsystem-Description";
    String SUBSYSTEM_LOCALIZATION = "Subsystem-Localization";
    String SUBSYSTEM_MANIFEST_VERSION = "Subsystem-ManifestVersion";

    // Various content types ... stored within Subsystem-Content.

    String IBM_PROVISION_CAPABILITY = "IBM-Provision-Capability";
    String CONTENT_FILES = "-files";
    String CONTENT_JARS = "-jars";
    String CONTENT_FEATURES = "-features";
    String CONTENT_BUNDLES = "-bundles";

    // IBM specific attributes ...

    String IBM_APPLIES_TO = "IBM-AppliesTo";
    String IBM_APP_FORCE_RESTART = "IBM-App-ForceRestart";
    String IBM_FEATURE_VERSION = "IBM-Feature-Version";
    String IBM_INSTALL_POLICY = "IBM-Install-Policy";
    String IBM_SHORT_NAME = "IBM-ShortName";
    String IBM_LICENSE_INFORMATION = "IBM-License-Information";
    String IBM_LICENSE_AGREEMENT = "IBM-License-Agreement";

    String WLP_ACTIVATION_TYPE = "WLP-Activation-Type";
    String WLP_ACTIVATION_TYPE_PARALLEL = "parallel";
    String WLP_DISABLE_ALL_FEATURES_ON_CONFLICT = "WLP-DisableAllFeatures-OnConflict";
    String WLP_ALSO_KNOWN_AS = "WLP-AlsoKnownAs";

    // ??

    String OSGI_SUBSYSTEM_FEATURE = "osgi.subsystem.feature";
    String OSGI_IDENTITY = "osgi.identity";
    String OSGI_IDENTITY_PREFIX = "osgi.identity=";

    String IBM_TOLERATES = "ibm.tolerates:";

    String[] IGNORED_AUTOFEATURE_PREFIXES = { "com.", "io.openliberty." };

}
