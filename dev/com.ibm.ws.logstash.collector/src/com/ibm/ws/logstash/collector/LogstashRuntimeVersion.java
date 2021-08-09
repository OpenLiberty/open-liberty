/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector;

import org.osgi.framework.Version;

/**
 *
 */
public interface LogstashRuntimeVersion {

    public static final String VERSION = "version";
    public static final Version VERSION_1_0 = new Version(1, 0, 0);
    public static final Version VERSION_1_1 = new Version(1, 1, 0);

    public Version getVersion();

}
