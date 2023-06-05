/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.install;

import java.io.File;

/**
 * This class contains constants for Install functions.
 */
public class InstallConstants {

    public static final String LOGGER_NAME = "com.ibm.ws.install";

    public static final String TO_CORE = "core";
    public static final String TO_USER = "usr";

    public static final String EVENT_TYPE_PROGRESS = "PROGRESS";

    public static final String USER_AGENT = "com.ibm.websphere.appserver/%s (%s; %s)";
    public static final String UA_PROPERTY_NAME = "user.agent";
    public static final String FEATURE_MANAGER = "featureManager";
    public static final String ASSET_MANAGER = "installUtility";
    public static final String IGNORE_FILE_PROPERTY = "com.ibm.websphere.install.utility.ignoreWhenFileExists";
    public static final String IGNORE_FILE_OPTION = System.getProperty(IGNORE_FILE_PROPERTY);

    public static final String DEFAULT_REPO_PROPERTIES_LOCATION = File.separator + "etc" + File.separator
                                                                  + "repositories.properties";
    public static final String OVERRIDE_PROPS_LOCATION_ENV_VAR = "WLP_REPOSITORIES_PROPS";
    public static final String REPO_PROPERTIES_PROXY_HOST = "proxyHost";
    public static final String REPO_PROPERTIES_PROXY_PORT = "proxyPort";
    public static final String REPO_PROPERTIES_PROXY_USER = "proxyUser";
    public static final String REPO_PROPERTIES_PROXY_USERPWD = "proxyUserPassword";
    public static final String REPO_PROPERTIES_PROXY_PWD = "proxyPassword";
    public static final int PROXY_AUTH_HTTP_RESPONSE_CODE = 407;

    public static final String REPOSITORY_LIBERTY_URL = "https://asset-websphere.ibm.com/ma/v1";

    public static final String ADDON = "addon";
    public static final String FEATURE = "feature";
    public static final String IFIX = "ifix";
    public static final String SAMPLE = "sample";
    public static final String PRODUCTSAMPLE = "productsample";
    public static final String OPENSOURCE = "opensource";

    public static final String ADDONS = "addons";
    public static final String FEATURES = "features";
    public static final String SAMPLES = "samples";

    public static final String NOVERSION = "noversion";
    public static final String VERSIONLESS = "VERSIONLESS";
    public static final String PRODUCTNAME = "IBM WebSphere Application Server Liberty";

    public static final String LICENSE_EPL_PREFIX = "https://www.eclipse.org/legal/epl-";
    public static final String LICENSE_FEATURE_TERMS = "http://www.ibm.com/licenses/wlp-featureterms-v1";
    public static final String LICENSE_FEATURE_TERMS_RESTRICTED = "http://www.ibm.com/licenses/wlp-featureterms-restricted-v1";

    public static final String ENVIRONMENT_VARIABLE_MAP = "environment.variable.map";
    public static final String ACTION_ERROR_MESSAGE = "action.error.message";
    public static final String ACTION_EXCEPTION_STACKTRACE = "action.exception.stacktrace";
    public static final String JSON_PROVIDED = "json.provided";
    public static final String IS_OPEN_LIBERTY = "is.open.liberty";
    public static final String CLEANUP_NEEDED = "cleanup.needed";
    public static final String IS_FEATURE_UTILITY = "is.feature.utility";
    public static final String RUNTIME_INSTALL_DIR = "runtime.install.dir";
    public static final String INSTALL_LOCAL_ESA = "install.local.esa";
    public static final String SINGLE_JSON_FILE = "single.json.file";
    public static final String FEATURES_TO_RESOLVE = "features.to.resolve";
    public static final String INDIVIDUAL_ESAS = "individual.esas";
    public static final String INSTALL_INDIVIDUAL_ESAS = "install.individual.esas";
    public static final String LICENSE_ACCEPT = "license.accept";
    public static final String INSTALL_KERNEL_INIT_CODE = "install.kernel.init.code";
    public static final String OVERRIDE_ENVIRONMENT_VARIABLES = "override.environment.variables";
    public static final String ACTION_FIND = "action.find";
    public static final String FROM_REPO = "from.repo";
    public static final String DOWNLOAD_ARTIFACT_LIST = "download.artifact.list";
    public static final String DOWNLOAD_INDIVIDUAL_ARTIFACT = "download.individual.artifact";
    public static final String DOWNLOAD_RESULT = "download.result";
    public static final String ACTION_RESULT = "action.result";
    public static final String IS_INSTALL_SERVER_FEATURE = "is.install.server.feature";
    public static final String UPGRADE_COMPLETE = "upgrade.complete";
    public static final String CAUSED_UPGRADE = "caused.upgrade";
    public static final String CLEANUP_UPGRADE = "cleanup.upgrade";
    public static final String ACTION_INSTALL = "action.install";
    public static final String TO_EXTENSION = "to.extension";
    public static final String DOWNLOAD_LOCATION = "download.location";
    public static final String CLEANUP_TEMP_LOCATION = "cleanup.temp.location";
    public static final String VERIFY_OPTION = "verify.option";
    public static final String ACTION_VERIFY = "action.verify";
    public static final String USER_PUBLIC_KEYS = "user.public.keys";
    public static final String KEYID_QUALIFIER = "keyid";
    public static final String KEYURL_QUALIFIER = "keyurl";
    public static final String DOWNLOAD_PUBKEYS = "download.pubkeys";

    /**
     * An enum for specifying what action to take if a file to be installed
     * already exists.
     */
    public enum ExistsAction {
        /**
         * Continue the install and ignore the file that exists
         */
        ignore,

        /**
         * Abort the install
         */
        fail,

        /**
         * Overwrite the existing file
         */
        replace;
    }

    /**
     * An enum for specifying download option
     */
    public enum DownloadOption {
        /**
         * download the specified feature only without dependencies
         */
        none,

        /**
         * download the feature with its required dependencies, which are not
         * installed
         */
        required,

        /**
         * download the feature with all its dependencies, no matter they are
         * installed or not
         */
        all;
    }

    /**
     * An enum for specifying --type option of find action
     */
    public enum AssetType {
        addon, feature, sample, opensource, all
    }

    /**
     * An enum for specifying verify option
     */
    public enum VerifyOption {
        enforce, warn, skip, all;
    }

}
