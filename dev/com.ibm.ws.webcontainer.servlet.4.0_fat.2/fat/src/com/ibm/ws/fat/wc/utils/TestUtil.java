/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.utils;

import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;

/**
 * TestUtil is a final utility class responsible for implementing logging across multiple VMs. It also contains many
 * convenience methods for logging property object contents, stacktraces, and header lines.
 *
 * @author Kyle Grucci
 *
 */
public final class TestUtil {

    public static String toEncodedString(Properties args) {
        StringBuffer buf = new StringBuffer();
        Enumeration names = args.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = args.getProperty(name);
            buf.append(URLEncoder.encode(name)).append("=").append(URLEncoder.encode(value));
            if (names.hasMoreElements())
                buf.append("&");
        }
        return buf.toString();
    }

}
