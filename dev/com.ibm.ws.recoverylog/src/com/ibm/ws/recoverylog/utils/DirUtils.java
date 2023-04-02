/*******************************************************************************
 * Copyright (c) 2006,2021 IBM Corporation and others.
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
package com.ibm.ws.recoverylog.utils;

import java.io.File;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.TraceConstants;

public final class DirUtils {
    private static final TraceComponent tc = Tr.register(DirUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Replaces forward and backward slashes in the source string with 'File.separator'
     * characters.
     */
    public static String createDirectoryPath(String source) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createDirectoryPath", source);

        String directoryPath = null;

        if (source != null) {
            directoryPath = "";

            final StringTokenizer tokenizer = new StringTokenizer(source, "\\/");

            while (tokenizer.hasMoreTokens()) {
                final String pathChunk = tokenizer.nextToken();

                directoryPath += pathChunk;

                if (tokenizer.hasMoreTokens()) {
                    directoryPath += File.separator;
                }

            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createDirectoryPath", directoryPath);
        return directoryPath;
    }

}
