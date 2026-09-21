/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.bvt.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceLoader {
    private static final String ENCODING = "utf8";

    private static String getContent(InputStream stream) throws Exception {
        if (stream == null)
            return null;
        InputStreamReader in = new InputStreamReader(stream, ENCODING);
        StringBuilder str = new StringBuilder();
        int i;
        do {
            i = in.read();
            if (i == -1)
                break;
            str.append((char) i);
        } while (true);
        return str.toString();
    }

    public static String getResource(String resource, ClassLoader loader) throws Exception, IOException {
        return getContent(loader.getResourceAsStream(resource));
    }

}
