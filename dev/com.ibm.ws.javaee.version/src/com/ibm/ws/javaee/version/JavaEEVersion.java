/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
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

public abstract class JavaEEVersion {
    public static final String VERSION = "version";
    public static final Version DEFAULT_VERSION = new Version(6, 0, 0);

    public static final Version VERSION_6_0 = new Version(6, 0, 0);
    public static final Version VERSION_7_0 = new Version(7, 0, 0);
    public static final Version VERSION_8_0 = new Version(8, 0, 0);
}
