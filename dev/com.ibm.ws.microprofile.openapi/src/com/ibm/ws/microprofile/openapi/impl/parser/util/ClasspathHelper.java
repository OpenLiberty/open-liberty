/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class ClasspathHelper {

    public static String loadFileFromClasspath(String location) {

        InputStream inputStream = ClasspathHelper.class.getResourceAsStream(location);

        if (inputStream == null) {
            inputStream = ClasspathHelper.class.getClassLoader().getResourceAsStream(location);
        }

        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(location);
        }

        if (inputStream != null) {
            try {
                return IOUtils.toString(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + location + " from the classpath", e);
            }
        }

        throw new RuntimeException("Could not find " + location + " on the classpath");
    }
}
