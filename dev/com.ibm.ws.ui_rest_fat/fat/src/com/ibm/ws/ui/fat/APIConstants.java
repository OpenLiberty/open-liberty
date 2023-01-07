/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat;

/**
 *
 */
public interface APIConstants {
    final String API_ROOT = "/ibm/api/adminCenter";
    final String API_V1_ROOT = API_ROOT + "/v1";
    final String API_V1_CATALOG = API_V1_ROOT + "/catalog";
    final String API_V1_TOOLBOX = API_V1_ROOT + "/toolbox";
    final String API_V1_ICONS = API_V1_ROOT + "/icons";
    final String API_V1_TOOLDATA = API_V1_ROOT + "/tooldata";

    // Helper URL used by the test flows to reset the catalog
    final String RESET_CATALOG_URL = API_V1_CATALOG + "?resetCatalog=true";
    final String RESET_TOOLBOX_URL = API_V1_TOOLBOX + "?resetToolbox=true";

    // Catalog.json constants
    static final String RESOURCES_ADMIN_CENTER_1_0 = FATSuite.RESOURCES_ADMIN_CENTER_1_0;
    static final String CATALOG_JSON = RESOURCES_ADMIN_CENTER_1_0 + "/catalog.json";

    static final String TEST_FEATURE_PATH = "features/";
    static final String TEST_BUNDLE_PATH = "bundles/";

    static final String MANIFEST_SUFFIX = ".mf";
    static final String PROPERTIES_SUFFIX = ".properties";
    static final String JAR_SUFFIX = ".jar";

    static final String LIB_PATH = "lib/";
    static final String FEATURE_PATH = "lib/features/";
    static final String FEATURE_L10N_PATH = "lib/features/l10n/";
    static final String USR_LIB_PATH = "/usr/extension/lib/";
    static final String USR_FEATURE_PATH = "/usr/extension/lib/features/";
    static final String USR_FEATURE_L10N_PATH = "/usr/extension/lib/features/l10n/";
    static final String ETC_EXTNS_PATH = "/etc/extensions/";
}
