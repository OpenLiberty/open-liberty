/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

@Trivial
public enum ServerPath {
    INSTALL_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_INSTALL_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_INSTALL_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            int index = userDir.lastIndexOf("usr/");
            return userDir.substring(0, index);
        }
    },
    USER_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_USER_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_USER_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir;
        }
    },
    OUTPUT_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_SERVER_OUTPUT_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir + "servers/" + serverName + "/";
        }
    },
    CONFIG_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_SERVER_CONFIG_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir + "servers/" + serverName + "/";
        }
    },
    SHARED_APPS_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_SHARED_APPS_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_SHARED_APPS_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir + "shared/apps/";
        }
    },
    SHARED_CONFIG_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_SHARED_CONFIG_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_SHARED_CONFIG_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir + "shared/config/";
        }
    },
    SHARED_RESC_DIR {
        @Override
        public String getSymbol() {
            return WsLocationConstants.SYMBOL_SHARED_RESC_DIR;
        }

        @Override
        public String getName() {
            return WsLocationConstants.LOC_SHARED_RESC_DIR;
        }

        @Override
        public String getDefault(String userDir, String serverName) {
            return userDir + "shared/resources/";
        }
    };

    public abstract String getSymbol();

    public abstract String getName();

    public abstract String getDefault(String userDir, String serverName);
}
