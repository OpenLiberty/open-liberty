/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module;

import java.io.File;

import com.ibm.ws.app.manager.internal.AppManagerConstants;

/**
 *
 */
public abstract class AbstractDeployedAppInfoFactory implements DeployedAppInfoFactory {

    protected enum BinaryType {
        ARCHIVE, LOOSE, DIRECTORY
    }

    protected BinaryType getApplicationType(File file, String path) {
        if (path.toLowerCase().endsWith(AppManagerConstants.XML_SUFFIX))
            return BinaryType.LOOSE;

        if (!file.isFile())
            return BinaryType.DIRECTORY;

        return BinaryType.ARCHIVE;
    }
}
