/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.io.IOException;

/**
 * Replaced by HashUtils
 */
//FIPS 140-3: Algorithm assessment complete; no changes required.
// This is a deprecated API, the HashUtils replacement has a SHA-256 method available.
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
