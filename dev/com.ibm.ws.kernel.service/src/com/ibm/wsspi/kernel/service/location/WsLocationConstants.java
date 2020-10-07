/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.location;

/**
 * Defines standard names for properties used by {@link WsResource} and {@WsLocationAdmin}.
 */
public interface WsLocationConstants {
    /**
     * Symbol representing the virtual root of a resource group.
     * This can be used with find/match methods to obtain a list
     * (via Iterator) of the elements in a group's search path.
     */
    String SYMBOL_ROOT_NODE = "${/}";

    String SYMBOL_PREFIX = "${";
    String SYMBOL_SUFFIX = "}";

    /**
     * The properties used by the user to configure the filesystem
     * layout of the server. These can be used when constructing symbols.
     */
    String LOC_VIRTUAL_ROOT = "/",
                    LOC_TMP_DIR = "tmp",
                    LOC_SERVER_NAME = "wlp.server.name",
                    LOC_PROCESS_TYPE = "wlp.process.type",
                    LOC_PROCESS_TYPE_CLIENT = "client",
                    LOC_PROCESS_TYPE_SERVER = "server",
                    LOC_INSTALL_DIR = "wlp.install.dir",
                    LOC_INSTALL_PARENT_DIR = "wlp.parent.dir",
                    LOC_USER_DIR = "wlp.user.dir",
                    LOC_USER_EXTENSION_DIR = "usr.extension.dir",
                    LOC_SERVER_CONFIG_DIR = "server.config.dir",
                    LOC_SERVER_OUTPUT_DIR = "server.output.dir",
                    LOC_SERVER_STATE_DIR = "server.state.dir",
                    LOC_SERVER_WORKAREA_DIR = "server.workarea.dir",
                    LOC_SHARED_APPS_DIR = "shared.app.dir",
                    LOC_SHARED_CONFIG_DIR = "shared.config.dir",
                    LOC_SHARED_RESC_DIR = "shared.resource.dir",
                    LOC_SERVER_UUID = "wlp.server.uuid",
                    LOC_SERVICE_BINDING_ROOT = "wlp.svc.binding.root";
    /**
     * Pre-constructed symbols for user-configured locations;
     * includes trailing slash.
     */
    String SYMBOL_SERVER_NAME = "${wlp.server.name}",
                    SYMBOL_PROCESS_TYPE = "${wlp.process.type}",
                    SYMBOL_INSTALL_DIR = "${wlp.install.dir}/",
                    SYMBOL_INSTALL_PARENT_DIR = "${wlp.parent.dir}/",
                    SYMBOL_USER_DIR = "${wlp.user.dir}/",
                    SYMBOL_USER_EXTENSION_DIR = "${usr.extension.dir}/",
                    SYMBOL_SERVER_CONFIG_DIR = "${server.config.dir}/",
                    SYMBOL_SERVER_OUTPUT_DIR = "${server.output.dir}/",
                    SYMBOL_SERVER_STATE_DIR = "${server.state.dir}/",
                    SYMBOL_SERVER_WORKAREA_DIR = "${server.workarea.dir}/",
                    SYMBOL_SHARED_APPS_DIR = "${shared.app.dir}/",
                    SYMBOL_SHARED_CONFIG_DIR = "${shared.config.dir}/",
                    SYMBOL_SHARED_RESC_DIR = "${shared.resource.dir}/",
                    SYMBOL_TMP_DIR = "${tmp}/",
                    SYMBOL_SERVICE_BINDING_ROOT = "${wlp.svc.binding.root}/";

}
