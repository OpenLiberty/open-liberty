/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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
package com.ibm.ws.library.internal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */

public final class SharedLibraryConstants {

    public static final String TR_GROUP = "SharedLibrary";
    public static final String NLS_PROPS = "com.ibm.ws.classloading.internal.resources.ClassLoadingServiceMessages";

    public static final String SERVICE_PID = "com.ibm.ws.classloading.sharedlibrary";

    @Trivial
    public enum SharedLibraryAttribute {
        name, id, description, filesetRef, apiTypeVisibility, fileRef, folderRef, pathRef
    }
}