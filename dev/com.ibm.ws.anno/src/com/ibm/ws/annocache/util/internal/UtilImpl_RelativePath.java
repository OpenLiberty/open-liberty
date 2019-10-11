/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import com.ibm.wsspi.annocache.util.Util_RelativePath;

/**
 *
 */
public class UtilImpl_RelativePath implements Util_RelativePath {

    public UtilImpl_RelativePath(String n_basePath, String n_relativePath, String n_fullPath) {
        this.n_basePath = n_basePath;
        this.n_relativePath = n_relativePath;
        this.n_fullPath = n_fullPath;
    }

    //

    protected final String n_basePath;
    protected final String n_relativePath;
    protected final String n_fullPath;

    @Override
    public String n_getBasePath() {
        return n_basePath;
    }

    @Override
    public String n_getRelativePath() {
        return n_relativePath;
    }

    @Override
    public String n_getFullPath() {
        return n_fullPath;
    }
}
