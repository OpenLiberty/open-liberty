/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module.internal;

import java.io.File;

import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;

public class ModuleInfoUtils {
    public static String getModuleURIFromLocation(String location) {
        int index = location.lastIndexOf('/');
        if (File.separatorChar != '/') {
            index = Math.max(index, location.lastIndexOf(File.separatorChar));
        }

        String moduleURI = location.substring(index + 1);
        if (moduleURI.endsWith(".xml")) {
            moduleURI = moduleURI.substring(0, moduleURI.length() - 4);
        }

        return moduleURI;
    }

    public static String getModuleName(ModuleDeploymentDescriptor dd, String moduleURI) {
        String moduleName = dd == null ? null : dd.getModuleName();
        if (moduleName == null) {
            moduleName = getModuleNameFromURI(moduleURI);
        }

        return moduleName;
    }

    public static String getModuleNameFromURI(String moduleURI) {
    	String moduleName = moduleURI.substring(moduleURI.lastIndexOf('/') + 1);
        if (moduleName.endsWith(".war") || moduleName.endsWith(".rar") || moduleName.endsWith(".jar")) {
            moduleName = moduleName.substring(0, moduleName.length() - 4);
        }

        return moduleName;
    }
}
