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
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.io.IOException;

/**
 * Replaced by HashUtils
 */
@Deprecated
public class MD5Utils {

    @Deprecated
    public static String getMD5String(String str) {
        return HashUtils.getMD5String(str);
    }

    @Deprecated
    public static String getFileMD5String(File file) throws IOException {
        return HashUtils.getFileMD5String(file);
    }
}
