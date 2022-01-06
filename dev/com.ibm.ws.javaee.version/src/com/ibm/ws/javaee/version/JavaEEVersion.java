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
    // 10,  https://jakarta.ee/xml/ns/jakartaee // jakarta

    public static final String VERSION_1_2_STR  = "1.2";
    public static final String VERSION_1_3_STR  = "1.3";
    public static final String VERSION_1_4_STR  = "1.4";
    public static final String VERSION_5_0_STR  = "5.0";
    public static final String VERSION_6_0_STR  = "6.0";
    public static final String VERSION_7_STR    = "7";    
    public static final String VERSION_7_0_STR  = "7.0";
    public static final String VERSION_8_0_STR  = "8.0";
    public static final String VERSION_9_STR    = "9";
    public static final String VERSION_9_0_STR  = "9.0";
    public static final String VERSION_10_STR   = "10";
    public static final String VERSION_10_0_STR = "10.0";
    
    public static final int VERSION_1_2_INT  =  12;
    public static final int VERSION_1_3_INT  =  13;
    public static final int VERSION_1_4_INT  =  14;
    public static final int VERSION_5_0_INT  =  50;
    public static final int VERSION_6_0_INT  =  60;
    public static final int VERSION_7_0_INT  =  70;
    public static final int VERSION_8_0_INT  =  80;
    public static final int VERSION_9_0_INT  =  90;
    public static final int VERSION_10_0_INT = 100;

    // These must be in increasing order.

    public static final int [] VERSION_INTS = {
            VERSION_1_2_INT,
            VERSION_1_3_INT,
            VERSION_1_4_INT,
            VERSION_5_0_INT,
            VERSION_6_0_INT,
            VERSION_7_0_INT,
            VERSION_8_0_INT,
            VERSION_9_0_INT
              // VERSION_10_0_INT // Not yet
    };

    public static final Version VERSION_1_2 = new Version(1, 2, 0);
    public static final Version VERSION_1_3 = new Version(1, 3, 0);
    public static final Version VERSION_1_4 = new Version(1, 4, 0);
    public static final Version VERSION_5_0 = new Version(5, 0, 0);
    public static final Version VERSION_6_0 = new Version(6, 0, 0);
    public static final Version VERSION_7_0 = new Version(7, 0, 0);
    public static final Version VERSION_8_0 = new Version(8, 0, 0);
    public static final Version VERSION_9_0 = new Version(9, 0, 0);
    public static final Version VERSION_10_0 = new Version(10, 0, 0);
    
    // These must be in increasing order.

    public static final Version[] VERSIONS = {
            VERSION_1_2,
            VERSION_1_3,
            VERSION_1_4,
            VERSION_5_0,
            VERSION_6_0,
            VERSION_7_0,
            VERSION_8_0,
            VERSION_9_0
              // VERSION_10_0 // Not yet
    };

    public static final Version DEFAULT_VERSION = VERSION_6_0;
}
