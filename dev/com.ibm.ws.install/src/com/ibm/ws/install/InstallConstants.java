/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
}
