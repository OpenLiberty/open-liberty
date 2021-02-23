/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

goog.provide("constants");

var constants = {
    
    // Constants used for environment
    ENVIRONMENT_COLLECTIVE: "collective",
    ENVIRONMENT_LOCAL: "local",
    
    // Constants used for embedded
    EMBEDDED_ADMIN_CENTER: "adminCenter",
    EMBEDDED_EXPLORE_TOOL: "exploreTool",

    // Hash for server config tool
    SERVER_CONFIG_HASH: "serverConfig",
    
    // Segments for explore tool paths
    EXPLORE_TOOL_HASH: "explore",
    EXPLORE_TOOL_SERVERS_SEGMENT: "servers",
    EXPLORE_TOOL_CONFIGURE_SEGMENT: "serverConfig",

    // Unit time for animations (in ms)
    ANIMATION_TIME_UNIT: 100,
    
    // Dropins directory location
    CONFIG_DROPINS_DEFAULTS_DIRECTORY: "${server.config.dir}/configDropins/defaults",
    CONFIG_DROPINS_OVERRIDES_DIRECTORY: "${server.config.dir}/configDropins/overrides",

    // Constants used for server status
    STARTED: "STARTED"
    
};