/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.version;

import org.osgi.framework.Version;

// TODO: Why is is this an abstract class and not an interface?

public abstract class JavaEEVersion {
    public static final String VERSION = "version";

    // 1.2
    // 1.3
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 6,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee
    // 8,   http://xmlns.jcp.org/xml/ns/javaee  // last javax
    // 9,   https://jakarta.ee/xml/ns/jakartaee // jakarta

    public static final Version VERSION_1_2 = new Version(1, 2, 0);
    public static final Version VERSION_1_3 = new Version(1, 3, 0);
    public static final Version VERSION_1_4 = new Version(1, 4, 0);
    public static final Version VERSION_5_0 = new Version(5, 0, 0);
    public static final Version VERSION_6_0 = new Version(6, 0, 0);
    public static final Version VERSION_7_0 = new Version(7, 0, 0);
    public static final Version VERSION_8_0 = new Version(8, 0, 0); // javax
    public static final Version VERSION_9_0 = new Version(9, 0, 0); // jakarta

    // These must be in increasing order.

    public static final Version[] VERSIONS = {
            VERSION_1_2,
            VERSION_1_3,
            VERSION_1_4,
            VERSION_5_0,
            VERSION_6_0,
            VERSION_7_0,
            VERSION_8_0, // javax
            VERSION_9_0  // jakarta
    };

    public static final Version DEFAULT_VERSION = VERSION_6_0;
}
