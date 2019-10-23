/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.File;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.util.internal.UtilImpl_FileUtils;

public class TargetCacheImpl_DataFile {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = TargetCacheImpl_DataFile.class.getSimpleName();

    //

    @Trivial
    protected static boolean exists(File file) {
        return ( (file != null) && UtilImpl_FileUtils.exists(file) );
    }

    //

    public TargetCacheImpl_DataFile(String name, File file) {
        this.name = name;
        this.file = file;
        this.hasFile = exists(this.file);
    }

    public String toString() {
        return
            super.toString() +
            "(" + name +
            ", " + ((file == null) ? null : file.getAbsolutePath()) +
            ", " + hasFile + ")";
    }

    //

    private final String name;
    
    @Trivial
    public String getName() {
        return name;
    }

    //

    private final File file;
    private boolean hasFile;

    @Trivial
    public File getFile() {
        return file;
    }

    @Trivial
    public boolean getHasFile() {
        return hasFile;
    }

    protected void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }
}
