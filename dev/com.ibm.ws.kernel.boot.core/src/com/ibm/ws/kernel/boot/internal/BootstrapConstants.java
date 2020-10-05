/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.util.ResourceBundle;

public final class BootstrapConstants {
    /** Since this launches the framework, we have to do translation ourselves.. */
    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.kernel.boot.resources.LauncherMessages");

    /** Bootstrap property specifing other properties resources to include. */
    public static final String BOOTPROP_INCLUDE = "bootstrap.include";

    /**
     * Properties for communicating information about the kernel/product from
     * the launcher to the log provider.
     */
    public static final String BOOTPROP_KERNEL_INFO = "websphere.kernel.info",
                    BOOTPROP_PRODUCT_INFO = "websphere.product.info";
    /**
     * Internally set property containing the output file name provided
     * on command line
     */
    public static final String CLI_ARG_ARCHIVE_TARGET = "archive";

    public static final String CLI_PACKAGE_INCLUDE_VALUE = "include";

    public static final String CLI_ROOT_PACKAGE_NAME = "server-root";

    /** Store command line arguments for the CommandLine service */
    public static final String INTERNAL_COMMAND_LINE_ARG_LIST = "commandline.args";

    /**
     * Property key designating packages exported into the framework by this jar
     */
    public static final String INITPROP_OSGI_EXTRA_PACKAGE = "org.osgi.framework.system.packages.extra";

    /** Kernel properties for clean start */
    public static final String INITPROP_OSGI_CLEAN = "org.osgi.framework.storage.clean";

    public static final String OSGI_CLEAN_VALUE = "onFirstInit";

    /**
     * Property key designating if system packages from previous java versions should be inherited
     * Default value is 'true'
     */
    public static final String INITPROP_WAS_INHERIT_SYSTEM_PACKAGES = "websphere.inherit.system.packages";

    /**
     * Property key designating packages exported into the framework by the
     * system bundle
     */
    public static final String INITPROP_OSGI_SYSTEM_PACKAGES = "org.osgi.framework.system.packages";

    /**
     * Allow to use system packages first, the default value of equinox is "true"
     */
    public static final String CONFIG_OSGI_PREFER_SYSTEM_PACKAGES = "osgi.resolver.preferSystemPackages";

    /** Provisioning configuration directory */
    public static final String PLATFORM_DIR_NAME = "platform",
                    FEATURES_DIR_NAME = "features";

    /** Timestamp for initial launch of the runtime */
    public static final String LAUNCH_TIME = "kernel.launch.time";

    /** WAS product install location */
    public static final String ENV_WAS_INSTALL_DIR = "WAS_INSTALL_DIR";
    /** WLP product install location */
    public static final String ENV_WLP_INSTALL_DIR = "WLP_INSTALL_DIR"; // implied/assumed location
    /** WLP user directory location: wlp/usr by default */
    public static final String ENV_WLP_USER_DIR = "WLP_USER_DIR";
    /** WLP output directory location: wlp/usr/servers by default */
    public static final String ENV_WLP_OUTPUT_DIR = "WLP_OUTPUT_DIR";
    /** WLP output directory location: wlp/usr/clients by default */
    public static final String ENV_WLP_CLIENT_OUTPUT_DIR = "WLP_CLIENT_OUTPUT_DIR";
    /** Log directory environment variable set by the script */
    public static final String ENV_X_LOG_DIR = "X_LOG_DIR";
    /** Log directory environment variable that may be present from direct invocation of java */
    public static final String ENV_LOG_DIR = "LOG_DIR";
    /** Log directory environment variable set by the script */
    public static final String ENV_X_LOG_FILE = "X_LOG_FILE";
    /** Log directory environment variable that may be present from direct invocation of java */
    public static final String ENV_LOG_FILE = "LOG_FILE";
    /** Product extensions added by embedder */
    public static final String ENV_PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER = "PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER";
    /** Product Extensions added by Environment Variable */
    public static final String ENV_PRODUCT_EXTENSIONS_ADDED_BY_ENV = "PRODUCT_EXTENSIONS_ADDED_BY_ENV";

    public static final String LOC_PROPERTY_INSTALL_DIR = "wlp.install.dir";
    public static final String LOC_INTERNAL_LIB_DIR = "wlp.lib.dir";

    public static final String LOC_PROPERTY_INSTANCE_DIR = "wlp.user.dir";

    /**
     * Used (especially in the runnable JAR case at present) to specify that we should ignore WLP_USER_DIR set from the environment, and use
     * something else as user dir. E.g. we might use: "${wlp.install.dir}/usr"
     */
    public static final String LOC_PROPERTY_IGNORE_INSTANCE_DIR_FROM_ENV = "wlp.ignore.user.dir.from.env";

    /** Tracks whether the user dir is set to the default "wlp.install.dir/usr", either via {@link LOC_PROPERTY_IGNORE_INSTANCE_DIR_FROM_ENV} or by virtue of being unset */
    public static final String LOC_PROPERTY_INSTANCE_DIR_IS_DEFAULT = "wlp.user.dir.isDefault";

    public static final String LOC_PROPERTY_SRVCFG_DIR = "server.config.dir";
    public static final String LOC_PROPERTY_SRVOUT_DIR = "server.output.dir";

    public static final String LOC_PROPERTY_CLIENTCFG_DIR = "client.config.dir";
    public static final String LOC_PROPERTY_CLIENTOUT_DIR = "client.output.dir";

    public static final String LOC_PROPERTY_SHARED_APP_DIR = "shared.app.dir";
    public static final String LOC_PROPERTY_SHARED_CONFIG_DIR = "shared.config.dir";
    public static final String LOC_PROPERTY_SHARED_RES_DIR = "shared.resource.dir";

    public static final String LOC_PROPERTY_SRVTMP_DIR = "server.tmp.dir";
    public static final String LOC_PROPERTY_CLIENTTMP_DIR = "client.tmp.dir";

    public static final String LOC_PROPERTY_PROCESS_TYPE = "wlp.process.type";
    public static final String LOC_PROCESS_TYPE_SERVER = "server";
    public static final String LOC_PROCESS_TYPE_CLIENT = "client";

    public static final String LOC_AREA_NAME_USR = "usr";
    public static final String LOC_AREA_NAME_SERVERS = "servers";
    public static final String LOC_AREA_NAME_CLIENTS = "clients";
    public static final String LOC_AREA_NAME_SHARED = "shared";
    public static final String LOC_AREA_NAME_EXTENSION = "extension";
    public static final String LOC_AREA_NAME_LIB = "lib";
    public static final String LOC_AREA_NAME_WORKING = "workarea"; // default
    public static final String LOC_AREA_NAME_WORKING_UTILS = "workarea-utils";
    public static final String LOC_AREA_NAME_APP = "apps";
    public static final String LOC_AREA_NAME_RES = "resources";
    public static final String LOC_AREA_NAME_CFG = "config";
    public static final String LOC_AREA_NAME_TMP = "tmp";
    public static final String LOC_AREA_NAME_DROP = "dropins";
    public static final String LOC_AREA_NAME_LOGS = "logs";

    public static final String DEFAULT_SERVER_NAME = "defaultServer";
    public static final String DEFAULT_CLIENT_NAME = "defaultClient";

    /** Server and Client names are not configurable via bootstrap property: it is specified on the command line only */
    public static final String INTERNAL_SERVER_NAME = "wlp.server.name";
    public static final String INTERNAL_CLIENT_NAME = "wlp.client.name";

    /** The workarea for Embedded servers may be specified by utility commands (i.e. not by the user) */
    public static final String LOC_INTERNAL_WORKAREA_DIR = "wlp.workarea.dir";

    /**
     * Indicates whether or not the server needs to be verified for existence.
     * Either null if the server does not need to be verified, or the name of
     * one of the {@link VerifyServer} values.
     */
    public static final String INTERNAL_VERIFY_SERVER = "verify.server";

    public enum VerifyServer {
        /**
         * Requires that the server exist.
         */
        EXISTS,

        /**
         * Create the server if it does not already exist.
         */
        CREATE,

        /**
         * Creates the default server if it does not already exist, but require
         * other servers to exist.
         */
        CREATE_DEFAULT,

        /**
         * Do no verification at all.
         */
        SKIP;

        /**
         * @return true if this VerifyServer value will result in the server dir being created
         */
        public boolean willCreate() {
            return this == CREATE || this == CREATE_DEFAULT;
        }
    }

    public static final String INTERNAL_USE_TEMPLATE = "template.extension";

    public static final String INTERNAL_START_SIMULATION = "server.no.start";

    /** name of a lock file used to lock each server */
    public static final String S_LOCK_FILE = ".sLock";

    public static final String S_COMMAND_FILE = ".sCommand";

    /** name of the directory for server command authorization checks */
    public static final String S_COMMAND_AUTH_DIR = ".sCommandAuth";

    /** name of the bootstrap / jvm property that specifies the command listener port */
    public static final String S_COMMAND_PORT_PROPERTY = "command.port";

    /** Default name of bootstrap properties file. */
    public static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";

    public static final String SERVER_XML = "server.xml";
    public static final String CLIENT_XML = "client.xml";

    /** Default value for {@link #ENV_X_LOG_FILE}. */
    public static final String CONSOLE_LOG = "console.log";

    public static final String SERVER_DUMP_FOLDER_PREFIX = "dump_";

    public static final String SERVER_INTROSPECTION_FOLDER_NAME = "introspections";

    public static final String SERVER_DUMPED_FLAG_FILE_NAME = ".dumped";

    public static final String SERVER_DUMPED_FILE_LOCATIONS = ".dumpedjava";

    public static final String SERVER_LIB_INVENTORY_FILE_NAME = "library-inventory";

    public static final String SERVER_PACKAGE_INFO_FILE_PREFIX = "package_";

    public static final String SERVER_TEMP_FOLDER_PREFIX = "temp_";

    public static final String SERVER_NAME_PREFIX = "wlp-";

    public static final String SERVER_RUNNING_FILE = ".sRunning";

    public static final String JAVA_SPEC_VERSION = "java.specification.version";

    public static final String JAVA_VENDOR = "java.vendor";

    public static final String JAVA_2_SECURITY_PROPERTY = "websphere.java.security";

    public static final String JAVA_2_SECURITY_NORETHROW = "websphere.java.security.norethrow";

    public static final String JAVA_2_SECURITY_UNIQUE = "websphere.java.security.unique";

    public final static String REQUEST_SERVER_CONTENT_PROPERTY = "com.ibm.ws.liberty.content.request";

    public final static String REQUEST_SERVER_FEATURES_PROPERTY = "com.ibm.ws.liberty.feature.request";

    /**
     * The number of milliseconds to wait between poll attempts.
     */
    public static final long POLL_INTERVAL_MS = 500;

    /**
     * The maximum number of poll attempts.
     */
    public static final int MAX_POLL_ATTEMPTS = 60;

    /** The JVM property that when set to true indicates disabling the command port by default */
    public final static String DEFAULT_COMMAND_PORT_DISABLED_PROPERTY = "com.ibm.ws.kernel.default.command.port.disabled";

    /** OSGi property to request clean, boolean value **/
    public static final String OSGI_CLEAN = "osgi.clean";

    /** The number of milliseconds to wait for the server process to start */
    public static final String SERVER_START_WAIT_TIME = "server.start.wait.time";

    /** The key for the SSL client command-line option "--autoAcceptSigner" **/
    public static final String AUTO_ACCEPT_SIGNER = "autoAcceptSignerCertificate";

    /**
     * The boot strap config key used to enable liberty boot mode.
     * The value of this key must set to the string 'true' to enable
     * liberty boot mode.
     */
    public static final String LIBERTY_BOOT_PROPERTY = "wlp.liberty.boot";

    public static final String ENV_SERVICE_BINDING_ROOT = "SERVICE_BINDING_ROOT";

    public static final String LOC_PROPERTY_SERVICE_BINDING_ROOT = "wlp.svc.binding.root";
}
